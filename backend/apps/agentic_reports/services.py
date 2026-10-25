from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from django.db import transaction
from django.contrib.auth import get_user_model
from django.utils import timezone

from agent_engine.contracts import parse_agent_engine_request
from agent_engine.integration_service import generate_personalized_plan

from apps.agentic_reports.models import (
    AgentRun,
    AgentRunStatus,
    AssessmentResult,
    DoctorReview,
    DoctorReviewStatus,
    DoctorAssignment,
    FinalReport,
    PatientProfile,
    ValidationStatus,
    WellnessPlan,
)
from apps.doctors.models import DoctorProfile
from apps.agentic_reports.assessment_sync import select_assessment, assessment_version, AssessmentConflict
from apps.agentic_reports.generation import profile_payload, profile_version, claim_generation, GenerationPending


class AgentRunFailed(RuntimeError):
    def __init__(self, agent_run: AgentRun, message: str):
        super().__init__(message)
        self.agent_run = agent_run


@dataclass(frozen=True)
class WellnessPlanCreationResult:
    assessment: AssessmentResult
    agent_run: AgentRun
    wellness_plan: WellnessPlan


def _assessment_payload(payload: dict[str, Any]) -> dict[str, Any]:
    return (
        payload.get("assessment")
        or payload.get("assessments")
        or payload.get("assessment_profile")
        or {}
    )


def _validation_status(validation: dict[str, Any]) -> str:
    raw = str(
        validation.get("status")
        or validation.get("overall_status")
        or ValidationStatus.REVIEW_REQUIRED
    ).upper()
    if raw in {ValidationStatus.PASS, ValidationStatus.FAIL, ValidationStatus.REVIEW_REQUIRED}:
        return raw
    if raw in {"ERROR", "FAILED"}:
        return ValidationStatus.FAIL
    return ValidationStatus.REVIEW_REQUIRED


def _first_error(response: dict[str, Any]) -> dict[str, Any]:
    errors = ((response.get("workflow") or {}).get("errors") or [])
    return errors[0] if errors else {
        "code": "agent_execution_failed",
        "message": "Agent execution failed.",
    }


def _persisted_engine_payload(
    profile: PatientProfile,
    assessment: AssessmentResult,
    payload: dict[str, Any],
) -> dict[str, Any]:
    patient_profile = profile_payload(profile)
    return {
        "patient_profile": patient_profile,
        "assessment": {
            "prakriti": assessment.prakriti_result,
            "vikriti": assessment.vikriti_result,
            "agni": assessment.agni_result,
        },
        "plan_days": payload.get("plan_days", 7),
        "runtime": {**(payload.get("runtime") or {}), "assessment_source": "persisted_db",
                    "assessment_id": assessment.pk, "assessment_version": assessment_version(assessment),
                    "profile_version": profile_version(profile)},
    }


def create_wellness_plan(user, payload: dict[str, Any]) -> WellnessPlanCreationResult:
    profile = PatientProfile.objects.filter(user=user).first()
    assessment = select_assessment(user, payload)
    if profile is not None and payload.get("profile_version") and payload["profile_version"] != profile_version(profile):
        raise AssessmentConflict("Your health profile changed on the server. Reload/save your profile before generating a plan.")
    if payload.get("assessment_id") and profile is None:
        from rest_framework.exceptions import ValidationError
        raise ValidationError({"patient_profile": "Save the authenticated patient's profile before generating a plan."})
    if profile is not None and assessment is not None:
        engine_payload = _persisted_engine_payload(profile, assessment, payload)
    else:
        # Keep direct callers compatible during migration.
        engine_payload = payload
        request = parse_agent_engine_request(engine_payload)
        assessment_block = _assessment_payload(payload)
        assessment = AssessmentResult.objects.create(
            user=user,
            prakriti_result=request.prakriti_result,
            vikriti_result=request.vikriti_result,
            agni_result=request.agni_result,
            canonical_payload=assessment_block,
        )
    engine_payload.setdefault("runtime", {}).update(
        assessment_id=assessment.pk, assessment_version=assessment_version(assessment)
    )
    request = parse_agent_engine_request(engine_payload)
    assignment = (
        DoctorAssignment.objects.filter(patient_profile=profile, active=True).first()
        if profile is not None
        else None
    )
    agent_run, created = claim_generation(user, assessment, engine_payload, payload.get("generation_id"))
    if not created:
        if agent_run.status == AgentRunStatus.FAILED:
            raise AgentRunFailed(agent_run, "The previous generation attempt failed.")
        existing_plan = getattr(agent_run, "wellness_plan", None)
        if existing_plan:
            return WellnessPlanCreationResult(assessment, agent_run, existing_plan)
        raise GenerationPending()

    try:
        response = generate_personalized_plan(engine_payload)
    except Exception as exc:
        agent_run.mark_failed(
            code="agent_execution_exception",
            message=str(exc),
            details={"exception_class": exc.__class__.__name__},
        )
        raise AgentRunFailed(agent_run, str(exc)) from exc

    workflow = response.get("workflow") or {}
    rag_status = workflow.get("rag") or {}
    if workflow.get("status") == "error" or workflow.get("errors"):
        error = _first_error(response)
        agent_run.mark_failed(
            code=str(error.get("code", "agent_execution_failed")),
            message=str(error.get("message", "Agent execution failed.")),
            details=error.get("details") or {},
            response_payload=response,
            rag_status=rag_status,
        )
        raise AgentRunFailed(agent_run, agent_run.error_message)

    response["assessment"] = {**(response.get("assessment") or {}),
                              "assessment_id": assessment.pk, "assessment_version": assessment_version(assessment)}
    try:
        with transaction.atomic():
            get_user_model().objects.select_for_update().get(pk=user.pk)
            latest = AssessmentResult.objects.filter(user=user).order_by("-created_at", "-pk").first()
            if latest is None or latest.pk != assessment.pk or assessment_version(latest) != engine_payload["runtime"]["assessment_version"]:
                raise AssessmentConflict("Assessment changed during generation. Sync and generate again.")
            if profile is not None and profile_version(PatientProfile.objects.get(pk=profile.pk)) != engine_payload["runtime"].get("profile_version"):
                raise AssessmentConflict("Health profile changed during generation. Reload/save the profile and generate again.")
            agent_run.mark_completed(response_payload=response, rag_status=rag_status)
            wellness_plan = WellnessPlan.objects.create(
                user=user,
                assessment=assessment,
                agent_run=agent_run,
                assignment=assignment,
                version=1,
                diet_plan=response.get("diet_plan") or {},
                lifestyle_plan=response.get("lifestyle_plan") or {},
                assessment_summary=response.get("assessment") or {},
                validation=response.get("validation") or {},
                validation_status=_validation_status(response.get("validation") or {}),
                workflow_status=str(workflow.get("workflow_status") or workflow.get("status") or ""),
                doctor_status=DoctorReviewStatus.PENDING,
            )
    except AssessmentConflict:
        agent_run.mark_failed(code="assessment_changed_during_generation", message="Assessment changed during generation.")
        raise
    except Exception as exc:
        agent_run.mark_failed(code="plan_persistence_failed", message="Generated plan could not be persisted.",
                              details={"exception_class": exc.__class__.__name__})
        raise AgentRunFailed(agent_run, "Generated plan could not be persisted.") from exc
    return WellnessPlanCreationResult(
        assessment=assessment,
        agent_run=agent_run,
        wellness_plan=wellness_plan,
    )


@transaction.atomic
def create_plan_revision(
    *,
    parent_plan: WellnessPlan,
    agent_run: AgentRun | None = None,
    diet_plan: dict[str, Any] | None = None,
    lifestyle_plan: dict[str, Any] | None = None,
    assessment_summary: dict[str, Any] | None = None,
    validation: dict[str, Any] | None = None,
    workflow_status: str | None = None,
) -> WellnessPlan:
    latest = (
        WellnessPlan.objects.select_for_update()
        .filter(lineage_id=parent_plan.lineage_id)
        .order_by("-version")
        .first()
    )
    next_version = (latest.version if latest else parent_plan.version) + 1
    validation_data = validation if validation is not None else parent_plan.validation
    return WellnessPlan.objects.create(
        user=parent_plan.user,
        assessment=parent_plan.assessment,
        agent_run=agent_run,
        lineage_id=parent_plan.lineage_id,
        parent_plan=latest or parent_plan,
        version=next_version,
        diet_plan=diet_plan if diet_plan is not None else parent_plan.diet_plan,
        lifestyle_plan=lifestyle_plan if lifestyle_plan is not None else parent_plan.lifestyle_plan,
        assessment_summary=assessment_summary if assessment_summary is not None else parent_plan.assessment_summary,
        validation=validation_data,
        validation_status=_validation_status(validation_data),
        workflow_status=workflow_status if workflow_status is not None else parent_plan.workflow_status,
        doctor_status=DoctorReviewStatus.PENDING,
    )


@transaction.atomic
def record_doctor_review(
    *,
    wellness_plan: WellnessPlan,
    doctor: DoctorProfile,
    decision: str,
    feedback: str = "",
    comments: str = "",
) -> DoctorReview:
    if decision not in DoctorReviewStatus.values:
        raise ValueError(f"Unsupported doctor review decision: {decision}")
    reviewed_at = timezone.now() if decision != DoctorReviewStatus.PENDING else None
    review = DoctorReview.objects.create(
        wellness_plan=wellness_plan,
        doctor=doctor,
        decision=decision,
        feedback=feedback,
        comments=comments,
        reviewed_at=reviewed_at,
    )
    wellness_plan.doctor_status = decision
    wellness_plan.save(update_fields=["doctor_status", "updated_at"])
    return review


@transaction.atomic
def finalize_report(
    *,
    wellness_plan: WellnessPlan,
    report_path: str,
    metadata: dict[str, Any] | None = None,
) -> FinalReport:
    if wellness_plan.doctor_status != DoctorReviewStatus.APPROVED:
        raise ValueError("Final report cannot be finalized before doctor approval.")
    return FinalReport.objects.create(
        wellness_plan=wellness_plan,
        user=wellness_plan.user,
        report_path=report_path,
        report_metadata=metadata or {},
    )
