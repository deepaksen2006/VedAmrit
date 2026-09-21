from typing import Any


def condition_agent(state: dict[str, Any]) -> dict:
    """
    MVP condition extension point.

    This agent records only information explicitly supplied by the user.
    It does not diagnose conditions or generate condition-specific advice.
    """
    patient_profile = state.get("patient_profile", {})

    health_conditions = patient_profile.get("health_conditions", [])
    allergies = patient_profile.get("allergies", [])
    food_exclusions = patient_profile.get("food_exclusions", [])

    if not isinstance(health_conditions, list):
        raise ValueError("health_conditions must be a list.")

    if not isinstance(allergies, list):
        raise ValueError("allergies must be a list.")

    if not isinstance(food_exclusions, list):
        raise ValueError("food_exclusions must be a list.")

    conditions_present = len(health_conditions) > 0

    warnings = []

    if conditions_present:
        warnings.append(
            "Health conditions were provided, but condition-specific "
            "dietary rules are not implemented in this MVP."
        )

    if allergies:
        warnings.append(
            "Allergies must be checked against selected food records "
            "by the safety agent before showing a final plan."
        )

    condition_context = {
        "conditions_present": conditions_present,
        "conditions": health_conditions,
        "allergies": allergies,
        "food_exclusions": food_exclusions,
        "implemented_condition_rules": False,
        "constraints": [],
        "warnings": warnings,
    }

    return {
        "condition_context": condition_context,
    }