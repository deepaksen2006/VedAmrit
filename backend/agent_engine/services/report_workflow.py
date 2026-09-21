"""Versioned two-report workflow with explicit, content-bound doctor approval."""
from __future__ import annotations

from copy import deepcopy
from hashlib import sha256
import json
from typing import Any
from uuid import uuid4

from agent_engine.services.report_models import DoctorDecision, ReportMetadata, ReportType, WorkflowStatus, utc_now
from agent_engine.services.report_validation import validate_report_state


APPROVAL_FIELDS = ("qualified_reviewer", "doctor_name", "registration_number", "qualification", "clinic", "signature", "approved")
PATIENT_FIELDS = ("name", "patient_id", "id", "age", "gender", "sex", "height_cm", "weight_kg", "dietary_preference", "allergies", "medicine_allergies", "food_intolerances", "foods_to_avoid", "health_conditions", "medication_restrictions", "pregnancy_information", "significant_dietary_restrictions", "main_complaint", "symptoms", "symptom_duration", "symptom_frequency", "symptom_severity", "triggers", "previous_treatment", "goal", "activity_level", "sleep_information", "sleep_quality", "stress_level", "current_medicines", "doctor_restrictions", "allergy_reactions", "digestion", "appetite", "goal_details", "dietary_preference_details")


def _patient_id(state: dict[str, Any]) -> str:
    profile = state.get("patient_profile") or state.get("user_input") or {}
    return str(profile.get("patient_id") or profile.get("id") or state.get("patient_id"))


def _content_hash(content: dict[str, Any]) -> str:
    # Approval metadata proves the approval; it must not itself invalidate it.
    normalized = deepcopy(content)
    doctor = normalized.get("doctor_review_data")
    if isinstance(doctor, dict):
        for key in ("approval_date", "approved_content_hash", "approved_report_version"):
            doctor.pop(key, None)
    return sha256(json.dumps(normalized, sort_keys=True, ensure_ascii=True, allow_nan=False).encode("utf-8")).hexdigest()


def assessment_data(state: dict[str, Any]) -> dict[str, Any]:
    """The only content eligible for Report 1."""
    raw = state.get("patient_profile") or state.get("user_input") or {}
    profile = {key: deepcopy(raw[key]) for key in PATIENT_FIELDS if key in raw}
    return {
        "patient_profile": profile,
        "pre_consultation": {key: profile.get(key) for key in (
            "main_complaint", "symptoms", "symptom_duration", "symptom_frequency",
            "symptom_severity", "triggers", "previous_treatment", "goal",
        )},
        "prakriti": deepcopy(state.get("prakriti_result") or {}),
        "vikriti": deepcopy(state.get("vikriti_result") or {}),
        "agni": deepcopy(state.get("agni_result") or {}),
        "disease_screening": deepcopy(state.get("disease_screening_result") or {}),
        "assessment_scores": {
            "prakriti": deepcopy((state.get("prakriti_result") or {}).get("scores") or {}),
            "vikriti": deepcopy((state.get("vikriti_result") or {}).get("scores") or {}),
            "agni": deepcopy((state.get("agni_result") or {}).get("category_scores") or {}),
        },
        "limitations": [
            "AI-generated preliminary assessment based on supplied information.",
            "SYMPTOM-BASED DISEASE SCREENING is screening/research only, not a medical diagnosis.",
            "Doctor review is required before personalized recommendations are finalized.",
        ],
    }


def final_content(state: dict[str, Any]) -> dict[str, Any]:
    """Reviewed report data only; deliberately never nests a full Report 1 object."""
    doctor = deepcopy(state.get("doctor_review") or {})
    validation = validate_report_state(state)
    normalized_diet = validation.pop("diet")
    validation.pop("profile", None)
    original_diet = doctor.get("edited_diet_plan") if doctor.get("edited_diet_plan") is not None else state.get("agent2_output") or {}
    diet_context = {key: deepcopy(original_diet[key]) for key in ("nutrition_target", "excluded_foods", "priority_foods") if key in original_diet}
    # Edits are consumed above; do not embed a second complete diet/lifestyle copy.
    doctor.pop("edited_diet_plan", None)
    edited_lifestyle = doctor.pop("edited_lifestyle_recommendations", None)
    return {
        "patient_profile": deepcopy({key: value for key, value in
                                    (state.get("patient_profile") or state.get("user_input") or {}).items()
                                    if key in PATIENT_FIELDS}),
        "assessment": {
            "prakriti": doctor.get("confirmed_prakriti") or deepcopy(state.get("prakriti_result") or {}),
            "vikriti": doctor.get("confirmed_vikriti") or deepcopy(state.get("vikriti_result") or {}),
            "agni": doctor.get("confirmed_agni") or deepcopy(state.get("agni_result") or {}),
            "symptoms": doctor.get("confirmed_symptoms") or (state.get("patient_profile") or {}).get("symptoms"),
            "doctor_assessment": doctor.get("assessment"),
        },
        "agent2_diet_data": {**diet_context, "diet_plan": normalized_diet},
        "agent3_lifestyle_data": deepcopy(edited_lifestyle if edited_lifestyle is not None else state.get("lifestyle_plan") or {}),
        "doctor_review_data": doctor,
        "validation": validation,
    }


class TwoReportWorkflow:
    """In-memory orchestration object; persistence can serialize `state` unchanged."""

    def __init__(self, state: dict[str, Any]) -> None:
        self.state = state
        self.session_id = str(state.get("session_id") or uuid4())
        self.state["session_id"] = self.session_id
        self.state.setdefault("patient_id", str(uuid4()))

    def create_assessment_report(self) -> dict[str, Any]:
        previous = self.state.get("assessment_report")
        data = assessment_data(self.state)
        if previous and previous["assessment_data"] == data:
            return deepcopy(previous)
        metadata = ReportMetadata(_patient_id(self.state), self.session_id, ReportType.AI_ASSESSMENT,
                                  status=WorkflowStatus.AI_ASSESSMENT_GENERATED)
        if previous:
            metadata.report_id = previous["metadata"]["report_id"]
            metadata.report_version = previous["metadata"]["report_version"] + 1
            metadata.created_at = previous["metadata"]["created_at"]
        report = {"metadata": metadata.as_dict(), "assessment_data": data}
        self.state["assessment_report"] = report
        if self.state.get("workflow_status") != WorkflowStatus.REJECTED.value:
            self.state["workflow_status"] = WorkflowStatus.AI_ASSESSMENT_GENERATED.value
        return report

    def register_agent3_draft(self) -> None:
        self.begin_doctor_review()

    def _ensure_open(self) -> None:
        if (self.state.get("workflow_status") == WorkflowStatus.REJECTED.value
                or (self.state.get("doctor_review") or {}).get("decision") == DoctorDecision.REJECT):
            raise PermissionError("This workflow run was rejected and is closed.")

    def begin_doctor_review(self) -> None:
        self._ensure_open()
        self.state["review_validation"] = validate_report_state(self.state)
        self.state["workflow_status"] = WorkflowStatus.PENDING_DOCTOR_REVIEW.value

    def request_changes(self, doctor_review: dict[str, Any] | None = None) -> None:
        self._ensure_open()
        review = deepcopy(doctor_review if doctor_review is not None else self.state.get("doctor_review") or {})
        review.update(decision=DoctorDecision.REQUEST_CHANGES.value, approved=False, review_datetime=utc_now())
        self.state["doctor_review"] = review
        self.state["workflow_status"] = WorkflowStatus.CHANGES_REQUIRED.value

    def reject(self, doctor_review: dict[str, Any] | None = None) -> None:
        self._ensure_open()
        review = deepcopy(doctor_review if doctor_review is not None else self.state.get("doctor_review") or {})
        review.update(decision=DoctorDecision.REJECT.value, approved=False, review_datetime=utc_now())
        self.state["doctor_review"] = review
        self.state["workflow_status"] = WorkflowStatus.REJECTED.value

    def record_decision(self, doctor_review: dict[str, Any]) -> None:
        self._ensure_open()
        decision = doctor_review.get("decision")
        if decision not in {item.value for item in DoctorDecision}:
            raise ValueError("Choose APPROVE, REQUEST_CHANGES, or REJECT explicitly.")
        if decision == DoctorDecision.APPROVE:
            self.approve(doctor_review)
        elif decision == DoctorDecision.REQUEST_CHANGES:
            self.request_changes(doctor_review)
        else:
            self.reject(doctor_review)

    def approve(self, doctor_review: dict[str, Any]) -> None:
        self._ensure_open()
        if doctor_review.get("decision") != DoctorDecision.APPROVE:
            raise ValueError("Explicit decision == APPROVE is required.")
        missing = [key for key in APPROVAL_FIELDS if not doctor_review.get(key) or (isinstance(doctor_review.get(key), str) and not doctor_review[key].strip())]
        missing += [key for key in ("qualified_reviewer", "approved") if doctor_review.get(key) is not True]
        if missing:
            raise ValueError("Explicit doctor approval is incomplete: " + ", ".join(missing))
        if not self.state.get("assessment_report"):
            raise ValueError("Generate Report 1 before doctor approval.")
        for medicine in doctor_review.get("medicines") or []:
            if (not isinstance(medicine, dict) or medicine.get("source") != "DOCTOR_ENTERED"
                    or not str(medicine.get("name") or "").strip()):
                raise ValueError("Medicines must be named, explicitly doctor-entered review entries.")
        doctor_review = deepcopy(doctor_review)
        doctor_review["review_datetime"] = utc_now()
        approved_content = final_content({**self.state, "doctor_review": doctor_review})
        failed_checks = approved_content.get("validation", {}).get("failed_checks") or []
        if failed_checks:
            raise ValueError("Doctor approval is blocked by validation failures: " + "; ".join(failed_checks))
        self.state["review_validation"] = deepcopy(approved_content["validation"])
        version = int(self.state.get("final_report_version") or 1)
        previous = self.state.get("final_report")
        if previous:
            version = previous["metadata"]["report_version"] + 1
        elif self.state.get("approved_snapshot"):
            version += 1
        self.state["final_report_version"] = version
        self.state.setdefault("final_report_id", str(uuid4()))
        doctor_review = deepcopy(doctor_review)
        doctor_review["approval_date"] = utc_now()
        doctor_review["approved_content_hash"] = _content_hash(approved_content)
        doctor_review["approved_report_version"] = int(self.state.get("final_report_version") or 1)
        self.state["doctor_review"] = doctor_review
        self.state["approved_snapshot"] = deepcopy(final_content(self.state))
        self.state["workflow_status"] = WorkflowStatus.DOCTOR_APPROVED.value

    def approval_is_current(self) -> bool:
        doctor = self.state.get("doctor_review") or {}
        if self.state.get("workflow_status") not in {WorkflowStatus.DOCTOR_APPROVED.value, WorkflowStatus.FINALIZED.value, WorkflowStatus.SENT_TO_PATIENT.value}:
            return False
        if doctor.get("decision") != DoctorDecision.APPROVE or doctor.get("approved") is not True or doctor.get("qualified_reviewer") is not True:
            return False
        if not self.state.get("approved_snapshot") or _content_hash(self.state["approved_snapshot"]) != doctor.get("approved_content_hash"):
            return False
        stored = self.state.get("final_report")
        if stored and stored["metadata"]["report_version"] == doctor.get("approved_report_version"):
            if _content_hash(stored["final_approved_data"]) != doctor.get("approved_content_hash"):
                return False
        return (doctor.get("approved_report_version") == int(self.state.get("final_report_version") or 1)
                and bool(doctor.get("approved_content_hash"))
                and doctor.get("approved_content_hash") == _content_hash(final_content(self.state)))

    def invalidate_if_changed(self) -> bool:
        if not self.approval_is_current() and self.state.get("doctor_review", {}).get("approved"):
            self.state["doctor_review"]["approved"] = False
            self.state["workflow_status"] = WorkflowStatus.CHANGES_REQUIRED.value
            return True
        return False

    def create_final_report(self) -> dict[str, Any]:
        if not self.approval_is_current():
            self.invalidate_if_changed()
            raise PermissionError("DOCTOR VERIFICATION REQUIRED")
        previous = self.state.get("final_report")
        if previous and previous["metadata"]["report_version"] == self.state.get("final_report_version"):
            return deepcopy(previous)
        version = int(self.state.get("final_report_version") or 1)
        metadata = ReportMetadata(_patient_id(self.state), self.session_id, ReportType.FINAL_PERSONALIZED,
                                  report_id=self.state["final_report_id"],
                                  report_version=version, status=WorkflowStatus.FINALIZED,
                                  approval_timestamp=(self.state.get("doctor_review") or {}).get("approval_date"))
        if previous:
            metadata.report_id = previous["metadata"]["report_id"]
            metadata.created_at = previous["metadata"]["created_at"]
        report = {"metadata": metadata.as_dict(), "final_approved_data": deepcopy(self.state["approved_snapshot"])}
        self.state["final_report"] = report
        self.state["workflow_status"] = WorkflowStatus.FINALIZED.value
        return deepcopy(report)
