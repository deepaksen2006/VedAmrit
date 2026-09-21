import unittest

from agent_engine.services.report_workflow import TwoReportWorkflow


class TwoReportWorkflowTests(unittest.TestCase):
    def _state(self):
        nutrition = {"calories": 100, "protein_g": 5, "carbs_g": 15, "fat_g": 2, "fiber_g": 3}
        return {
            "patient_profile": {"id": "P001", "name": "Patient", "allergies": [], "food_intolerances": [], "foods_to_avoid": [], "health_conditions": []},
            "prakriti_result": {"constitution": "Vata", "scores": {"Vata": 21}, "primary_dosha": "Vata", "secondary_dosha": "Pitta"},
            "vikriti_result": {"dominant_dosha": "Vata", "scores": {"Vata": 10}},
            "agni_result": {"status": "Vishamagni", "category_scores": {"Vishamagni": 10}},
            "meal_plan": {"days": [{"day": 1, "meals": [{"meal": "Breakfast", "foods": [{"food_name": "Moong", "portion_g": 100, "nutrition": nutrition}], "meal_total": nutrition}], "daily_total": nutrition}]},
            "lifestyle_plan": {"recommendations": []},
        }

    def _approval(self):
        return {"decision": "APPROVE", "qualification": "Physician", "clinic": "Test clinic", "qualified_reviewer": True, "doctor_name": "Dr Example", "registration_number": "REG-1", "signature": "Dr Example", "approved": True, "validation_reviewed": True}

    def test_assessment_report_has_no_diet_or_doctor_data(self):
        flow = TwoReportWorkflow(self._state())
        report = flow.create_assessment_report()
        self.assertEqual(report["metadata"]["report_type"], "AI_ASSESSMENT")
        self.assertNotIn("agent2_diet_data", report["assessment_data"])
        self.assertNotIn("doctor_review_data", report["assessment_data"])

    def test_final_report_is_blocked_without_explicit_approval(self):
        flow = TwoReportWorkflow(self._state())
        with self.assertRaises(PermissionError):
            flow.create_final_report()

    def test_content_change_invalidates_approval(self):
        flow = TwoReportWorkflow(self._state())
        flow.create_assessment_report()
        flow.approve(self._approval())
        self.assertTrue(flow.approval_is_current())
        flow.state["lifestyle_plan"] = {"recommendations": [{"personalized_recommendation": "Changed"}]}
        self.assertTrue(flow.invalidate_if_changed())
        self.assertFalse(flow.state["doctor_review"]["approved"])

    def test_final_report_is_version_bound_after_approval(self):
        flow = TwoReportWorkflow(self._state())
        flow.create_assessment_report()
        flow.approve(self._approval())
        report = flow.create_final_report()
        self.assertEqual(report["metadata"]["report_type"], "FINAL_PERSONALIZED")
        self.assertEqual(report["metadata"]["status"], "FINALIZED")


if __name__ == "__main__":
    unittest.main()
