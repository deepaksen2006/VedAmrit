import unittest

from agent_engine.assessment_adapter import (
    adapt_agni_result,
    adapt_prakriti_result,
    adapt_vikriti_result,
    canonical_assessment_state_agent,
)


class CanonicalAssessmentAdapterTests(unittest.TestCase):
    def test_canonical_prakriti_passes_through_unchanged(self):
        result = {
            "prakritiType": "Vata-Pitta",
            "primaryDosha": "Vata",
            "secondaryDosha": "Pitta",
            "scores": {"Vata": 11, "Pitta": 10, "Kapha": 0},
            "percentages": {"Vata": 52.4, "Pitta": 47.6, "Kapha": 0.0},
        }

        adapted = adapt_prakriti_result(result)

        self.assertEqual(adapted["constitution"], "Vata-Pitta")
        self.assertEqual(adapted["primary_dosha"], "Vata")
        self.assertEqual(adapted["secondary_dosha"], "Pitta")
        self.assertEqual(adapted["scores"], result["scores"])

    def test_canonical_vikriti_passes_through_unchanged(self):
        result = {
            "dominantDosha": "Pitta",
            "scores": {"Vata": 7, "Pitta": 23, "Kapha": 9},
            "severities": {"Vata": "Low", "Pitta": "Moderate", "Kapha": "Low"},
        }

        adapted = adapt_vikriti_result(result)

        self.assertEqual(adapted["dominant_dosha"], "Pitta")
        self.assertEqual(adapted["scores"], result["scores"])
        self.assertEqual(adapted["severities"], result["severities"])

    def test_canonical_agni_passes_through_unchanged(self):
        result = {
            "dominantAgni": "Vishamagni",
            "scores": {"Mandagni": 2, "Vishamagni": 5, "Samagni": 3, "Tikshnagni": 1},
        }

        adapted = adapt_agni_result(result)

        self.assertEqual(adapted["status"], "Vishamagni")
        self.assertEqual(adapted["category_scores"], result["scores"])

    def test_python_does_not_rescore_questionnaire_answers(self):
        state = canonical_assessment_state_agent(
            {
                "patient_profile": {"name": "Patient"},
                "questionnaire_answers": {1: "Kapha"},
                "vikriti_answers": {"V1": 5},
                "agni_answers": {"A1": 1},
                "prakriti_result": {
                    "prakritiType": "Vata",
                    "primaryDosha": "Vata",
                    "scores": {"Vata": 21, "Pitta": 0, "Kapha": 0},
                },
                "vikriti_result": {
                    "dominantDosha": "Pitta",
                    "scores": {"Vata": 7, "Pitta": 21, "Kapha": 7},
                },
                "agni_result": {
                    "dominantAgni": "Samagni",
                    "scores": {"Samagni": 11},
                },
            }
        )

        self.assertEqual(state["prakriti_result"]["constitution"], "Vata")
        self.assertEqual(state["vikriti_result"]["dominant_dosha"], "Pitta")
        self.assertEqual(state["agni_result"]["status"], "Samagni")
        self.assertEqual(state["assessment_source"], "android_canonical_results")

    def test_malformed_canonical_assessment_data_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "incomplete"):
            canonical_assessment_state_agent(
                {
                    "patient_profile": {"name": "Patient"},
                    "prakriti_result": "not a dictionary",
                    "vikriti_result": {"dominantDosha": "Pitta"},
                    "agni_result": {"dominantAgni": "Samagni"},
                }
            )

    def test_missing_required_assessment_fields_are_rejected(self):
        with self.assertRaisesRegex(ValueError, "prakriti_result"):
            canonical_assessment_state_agent(
                {
                    "patient_profile": {"name": "Patient"},
                    "prakriti_result": {"scores": {"Vata": 21}},
                    "vikriti_result": {"dominantDosha": "Pitta"},
                    "agni_result": {"dominantAgni": "Samagni"},
                }
            )


if __name__ == "__main__":
    unittest.main()
