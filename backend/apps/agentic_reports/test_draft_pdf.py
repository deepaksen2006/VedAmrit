"""PDF endpoint tests use persisted synthetic fixtures; no AI calls on download."""
from copy import deepcopy
from io import BytesIO
from pathlib import Path
from unittest.mock import patch
from django.contrib.auth import get_user_model
from django.test import override_settings
from rest_framework.test import APITestCase
from pypdf import PdfReader
from . import test_draft_generation as fixtures
from .tests import successful_agent_response
from .draft_pdf import DISCLAIMER, LEGACY_WARNING, WARNING_EXPLANATION
from .models import WellnessPlan, AgentRun, FinalReport


@override_settings(DEBUG=True, AGENTIC_DRAFT_PREVIEW_ENABLED=True)
class DraftPdfTests(APITestCase):
    setUp = fixtures.DraftGenerationTests.setUp
    sign_in = fixtures.DraftGenerationTests.sign_in
    generate = fixtures.DraftGenerationTests.generate

    def plan(self, warnings=None):
        from uuid import uuid4
        self.payload["generation_id"] = str(uuid4())
        response = successful_agent_response("REVIEW_REQUIRED")
        response["validation"].update(warnings=[LEGACY_WARNING] if warnings is None else warnings,
            diet_validation={"valid": True, "issues": [], "checks": {key: "PASS" for key in
                ("food_data", "nutrition", "meal_structure", "ayurvedic_evidence", "patient_constraints")}})
        response["lifestyle_plan"]["recommendations"] = [{"category": "daily_routine",
            "personalized_recommendation": "Synthetic fixture recommendation - keep a consistent routine.",
            "patient_specific_reason": "Synthetic fixture assessment context.", "safety_consideration": "Review symptoms with your clinician.",
            "ayurvedic_basis": {"source": "fixture.pdf", "page": "12", "verified": False}}]
        result, _ = self.generate(response=response)
        self.assertEqual(result.status_code, 201, result.data)
        return WellnessPlan.objects.get(pk=result.data["id"])

    def download(self, plan):
        return self.client.get(f"/api/agentic/plans/{plan.pk}/draft-pdf/")

    def test_personalized_pdf_bytes_seven_days_28_meals_disclaimer_each_page(self):
        plan = self.plan()
        result = self.download(plan)
        self.assertEqual(result.status_code, 200, getattr(result, "data", None))
        self.assertEqual(result["Content-Type"], "application/pdf")
        self.assertTrue(result.content.startswith(b"%PDF-"))
        self.assertIn(f"Draft-Plan-{plan.pk}", result["Content-Disposition"])
        reader = PdfReader(BytesIO(result.content))
        pages = [page.extract_text() for page in reader.pages]
        text = "\n".join(pages)
        self.assertTrue(all(DISCLAIMER in page for page in pages))
        for day in range(1, 8):
            self.assertEqual(text.count(f"Day {day} - Meal schedule"), 1)
        for meal in ("Breakfast", "Lunch", "Snack", "Dinner"):
            self.assertEqual(text.splitlines().count(meal), 7)
        self.assertIn("Synthetic test patient", text)
        self.assertIn("Assessment #", text)
        self.assertIn("Generated", text)
        self.assertIn("Protein g", text)
        self.assertIn("fixture.pdf", text)
        self.assertIn(LEGACY_WARNING, " ".join(text.split()))
        self.assertIn("legacy validator", text)
        plan.refresh_from_db()
        self.assertEqual(plan.doctor_status, "PENDING")
        self.assertFalse(FinalReport.objects.exists())
        # One synthetic QA artifact for rendering inspection, never a patient record.
        preview = Path("tmp_agent_engine_reports/task3_1_preview.pdf")
        preview.parent.mkdir(parents=True, exist_ok=True)
        preview.write_bytes(result.content)

    def test_repeat_download_is_identical_and_never_invokes_graph_or_creates_plans(self):
        plan = self.plan()
        counts = (AgentRun.objects.count(), WellnessPlan.objects.count())
        with patch("apps.agentic_reports.services.generate_personalized_plan") as graph:
            first, second = self.download(plan), self.download(plan)
            graph.assert_not_called()
        self.assertEqual(first.content, second.content)
        self.assertEqual(counts, (AgentRun.objects.count(), WellnessPlan.objects.count()))
        self.assertEqual(self.client.get(f"/api/agentic/plans/{plan.pk}/report/").status_code, 409)
        self.assertFalse(FinalReport.objects.exists())

    def test_jwt_owner_and_development_gates(self):
        plan = self.plan()
        self.client.credentials()
        self.assertEqual(self.download(plan).status_code, 401)
        other = get_user_model().objects.create_user(username="other-pdf-fixture")
        self.sign_in(other)
        self.assertEqual(self.download(plan).status_code, 404)
        self.sign_in(self.patient)
        with override_settings(DEBUG=False):
            self.assertEqual(self.download(plan).status_code, 403)
        with override_settings(AGENTIC_DRAFT_PREVIEW_ENABLED=False):
            self.assertEqual(self.download(plan).status_code, 403)
        from rest_framework_simplejwt.tokens import AccessToken
        from datetime import timedelta
        token = AccessToken.for_user(self.patient)
        token.set_exp(lifetime=timedelta(seconds=-1))
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {token}", HTTP_X_VEDAMRIT_DRAFT_PREVIEW="1")
        self.assertEqual(self.download(plan).status_code, 401)

    def test_clinical_or_unrecognized_review_warning_is_blocked(self):
        for warning in ("Doctor must review a health condition.", "REVIEW REQUIRED: allergies is unknown; record an explicit none."):
            plan = self.plan([warning])
            result = self.download(plan)
            self.assertEqual(result.status_code, 409)
            self.assertIn(warning, " ".join(result.data["reasons"]))

    def test_no_review_reason_is_not_silently_approved(self):
        self.assertEqual(self.download(self.plan([])).status_code, 409)

    def test_failed_validation_and_broken_meal_structure_block_pdf(self):
        plan = self.plan()
        plan.validation_status = "FAIL"
        plan.save()
        self.assertEqual(self.download(plan).status_code, 409)
        plan.validation_status = "REVIEW_REQUIRED"
        plan.diet_plan["days"][0]["meals"].pop()
        plan.save()
        self.assertEqual(self.download(plan).status_code, 409)

    def test_stale_profile_and_tampered_lineage_block_pdf(self):
        plan = self.plan()
        self.client.patch("/api/profile/", {"foods_to_avoid": ["fixture food"]}, format="json")
        self.assertEqual(self.download(plan).status_code, 409)
        self.assertFalse(self.client.get(f"/api/agentic/plans/{plan.pk}/").data["draft_pdf_available"])

    def test_known_clinical_context_cannot_hide_behind_legacy_warning(self):
        self.client.patch("/api/profile/", {"health_conditions": ["fixture condition"]}, format="json")
        self.profile.refresh_from_db()
        from .generation import profile_version
        self.payload["profile_version"] = profile_version(self.profile)
        plan = self.plan()
        self.assertEqual(self.download(plan).status_code, 409)

    def test_real_saved_graph_pdf(self):
        import os
        if os.getenv("RUN_AGENTIC_REAL_SMOKE") != "1":
            self.skipTest("Real graph PDF smoke is opt-in.")
        result = self.client.post("/api/agentic/plans/", self.payload, format="json")
        self.assertEqual(result.status_code, 201, result.data)
        plan = WellnessPlan.objects.get(pk=result.data["id"])
        pdf = self.download(plan)
        self.assertEqual(pdf.status_code, 200, getattr(pdf, "data", None))
        reader = PdfReader(BytesIO(pdf.content))
        text = "\n".join(page.extract_text() for page in reader.pages)
        for meal in ("Breakfast", "Lunch", "Snack", "Dinner"):
            self.assertEqual(text.splitlines().count(meal), 7)
        for page in reader.pages:
            self.assertIn(DISCLAIMER, page.extract_text())
        self.assertIn("Retrieved reference:", text)
        self.assertEqual(AgentRun.objects.count(), 1)
        self.assertEqual(WellnessPlan.objects.count(), 1)
        self.assertFalse(FinalReport.objects.exists())
        Path("tmp_agent_engine_reports/task3_1_preview.pdf").write_bytes(pdf.content)
        print(f"REAL_GRAPH_PDF_SMOKE pages={len(reader.pages)} meals=28 doctor=PENDING db=isolated-SQLite")

    def test_profile_change_while_rendering_blocks_the_response(self):
        plan = self.plan()
        def renderer(_plan):
            self.client.patch("/api/profile/", {"foods_to_avoid": ["fixture food"]}, format="json")
            return b"%PDF-1.4 fixture"
        with patch("apps.agentic_reports.draft_pdf.render_draft_pdf", side_effect=renderer):
            self.assertEqual(self.download(plan).status_code, 409)
