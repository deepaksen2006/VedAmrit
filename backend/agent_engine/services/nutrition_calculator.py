import math
from typing import Any


NUTRIENT_FIELDS = (
    "calories",
    "protein_g",
    "carbohydrates_g",
    "fat_g",
    "fiber_g",
)


def to_valid_number(value: Any) -> float | None:
    """Return a finite numeric value, otherwise None."""
    if isinstance(value, bool):
        return None

    try:
        numeric_value = float(value)
    except (TypeError, ValueError):
        return None

    if not math.isfinite(numeric_value):
        return None

    return numeric_value


def calculate_energy_target(patient_profile: dict[str, Any]) -> dict[str, Any]:
    """
    Calculate target daily calorie and macronutrient intake using the
    Mifflin-St Jeor equation (Mifflin et al., 1990).

    Source:
    Mifflin MD, St Jeor ST, Hill LA, Scott BJ, Daugherty SA, Koh YO.
    A new predictive equation for resting energy expenditure in healthy individuals.
    Am J Clin Nutr. 1990;51(2):241-247.
    """
    age = patient_profile.get("age")
    gender = str(patient_profile.get("gender", "")).strip().lower()
    height_cm = patient_profile.get("height_cm")
    weight_kg = patient_profile.get("weight_kg")
    goal = str(patient_profile.get("goal", "general wellness")).strip().lower()

    if not age or not height_cm or not weight_kg:
        return {
            "target_calories": 2000.0,
            "target_protein_g": 60.0,
            "target_carbs_g": 250.0,
            "target_fat_g": 65.0,
            "target_fiber_g": 30.0,
            "calculation_basis": "Default baseline (missing profile metrics for BMR)",
        }

    # Mifflin-St Jeor BMR
    if gender in ("female", "f", "woman"):
        bmr = (10 * weight_kg) + (6.25 * height_cm) - (5 * age) - 161
    else:
        bmr = (10 * weight_kg) + (6.25 * height_cm) - (5 * age) + 5

    # Light activity multiplier (1.375)
    tdee = bmr * 1.375

    if "loss" in goal or "management" in goal or "reduce" in goal:
        target_calories = max(1200.0, tdee - 400.0)
    elif "gain" in goal or "muscle" in goal or "increase" in goal:
        target_calories = tdee + 400.0
    else:
        target_calories = tdee

    target_calories = round(target_calories, 1)

    # Macronutrient distribution: Protein ~18%, Carbs ~52%, Fat ~30%
    target_protein_g = round((target_calories * 0.18) / 4.0, 1)
    target_carbs_g = round((target_calories * 0.52) / 4.0, 1)
    target_fat_g = round((target_calories * 0.30) / 9.0, 1)
    target_fiber_g = 30.0

    return {
        "bmr": round(bmr, 1),
        "tdee": round(tdee, 1),
        "target_calories": target_calories,
        "target_protein_g": target_protein_g,
        "target_carbs_g": target_carbs_g,
        "target_fat_g": target_fat_g,
        "target_fiber_g": target_fiber_g,
        "calculation_basis": "Mifflin-St Jeor BMR (1990) with TDEE activity factor 1.375 and goal adjustment",
        "source": "Mifflin MD et al., Am J Clin Nutr. 1990;51(2):241-247.",
    }


def calculate_nutrition_summary(
    food_candidates: list[dict[str, Any]],
) -> dict[str, Any]:
    """
    Sum nutrition values that actually exist in the selected CSV rows.

    The values are not portion-adjusted because the current CSV has no
    serving-size or quantity information.
    """
    values: dict[str, float | None] = {}
    missing_by_nutrient: dict[str, list[str]] = {}

    for nutrient_name in NUTRIENT_FIELDS:
        total = 0.0
        missing_food_ids = []

        for food in food_candidates:
            nutrition = food.get("nutrition", {})
            numeric_value = to_valid_number(nutrition.get(nutrient_name))

            if numeric_value is None:
                missing_food_ids.append(
                    food.get("food_id", food.get("name", "unknown_food"))
                )
            else:
                total += numeric_value

        values[nutrient_name] = (
            round(total, 2) if not missing_food_ids else None
        )
        missing_by_nutrient[nutrient_name] = missing_food_ids

    return {
        "calculation_basis": (
            "Unadjusted sum of selected food-dataset row values"
        ),
        "serving_sizes_available": False,
        "values": values,
        "missing_by_nutrient": missing_by_nutrient,
    }