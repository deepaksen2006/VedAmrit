"""JWT/ORM integration tests; engine mocked unless RUN_AGENTIC_REAL_SMOKE=1."""
import os
from copy import deepcopy
from uuid import uuid4
from unittest.mock import patch
from django.contrib.auth import get_user_model
from django.test import override_settings
from rest_framework.test import APITestCase
from rest_framework_simplejwt.tokens import RefreshToken
from .models import AgentRun, AssessmentResult, PatientProfile, WellnessPlan, FinalReport
from .generation import profile_version
from .tests import sample_payload, successful_agent_response


@override_settings(DEBUG=True, AGENTIC_DRAFT_PREVIEW_ENABLED=True)
class DraftGenerationTests(APITestCase):
    def setUp(self):
        self.patient = get_user_model().objects.create_user(username="draft-fixture", password="test-only")
        self.sign_in(self.patient)
        profile = {"name": "Synthetic test patient", "age": 34, "gender": "female", "height_cm": "164",
            "weight_kg": "58", "dietary_preference": "vegetarian", "goal": "general wellness", "activity_level": "moderate",
            "allergies": [], "intolerances": [], "foods_to_avoid": [], "health_conditions": [], "current_medicines": [],
            "medicine_allergies": [], "medication_restrictions": [], "doctor_restrictions": [], "pregnancy_status": "not_pregnant"}
        saved = self.client.put("/api/profile/", profile, format="json")
        self.assertEqual(saved.status_code, 200, saved.data)
        assessment = self.client.post("/api/agentic/assessments/", {"assessment": sample_payload()["assessment"]}, format="json").data
        self.payload = {"assessment_id": assessment["id"], "assessment_version": assessment["assessment_version"],
            "profile_version": saved.data["profile_version"], "generation_id": str(uuid4()), "plan_days": 7}
        self.profile = PatientProfile.objects.get(user=self.patient)

    def sign_in(self, patient):
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {RefreshToken.for_user(patient).access_token}",
                                HTTP_X_VEDAMRIT_DRAFT_PREVIEW="1")

    def generate(self, payload=None, response=None):
        with patch("apps.agentic_reports.services.generate_personalized_plan", return_value=response or successful_agent_response()) as engine:
            result = self.client.post("/api/agentic/plans/", payload or self.payload, format="json")
        return result, engine

    def test_jwt_persistence_draft_and_restoration(self):
        result, engine = self.generate()
        self.assertEqual(result.status_code, 201, result.data)
        self.assertTrue(result.data["draft_view_allowed"])
        self.assertEqual(result.data["draft_label"], "AI-Generated Draft \u2014 Not Doctor Reviewed")
        self.assertEqual(result.data["doctor_status"], "PENDING")
        self.assertEqual(result.data["profile_version"], self.payload["profile_version"])
        self.assertEqual(result.data["generation_id"], self.payload["generation_id"])
        self.assertEqual(len(result.data["diet_plan"]["days"]), 7)
        self.assertEqual(engine.call_args.args[0]["patient_profile"]["allergies"], [])
        plan = WellnessPlan.objects.get()
        self.assertEqual(plan.agent_run.assessment_id, plan.assessment_id)
        self.assertEqual(plan.user_id, self.patient.id)
        self.assertEqual(self.client.get(f"/api/agentic/plans/{plan.pk}/").data, result.data)
        self.assertEqual(self.client.get("/api/agentic/plans/").data[0]["profile_version"], self.payload["profile_version"])
        self.assertEqual(self.client.get(f"/api/agentic/plans/{plan.pk}/report/").status_code, 409)
        self.assertFalse(FinalReport.objects.exists())

    def test_duplicate_retry_and_lost_response_recover_single_run(self):
        first, _ = self.generate()
        retry, engine = self.generate()
        engine.assert_not_called()
        self.assertEqual(retry.data["id"], first.data["id"])
        self.assertEqual(AgentRun.objects.count(), 1)
        self.assertEqual(WellnessPlan.objects.count(), 1)
        status = self.client.get(f"/api/agentic/generations/{self.payload['generation_id']}/")
        self.assertEqual(status.data["status"], "COMPLETED")
        self.assertEqual(status.data["plan_id"], first.data["id"])

    def test_running_retry_does_not_start_second_graph(self):
        from .generation import claim_generation
        from .services import _persisted_engine_payload
        assessment = AssessmentResult.objects.get(pk=self.payload["assessment_id"])
        claim_generation(self.patient, assessment, _persisted_engine_payload(self.profile, assessment, self.payload), self.payload["generation_id"])
        result, engine = self.generate()
        self.assertEqual(result.status_code, 202)
        engine.assert_not_called()
        self.assertEqual(AgentRun.objects.count(), 1)

    def test_same_id_cannot_be_reused_with_different_inputs(self):
        self.generate()
        self.profile.weight_kg = 64
        self.profile.save()
        payload = {**self.payload, "profile_version": profile_version(self.profile)}
        result, engine = self.generate(payload)
        self.assertEqual(result.status_code, 409)
        engine.assert_not_called()

    def test_stale_profile_is_rejected_before_graph(self):
        self.profile.allergies = ["peanut"]
        self.profile.save()
        result, engine = self.generate()
        self.assertEqual(result.status_code, 409)
        engine.assert_not_called()
        self.assertFalse(AgentRun.objects.exists())

    def test_profile_change_during_graph_does_not_publish_plan(self):
        def changed(_payload):
            self.client.patch("/api/profile/", {"allergies": ["peanut"]}, format="json")
            return successful_agent_response()
        with patch("apps.agentic_reports.services.generate_personalized_plan", side_effect=changed):
            result = self.client.post("/api/agentic/plans/", self.payload, format="json")
        self.assertEqual(result.status_code, 409)
        self.assertFalse(WellnessPlan.objects.exists())
        self.assertEqual(AgentRun.objects.get().status, "FAILED")

    def test_unknown_safety_fields_are_not_coerced_to_none_known(self):
        self.profile.allergies = None
        self.profile.save()
        result, engine = self.generate({**self.payload, "profile_version": profile_version(self.profile)})
        self.assertEqual(result.status_code, 400)
        engine.assert_not_called()
        self.assertFalse(AgentRun.objects.exists())

    def test_safety_failure_hides_content_but_preserves_review_data(self):
        result, _ = self.generate(response=successful_agent_response("FAIL"))
        self.assertEqual(result.status_code, 201)
        self.assertFalse(result.data["draft_view_allowed"])
        self.assertIsNone(result.data["diet_plan"])
        self.assertIsNone(result.data["lifestyle_plan"])
        self.assertTrue(WellnessPlan.objects.get().diet_plan)
        self.assertEqual(result.data["doctor_status"], "PENDING")

    def test_incomplete_structure_is_blocked_even_if_engine_says_pass(self):
        response = successful_agent_response()
        response["diet_plan"]["days"][0]["meals"].pop()
        result, _ = self.generate(response=response)
        self.assertFalse(result.data["draft_view_allowed"])
        self.assertIsNone(result.data["diet_plan"])

    def test_preview_requires_both_server_debug_and_client_opt_in(self):
        result, _ = self.generate()
        path = f"/api/agentic/plans/{result.data['id']}/"
        with override_settings(DEBUG=False, AGENTIC_DRAFT_PREVIEW_ENABLED=True):
            self.assertIsNone(self.client.get(path).data["diet_plan"])
        with override_settings(AGENTIC_DRAFT_PREVIEW_ENABLED=False):
            self.assertIsNone(self.client.get(path).data["diet_plan"])
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {RefreshToken.for_user(self.patient).access_token}")
        self.assertIsNone(self.client.get(path).data["diet_plan"])

    def test_missing_expired_jwt_and_other_account_never_reveal_plan(self):
        from datetime import timedelta
        from rest_framework_simplejwt.tokens import AccessToken
        result, _ = self.generate()
        path = f"/api/agentic/plans/{result.data['id']}/"
        status_path = f"/api/agentic/generations/{self.payload['generation_id']}/"
        self.client.credentials()
        self.assertEqual(self.client.get(status_path).status_code, 401)
        expired = AccessToken.for_user(self.patient)
        expired.set_exp(lifetime=timedelta(seconds=-1))
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {expired}")
        self.assertEqual(self.client.post("/api/agentic/plans/", self.payload, format="json").status_code, 401)
        other = get_user_model().objects.create_user(username="other-draft-fixture")
        self.sign_in(other)
        self.assertEqual(self.client.get(path).status_code, 404)
        self.assertEqual(self.client.get(status_path).status_code, 404)
        self.assertEqual(self.client.get("/api/agentic/plans/").data, [])

    def test_profile_changed_after_generation_hides_stale_draft(self):
        result, _ = self.generate()
        self.client.patch("/api/profile/", {"allergies": ["peanut"]}, format="json")
        detail = self.client.get(f"/api/agentic/plans/{result.data['id']}/")
        self.assertFalse(detail.data["inputs_current"])
        self.assertFalse(detail.data["draft_view_allowed"])
        self.assertIsNone(detail.data["diet_plan"])
        self.assertFalse(self.client.get("/api/agentic/plans/").data[0]["inputs_current"])

    def test_graph_exception_is_durable_failure_without_plan(self):
        with patch("apps.agentic_reports.services.generate_personalized_plan", side_effect=RuntimeError("fixture failure")):
            result = self.client.post("/api/agentic/plans/", self.payload, format="json")
        self.assertEqual(result.status_code, 502)
        self.assertEqual(AgentRun.objects.get().status, "FAILED")
        self.assertFalse(WellnessPlan.objects.exists())

    def test_persistence_failure_is_recorded_and_never_returns_success(self):
        with patch("apps.agentic_reports.services.generate_personalized_plan", return_value=successful_agent_response()), \
             patch("apps.agentic_reports.services.WellnessPlan.objects.create", side_effect=RuntimeError("fixture persistence failure")):
            result = self.client.post("/api/agentic/plans/", self.payload, format="json")
        self.assertEqual(result.status_code, 502)
        self.assertEqual(AgentRun.objects.get().status, "FAILED")
        self.assertEqual(AgentRun.objects.get().error_code, "plan_persistence_failed")
        self.assertFalse(WellnessPlan.objects.exists())

    def test_runtime_paths_are_never_accepted_from_patient(self):
        result, engine = self.generate({**self.payload, "runtime": {"report_dir": "untrusted", "food_dataset_path": "untrusted"}})
        self.assertEqual(result.status_code, 201)
        self.assertNotIn("report_dir", engine.call_args.args[0]["runtime"])

    def test_real_graph_vegan_allergy_and_intolerance_smoke(self):
        if os.getenv("RUN_AGENTIC_REAL_SMOKE") != "1":
            self.skipTest("Real graph smoke is opt-in.")
        from agent_engine.services.food_restrictions import matches_food_restriction
        saved = self.client.patch("/api/profile/", {"dietary_preference": "vegan", "allergies": ["peanut"],
            "food_intolerances": ["milk"], "foods_to_avoid": ["brinjal"]}, format="json")
        payload = {**self.payload, "profile_version": saved.data["profile_version"]}
        result = self.client.post("/api/agentic/plans/", payload, format="json")
        self.assertEqual(result.status_code, 201, result.data)
        self.assertTrue(result.data["draft_view_allowed"], result.data["validation"])
        for day in result.data["diet_plan"]["days"]:
            for meal in day["meals"]:
                for food in meal["foods"]:
                    self.assertEqual(food["diet_type"].lower(), "vegan")
                    for restriction in ("peanut", "milk", "brinjal", "egg", "honey"):
                        self.assertFalse(matches_food_restriction(food, restriction), food["food_name"])
        self.assertEqual(result.data["doctor_status"], "PENDING")
        print("REAL_GRAPH_RESTRICTIONS_SMOKE vegan peanut-allergy milk-intolerance brinjal-avoidance passed database=in-memory")

    def test_real_graph_rag_and_orm_smoke(self):
        if os.getenv("RUN_AGENTIC_REAL_SMOKE") != "1":
            self.skipTest("Opt in to the installed real graph and local semantic RAG smoke.")
        # No engine mocks: actual JWT endpoint, local CSV, graph, Chroma and ORM.
        import time
        started = time.monotonic()
        result = self.client.post("/api/agentic/plans/", self.payload, format="json")
        self.assertEqual(result.status_code, 201, result.data)
        self.assertTrue(result.data["draft_view_allowed"], result.data["validation"])
        self.assertEqual(result.data["doctor_status"], "PENDING")
        self.assertEqual(result.data["validation"]["diet_validation"]["valid"], True)
        self.assertEqual(result.data["rag_status"]["status"], "retrieved")
        self.assertGreater(result.data["rag_status"]["evidence_count"], 0)
        for day in result.data["diet_plan"]["days"]:
            self.assertEqual({meal["meal"].lower() for meal in day["meals"]}, {"breakfast", "lunch", "snack", "dinner"})
        print(f"REAL_GRAPH_JWT_ORM_SMOKE seconds={time.monotonic()-started:.2f} days=7 meals=28 rag={result.data['rag_status']['evidence_count']} doctor=PENDING database=in-memory")
