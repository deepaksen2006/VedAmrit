from typing import Any


class PortionCalculator:

    def calculate_portions(
        self,
        meals: dict[str, Any],
    ) -> dict[str, Any]:

        result = {}

        for meal_name, meal_data in meals.items():

            target_calories = meal_data.get("target_calories")
            foods = meal_data.get("foods", [])

            if not foods:
                result[meal_name] = {
                    "target_calories": target_calories,
                    "actual_calories": 0,
                    "calorie_difference": 0,
                    "within_target": True,
                    "foods": [],
                }
                continue

            meal_foods = []

            if target_calories is not None:

                # Start by dividing the target equally
                calories_per_food = (
                    target_calories / len(foods)
                )

            else:
                calories_per_food = None

            for food in foods:

                nutrition = food.get("nutrition", {})
                calories_per_100g = nutrition.get("calories", 0)

                serving = food.get("serving", {})

                default_g = serving.get("default_g", 100)
                min_g = serving.get("min_g", 50)
                max_g = serving.get("max_g", 300)

                if (
                    calories_per_food is not None
                    and calories_per_100g > 0
                ):

                    calculated_g = (
                        calories_per_food
                        / calories_per_100g
                    ) * 100

                    # Respect serving limits
                    portion_g = max(
                        min_g,
                        min(max_g, calculated_g)
                    )

                    # Round to nearest 5 g
                    portion_g = round(portion_g / 5) * 5

                else:
                    portion_g = default_g

                actual_calories = (
                    calories_per_100g
                    * portion_g
                    / 100
                )

                meal_foods.append({
                    "food_id": food["food_id"],
                    "food_name": food["food_name"],
                    "portion_g": portion_g,
                    "calories": round(actual_calories, 1),
                    "ranking_score": food["ranking"]["score"],
                    "min_serving_g": min_g,
                    "max_serving_g": max_g,
                })

            actual_meal_calories = sum(
                food["calories"]
                for food in meal_foods
            )

            if target_calories is not None:

                difference = (
                    actual_meal_calories
                    - target_calories
                )

                tolerance = target_calories * 0.10

                within_target = (
                    abs(difference) <= tolerance
                )

            else:
                difference = 0
                within_target = True

            result[meal_name] = {
                "target_calories": target_calories,
                "actual_calories": round(
                    actual_meal_calories,
                    1
                ),
                "calorie_difference": round(
                    difference,
                    1
                ),
                "within_target": within_target,
                "foods": meal_foods,
            }

        return result