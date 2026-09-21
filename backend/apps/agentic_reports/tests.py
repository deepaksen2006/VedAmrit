from __future__ import annotations

from copy import deepcopy
from decimal import Decimal
from unittest.mock import patch

from django.contrib.auth import get_user_model
from django.db import IntegrityError
from django.test import TestCase
from rest_framework.test import APITestCase

from apps.agentic_reports.models import (
    AgentRun,
    AgentRunStatus,
    AssessmentResult,
    DoctorReviewStatus,
    FinalReport,
    ValidationStatus,
    WellnessPlan,
)
from apps.agentic_reports.services import (
    AgentRunFailed,
    create_plan_revision,
    create_wellness_plan,
    finalize_report,
    record_doctor_review,
)
from apps.doctors.models import DoctorProfile


def sample_payload() -> dict:
    return {
        "patient_profile": {
            "fullName": "Anaya Rao",
            "age": "34",
            "gender": "Female",
            "height": "164",
            "weight": "58",
            "dietary_preference": "Vegetarian",
            "goal": "general wellness",
            "activity_level": "moderate",
            "allergies": [],
            "food_intolerances": [],
            "foods_to_avoid": [],
            "health_conditions": [],
            "medicine_allergies": [],
            "medication_restrictions": [],
            "doctor_restrictions": [],
            "pregnancy_information": "not_applicable",
        },
        "assessment": {
            "prakriti": {
                "scores": {"Vata": 18, "Pitta": 10, "Kapha": 4},
                "percentages": {"Vata": 56.2, "Pitta": 31.3, "Kapha": 12.5},
                "primaryDosha": "Vata",
                "secondaryDosha": "Pitta",
                "prakritiType": "Vata-Pitta",
                "borderline": False,
                "questionsAnswered": 21,
            },
            "vikriti": {
                "scores": {"Vata": 3, "Pitta": 19, "Kapha": 6},
                "severities": {"Vata": "Low", "Pitta": "Moderate", "Kapha": "Low"},
                "percentages": {"Vata": 10.7, "Pitta": 67.9, "Kapha": 21.4},
                "dominantDosha": "Pitta",
                "completedAtMillis": 1720000000000,
                "prakritiType": "Vata-Pitta",
            },
            "agni": {
                "scores": {"Mandagni": 2, "Vishamagni": 1, "Samagni": 8, "Tikshnagni": 0},
                "dominantAgni": "Samagni",
                "mixedAgniTypes": [],
                "isMixed": False,
                "completedAtMillis": 1720000001000,
            },
        },
        "plan_days": 1,
    }


def successful_agent_response(validation_status: str = "PASS") -> dict:
    return {
        "assessment": {
            "source": "android_canonical_results",
            "prakriti": sample_payload()["assessment"]["prakriti"],
            "vikriti": sample_payload()["assessment"]["vikriti"],
            "agni": sample_payload()["assessment"]["agni"],
        },
        "diet_plan": {"duration_days": 1, "days": [{"day": 1}]},
        "lifestyle_plan": {"recommendations": [{"category": "daily_routine"}]},
        "validation": {"status": validation_status, "warnings": []},
        "workflow": {
            "status": "review_required",
            "workflow_status": "PENDING_DOCTOR_REVIEW",
            "errors": [],
            "rag": {"status": "available", "available": True, "embedding_count": 1532},
        },
    }


def failed_agent_response() -> dict:
    return {
        "assessment": None,
        "diet_plan": None,
        "lifestyle_plan": None,
        "validation": {"status": "ERROR"},
        "workflow": {
            "status": "error",
            "errors": [{"code": "agent_execution_failure", "message": "boom"}],
            "rag": {"status": "available", "available": True},
        },
    }


class AgenticReportPersistenceTests(TestCase):
    def setUp(self):
        User = get_user_model()
        self.user = User.objects.create_user(username="patient", password="pw")
        self.other_user = User.objects.create_user(username="other", password="pw")
        doctor_user = User.objects.create_user(username="doctor", password="pw")
        self.doctor = DoctorProfile.objects.create(
            user=doctor_user,
            full_name="Dr. Mira",
            phone="9999999999",
            email="doctor@example.com",
            bams_registration_number="BAMS-001",
            specialization="Ayurveda",
            experience=8,
            clinic_name="Ved Clinic",
            consultation_fee=Decimal("500.00"),
            is_verified=True,
        )

    def create_plan(self, validation_status: str = "PASS"):
        with patch(
            "apps.agentic_reports.services.generate_personalized_plan",
            return_value=successful_agent_response(validation_status),
        ):
            return create_wellness_plan(self.user, sample_payload())

    def test_assessment_result_persistence_preserves_canonical_values(self):
        result = self.create_plan()
        assessment = AssessmentResult.objects.get(pk=result.assessment.pk)

        self.assertEqual(assessment.user, self.user)
        self.assertEqual(assessment.prakriti_result, sample_payload()["assessment"]["prakriti"])
        self.assertEqual(assessment.vikriti_result["dominantDosha"], "Pitta")
        self.assertEqual(assessment.agni_result["dominantAgni"], "Samagni")

    def test_successful_agent_run_and_wellness_plan_v1_creation(self):
        result = self.create_plan()
        result.agent_run.refresh_from_db()

        self.assertEqual(result.agent_run.status, AgentRunStatus.COMPLETED)
        self.assertIsNotNone(result.agent_run.completed_at)
        self.assertEqual(result.agent_run.rag_status["status"], "available")
        self.assertEqual(result.wellness_plan.version, 1)
        self.assertEqual(result.wellness_plan.doctor_status, DoctorReviewStatus.PENDING)
        self.assertEqual(result.wellness_plan.validation_status, ValidationStatus.PASS)

    def test_failed_agent_run_does_not_create_fake_plan(self):
        with patch(
            "apps.agentic_reports.services.generate_personalized_plan",
            return_value=failed_agent_response(),
        ):
            with self.assertRaises(AgentRunFailed) as failure:
                create_wellness_plan(self.user, sample_payload())

        run = failure.exception.agent_run
        run.refresh_from_db()
        self.assertEqual(run.status, AgentRunStatus.FAILED)
        self.assertEqual(run.error_code, "agent_execution_failure")
        self.assertEqual(WellnessPlan.objects.count(), 0)

    def test_version_uniqueness_within_lineage(self):
        result = self.create_plan()
        with self.assertRaises(IntegrityError):
            WellnessPlan.objects.create(
                user=self.user,
                assessment=result.assessment,
                lineage_id=result.wellness_plan.lineage_id,
                version=1,
            )

    def test_revision_creation_preserves_old_version(self):
        result = self.create_plan()
        v1 = result.wellness_plan
        v2 = create_plan_revision(
            parent_plan=v1,
            diet_plan={"duration_days": 1, "days": [{"day": 1, "revised": True}]},
            validation={"status": "REVIEW_REQUIRED"},
        )

        v1.refresh_from_db()
        self.assertEqual(v1.version, 1)
        self.assertNotIn("revised", str(v1.diet_plan))
        self.assertEqual(v2.version, 2)
        self.assertEqual(v2.parent_plan, v1)
        self.assertEqual(v2.lineage_id, v1.lineage_id)
        self.assertEqual(v2.doctor_status, DoctorReviewStatus.PENDING)

    def test_validation_pass_does_not_approve_doctor_status(self):
        result = self.create_plan(validation_status="PASS")

        self.assertEqual(result.wellness_plan.validation_status, ValidationStatus.PASS)
        self.assertEqual(result.wellness_plan.doctor_status, DoctorReviewStatus.PENDING)

    def test_doctor_approval_changes_requested_and_rejection(self):
        approved = self.create_plan().wellness_plan
        approval = record_doctor_review(
            wellness_plan=approved,
            doctor=self.doctor,
            decision=DoctorReviewStatus.APPROVED,
            feedback="Approved",
        )
        approved.refresh_from_db()
        self.assertEqual(approval.decision, DoctorReviewStatus.APPROVED)
        self.assertEqual(approved.doctor_status, DoctorReviewStatus.APPROVED)

        changes_plan = self.create_plan().wellness_plan
        changes = record_doctor_review(
            wellness_plan=changes_plan,
            doctor=self.doctor,
            decision=DoctorReviewStatus.CHANGES_REQUESTED,
            feedback="Revise dinner",
        )
        changes_plan.refresh_from_db()
        self.assertEqual(changes.decision, DoctorReviewStatus.CHANGES_REQUESTED)
        self.assertEqual(changes_plan.doctor_status, DoctorReviewStatus.CHANGES_REQUESTED)

        rejected_plan = self.create_plan().wellness_plan
        rejection = record_doctor_review(
            wellness_plan=rejected_plan,
            doctor=self.doctor,
            decision=DoctorReviewStatus.REJECTED,
            feedback="Unsafe",
        )
        rejected_plan.refresh_from_db()
        self.assertEqual(rejection.decision, DoctorReviewStatus.REJECTED)
        self.assertEqual(rejected_plan.doctor_status, DoctorReviewStatus.REJECTED)

    def test_final_report_requires_doctor_approval(self):
        plan = self.create_plan().wellness_plan

        with self.assertRaises(ValueError):
            finalize_report(wellness_plan=plan, report_path="reports/final.pdf")

        self.assertEqual(FinalReport.objects.count(), 0)

    def test_final_report_can_be_finalized_after_approval(self):
        plan = self.create_plan().wellness_plan
        record_doctor_review(
            wellness_plan=plan,
            doctor=self.doctor,
            decision=DoctorReviewStatus.APPROVED,
        )
        plan.refresh_from_db()

        report = finalize_report(
            wellness_plan=plan,
            report_path="reports/final.pdf",
            metadata={"version": plan.version},
        )

        self.assertEqual(report.user, self.user)
        self.assertEqual(report.wellness_plan, plan)
        self.assertEqual(report.report_metadata["version"], 1)

    def test_user_ownership_relationships(self):
        result = self.create_plan()

        self.assertEqual(result.assessment.user, self.user)
        self.assertEqual(result.agent_run.user, self.user)
        self.assertEqual(result.wellness_plan.user, self.user)
        self.assertFalse(
            WellnessPlan.objects.filter(user=self.other_user, pk=result.wellness_plan.pk).exists()
        )


class AgenticReportsApiTests(APITestCase):
    def setUp(self):
        User = get_user_model()
        self.patient = User.objects.create_user(username="api-patient", password="pw")
        self.patient_b = User.objects.create_user(username="api-patient-b", password="pw")
        self.doctor_user = User.objects.create_user(username="api-doctor", password="pw")
        self.unverified_doctor_user = User.objects.create_user(username="api-unverified", password="pw")
        self.doctor = DoctorProfile.objects.create(
            user=self.doctor_user,
            full_name="Dr. API",
            phone="8888888888",
            email="api-doctor@example.com",
            bams_registration_number="BAMS-API",
            specialization="Ayurveda",
            experience=9,
            clinic_name="API Clinic",
            consultation_fee=Decimal("700.00"),
            is_verified=True,
        )
        self.unverified_doctor = DoctorProfile.objects.create(
            user=self.unverified_doctor_user,
            full_name="Dr. Waiting",
            phone="7777777777",
            email="waiting@example.com",
            bams_registration_number="BAMS-WAIT",
            specialization="Ayurveda",
            experience=2,
            clinic_name="Waiting Clinic",
            consultation_fee=Decimal("300.00"),
            is_verified=False,
        )

    def authenticate_patient(self):
        self.client.force_authenticate(user=self.patient)

    def authenticate_doctor(self):
        self.client.force_authenticate(user=self.doctor_user)

    def create_api_plan(self, user=None, validation_status="PASS"):
        self.client.force_authenticate(user=user or self.patient)
        with patch(
            "apps.agentic_reports.services.generate_personalized_plan",
            return_value=successful_agent_response(validation_status),
        ):
            response = self.client.post("/api/agentic/plans/", sample_payload(), format="json")
        self.assertEqual(response.status_code, 201, response.data)
        return response

    def test_authenticated_plan_generation_with_valid_canonical_assessment(self):
        response = self.create_api_plan()

        self.assertIn("id", response.data)
        self.assertEqual(response.data["assessment_summary"]["source"], "android_canonical_results")
        self.assertEqual(response.data["diet_plan"]["duration_days"], 1)
        self.assertEqual(response.data["validation_status"], "PASS")
        self.assertEqual(response.data["doctor_status"], "PENDING")
        self.assertEqual(response.data["rag_status"]["status"], "available")

    def test_unauthenticated_generation_is_rejected(self):
        response = self.client.post("/api/agentic/plans/", sample_payload(), format="json")

        self.assertEqual(response.status_code, 401)

    def test_malformed_assessment_returns_400(self):
        self.authenticate_patient()
        payload = sample_payload()
        payload["assessment"]["prakriti"] = "bad"

        response = self.client.post("/api/agentic/plans/", payload, format="json")

        self.assertEqual(response.status_code, 400)

    def test_missing_required_profile_information_returns_400(self):
        self.authenticate_patient()
        payload = sample_payload()
        del payload["patient_profile"]["dietary_preference"]

        response = self.client.post("/api/agentic/plans/", payload, format="json")

        self.assertEqual(response.status_code, 400)

    def test_plan_list_detail_and_status(self):
        created = self.create_api_plan()
        plan_id = created.data["id"]

        list_response = self.client.get("/api/agentic/plans/")
        detail_response = self.client.get(f"/api/agentic/plans/{plan_id}/")
        status_response = self.client.get(f"/api/agentic/plans/{plan_id}/status/")

        self.assertEqual(list_response.status_code, 200)
        self.assertEqual(len(list_response.data), 1)
        self.assertEqual(detail_response.status_code, 200)
        self.assertEqual(detail_response.data["id"], plan_id)
        self.assertEqual(status_response.status_code, 200)
        self.assertEqual(status_response.data["doctor_status"], "PENDING")

    def test_patient_a_cannot_access_patient_b_plan(self):
        created = self.create_api_plan(user=self.patient_b)
        self.client.force_authenticate(user=self.patient)

        response = self.client.get(f"/api/agentic/plans/{created.data['id']}/")

        self.assertEqual(response.status_code, 404)

    def test_patient_cannot_access_doctor_queue_or_actions(self):
        created = self.create_api_plan()
        plan_id = created.data["id"]

        queue = self.client.get("/api/agentic/doctor/review-queue/")
        approve = self.client.post(f"/api/agentic/doctor/plans/{plan_id}/approve/", {}, format="json")
        changes = self.client.post(f"/api/agentic/doctor/plans/{plan_id}/request-changes/", {"feedback": "revise"}, format="json")
        reject = self.client.post(f"/api/agentic/doctor/plans/{plan_id}/reject/", {}, format="json")

        self.assertEqual(queue.status_code, 403)
        self.assertEqual(approve.status_code, 403)
        self.assertEqual(changes.status_code, 403)
        self.assertEqual(reject.status_code, 403)

    def test_doctor_queue_detail_approve_request_changes_and_reject(self):
        first = self.create_api_plan()
        second = self.create_api_plan()
        third = self.create_api_plan()
        self.authenticate_doctor()

        queue = self.client.get("/api/agentic/doctor/review-queue/")
        detail = self.client.get(f"/api/agentic/doctor/plans/{first.data['id']}/")
        approve = self.client.post(
            f"/api/agentic/doctor/plans/{first.data['id']}/approve/",
            {"feedback": "approved"},
            format="json",
        )
        changes = self.client.post(
            f"/api/agentic/doctor/plans/{second.data['id']}/request-changes/",
            {"feedback": "revise dinner"},
            format="json",
        )
        reject = self.client.post(
            f"/api/agentic/doctor/plans/{third.data['id']}/reject/",
            {"feedback": "unsafe"},
            format="json",
        )

        self.assertEqual(queue.status_code, 200)
        self.assertGreaterEqual(len(queue.data), 3)
        self.assertEqual(detail.status_code, 200)
        self.assertEqual(approve.status_code, 200)
        self.assertEqual(approve.data["plan"]["doctor_status"], "APPROVED")
        self.assertEqual(changes.status_code, 201)
        self.assertEqual(changes.data["revision"]["version"], 2)
        self.assertEqual(changes.data["revision"]["doctor_status"], "PENDING")
        self.assertEqual(reject.status_code, 200)
        self.assertEqual(reject.data["plan"]["doctor_status"], "REJECTED")

    def test_unverified_doctor_cannot_use_doctor_endpoints(self):
        created = self.create_api_plan()
        self.client.force_authenticate(user=self.unverified_doctor_user)

        response = self.client.post(f"/api/agentic/doctor/plans/{created.data['id']}/approve/", {}, format="json")

        self.assertEqual(response.status_code, 403)

    def test_validation_pass_does_not_equal_doctor_approval(self):
        created = self.create_api_plan(validation_status="PASS")

        self.assertEqual(created.data["validation_status"], "PASS")
        self.assertEqual(created.data["doctor_status"], "PENDING")

    def test_final_report_blocked_before_approval_and_available_after_approval(self):
        created = self.create_api_plan()
        plan_id = created.data["id"]

        blocked = self.client.get(f"/api/agentic/plans/{plan_id}/report/")
        self.assertEqual(blocked.status_code, 409)

        self.authenticate_doctor()
        approve = self.client.post(f"/api/agentic/doctor/plans/{plan_id}/approve/", {}, format="json")
        self.assertEqual(approve.status_code, 200)

        self.client.force_authenticate(user=self.patient)
        available = self.client.get(f"/api/agentic/plans/{plan_id}/report/")
        self.assertEqual(available.status_code, 200)
        self.assertIn("report_reference", available.data)
        self.assertNotIn("agentic://", available.data["report_reference"])

    def test_agent_failure_returns_controlled_response(self):
        self.authenticate_patient()
        with patch(
            "apps.agentic_reports.services.generate_personalized_plan",
            return_value=failed_agent_response(),
        ):
            response = self.client.post("/api/agentic/plans/", sample_payload(), format="json")

        self.assertEqual(response.status_code, 502)
        self.assertEqual(response.data["error_code"], "agent_execution_failure")
        run = AgentRun.objects.get(pk=response.data["agent_run_id"])
        self.assertEqual(run.status, AgentRunStatus.FAILED)
        self.assertEqual(WellnessPlan.objects.count(), 0)
