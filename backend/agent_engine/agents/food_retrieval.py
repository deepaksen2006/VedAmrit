from pathlib import Path
from typing import Any

from agent_engine.services.food_repository import FoodRepository
from agent_engine.services.food_restrictions import matches_food_restriction


PROJECT_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_CSV_PATH = PROJECT_ROOT / "data" / "demo_foods.csv"

NON_VEG_KEYWORDS = {
    "beef", "shrimp", "lobster", "fish", "mutton", "chicken", "pork",
    "egg", "meat", "salmon", "tuna", "prawn", "crab", "squid", "anchovy",
    "turkey", "lamb", "duck", "bacon", "ham",
}

DAIRY_KEYWORDS = {
    "milk", "cheese", "ghee", "butter", "curd", "paneer", "goat cheese",
    "honey", "yogurt", "malai", "dahi", "cream",
}


def normalize_food_name(value: str) -> str:
    return " ".join(str(value).strip().lower().split())


def is_non_vegetarian(food_name: str) -> bool:
    name_norm = normalize_food_name(food_name)
    return any(keyword in name_norm for keyword in NON_VEG_KEYWORDS)


def is_non_vegan(food_name: str) -> bool:
    name_norm = normalize_food_name(food_name)
    if is_non_vegetarian(food_name):
        return True
    return any(keyword in name_norm for keyword in DAIRY_KEYWORDS)


def score_food_candidate(
    candidate: dict[str, Any],
    primary_dosha: str,
    agni_status: str,
    goal: str,
    health_conditions: list[str],
) -> float:
    score = 0.0
    ayurvedic = candidate.get("ayurvedic_attributes", {})

    if primary_dosha:
        dosha_key = f"{primary_dosha.lower()}_effect"
        dosha_effect = str(ayurvedic.get(dosha_key, "")).strip().lower()
        if dosha_effect == "balancing":
            score += 5.0
        elif dosha_effect == "soothing":
            score += 3.0
        elif dosha_effect == "may increase":
            score -= 5.0

    agni_suitability = str(
        ayurvedic.get("agni_suitability", "")
    ).strip().lower()
    agni_status_lower = str(agni_status).strip().lower()
    if agni_status_lower and agni_status_lower in agni_suitability:
        score += 3.0
    if "easy to digest" in agni_suitability:
        score += 1.0

    best_for = [
        str(item).strip().lower()
        for item in candidate.get("best_for", [])
    ]
    goal_lower = str(goal).strip().lower()
    if goal_lower and goal_lower in best_for:
        score += 3.0

    avoid_or_limit_when = [
        str(item).strip().lower()
        for item in candidate.get("avoid_or_limit_when", [])
    ]
    for condition in health_conditions:
        condition_lower = str(condition).strip().lower()
        if condition_lower and condition_lower in avoid_or_limit_when:
            score -= 5.0

    return score


def food_retrieval_agent(state: dict[str, Any]) -> dict[str, Any]:
    prakriti_result = state.get("prakriti_result", {})
    constitution = prakriti_result.get(
        "constitution",
        state.get("constitution"),
    )
    primary_dosha = prakriti_result.get("primary_dosha") or "Vata"

    if not constitution:
        raise ValueError(
            "A Prakriti constitution is required before food retrieval."
        )

    patient_profile = state.get("patient_profile", {})
    exclusions = patient_profile.get("food_exclusions", [])
    foods_to_avoid = patient_profile.get("foods_to_avoid", [])
    allergies = patient_profile.get("allergies", [])
    dietary_preference = str(
        patient_profile.get("dietary_preference", "unspecified")
    ).strip().lower()
    health_conditions = patient_profile.get("health_conditions", [])
    goal = patient_profile.get("goal", "")

    agni_result = state.get("agni_result", {})
    agni_status = agni_result.get("status") or "Samagni"

    def normalize_list(value: Any) -> list[str]:
        if value is None:
            return []
        if isinstance(value, str):
            return [item.strip() for item in value.split(",") if item.strip()]
        if isinstance(value, (list, tuple, set)):
            return [str(item).strip() for item in value if str(item).strip()]
        return []

    exclusions = normalize_list(exclusions)
    foods_to_avoid = normalize_list(foods_to_avoid)
    allergies = normalize_list(allergies)
    health_conditions = normalize_list(health_conditions)
    all_exclusions = list(dict.fromkeys(exclusions + foods_to_avoid))

    csv_path = Path(state.get("food_dataset_path", DEFAULT_CSV_PATH))
    repository = FoodRepository(csv_path)
    foods = repository.get_all_foods()

    selected_candidates: list[dict[str, Any]] = []
    excluded_log: list[dict[str, Any]] = []

    for food in foods:
        food_name = str(food.get("food_name", ""))
        excluded_by: str | None = None

        for exclusion in all_exclusions:
            if matches_food_restriction(food, exclusion):
                excluded_by = f"Explicit food exclusion ({exclusion})"
                break

        if not excluded_by:
            for allergy in allergies:
                if matches_food_restriction(food, allergy):
                    excluded_by = f"Allergy match ({allergy})"
                    break

        if not excluded_by:
            if dietary_preference in {"vegetarian", "veg"}:
                if is_non_vegetarian(food_name):
                    excluded_by = (
                        "Dietary preference violation "
                        "(non-vegetarian item)"
                    )
            elif dietary_preference == "vegan" and is_non_vegan(food_name):
                excluded_by = (
                    "Dietary preference violation (non-vegan item)"
                )

        if excluded_by:
            excluded_log.append(
                {
                    "food_id": food.get("food_id"),
                    "food_name": food_name,
                    "reason": excluded_by,
                }
            )
            continue

        candidate = dict(food)
        candidate["ayurvedic_rank_score"] = score_food_candidate(
            candidate=candidate,
            primary_dosha=primary_dosha,
            agni_status=agni_status,
            goal=goal,
            health_conditions=health_conditions,
        )
        selected_candidates.append(candidate)

    selected_candidates.sort(
        key=lambda item: item["ayurvedic_rank_score"],
        reverse=True,
    )

    if not selected_candidates:
        raise ValueError(
            "No compatible food candidates remain after applying patient "
            "constraints."
        )

    return {
        "food_candidates": selected_candidates,
        "food_retrieval_context": {
            "constitution": constitution,
            "primary_dosha": primary_dosha,
            "agni_status": agni_status,
            "candidate_count": len(selected_candidates),
            "excluded_food_count": len(excluded_log),
            "excluded_foods_log": excluded_log,
            "dietary_preference_applied": dietary_preference,
            "source": "data/demo_foods.csv",
        },
    }
