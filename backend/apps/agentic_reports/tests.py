from __future__ import annotations

from copy import deepcopy
from decimal import Decimal
from unittest.mock import patch

from django.contrib.auth import get_user_model
from django.core.exceptions import ValidationError
from django.db import IntegrityError
from django.test import TestCase
from rest_framework.test import APITestCase
from rest_framework_simplejwt.tokens import RefreshToken

from apps.agentic_reports.models import (
    AgentRun,
    AgentRunStatus,
    AssessmentResult,
    DoctorAssignment,
    DoctorReviewStatus,
    FinalReport,
    PatientProfile,
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
            "rag": {
                "status": "available",
                "available": True,
                "embedding_count": 1532,
                "chunk_count": 1532,
                "vectorstore_path": "/internal/vectorstore/path",
            },
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

    def test_persisted_profile_and_assessment_override_generation_payload(self):
        profile = PatientProfile.objects.create(
            user=self.user,
            name="Persisted Patient",
            age=42,
            gender="female",
            height_cm=160,
            weight_kg=62,
            dietary_preference="vegetarian",
            goal="sleep support",
            activity_level="low",
            allergies=["peanut"],
            intolerances=[],
            foods_to_avoid=[],
            health_conditions=[],
            current_medicines=[],
            medicine_allergies=[],
            medication_restrictions=[],
            doctor_restrictions=[],
            pregnancy_status="not_pregnant",
        )
        assessment = AssessmentResult.objects.create(
            user=self.user,
            prakriti_result={**sample_payload()["assessment"]["prakriti"], "primaryDosha": "Kapha"},
            vikriti_result=sample_payload()["assessment"]["vikriti"],
            agni_result=sample_payload()["assessment"]["agni"],
            canonical_payload={"source": "database"},
        )
        with patch(
            "apps.agentic_reports.services.generate_personalized_plan",
            return_value=successful_agent_response(),
        ) as generate:
            result = create_wellness_plan(self.user, sample_payload())

        engine_payload = generate.call_args.args[0]
        self.assertEqual(result.assessment.pk, assessment.pk)
        self.assertEqual(AssessmentResult.objects.filter(user=self.user).count(), 1)
        self.assertEqual(engine_payload["patient_profile"]["name"], profile.name)
        self.assertEqual(engine_payload["patient_profile"]["goal"], profile.goal)
        self.assertEqual(engine_payload["assessment"]["prakriti"]["primaryDosha"], "Kapha")
        self.assertEqual(engine_payload["runtime"]["assessment_source"], "persisted_db")

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

    def test_revisions_form_immutable_v1_v2_v3_chain(self):
        v1 = self.create_plan().wellness_plan
        v2 = create_plan_revision(
            parent_plan=v1,
            diet_plan={"version": 2},
            validation={"status": "REVIEW_REQUIRED"},
        )
        v3 = create_plan_revision(
            parent_plan=v1,
            diet_plan={"version": 3},
            validation={"status": "REVIEW_REQUIRED"},
        )

        v1.refresh_from_db()
        v2.refresh_from_db()
        self.assertEqual(v1.version, 1)
        self.assertEqual(v1.diet_plan["duration_days"], 1)
        self.assertEqual(v2.version, 2)
        self.assertEqual(v2.parent_plan, v1)
        self.assertEqual(v3.version, 3)
        self.assertEqual(v3.parent_plan, v2)
        self.assertEqual(
            list(WellnessPlan.objects.filter(lineage_id=v1.lineage_id).order_by("version").values_list("version", flat=True)),
            [1, 2, 3],
        )

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


class PatientProfileModelTests(TestCase):
    def setUp(self):
        User = get_user_model()
        self.user = User.objects.create_user(username="profile-patient", password="pw")

    def test_patient_profile_is_associated_with_user(self):
        profile = PatientProfile.objects.create(
            user=self.user,
            name="Anaya Rao",
            age=34,
            gender="female",
            height_cm=Decimal("164.00"),
            weight_kg=Decimal("58.00"),
            dietary_preference="vegetarian",
            goal="general wellness",
            activity_level="moderate",
            allergies=[],
            intolerances=["lactose"],
            foods_to_avoid=["curd at night"],
            health_conditions=["acidity"],
            current_medicines=["vitamin d"],
            medicine_allergies=[],
            medication_restrictions=[],
            doctor_restrictions=["low sodium"],
            pregnancy_status="not_pregnant",
        )

        self.assertEqual(self.user.patient_profile, profile)
        self.assertEqual(profile.allergies, [])
        self.assertEqual(profile.intolerances, ["lactose"])

    def test_patient_profile_preserves_unanswered_safety_lists(self):
        profile = PatientProfile.objects.create(
            user=self.user,
            name="Anaya Rao",
            age=34,
            gender="female",
            height_cm=Decimal("164.00"),
            weight_kg=Decimal("58.00"),
        )

        self.assertIsNone(profile.allergies)
        self.assertIsNone(profile.current_medicines)
        self.assertIsNone(profile.doctor_restrictions)

    def test_patient_profile_rejects_malformed_safety_lists(self):
        profile = PatientProfile(
            user=self.user,
            name="Anaya Rao",
            allergies="peanut",
            current_medicines=["vitamin d", 12],
        )

        with self.assertRaises(ValidationError) as error:
            profile.full_clean()

        self.assertIn("allergies", error.exception.message_dict)
        self.assertIn("current_medicines", error.exception.message_dict)


class PatientProfileApiTests(APITestCase):
    def setUp(self):
        User = get_user_model()
        self.user = User.objects.create_user(username="profile-api-patient", password="pw")
        self.other_user = User.objects.create_user(username="profile-api-other", password="pw")

    def authenticate(self, user=None):
        token = RefreshToken.for_user(user or self.user)
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {token.access_token}")

    def profile_payload(self) -> dict:
        return {
            "name": "Anaya Rao",
            "age": 34,
            "gender": "female",
            "height_cm": "164.00",
            "weight_kg": "58.00",
            "dietary_preference": "vegetarian",
            "goal": "general wellness",
            "activity_level": "moderate",
            "allergies": [],
            "food_intolerances": ["lactose"],
            "foods_to_avoid": ["curd at night"],
            "health_conditions": ["acidity"],
            "current_medicines": ["vitamin d"],
            "medicine_allergies": [],
            "medication_restrictions": [],
            "doctor_restrictions": ["low sodium"],
            "pregnancy_information": "not_pregnant",
        }

    def test_profile_requires_jwt_authentication(self):
        response = self.client.get("/api/profile/")

        self.assertEqual(response.status_code, 401)

    def test_get_creates_profile_for_authenticated_user(self):
        self.authenticate()

        response = self.client.get("/api/profile/")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data["user_id"], self.user.id)
        self.assertTrue(PatientProfile.objects.filter(user=self.user).exists())

    def test_put_stores_profile_for_jwt_user_and_ignores_submitted_user_id(self):
        self.authenticate()
        payload = self.profile_payload()
        payload["user_id"] = self.other_user.id

        response = self.client.put("/api/profile/", payload, format="json")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data["user_id"], self.user.id)
        self.assertEqual(response.data["intolerances"], ["lactose"])
        self.assertEqual(response.data["pregnancy_status"], "not_pregnant")
        self.assertFalse(PatientProfile.objects.filter(user=self.other_user).exists())
        profile = PatientProfile.objects.get(user=self.user)
        self.assertEqual(profile.intolerances, ["lactose"])
        self.assertEqual(profile.pregnancy_status, "not_pregnant")

    def test_patch_updates_existing_profile_partially(self):
        PatientProfile.objects.create(
            user=self.user,
            name="Anaya Rao",
            allergies=None,
            doctor_restrictions=None,
        )
        self.authenticate()

        response = self.client.patch(
            "/api/profile/",
            {"allergies": [], "doctor_restrictions": ["low sodium"]},
            format="json",
        )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data["allergies"], [])
        self.assertEqual(response.data["doctor_restrictions"], ["low sodium"])
        self.assertEqual(response.data["name"], "Anaya Rao")

    def test_profile_preserves_unanswered_vs_explicit_none(self):
        self.authenticate()

        response = self.client.put(
            "/api/profile/",
            {
                "name": "Anaya Rao",
                "age": 34,
                "gender": "female",
                "allergies": [],
                "current_medicines": None,
                "doctor_restrictions": None,
            },
            format="json",
        )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data["allergies"], [])
        self.assertIsNone(response.data["current_medicines"])
        self.assertIsNone(response.data["doctor_restrictions"])

    def test_profile_rejects_malformed_safety_lists(self):
        self.authenticate()

        response = self.client.patch(
            "/api/profile/",
            {"allergies": "peanut"},
            format="json",
        )

        self.assertEqual(response.status_code, 400)
        self.assertIn("allergies", response.data)


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
        self.doctor_b = DoctorProfile.objects.create(
            user=User.objects.create_user(username="api-doctor-b", password="pw"),
            full_name="Dr. Bina",
            phone="8888888887",
            email="api-doctor-b@example.com",
            bams_registration_number="BAMS-API-B",
            specialization="Ayurveda",
            experience=6,
            clinic_name="Bina Clinic",
            consultation_fee=Decimal("450.00"),
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
        patient = user or self.patient
        PatientProfile.objects.update_or_create(
            user=patient,
            defaults={
                "name": "Anaya Rao",
                "age": 34,
                "gender": "female",
                "height_cm": "164.00",
                "weight_kg": "58.00",
                "dietary_preference": "vegetarian",
                "goal": "general wellness",
                "activity_level": "moderate",
                "allergies": [],
                "intolerances": [],
                "foods_to_avoid": [],
                "health_conditions": [],
                "current_medicines": [],
                "medicine_allergies": [],
                "medication_restrictions": [],
                "doctor_restrictions": [],
                "pregnancy_status": "not_pregnant",
            },
        )
        with patch(
            "apps.agentic_reports.services.generate_personalized_plan",
            return_value=successful_agent_response(validation_status),
        ):
            response = self.client.post("/api/agentic/plans/", sample_payload(), format="json")
        self.assertEqual(response.status_code, 201, response.data)
        profile, _ = PatientProfile.objects.get_or_create(user=patient)
        assignment, _ = DoctorAssignment.objects.get_or_create(
            patient_profile=profile,
            doctor=self.doctor,
        )
        WellnessPlan.objects.filter(pk=response.data["id"]).update(assignment=assignment)
        return response

    def test_authenticated_plan_generation_with_valid_canonical_assessment(self):
        response = self.create_api_plan()

        self.assertIn("id", response.data)
        self.assertEqual(response.data["assessment_summary"]["source"], "android_canonical_results")
        self.assertEqual(response.data["diet_plan"]["duration_days"], 1)
        self.assertEqual(response.data["validation_status"], "PASS")
        self.assertEqual(response.data["doctor_status"], "PENDING")
        self.assertEqual(response.data["rag_status"]["status"], "available")

    def test_canonical_assessment_can_be_persisted_without_agent_execution(self):
        self.authenticate_patient()
        payload = {
            "assessment_profile": {
                "prakriti_result": sample_payload()["assessment"]["prakriti"],
                "vikriti_result": sample_payload()["assessment"]["vikriti"],
                "agni_result": sample_payload()["assessment"]["agni"],
                "symptoms_result": {"possibleCondition": "none"},
            },
            "user_id": self.patient_b.id,
        }

        response = self.client.post("/api/agentic/assessments/", payload, format="json")

        self.assertEqual(response.status_code, 201, response.data)
        self.assertEqual(response.data["prakriti_result"], sample_payload()["assessment"]["prakriti"])
        self.assertEqual(response.data["vikriti_result"], sample_payload()["assessment"]["vikriti"])
        self.assertEqual(response.data["agni_result"], sample_payload()["assessment"]["agni"])
        assessment = AssessmentResult.objects.get(pk=response.data["id"])
        self.assertEqual(assessment.user_id, self.patient.id)
        self.assertEqual(assessment.canonical_payload["symptoms_result"], {"possibleCondition": "none"})
        self.assertEqual(AgentRun.objects.count(), 0)
        self.assertEqual(WellnessPlan.objects.count(), 0)

    def test_latest_assessment_is_scoped_to_authenticated_patient(self):
        own = AssessmentResult.objects.create(
            user=self.patient,
            prakriti_result=sample_payload()["assessment"]["prakriti"],
            vikriti_result=sample_payload()["assessment"]["vikriti"],
            agni_result=sample_payload()["assessment"]["agni"],
            canonical_payload=sample_payload()["assessment"],
        )
        AssessmentResult.objects.create(
            user=self.patient_b,
            prakriti_result={**sample_payload()["assessment"]["prakriti"], "prakritiType": "Kapha"},
            vikriti_result=sample_payload()["assessment"]["vikriti"],
            agni_result=sample_payload()["assessment"]["agni"],
            canonical_payload={},
        )
        self.authenticate_patient()

        response = self.client.get("/api/agentic/assessments/latest/")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data["id"], own.id)
        self.assertEqual(response.data["prakriti_result"]["prakritiType"], "Vata-Pitta")

    def test_malformed_canonical_assessment_persistence_returns_400(self):
        self.authenticate_patient()
        payload = {
            "assessment": {
                "prakriti": "bad",
                "vikriti": sample_payload()["assessment"]["vikriti"],
                "agni": sample_payload()["assessment"]["agni"],
            }
        }

        response = self.client.post("/api/agentic/assessments/", payload, format="json")

        self.assertEqual(response.status_code, 400)
        self.assertEqual(AssessmentResult.objects.count(), 0)

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
        self.assertEqual(detail_response.data["rag_status"]["embedding_count"], 1532)
        self.assertEqual(detail_response.data["rag_status"]["chunk_count"], 1532)
        self.assertNotIn("vectorstore_path", detail_response.data["rag_status"])
        self.assertNotIn("vectorstore_path", status_response.data["rag_status"])

    def test_patient_a_cannot_access_patient_b_plan(self):
        created = self.create_api_plan(user=self.patient_b)
        self.client.force_authenticate(user=self.patient)

        response = self.client.get(f"/api/agentic/plans/{created.data['id']}/")

        self.assertEqual(response.status_code, 404)

    def test_doctors_only_access_plans_for_assigned_patients(self):
        created = self.create_api_plan(user=self.patient)
        profile = PatientProfile.objects.get(user=self.patient)
        assignment = DoctorAssignment.objects.get(patient_profile=profile)
        assignment.doctor = self.doctor_b
        assignment.save(update_fields=["doctor"])

        self.client.force_authenticate(user=self.doctor_user)
        detail = self.client.get(f"/api/agentic/doctor/plans/{created.data['id']}/")
        queue = self.client.get("/api/agentic/doctor/review-queue/")

        self.assertEqual(detail.status_code, 404)
        self.assertEqual(queue.status_code, 200)
        self.assertEqual(queue.data, [])

        self.client.force_authenticate(user=self.doctor_b.user)
        assigned_detail = self.client.get(f"/api/agentic/doctor/plans/{created.data['id']}/")
        self.assertEqual(assigned_detail.status_code, 200)

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
