from typing import Any

MEAL_ORDER = ["breakfast", "lunch", "snack", "dinner"]


class DietMealPlanner:

    def build_day(
        self,
        ranked_foods: list[dict[str, Any]],
        daily_target: dict[str, Any],
    ) -> dict[str, Any]:

        meal_foods = [
            food
            for food in ranked_foods
            if self._is_meal_food(food)
        ]

        if not meal_foods:
            raise ValueError("No suitable meal foods available.")

        meals = {}

        for meal in MEAL_ORDER:

            candidates = [
                food
                for food in meal_foods
                if meal in food.get(
                    "meal_suitability",
                    []
                )
            ]

            target = self._get_meal_target(
                daily_target,
                meal
            )

            # Snack gets one food.
            # Other meals get up to two foods.
            limit = 1 if meal == "snack" else 2

            selected = self._select_foods(
                candidates,
                limit
            )

            meals[meal] = {
                "target_calories": target,
                "foods": selected,
            }

        return meals

    @staticmethod
    def _get_meal_target(
        daily_target: dict[str, Any],
        meal: str,
    ):

        calories = daily_target.get("calories")

        if calories is None:
            return None

        percentages = {
            "breakfast": 0.25,
            "lunch": 0.35,
            "snack": 0.10,
            "dinner": 0.30,
        }

        return round(
            calories * percentages[meal],
            1
        )

    @staticmethod
    def _is_meal_food(food):

        food_type = food.get(
            "food_type",
            ""
        ).lower()

        excluded_types = {
            "spice",
            "oil",
            "ingredient",
            "condiment",
            "sweetener",
        }

        return food_type not in excluded_types

    @staticmethod
    def _select_foods(
        candidates,
        limit,
    ):

        selected = []

        for food in candidates:

            selected.append(food)

            if len(selected) >= limit:
                break

        return selected