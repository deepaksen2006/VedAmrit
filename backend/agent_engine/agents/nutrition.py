from pathlib import Path
from typing import Any

from agent_engine.services.food_repository import FoodRepository


# Project root:
# ayurvedic_diet_agent/
PROJECT_ROOT = Path(__file__).resolve().parents[1]

# Default food dataset:
# ayurvedic_diet_agent/data/demo_foods.csv
DEFAULT_CSV_PATH = PROJECT_ROOT / "data" / "demo_foods.csv"


def nutrition_agent(
    state: dict[str, Any],
) -> dict[str, Any]:
    """
    Reads nutrition information from demo_foods.csv.

    Nutrition values are stored per 100g in the dataset.
    Agent 2 later uses serving sizes and portions to build
    the personalized meal plan.
    """

    # Allow the dataset path to be overridden through state.
    csv_path = Path(
        state.get(
            "food_dataset_path",
            DEFAULT_CSV_PATH,
        )
    )

    # Load food data.
    repository = FoodRepository(csv_path)

    foods = repository.get_all_foods()

    if not foods:
        raise ValueError(
            f"No foods found in the food dataset: {csv_path}"
        )

    nutrition_records = []

    for food in foods:

        nutrition = food.get("nutrition", {})
        serving = food.get("serving", {})

        nutrition_records.append(
            {
                "food_id": food.get("food_id"),
                "food_name": food.get("food_name"),

                # Nutrition per 100g
                "calories_per_100g": nutrition.get(
                    "calories",
                    0.0,
                ),
                "protein_g_per_100g": nutrition.get(
                    "protein_g",
                    0.0,
                ),
                "carbs_g_per_100g": nutrition.get(
                    "carbs_g",
                    0.0,
                ),
                "fat_g_per_100g": nutrition.get(
                    "fat_g",
                    0.0,
                ),
                "fiber_g_per_100g": nutrition.get(
                    "fiber_g",
                    0.0,
                ),

                # Serving information
                "default_serving_g": serving.get(
                    "default_g",
                    0.0,
                ),
                "min_serving_g": serving.get(
                    "min_g",
                    0.0,
                ),
                "max_serving_g": serving.get(
                    "max_g",
                    0.0,
                ),
            }
        )

    nutrition_context = {
        "source": "data/demo_foods.csv",

        "food_count": len(
            nutrition_records
        ),

        "nutrition_records": nutrition_records,

        # These will be calculated later by
        # the diet-target component.
        "daily_targets": {
            "calories": None,
            "protein_g": None,
            "carbohydrates_g": None,
            "fat_g": None,
            "fiber_g": None,
        },

        "nutrition_notes": [
            "Nutrition values are read from "
            "demo_foods.csv.",

            "Nutrition values are provided per 100g.",

            "Serving-size ranges are available "
            "in the food dataset.",

            "Daily nutrition targets are calculated "
            "by the diet-target component when "
            "patient measurements are available.",
        ],
    }

    return {
        "nutrition_context": nutrition_context,
    }
