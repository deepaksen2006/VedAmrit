import unittest
from unittest.mock import patch

from agent_engine.integration_service import generate_personalized_plan


def valid_payload():
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
            "foods_to_avoid": ["brinjal"],
            "health_conditions": [],
            "medicine_allergies": [],
            "medication_restrictions": [],
            "doctor_restrictions": [],
            "pregnancy_information": "not_applicable",
            "sleep_quality": "light sleep",
            "stress_level": "medium",
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
                "answers": {"V1": "not_at_all", "P1": "very_often", "K1": "somewhat"},
                "scores": {"Vata": 3, "Pitta": 19, "Kapha": 6},
                "severities": {"Vata": "Low", "Pitta": "Moderate", "Kapha": "Low"},
                "percentages": {"Vata": 10.7, "Pitta": 67.9, "Kapha": 21.4},
                "dominantDosha": "Pitta",
                "completedAtMillis": 1720000000000,
                "prakritiType": "Vata-Pitta",
            },
            "agni": {
                "answers": {"1": "mandagni", "2": "vishamagni"},
                "scores": {"Mandagni": 2, "Vishamagni": 1, "Samagni": 8, "Tikshnagni": 0},
                "dominantAgni": "Samagni",
                "mixedAgniTypes": [],
                "isMixed": False,
                "completedAtMillis": 1720000001000,
            },
        },
        "plan_days": 3,
    }


def graph_result():
    return {
        "agent2_output": {
            "diet_plan": {"duration_days": 3, "days": []},
            "validation": {"status": "PASS"},
        },
        "lifestyle_plan": {"recommendations": [], "safety_flags": []},
        "validation_result": {"status": "PASS"},
        "workflow_status": "PENDING_DOCTOR_REVIEW",
        "session_id": "session-1",
        "patient_id": "patient-1",
        "final_response": "DOCTOR VERIFICATION REQUIRED",
    }


class IntegrationServiceContractTests(unittest.TestCase):
    def test_canonical_assessment_results_pass_through_unchanged(self):
        payload = valid_payload()
        with patch("agent_engine.integration_service.graph.invoke", return_value=graph_result()):
            response = generate_personalized_plan(payload)

        self.assertEqual(response["assessment"]["prakriti"], payload["assessment"]["prakriti"])
        self.assertEqual(response["assessment"]["vikriti"], payload["assessment"]["vikriti"])
        self.assertEqual(response["assessment"]["agni"], payload["assessment"]["agni"])
        self.assertEqual(response["assessment"]["source"], "android_canonical_results")

    def test_python_does_not_rescore_questionnaire_answers(self):
        payload = valid_payload()
        payload["assessment"]["prakriti"]["answers"] = {"fake": "Kapha"}
        payload["assessment"]["vikriti"]["answers"] = {"V1": "very_often"}
        payload["assessment"]["agni"]["answers"] = {"1": "tikshnagni"}

        with patch("agent_engine.integration_service.graph.invoke", return_value=graph_result()) as invoke:
            response = generate_personalized_plan(payload)

        state = invoke.call_args.args[0]
        self.assertEqual(state["prakriti_result"]["constitution"], "Vata-Pitta")
        self.assertEqual(state["vikriti_result"]["dominant_dosha"], "Pitta")
        self.assertEqual(state["agni_result"]["status"], "Samagni")
        self.assertEqual(response["workflow"]["assessment_source"], "android_canonical_results")

    def test_malformed_canonical_assessment_data_is_rejected(self):
        payload = valid_payload()
        payload["assessment"]["prakriti"] = "not-json-object"

        with patch("agent_engine.integration_service.graph.invoke") as invoke:
            response = generate_personalized_plan(payload)

        self.assertEqual(response["workflow"]["status"], "error")
        self.assertEqual(response["workflow"]["errors"][0]["code"], "malformed_canonical_assessment")
        invoke.assert_not_called()

    def test_missing_required_assessment_fields_are_rejected(self):
        payload = valid_payload()
        del payload["assessment"]["agni"]["dominantAgni"]

        with patch("agent_engine.integration_service.graph.invoke") as invoke:
            response = generate_personalized_plan(payload)

        self.assertEqual(response["workflow"]["status"], "error")
        self.assertEqual(response["workflow"]["errors"][0]["code"], "missing_required_assessment_field")
        invoke.assert_not_called()

    def test_missing_required_patient_data_is_rejected(self):
        payload = valid_payload()
        del payload["patient_profile"]["dietary_preference"]

        with patch("agent_engine.integration_service.graph.invoke") as invoke:
            response = generate_personalized_plan(payload)

        self.assertEqual(response["workflow"]["status"], "error")
        self.assertEqual(response["workflow"]["errors"][0]["code"], "missing_required_field")
        self.assertEqual(response["workflow"]["errors"][0]["field"], "patient_profile.dietary_preference")
        invoke.assert_not_called()

    def test_dietary_filter_inputs_are_preserved_for_agent_state(self):
        payload = valid_payload()

        with patch("agent_engine.integration_service.graph.invoke", return_value=graph_result()) as invoke:
            generate_personalized_plan(payload)

        profile = invoke.call_args.args[0]["patient_profile"]
        self.assertEqual(profile["dietary_preference"], "vegetarian")
        self.assertEqual(profile["foods_to_avoid"], ["brinjal"])
        self.assertEqual(profile["health_conditions"], [])
        self.assertEqual(profile["activity_level"], "moderate")

    def test_output_contract_has_stable_top_level_sections(self):
        payload = valid_payload()
        with patch("agent_engine.integration_service.graph.invoke", return_value=graph_result()):
            response = generate_personalized_plan(payload)

        self.assertEqual(
            set(response.keys()),
            {"assessment", "diet_plan", "lifestyle_plan", "validation", "workflow"},
        )
        self.assertIn("rag", response["workflow"])
        self.assertIn("doctor_review_required", response["workflow"])

    def test_android_assessment_profile_shape_is_accepted(self):
        payload = valid_payload()
        payload["assessment_profile"] = {
            "prakriti_result": payload["assessment"]["prakriti"],
            "vikriti_result": payload["assessment"]["vikriti"],
            "agni_result": payload["assessment"]["agni"],
        }
        del payload["assessment"]

        with patch("agent_engine.integration_service.graph.invoke", return_value=graph_result()):
            response = generate_personalized_plan(payload)

        self.assertEqual(response["assessment"]["agni"]["dominantAgni"], "Samagni")

    def test_rag_unavailable_is_explicit_not_silent(self):
        payload = valid_payload()
        with patch("agent_engine.integration_service.graph.invoke", return_value=graph_result()):
            response = generate_personalized_plan(payload)

        rag = response["workflow"]["rag"]
        self.assertIn("available", rag)
        if not rag["available"]:
            self.assertEqual(rag["error"]["code"], "rag_vector_store_unavailable")
            self.assertIn("fallback", rag)


if __name__ == "__main__":
    unittest.main()
