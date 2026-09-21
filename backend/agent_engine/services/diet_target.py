from typing import Any


def calculate_diet_target(
    patient_profile: dict[str, Any],
) -> dict[str, Any]:
    """
    Calculate a simple daily nutrition target.

    Adult users:
        Uses Mifflin-St Jeor + activity factor.

    Users under 18:
        Does not calculate an adult calorie deficit/surplus.
        Returns a pediatric review flag instead.
    """

    age = patient_profile.get("age")
    gender = (
        patient_profile.get("gender") or ""
    ).lower()

    weight = patient_profile.get("weight_kg")
    height = patient_profile.get("height_cm")

    goal = (
        patient_profile.get("goal") or ""
    ).lower()

    # --------------------------------------------------
    # AGE CHECK
    # --------------------------------------------------

    if age is not None and age < 18:

        return {
            "status": "age_specific_guidance_required",
            "calories": None,
            "protein_g": None,
            "carbs_g": None,
            "fat_g": None,
            "fiber_g": None,
            "message": (
                "User is under 18. Adult calorie "
                "equations and weight-loss targets "
                "are not used."
            ),
        }

    # --------------------------------------------------
    # MISSING METRICS
    # --------------------------------------------------

    if weight is None or height is None or age is None:

        return {
            "status": "general_target",
            "calories": 2000,
            "protein_g": 75,
            "carbs_g": 275,
            "fat_g": 65,
            "fiber_g": 30,
            "message": (
                "Basic general nutrition target used "
                "because age, height, or weight is missing."
            ),
        }

    # --------------------------------------------------
    # MIFflin-ST JEOR
    # --------------------------------------------------

    if gender == "female":

        bmr = (
            10 * weight
            + 6.25 * height
            - 5 * age
            - 161
        )

    else:

        bmr = (
            10 * weight
            + 6.25 * height
            - 5 * age
            + 5
        )

    # Moderate/light activity assumption
    activity_factor = 1.375

    tdee = bmr * activity_factor

    # --------------------------------------------------
    # GOAL ADJUSTMENT
    # --------------------------------------------------

    if "weight loss" in goal:

        calories = tdee - 300

    elif "weight gain" in goal:

        calories = tdee + 300

    else:

        calories = tdee

    calories = max(calories, 1200)

    # --------------------------------------------------
    # MACRO TARGETS
    # --------------------------------------------------

    protein_g = weight * 1.2

    protein_calories = protein_g * 4

    fat_calories = calories * 0.25

    fat_g = fat_calories / 9

    remaining_calories = (
        calories
        - protein_calories
        - fat_calories
    )

    carbs_g = remaining_calories / 4

    fiber_g = 30

    return {
        "status": "calculated",
        "calories": round(calories, 1),
        "protein_g": round(protein_g, 1),
        "carbs_g": round(carbs_g, 1),
        "fat_g": round(fat_g, 1),
        "fiber_g": fiber_g,
        "bmr": round(bmr, 1),
        "tdee": round(tdee, 1),
        "activity_factor": activity_factor,
        "goal": goal,
    }