import copy
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from test_two_report_workflow import TwoReportWorkflowTests
from agent_engine.services.report_workflow import TwoReportWorkflow
from agent_engine.services.report_validation import validate_report_state
from agent_engine.services.two_report_pdf import TwoReportPDFGenerator
from agent_engine.agents.lifestyle import lifestyle_agent


class ReportSecurityTests(unittest.TestCase):
    def setUp(self):
        fixtures = TwoReportWorkflowTests()
        self.state = fixtures._state()
        self.approval = fixtures._approval()
        self.flow = TwoReportWorkflow(self.state)
        self.flow.create_assessment_report()

    def test_string_booleans_and_blank_signature_rejected(self):
        for key, value in (("approved", "false"), ("qualified_reviewer", "yes"), ("signature", " ")):
            with self.subTest(key=key), self.assertRaises(ValueError):
                self.flow.approve({**self.approval, key: value})

    def test_version_tampering_and_rejection_block_export(self):
        self.flow.approve(self.approval)
        self.state["final_report_version"] = 99
        with self.assertRaises(PermissionError):
            self.flow.create_final_report()
        self.flow.reject()
        self.assertEqual(self.state["doctor_review"]["decision"], "REJECT")
        self.assertFalse(self.flow.approval_is_current())

    def test_regeneration_reuses_identity_and_edits_require_new_version(self):
        self.flow.approve(self.approval)
        first = self.flow.create_final_report()
        self.assertEqual(first, self.flow.create_final_report())
        self.state["patient_profile"]["goal"] = "Changed goal"
        self.assertTrue(self.flow.invalidate_if_changed())
        self.flow.approve(self.approval)
        second = self.flow.create_final_report()
        self.assertEqual(first["metadata"]["report_id"], second["metadata"]["report_id"])
        self.assertEqual(second["metadata"]["report_version"], 2)

    def test_doctor_edited_diet_is_validated_and_original_is_untouched(self):
        original = copy.deepcopy(self.state["meal_plan"])
        edited = copy.deepcopy(original)
        edited["days"][0]["meals"][0]["foods"][0]["food_name"] = "peanut"
        self.state["patient_profile"]["allergies"] = ["peanut"]
        self.state["doctor_review"] = {"edited_diet_plan": edited}
        self.assertIn("Restricted food", str(validate_report_state(self.state)["failed_checks"]))
        self.assertEqual(original, self.state["meal_plan"])

    def test_named_meal_missing_macros_are_unknown(self):
        self.state["agent2_output"] = {"diet_plan": {"Breakfast": {"foods": [{"food_name": "Test food", "portion_g": 50, "calories": 20}]}}}
        result = validate_report_state(self.state)
        self.assertEqual(result["nutrition_total"]["calories"], 20)
        self.assertIsNone(result["nutrition_total"]["protein_g"])
        self.assertEqual(result["status"], "REVIEW_REQUIRED")

    def test_stored_final_content_tampering_revokes_approval(self):
        self.flow.approve(self.approval)
        self.flow.create_final_report()
        self.state["final_report"]["final_approved_data"]["patient_profile"]["name"] = "Changed"
        self.assertFalse(self.flow.approval_is_current())

    def test_medicine_edits_require_reapproval_and_allergy_conflicts_block(self):
        approval = {**self.approval, "medicines": [{"name": "SYNTHETIC TEST MEDICINE", "dosage": "TEST ONLY", "source": "DOCTOR_ENTERED", "doctor_approval_status": "DOCTOR APPROVED"}]}
        self.flow.approve(approval)
        self.state["doctor_review"]["medicines"][0]["dosage"] = "CHANGED TEST VALUE"
        self.assertFalse(self.flow.approval_is_current())
        self.state["patient_profile"]["medicine_allergies"] = ["SYNTHETIC TEST MEDICINE"]
        with self.assertRaises(ValueError):
            self.flow.approve(approval)

    def test_weekly_totals_and_negative_nutrition(self):
        day = self.state["meal_plan"]["days"][0]
        self.state["meal_plan"]["days"] = [copy.deepcopy(day) for _ in range(8)]
        result = validate_report_state(self.state)
        self.assertEqual([week["totals"]["calories"] for week in result["weekly_totals"]], [700, 100])
        self.state["meal_plan"]["days"][0]["meals"][0]["foods"][0]["nutrition"]["calories"] = -1
        self.assertIn("Negative nutrient", str(validate_report_state(self.state)["failed_checks"]))

    def test_no_doctor_information_leaks_into_assessment_profile(self):
        self.state["patient_profile"]["doctor_name"] = "PRIVATE DOCTOR"
        self.assertNotIn("PRIVATE DOCTOR", str(self.flow.create_assessment_report()))

    @patch("agent_engine.agents.lifestyle.get_lifestyle_rag_evidence", return_value=[])
    def test_agni_changes_recommendation_and_context_reaches_retrieval(self, retrieve):
        outputs = []
        for agni in ("Mandagni", "Vishamagni", "Tikshnagni"):
            self.state["agni_result"]["status"] = agni
            outputs.append(lifestyle_agent(self.state)["lifestyle_plan"])
        texts = [next(r["personalized_recommendation"] for r in out["recommendations"] if r["category"] == "meal_timing") for out in outputs]
        self.assertEqual(len(set(texts)), 3)
        self.assertIn("Tikshnagni", retrieve.call_args.kwargs["patient_context"])
        self.assertTrue(outputs[-1]["safety_flags"])

    def test_long_tables_and_english_gate(self):
        with tempfile.TemporaryDirectory() as directory:
            generator = TwoReportPDFGenerator(directory)
            generator.generate_assessment_report(self.state)
            meal = self.state["meal_plan"]["days"][0]["meals"][0]
            meal["foods"] *= 90
            meal.pop("meal_total")
            self.state["meal_plan"]["days"][0].pop("daily_total")
            self.flow.approve(self.approval)
            path = generator.generate_final_report(self.state)
            self.assertTrue(Path(path).read_bytes().startswith(b"%PDF-"))
            generator.generate_final_report(self.state)
            with self.assertRaises(ValueError):
                generator._p("हिन्दी")


if __name__ == "__main__":
    unittest.main()
