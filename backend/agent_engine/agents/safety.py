from typing import Any

from agent_engine.services.nutrition_calculator import (
    NUTRIENT_FIELDS,
    calculate_nutrition_summary,
)


EXPECTED_MEALS = {"breakfast", "lunch", "dinner"}


def normalize_text(value: str) -> str:
    return " ".join(str(value).strip().lower().split())


def nutrition_values_match(
    expected: dict[str, Any],
    actual: dict[str, Any],
) -> bool:
    """Compare calculated nutrition values safely."""
    for nutrient in NUTRIENT_FIELDS:
        expected_value = expected["values"].get(nutrient)
        actual_value = actual.get("values", {}).get(nutrient)

        if expected_value != actual_value:
            return False

    return True


def safety_fact_checker_agent(state: dict[str, Any]) -> dict:
    """
    Validates data consistency and supported patient constraints.

    Serious data errors result in REVIEW_REQUIRED.
    This agent does not silently repair an invalid plan.
    """
    meal_plan = state.get("meal_plan", {})
    food_candidates = state.get("food_candidates", [])
    patient_profile = state.get("patient_profile", {})
    condition_context = state.get("condition_context", {})
    diet_validation = state.get("validation_result", {})

    warnings = []
    failed_checks = []

    checks = {
        "food_data": "PASS",
        "meal_structure": "PASS",
        "nutrition": "PASS",
        "patient_constraints": "PASS",
        "ayurvedic_evidence": "PASS",
    }

    candidate_ids = {
        food.get("food_id")
        for food in food_candidates
        if food.get("food_id")
    }

    candidate_names = {
        normalize_text(
            food.get("name") or food.get("food_name", "")
        )
        for food in food_candidates
    }

    ALLOWED_MEAL_TYPES = {"breakfast", "lunch", "snack", "dinner"}

    duration_days = meal_plan.get("duration_days")
    if not isinstance(duration_days, int) or not (1 <= duration_days <= 7):
        checks["meal_structure"] = "FAIL"
        failed_checks.append("Invalid meal-plan duration.")

    plan_days = meal_plan.get("days", [])

    if len(plan_days) != duration_days:
        checks["meal_structure"] = "FAIL"
        failed_checks.append(
            "Meal-plan day count does not match the requested duration."
        )

    exclusions = {
        normalize_text(item)
        for item in patient_profile.get("food_exclusions", [])
    }

    allergies = {
        normalize_text(item)
        for item in patient_profile.get("allergies", [])
    }

    selected_foods = []

    for day_plan in plan_days:
        meals = day_plan.get("meals", [])

        if len(meals) != 4 or {str(meal.get("meal", "")).lower() for meal in meals} != ALLOWED_MEAL_TYPES:
            checks["meal_structure"] = "FAIL"
            failed_checks.append(
                f"Day {day_plan.get('day')} has invalid meal count."
            )
            continue

        day_foods = []

        for meal in meals:
            meal_type = str(
                meal.get("meal") or meal.get("type") or ""
            ).strip().lower()

            if meal_type not in ALLOWED_MEAL_TYPES:
                checks["meal_structure"] = "FAIL"
                failed_checks.append(
                    f"Day {day_plan.get('day')} has invalid meal type: {meal_type}."
                )

            foods = meal.get("foods", [])

            if not foods:
                checks["meal_structure"] = "FAIL"
                failed_checks.append(
                    f"{meal_type} has no selected foods."
                )

            for food in foods:
                food_id = food.get("food_id")
                food_name = normalize_text(
                    food.get("food_name") or food.get("name", "")
                )

                if candidate_ids and food_id and food_id not in candidate_ids:
                    checks["food_data"] = "FAIL"
                    failed_checks.append(
                        f"Food ID '{food_id}' is not an approved candidate."
                    )

                if candidate_names and food_name and food_name not in candidate_names:
                    checks["food_data"] = "FAIL"
                    failed_checks.append(
                        f"Food '{food_name}' is not in the dataset candidates."
                    )

                if food_name in exclusions:
                    checks["patient_constraints"] = "FAIL"
                    failed_checks.append(
                        f"Excluded food selected: {food_name}."
                    )

                if food_name in allergies:
                    checks["patient_constraints"] = "FAIL"
                    failed_checks.append(
                        f"Allergy-matched food selected: {food_name}."
                    )

                day_foods.append(food)
                selected_foods.append(food)

    if patient_profile.get("allergies"):
        checks["patient_constraints"] = "REVIEW_REQUIRED"
        warnings.append(
            "Allergy filtering uses recorded ingredient/allergen metadata; "
            "label accuracy and practitioner review remain necessary."
        )

    dataset_has_diet_type = bool(food_candidates) and all(
        "diet_type" in food
        for food in food_candidates
    )

    if (
        patient_profile.get("dietary_preference", "unspecified")
        != "unspecified"
        and not dataset_has_diet_type
    ):
        checks["patient_constraints"] = "REVIEW_REQUIRED"
        warnings.append(
            "Dietary preference cannot yet be verified because the CSV "
            "does not include a diet-type field."
        )

    if condition_context.get("conditions_present"):
        checks["patient_constraints"] = "REVIEW_REQUIRED"
        warnings.append(
            "Condition-specific dietary safety rules are not implemented; "
            "practitioner review is required."
        )

    ayurveda_context = state.get("ayurveda_context")

    if (
        ayurveda_context is not None
        and ayurveda_context.get("supported") is False
    ):
        checks["ayurvedic_evidence"] = "REVIEW_REQUIRED"
        warnings.append(
            "Ayurvedic evidence was insufficient for one or more requested claims."
        )

    has_failure = "FAIL" in checks.values()
    needs_review = "REVIEW_REQUIRED" in checks.values()

    if diet_validation.get("valid") is False:
        checks["nutrition"] = "REVIEW_REQUIRED"
        needs_review = True
        failed_checks.extend(
            diet_validation.get("issues", [])
        )

    status = "PASS"

    if has_failure:
        status = "FAIL"
    elif needs_review:
        status = "REVIEW_REQUIRED"

    validation_result = {
        **diet_validation,
        "status": status,
        "overall_status": (
            "REVIEW_REQUIRED"
            if status == "REVIEW_REQUIRED"
            else diet_validation.get("overall_status", "PASS")
        ),
        "checks": checks,
        "warnings": warnings,
        "failed_checks": failed_checks,
        "validated_food_count": len(selected_foods),
    }

    return {
        "validation_result": validation_result,
    }
