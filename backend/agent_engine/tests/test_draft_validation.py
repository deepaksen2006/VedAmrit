import unittest
from copy import deepcopy
from unittest.mock import patch
from agent_engine.agents.food_filter import FoodFilter
from agent_engine.agents.diet_planning import _calculate_portion, fit_meal_portions_to_target
from agent_engine.agents.lifestyle import lifestyle_agent
from agent_engine.contracts import parse_agent_engine_request
from agent_engine.integration_service import _service_response, _workflow_state
from agent_engine.tests.test_integration_service import valid_payload



class DraftValidationTests(unittest.TestCase):
    def test_serving_metadata_and_residual_calories_remain_valid(self):
        def food(name, calories, low, high):
            return {"food_name": name, "nutrition": {"calories": calories}, "serving": {"min_g": low, "max_g": high}}
        foods = [food("limited", 100, 20, 50), food("flexible", 100, 20, 500)]
        portions = fit_meal_portions_to_target(foods, 400)
        self.assertEqual(sum(item["nutrition"]["calories"] for item in portions), 400)
        self.assertEqual(portions[0]["max_serving_g"], 50)
        self.assertTrue(all(item["min_serving_g"] <= item["portion_g"] <= item["max_serving_g"] for item in portions))

    def test_actual_ingredients_override_incorrect_vegan_csv_label(self):
        foods = [{"food_id": "a", "food_name": "Test milk dish", "diet_type": "vegan", "ingredients": ["milk"], "allergens": []},
                 {"food_id": "b", "food_name": "Test lentils", "diet_type": "vegan", "ingredients": ["lentils"], "allergens": []}]
        allowed, rejected = FoodFilter().filter_foods(foods, {"dietary_preference": "vegan"})
        self.assertEqual([item["food_id"] for item in allowed], ["b"])
        self.assertEqual(len(rejected), 1)
        allowed, _ = FoodFilter().filter_foods(foods, {"dietary_preference": "vegetarian", "food_intolerances": ["milk"]})
        self.assertEqual([item["food_id"] for item in allowed], ["b"])
        allowed, _ = FoodFilter().filter_foods(foods, {"allergies": ["lentils"], "foods_to_avoid": ["milk"]})
        self.assertEqual(allowed, [])

    def test_final_review_does_not_hide_nutrition_failure(self):
        request = parse_agent_engine_request({**valid_payload(), "plan_days": 7})
        response = {"diet_plan": {"duration_days": 7, "days": []}, "lifestyle_plan": {"recommendations": []}}
        state = {"agent2_output": {"diet_plan": response["diet_plan"]}, "lifestyle_plan": response["lifestyle_plan"],
            "review_validation": {"status": "REVIEW_REQUIRED", "failed_checks": []},
            "validation_result": {"status": "REVIEW_REQUIRED", "valid": False, "issues": ["Unsafe portions"]}}
        result = _service_response(request, state, {})
        self.assertEqual(result["workflow"]["status"], "validation_failed")
        self.assertIn("Unsafe portions", result["validation"]["failed_checks"])
        self.assertFalse(result["validation"]["draft_view_allowed"])

    def test_lifestyle_real_retrieval_metadata_and_fallback_are_truthful(self):
        request = parse_agent_engine_request(valid_payload())
        state = _workflow_state(request, {"available": True})
        with patch("agent_engine.agents.lifestyle.get_lifestyle_rag_evidence", return_value=[
            {"content": "meal routine", "metadata": {"source_book": "fixture.pdf", "page": 0}}]):
            result = lifestyle_agent(state)
        self.assertEqual(result["rag_status"]["status"], "retrieved")
        self.assertEqual(result["rag_status"]["evidence_count"], 1)
        self.assertTrue(result["lifestyle_plan"]["recommendations"])
        self.assertEqual(result["lifestyle_plan"]["recommendations"][0]["ayurvedic_basis"]["page"], 0)
        with patch("agent_engine.agents.lifestyle.get_lifestyle_rag_evidence", side_effect=RuntimeError("unavailable")):
            fallback = lifestyle_agent(state)
        self.assertEqual(fallback["rag_status"]["status"], "retrieval_failed")
        self.assertTrue(fallback["rag_status"]["fallback"])
        self.assertTrue(all(item["ayurvedic_basis"]["source"] == "Not provided" for item in fallback["lifestyle_plan"]["recommendations"]))
