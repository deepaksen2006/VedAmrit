from pathlib import Path

from agent_engine.agents.food_filter import FoodFilter
from agent_engine.services.food_repository import FoodRepository


def test_milk_and_curd_exclusions_cover_related_dishes():
    foods = FoodRepository(Path("data/demo_foods.csv")).get_all_foods()
    allowed, excluded = FoodFilter().filter_foods(
        foods,
        {
            "dietary_preference": "Vegetarian",
            "allergies": [],
            "foods_to_avoid": ["milk", "curd"],
        },
    )

    excluded_names = {item["food_name"] for item in excluded}
    forbidden_names = {
        "Oats Porridge",
        "Paratha with Yogurt",
        "Palak Paneer",
        "Vegetable Biryani",
        "Curd Rice",
        "Cucumber Raita Snack",
    }

    assert not {food["food_name"] for food in allowed} & forbidden_names
    assert forbidden_names <= excluded_names