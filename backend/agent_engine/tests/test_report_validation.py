import unittest

from agent_engine.services.report_validation import validate_report_state


class ReportValidationTests(unittest.TestCase):
    def _state(self):
        nutrition = {"calories": 100, "protein_g": 5, "carbs_g": 15, "fat_g": 2, "fiber_g": 3}
        return {
            "patient_profile": {"name": "Patient", "allergies": [], "foods_to_avoid": []},
            "prakriti_result": {
                "scores": {"Vata": 21, "Pitta": 0, "Kapha": 0},
                "percentages": {"Vata": 100.0, "Pitta": 0.0, "Kapha": 0.0},
                "primary_dosha": "Vata",
                "secondary_dosha": None,
                "constitution": "Vata",
                "assessment_method": "canonical_android_prakriti_result",
            },
            "vikriti_result": {
                "scores": {"Vata": 7, "Pitta": 21, "Kapha": 7},
                "dominant_dosha": "Pitta",
                "assessment_method": "canonical_android_vikriti_result",
            },
            "agni_result": {
                "category_scores": {"Mandagni": 0, "Vishamagni": 0, "Samagni": 11, "Tikshnagni": 0},
                "status": "Samagni",
                "assessment_method": "canonical_android_agni_result",
            },
            "meal_plan": {
                "days": [
                    {
                        "day": 1,
                        "meals": [
                            {
                                "meal": "Breakfast",
                                "foods": [{"food_name": "Moong dal", "nutrition": nutrition}],
                                "meal_total": nutrition,
                            }
                        ],
                        "daily_total": nutrition,
                    }
                ]
            },
        }

    def test_recalculates_plan_total_from_displayed_foods(self):
        snapshot = validate_report_state(self._state())
        self.assertEqual(snapshot["nutrition_total"]["calories"], 100.0)
        self.assertEqual(snapshot["status"], "REVIEW_REQUIRED")
        self.assertTrue(any("portion" in w for w in snapshot["warnings"]))

    def test_does_not_rescore_canonical_assessment_results(self):
        state = self._state()
        state["questionnaire_answers"] = {1: "Kapha"}
        state["vikriti_answers"] = {"V1": 5}
        state["agni_answers"] = {"A1": 1}
        state["prakriti_result"]["scores"]["Vata"] = 1

        snapshot = validate_report_state(state)

        self.assertNotIn("displayed scores do not match", " ".join(snapshot["failed_checks"]))
        self.assertEqual(snapshot["assessment_checks"]["prakriti"], "Preliminary AI-based Assessment - Doctor Verification Required")

    def test_rejects_restricted_ingredient_not_just_food_name(self):
        state = self._state()
        state["patient_profile"]["allergies"] = ["peanut"]
        state["meal_plan"]["days"][0]["meals"][0]["foods"][0]["ingredients"] = "moong dal, peanut"
        snapshot = validate_report_state(state)
        self.assertIn("Restricted food selected", " ".join(snapshot["failed_checks"]))

    def test_incomplete_canonical_assessments_are_rejected(self):
        state = self._state()
        state["prakriti_result"] = {}
        state["vikriti_result"] = {"scores": {"Vata": 1}}
        state["agni_result"] = {"category_scores": {"Samagni": 1}}

        snapshot = validate_report_state(state)

        self.assertIn("Prakriti assessment is incomplete.", snapshot["failed_checks"])
        self.assertIn("Vikriti assessment is incomplete.", snapshot["failed_checks"])
        self.assertIn("Agni assessment is incomplete.", snapshot["failed_checks"])


if __name__ == "__main__":
    unittest.main()
