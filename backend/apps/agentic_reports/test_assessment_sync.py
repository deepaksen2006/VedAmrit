from copy import deepcopy
from uuid import uuid4
from unittest.mock import patch

from django.contrib.auth import get_user_model
from rest_framework.test import APITestCase
from rest_framework_simplejwt.tokens import RefreshToken

from .assessment_sync import assessment_version, content_version
from .models import AssessmentResult, PatientProfile, AgentRun, WellnessPlan
from .tests import sample_payload, successful_agent_response


class AssessmentSynchronizationTests(APITestCase):
    def setUp(self):
        self.patient = get_user_model().objects.create_user(username="sync-patient", password="test-password")
        self.other = get_user_model().objects.create_user(username="other-patient", password="test-password")
        self.sign_in(self.patient)
        self.submission = str(uuid4())
        self.content = sample_payload()["assessment"]

    def sign_in(self, user):
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {RefreshToken.for_user(user).access_token}")

    def submit(self, content=None, submission=None):
        return self.client.post("/api/agentic/assessments/", {
            "assessment_profile": content or self.content,
            "submission_id": submission or self.submission,
            "user_id": self.other.pk,  # must never decide ownership
        }, format="json")

    def plan_payload(self, payload):
        from .generation import profile_version
        profile = PatientProfile.objects.filter(user=self.patient).first()
        return {**payload, "profile_version": profile_version(profile) if profile else "0" * 64}

    def profile(self):
        PatientProfile.objects.create(user=self.patient, name="Sync patient", age=34, gender="female",
            height_cm=164, weight_kg=58, dietary_preference="vegetarian", goal="general wellness",
            activity_level="moderate", allergies=[], intolerances=[], foods_to_avoid=[], health_conditions=[],
            medicine_allergies=[], medication_restrictions=[], doctor_restrictions=[], pregnancy_status="not_pregnant")

    def test_retry_after_lost_ack_is_idempotent_and_latest_is_unchanged(self):
        first = self.submit()
        retry = self.submit()
        self.assertEqual(first.status_code, 201)
        self.assertEqual(retry.status_code, 200)
        self.assertEqual(first.data["id"], retry.data["id"])
        self.assertEqual(first.data["assessment_version"], retry.data["assessment_version"])
        self.assertEqual(AssessmentResult.objects.count(), 1)
        saved = AssessmentResult.objects.get()
        self.assertEqual(saved.user_id, self.patient.pk)
        self.assertEqual(saved.prakriti_result, self.content["prakriti"])
        self.assertEqual(saved.vikriti_result, self.content["vikriti"])
        self.assertEqual(saved.agni_result, self.content["agni"])
        latest = self.client.get("/api/agentic/assessments/latest/")
        self.assertEqual(latest.data["id"], first.data["id"])

    def test_same_submission_cannot_mutate_content(self):
        first = self.submit()
        edited = deepcopy(self.content)
        edited["prakriti"]["scores"]["Vata"] += 1
        conflict = self.submit(edited)
        self.assertEqual(conflict.status_code, 409)
        self.assertEqual(AssessmentResult.objects.count(), 1)
        self.assertEqual(assessment_version(AssessmentResult.objects.get()), first.data["assessment_version"])

    def test_same_id_in_other_account_never_shares_assessment(self):
        first = self.submit()
        self.sign_in(self.other)
        self.assertEqual(self.client.get("/api/agentic/assessments/latest/").status_code, 404)
        second = self.submit()
        self.assertEqual(second.status_code, 201)
        self.assertNotEqual(first.data["id"], second.data["id"])
        self.assertEqual(AssessmentResult.objects.filter(user=self.other).count(), 1)

    def test_all_three_results_and_scores_are_mandatory(self):
        for key in ("prakriti", "vikriti", "agni"):
            missing = deepcopy(self.content)
            del missing[key]
            result = self.submit(missing)
            self.assertEqual(result.status_code, 400)
        self.assertEqual(AssessmentResult.objects.count(), 0)

    def test_missing_and_expired_jwt_do_not_save_results(self):
        from rest_framework_simplejwt.tokens import AccessToken
        from datetime import timedelta
        self.client.credentials()
        self.assertEqual(self.submit().status_code, 401)
        expired = AccessToken.for_user(self.patient)
        expired.set_exp(lifetime=timedelta(seconds=-1))
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {expired}")
        self.assertEqual(self.submit().status_code, 401)
        self.assertEqual(AssessmentResult.objects.count(), 0)

    def test_version_is_key_order_and_integer_float_stable(self):
        self.assertEqual(content_version({"a": 1, "b": {"c": 2}}), content_version({"b": {"c": 2.0}, "a": 1.0}))

    def test_generation_pins_exact_assessment_and_audits_engine_input(self):
        self.profile()
        assessment = self.submit().data
        payload = self.plan_payload({**sample_payload(), "assessment_id": assessment["id"], "assessment_version": assessment["assessment_version"]})
        with patch("apps.agentic_reports.services.generate_personalized_plan", return_value=successful_agent_response()) as generate:
            plan = self.client.post("/api/agentic/plans/", self.plan_payload(payload), format="json")
        self.assertEqual(plan.status_code, 201, plan.data)
        self.assertEqual(plan.data["assessment_id"], assessment["id"])
        self.assertEqual(plan.data["assessment_version"], assessment["assessment_version"])
        self.assertEqual(generate.call_args.args[0]["assessment"], self.content)
        runtime = generate.call_args.args[0]["runtime"]
        self.assertEqual(runtime["assessment_id"], assessment["id"])
        self.assertEqual(runtime["assessment_version"], assessment["assessment_version"])
        self.assertEqual(AgentRun.objects.get().assessment_id, assessment["id"])

    def test_stale_reference_version_or_submitted_content_never_invokes_ai(self):
        self.profile()
        old = self.submit().data
        newer = deepcopy(self.content)
        newer["agni"]["scores"]["Samagni"] = 9
        current = self.submit(newer, str(uuid4())).data
        cases = [
            {"assessment_id": old["id"], "assessment_version": old["assessment_version"]},
            {"assessment_id": current["id"], "assessment_version": "0" * 64},
            {**sample_payload(), "assessment_id": current["id"], "assessment_version": current["assessment_version"]},
            sample_payload(),  # legacy clients also cannot silently discard submitted content
        ]
        with patch("apps.agentic_reports.services.generate_personalized_plan") as generate:
            for payload in cases:
                result = self.client.post("/api/agentic/plans/", self.plan_payload(payload), format="json")
                self.assertEqual(result.status_code, 409 if payload.get("assessment_id") else 400, result.data)
            generate.assert_not_called()
        self.assertEqual(WellnessPlan.objects.count(), 0)

    def test_other_patients_assessment_reference_is_rejected(self):
        saved = self.submit().data
        self.sign_in(self.other)
        with patch("apps.agentic_reports.services.generate_personalized_plan") as generate:
            result = self.client.post("/api/agentic/plans/", {
                "assessment_id": saved["id"], "assessment_version": saved["assessment_version"]
            }, format="json")
            self.assertEqual(result.status_code, 400)
            generate.assert_not_called()

    def test_reference_requires_both_id_and_version(self):
        result = self.client.post("/api/agentic/plans/", {"assessment_id": 1}, format="json")
        self.assertEqual(result.status_code, 400)

    def test_assessment_change_during_generation_does_not_publish_stale_plan(self):
        self.profile()
        saved = self.submit().data
        def generate(_payload):
            newer = deepcopy(self.content)
            newer["agni"]["scores"]["Samagni"] = 10
            self.submit(newer, str(uuid4()))
            return successful_agent_response()
        with patch("apps.agentic_reports.services.generate_personalized_plan", side_effect=generate):
            response = self.client.post("/api/agentic/plans/", {
                **self.plan_payload(sample_payload()), "assessment_id": saved["id"], "assessment_version": saved["assessment_version"]
            }, format="json")
        self.assertEqual(response.status_code, 409)
        self.assertEqual(WellnessPlan.objects.count(), 0)
        self.assertEqual(AgentRun.objects.get().status, "FAILED")
