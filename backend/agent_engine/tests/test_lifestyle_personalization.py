import unittest
from unittest.mock import patch

from agent_engine.agents.lifestyle import lifestyle_agent


class LifestylePersonalizationTests(unittest.TestCase):
    @patch("agent_engine.agents.lifestyle.get_lifestyle_rag_evidence")
    def test_recommendations_are_factor_specific_and_safety_labeled(self, retrieve):
        retrieve.return_value = [{"content": "daily routine and sleep guidance", "metadata": {"source_book": "VedAmrit Lifestyle RAG", "page": "12"}}]
        state = {
            "patient_profile": {"age": 35, "gender": "female", "goal": "digestive comfort", "activity_level": "low", "sleep_quality": "poor", "stress_level": "high", "allergies": ["peanut"], "food_intolerances": [], "foods_to_avoid": [], "health_conditions": []},
            "prakriti_result": {"primary_dosha": "Vata"}, "vikriti_result": {"dominant_dosha": "Vata"}, "agni_result": {"status": "Vishamagni"},
        }
        result = lifestyle_agent(state)["lifestyle_plan"]
        recommendation = result["recommendations"][0]
        self.assertEqual(recommendation["ai_generated_status"], "AI-GENERATED — DOCTOR REVIEW REQUIRED")
        self.assertIn("agni", retrieve.call_args.kwargs["patient_context"])
        self.assertNotIn("personalization_factors", result)
        self.assertNotIn("factors_used", recommendation)
        self.assertIn("patient_specific_reason", recommendation)
        self.assertIn("safety_consideration", recommendation)
        self.assertIn("Ayurvedic", recommendation["ayurvedic_basis"]["support"])


if __name__ == "__main__":
    unittest.main()
