"""Transient Agent 3 projection of the existing shared DietPlanningState."""
from agent_engine.services.report_validation import selected_diet


LIFESTYLE_PROFILE_FIELDS = (
    "age", "gender", "goal", "dietary_preference", "activity_level",
    "sleep_information", "sleep_quality", "stress_level", "symptoms",
    "allergies", "food_intolerances", "medicine_allergies", "foods_to_avoid",
    "health_conditions", "medication_restrictions", "pregnancy_information",
    "current_medicines", "doctor_restrictions", "digestion", "appetite",
)


def project_lifestyle_input(patient_state):
    profile = patient_state.get("patient_profile") or patient_state.get("user_input") or {}
    doctor = patient_state.get("doctor_review") or {}
    diet = selected_diet(patient_state)
    categories = list(dict.fromkeys(meal["meal"] for day in diet.get("days", [])
                                   for meal in day.get("meals", []) if meal.get("meal")))
    return {
        "patient_profile": {key: profile[key] for key in LIFESTYLE_PROFILE_FIELDS if key in profile},
        "prakriti_result": {key: (patient_state.get("prakriti_result") or {}).get(key)
                            for key in ("constitution", "primary_dosha")},
        "vikriti_result": {"dominant_dosha": (patient_state.get("vikriti_result") or {}).get("dominant_dosha")},
        "agni_result": {"status": (patient_state.get("agni_result") or {}).get("status")},
        "disease_symptoms": {key: value for key, value in (patient_state.get("disease_symptoms") or {}).items() if value},
        "doctor_review": {key: doctor[key] for key in ("confirmed_prakriti", "confirmed_vikriti", "confirmed_agni", "food_restrictions") if key in doctor},
        "meal_plan": {"days": [{"meals": [{"meal": category} for category in categories]}]},
    }
