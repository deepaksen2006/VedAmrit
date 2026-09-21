from typing import Any


def normalize_dietary_preference(value: Any) -> str:
    """
    Normalize user dietary preference string.
    Supported normalized outputs: 'Vegetarian', 'Vegan', 'Non-vegetarian', 'unspecified'.
    """
    if not value or str(value).strip().lower() in ("none", "null", "undefined", ""):
        return "unspecified"

    val_clean = str(value).strip().lower()

    if val_clean in ("vegetarian", "vegetrain", "veg", "pure veg", "lacto-vegetarian"):
        return "Vegetarian"

    if val_clean in ("vegan", "strict veg", "plant-based"):
        return "Vegan"

    if val_clean in ("non-vegetarian", "non-veg", "nonveg", "non vegetarian"):
        return "Non-vegetarian"

    if val_clean in ("eggetarian", "egg-vegetarian"):
        return "Eggetarian"

    return str(value).strip().title()


def normalize_goal(value: Any) -> str:
    """
    Normalize user health goal string.
    Supported normalized outputs: 'weight_management', 'weight_loss', 'weight_gain',
    'general_wellness', 'digestion', 'unspecified'.
    """
    if not value or str(value).strip().lower() in ("none", "null", "undefined", ""):
        return "unspecified"

    val_clean = str(value).strip().lower()

    if any(kw in val_clean for kw in ("loss", "reduce", "weight_loss", "slimming")):
        return "weight_loss"

    if any(kw in val_clean for kw in ("gain", "muscle", "weight_gain", "bulking")):
        return "weight_gain"

    if any(kw in val_clean for kw in ("management", "mangement", "maintain", "weight_management", "weight control")):
        return "weight_management"

    if any(kw in val_clean for kw in ("digest", "gut", "agni", "acidity")):
        return "digestion"

    if any(kw in val_clean for kw in ("wellness", "health", "fitness", "general")):
        return "general_wellness"

    return val_clean.replace(" ", "_")


def normalize_vikriti_result(vikriti_data: dict[str, Any]) -> dict[str, Any]:
    """
    Normalize Vikriti scoring results, detecting and explicitly flagging score ties.
    Prevents false unique dominance assignments when scores are tied.
    """
    scores = vikriti_data.get("scores", {"Vata": 0, "Pitta": 0, "Kapha": 0})
    if not isinstance(scores, dict) or not scores:
        return {
            "dominant": ["Vata"],
            "tie": False,
            "scores": {"Vata": 0, "Pitta": 0, "Kapha": 0},
        }

    max_score = max(scores.values())
    dominant_doshas = [dosha for dosha, score in scores.items() if score == max_score]

    is_tie = len(dominant_doshas) > 1

    return {
        "dominant": dominant_doshas,
        "primary": dominant_doshas[0],
        "tie": is_tie,
        "max_score": max_score,
        "scores": scores,
    }


def normalize_patient_profile(profile_data: dict[str, Any]) -> dict[str, Any]:
    """
    Normalize patient profile input dictionary.
    """
    if not isinstance(profile_data, dict):
        profile_data = {}

    name = str(profile_data.get("name", "")).strip()

    age = profile_data.get("age")
    if age is not None:
        try:
            age = int(age)
            if age <= 0:
                age = None
        except (ValueError, TypeError):
            age = None

    gender = str(profile_data.get("gender", "")).strip().lower()
    blood_group = str(profile_data.get("blood_group", "")).strip()

    height_cm = profile_data.get("height_cm")
    if height_cm is not None:
        try:
            height_cm = float(height_cm)
            if height_cm <= 0:
                height_cm = None
        except (ValueError, TypeError):
            height_cm = None

    weight_kg = profile_data.get("weight_kg")
    if weight_kg is not None:
        try:
            weight_kg = float(weight_kg)
            if weight_kg <= 0:
                weight_kg = None
        except (ValueError, TypeError):
            weight_kg = None

    dietary_preference = normalize_dietary_preference(profile_data.get("dietary_preference"))

    allergies = profile_data.get("allergies", [])
    if not isinstance(allergies, list):
        allergies = []
    allergies = [str(item).strip().lower() for item in allergies if str(item).strip()]

    foods_to_avoid = profile_data.get("foods_to_avoid", [])
    if not isinstance(foods_to_avoid, list):
        foods_to_avoid = []
    foods_to_avoid = [str(item).strip().lower() for item in foods_to_avoid if str(item).strip()]

    food_exclusions = profile_data.get("food_exclusions", [])
    if not isinstance(food_exclusions, list):
        food_exclusions = []
    food_exclusions = [str(item).strip().lower() for item in food_exclusions if str(item).strip()]

    combined_exclusions = list(dict.fromkeys(foods_to_avoid + food_exclusions))

    health_conditions = profile_data.get("health_conditions", [])
    if not isinstance(health_conditions, list):
        health_conditions = []
    health_conditions = [str(item).strip() for item in health_conditions if str(item).strip()]

    goal = normalize_goal(profile_data.get("goal"))

    return {
        # Preserve supplied general/legacy clinical fields. Never merge stale user_input
        # back over this canonical profile or silently discard questionnaire answers.
        **{key: value for key, value in profile_data.items()
           if key not in {"season", "current_season", "seasonal_guidance"}},
        "name": name,
        "age": age,
        "gender": gender,
        "blood_group": blood_group,
        "height_cm": height_cm,
        "weight_kg": weight_kg,
        "dietary_preference": dietary_preference,
        "allergies": allergies,
        "foods_to_avoid": foods_to_avoid,
        "food_exclusions": combined_exclusions,
        "health_conditions": health_conditions,
        "goal": goal,
    }
