from pathlib import Path
from typing import Any
from itertools import combinations, chain

from agent_engine.agents.diet_context import prepare_diet_context
from agent_engine.agents.food_filter import FoodFilter
from agent_engine.agents.food_ranking import FoodRanker
from agent_engine.agents.diet_validation import DietValidator
from agent_engine.agents.diet_replanner import DietReplanner
from agent_engine.services.food_repository import FoodRepository
from agent_engine.services.nutrition_calculator import calculate_energy_target


MAX_RETRIES = 3

PROJECT_ROOT = Path(__file__).resolve().parents[1]

DEFAULT_CSV_PATH = PROJECT_ROOT / "data" / "demo_foods.csv"


MEAL_STRUCTURE = (
    "Breakfast",
    "Lunch",
    "Snack",
    "Dinner",
)


MEAL_CALORIE_DISTRIBUTION = {
    "Breakfast": 0.25,
    "Lunch": 0.35,
    "Snack": 0.10,
    "Dinner": 0.30,
}


def _calculate_portion(
    food: dict[str, Any],
    target_calories: float,
) -> dict[str, Any]:
    """
    Calculate a practical portion for one food
    using nutrition values stored per 100g.
    """

    nutrition = food.get("nutrition", {})
    serving = food.get("serving", {})

    calories_per_100g = float(
        nutrition.get("calories", 0) or 0
    )

    min_g = float(
        serving.get("min_g", 0) or 0
    )

    max_g = float(
        serving.get("max_g", 0) or 0
    )

    default_g = float(
        serving.get("default_g", 0) or 0
    )

    # Calculate portion based on calories.
    if calories_per_100g <= 0:
        portion_g = default_g or min_g or 100.0
    else:
        portion_g = (
            target_calories
            / calories_per_100g
            * 100
        )

    # Apply minimum serving limit.
    if min_g > 0:
        portion_g = max(
            portion_g,
            min_g,
        )

    # Apply maximum serving limit.
    if max_g > 0:
        portion_g = min(
            portion_g,
            max_g,
        )

    # Round to nearest 5g.
    portion_g = round(
        portion_g / 5
    ) * 5

    # Apply limits again after rounding.
    portion_g = max(
        portion_g,
        min_g,
    )

    if max_g > 0:
        portion_g = min(
            portion_g,
            max_g,
        )

    # Convert grams into a multiplier
    # because nutrition is stored per 100g.
    multiplier = portion_g / 100.0

    actual_calories = (
        float(nutrition.get("calories", 0) or 0)
        * multiplier
    )

    actual_protein = (
        float(nutrition.get("protein_g", 0) or 0)
        * multiplier
    )

    actual_carbs = (
        float(nutrition.get("carbs_g", 0) or 0)
        * multiplier
    )

    actual_fat = (
        float(nutrition.get("fat_g", 0) or 0)
        * multiplier
    )

    actual_fiber = (
        float(nutrition.get("fiber_g", 0) or 0)
        * multiplier
    )

    return {
        "food_id": food.get("food_id"),

        "food_name": food.get("food_name"),

        "portion_g": portion_g,
        "serving": dict(serving),
        "min_serving_g": min_g,
        "max_serving_g": max_g,
        "ingredients": food.get("ingredients", []),
        "allergens": food.get("allergens", []),
        "diet_type": food.get("diet_type"),

        "nutrition": {
            "calories": round(
                actual_calories,
                1,
            ),
            "protein_g": round(
                actual_protein,
                1,
            ),
            "carbs_g": round(
                actual_carbs,
                1,
            ),
            "fat_g": round(
                actual_fat,
                1,
            ),
            "fiber_g": round(
                actual_fiber,
                1,
            ),
        },

        "ranking_score": food.get(
            "ranking",
            {},
        ).get(
            "score",
            0,
        ),

        "ranking_reasons": food.get(
            "ranking",
            {},
        ).get(
            "reasons",
            [],
        ),
    }


def fit_meal_portions_to_target(
    meal_foods: list[dict[str, Any]],
    target_meal_calories: float,
) -> list[dict[str, Any]]:
    """
    Allocate a meal calorie target across selected foods
    and calculate portions from dataset nutrition values.
    """
    if not meal_foods:
        return []

    food_target = target_meal_calories / len(meal_foods)
    result = [
        _calculate_portion(food=food, target_calories=food_target)
        for food in meal_foods
    ]

    current_cals = sum(item["nutrition"]["calories"] for item in result)
    diff = target_meal_calories - current_cals

    # If initial sum deviates noticeably and adjustment is possible, fine-tune targets
    if abs(diff) > target_meal_calories * 0.05 and current_cals > 0:
        ratio = target_meal_calories / current_cals
        adjusted = [
            _calculate_portion(food=food, target_calories=food_target * ratio)
            for food in meal_foods
        ]
        adj_cals = sum(item["nutrition"]["calories"] for item in adjusted)
        if abs(target_meal_calories - adj_cals) < abs(diff):
            result = adjusted

    # Redistribute residual energy after one food hits a serving bound. Keep the
    # existing 5g rounding and source limits rather than relaxing validation.
    for _ in range(3):
        for index, food in enumerate(meal_foods):
            remaining = target_meal_calories - sum(item["nutrition"]["calories"] for item in result)
            adjusted = _calculate_portion(food, result[index]["nutrition"]["calories"] + remaining)
            if abs(remaining - (adjusted["nutrition"]["calories"] - result[index]["nutrition"]["calories"])) < abs(remaining):
                result[index] = adjusted
    return result


def build_single_day_plan(
    day_number: int,
    ranked_candidates: list[dict[str, Any]],
    target_daily_calories: float,
    start_offset: int = 0,
) -> dict[str, Any]:
    """
    Build one day containing:
    Breakfast, Lunch, Snack and Dinner.
    """
    if not ranked_candidates:
        raise ValueError(
            "No eligible food candidates available "
            "for meal plan generation."
        )

    # Exclude non-dish items like standalone spices/oils from being chosen as primary meals
    dish_candidates = [
        food for food in ranked_candidates
        if str(food.get("food_type", "")).lower() not in {"ingredient", "spice", "oil", "condiment"}
    ]
    if not dish_candidates:
        dish_candidates = ranked_candidates

    def get_pool_for_meal(meal_name: str, count: int) -> list[dict[str, Any]]:
        suitable = [
            food for food in dish_candidates
            if meal_name.lower() in str(food.get("meal_suitability", "")).lower()
        ]
        if not suitable:
            suitable = dish_candidates

        offset = (start_offset + (day_number - 1) * count) % len(suitable)
        rotated = suitable[offset:] + suitable[:offset]
        preferred = rotated[:count]
        target = target_daily_calories * MEAL_CALORIE_DISTRIBUTION[meal_name]
        # Some pairs have minimum portions above the meal target. Select a
        # feasible one/two-food pool from the same ranked, filtered candidates.
        pools = chain([preferred], ([food] for food in rotated), combinations(rotated, 2))
        for candidate in pools:
            calculated = fit_meal_portions_to_target(list(candidate), target)
            if abs(sum(item["nutrition"]["calories"] for item in calculated) - target) <= target * .10:
                return list(candidate)
        return preferred

    meal_pools = {
        "Breakfast": get_pool_for_meal("Breakfast", 1 if target_daily_calories * 0.25 < 350 else 2),
        "Lunch": get_pool_for_meal("Lunch", 2),
        "Snack": get_pool_for_meal("Snack", 1),
        "Dinner": get_pool_for_meal("Dinner", 1 if target_daily_calories * 0.30 < 350 else 2),
    }

    meals = []

    day_cals = 0.0
    day_protein = 0.0
    day_carbs = 0.0
    day_fat = 0.0
    day_fiber = 0.0

    for meal_name in MEAL_STRUCTURE:

        target_meal_cals = (
            target_daily_calories
            * MEAL_CALORIE_DISTRIBUTION[
                meal_name
            ]
        )

        selected_foods = (
            fit_meal_portions_to_target(
                meal_foods=meal_pools[
                    meal_name
                ],
                target_meal_calories=(
                    target_meal_cals
                ),
            )
        )

        meal_cals = sum(
            food["nutrition"]["calories"]
            for food in selected_foods
        )

        meal_protein = sum(
            food["nutrition"]["protein_g"]
            for food in selected_foods
        )

        meal_carbs = sum(
            food["nutrition"]["carbs_g"]
            for food in selected_foods
        )

        meal_fat = sum(
            food["nutrition"]["fat_g"]
            for food in selected_foods
        )

        meal_fiber = sum(
            food["nutrition"]["fiber_g"]
            for food in selected_foods
        )

        day_cals += meal_cals
        day_protein += meal_protein
        day_carbs += meal_carbs
        day_fat += meal_fat
        day_fiber += meal_fiber

        meals.append(
            {
                "meal": meal_name,

                "target_calories": round(
                    target_meal_cals,
                    1,
                ),

                "foods": selected_foods,

                "actual_calories": round(
                    meal_cals,
                    1,
                ),

                "meal_total": {
                    "calories": round(
                        meal_cals,
                        1,
                    ),
                    "protein_g": round(
                        meal_protein,
                        1,
                    ),
                    "carbs_g": round(
                        meal_carbs,
                        1,
                    ),
                    "fat_g": round(
                        meal_fat,
                        1,
                    ),
                    "fiber_g": round(
                        meal_fiber,
                        1,
                    ),
                },
            }
        )

    return {
        "day": day_number,

        "meals": meals,

        "daily_total": {
            "calories": round(
                day_cals,
                1,
            ),
            "protein_g": round(
                day_protein,
                1,
            ),
            "carbs_g": round(
                day_carbs,
                1,
            ),
            "fat_g": round(
                day_fat,
                1,
            ),
            "fiber_g": round(
                day_fiber,
                1,
            ),
        },
    }


def diet_planning_agent(
    state: dict[str, Any],
) -> dict[str, Any]:
    """
    Agent 2: Personalized Ayurvedic Diet Agent.

    Pipeline:
    Context
        ↓
    Nutrition Target
        ↓
    Food Repository
        ↓
    Hard Filtering
        ↓
    Ayurvedic Ranking
        ↓
    Portion Calculation
        ↓
    Validation
    """

    # ---------------------------------------------------------
    # 1. Prepare standardized patient + Ayurveda context
    # ---------------------------------------------------------

    context_res = prepare_diet_context(state)

    patient_profile = context_res[
        "patient_profile"
    ]

    ayurvedic_assessment = context_res[
        "ayurvedic_assessment"
    ]

    # ---------------------------------------------------------
    # 2. Number of days
    # ---------------------------------------------------------

    raw_plan_days = state.get("plan_days")
    if raw_plan_days is not None and isinstance(raw_plan_days, (int, float, str)):
        try:
            plan_days = int(raw_plan_days)
            if not (1 <= plan_days <= 7):
                plan_days = 7
        except (ValueError, TypeError):
            plan_days = 7
    else:
        plan_days = 7

    # ---------------------------------------------------------
    # 3. Calculate nutrition target
    # ---------------------------------------------------------

    energy_target = calculate_energy_target(
        patient_profile
    )

    target_cals = float(
        energy_target.get(
            "target_calories"
        )
        or 2000.0
    )

    # ---------------------------------------------------------
    # 4. Load food dataset
    # ---------------------------------------------------------

    csv_path = Path(
        state.get(
            "food_dataset_path",
            DEFAULT_CSV_PATH,
        )
    )

    repository = FoodRepository(csv_path)

    all_foods = repository.get_all_foods()

    if not all_foods:
        raise ValueError(
            "No foods found in demo_foods.csv."
        )

    # ---------------------------------------------------------
    # 5. Hard safety filters
    # ---------------------------------------------------------

    filter_engine = FoodFilter()

    (
        eligible_candidates,
        excluded_foods,
    ) = filter_engine.filter_foods(
        foods=all_foods,
        patient_profile=patient_profile,
    )

    excluded_log = [
        {
            "food_name": food.get(
                "food_name",
                "Unknown",
            ),
            "reason": food.get(
                "reason",
                "Safety filter",
            ),
        }
        for food in excluded_foods
    ]

    if not eligible_candidates:
        raise ValueError(
            "No food candidates remain after "
            "applying hard safety filters."
        )

    # ---------------------------------------------------------
    # 6. Ayurvedic ranking
    # ---------------------------------------------------------

    ranker = FoodRanker()

    ranked_candidates = ranker.rank_foods(
        foods=eligible_candidates,

        ayurvedic_assessment=(
            ayurvedic_assessment
        ),

        patient_profile=patient_profile,
    )

    if not ranked_candidates:
        raise ValueError(
            "No foods remain after Ayurvedic ranking."
        )

    # ---------------------------------------------------------
    # 7. Build and validate diet
    # ---------------------------------------------------------

    validator = DietValidator()
    replanner = DietReplanner()

    def build_candidate_day(day_number: int, start_offset: int):
        return build_single_day_plan(
            day_number=day_number,
            ranked_candidates=ranked_candidates,
            target_daily_calories=target_cals,
            start_offset=start_offset,
        )

    def validate_candidate_days(
        candidate_days: list[dict[str, Any]],
        profile: dict[str, Any],
        daily_target: float,
    ) -> dict[str, Any]:
        return validator.validate(
            diet_plan={},
            patient_profile=profile,
            daily_target_calories=daily_target,
            days=candidate_days,
        )

    replan_result = replanner.replan_days(
        ranked_foods=ranked_candidates,
        daily_target=target_cals,
        plan_days=plan_days,
        patient_profile=patient_profile,
        build_day=build_candidate_day,
        validate_days=validate_candidate_days,
        max_attempts=MAX_RETRIES,
    )

    final_plan_days = replan_result["plan"]
    val_res = replan_result["validation"]

    if val_res.get("valid"):
        val_res["overall_status"] = "PASS"
        val_res["safety"] = "PASS"
        val_res["nutrition"] = "PASS"
        val_res["portion"] = "PASS"
    else:
        val_res["overall_status"] = "REVIEW_REQUIRED"
        val_res["safety"] = "FAIL"
        val_res["nutrition"] = "REVIEW_REQUIRED"
        val_res["portion"] = "REVIEW_REQUIRED"
        val_res.setdefault("issues", []).append(
            "No diet plan within the configured calorie tolerance was found."
        )

    val_res["replanned"] = replan_result.get("replanned", False)

    # ---------------------------------------------------------
    # 8. Collect sources
    # ---------------------------------------------------------

    unique_sources = []

    for candidate in ranked_candidates[:15]:

        for source in candidate.get(
            "sources",
            [],
        ):

            if source not in unique_sources:

                unique_sources.append(
                    source
                )

    # ---------------------------------------------------------
    # 9. Final Agent 2 output
    # ---------------------------------------------------------

    formatted_output = {
        "diet_plan": {
            "duration_days": plan_days,

            "daily_target": {
                "calories": target_cals,

                "protein_g": energy_target.get(
                    "target_protein_g",
                    0,
                ),

                "carbs_g": energy_target.get(
                    "target_carbs_g",
                    0,
                ),

                "fat_g": energy_target.get(
                    "target_fat_g",
                    0,
                ),

                "fiber_g": energy_target.get(
                    "target_fiber_g",
                    0,
                ),

                "calculation_basis": energy_target.get(
                    "calculation_basis",
                    "Not provided",
                ),
            },

            "days": final_plan_days,
        },

        "validation": val_res,

        "sources": unique_sources,

        "warnings": [],

        "excluded_foods_log": excluded_log,
    }

    return {
        "food_candidates": ranked_candidates,
        "meal_plan": formatted_output[
            "diet_plan"
        ],

        "agent2_output": formatted_output,

        "validation_result": val_res,
    }
