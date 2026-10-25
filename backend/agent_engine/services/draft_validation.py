"""Technical draft checks; these do not grant approval or clinical validity."""
import math
from agent_engine.agents.food_filter import FoodFilter


def draft_failures(diet, lifestyle, profile=None):
    def number(value):
        return isinstance(value, (int, float)) and not isinstance(value, bool) and math.isfinite(value)
    if not isinstance(diet, dict) or not isinstance(lifestyle, dict):
        return ["Diet or lifestyle output is unavailable."]
    failures = []
    days = diet.get("days") or []
    if not isinstance(days, list) or not all(isinstance(day, dict) for day in days):
        return ["Invalid diet day structure."]
    if diet.get("duration_days") != 7 or len(days) != 7 or {day.get("day") for day in days} != set(range(1, 8)):
        failures.append("A complete seven-day diet is required.")
    for day in days:
        meals = day.get("meals") or []
        if not isinstance(meals, list) or not all(isinstance(meal, dict) for meal in meals):
            failures.append("Invalid meal structure.")
            continue
        if len(meals) != 4 or {str(meal.get("meal", "")).lower() for meal in meals} != {"breakfast", "lunch", "snack", "dinner"}:
            failures.append("Each day requires breakfast, lunch, snack and dinner.")
        for meal in meals:
            foods = meal.get("foods") or []
            if not isinstance(foods, list) or not foods or not all(isinstance(food, dict) for food in foods):
                failures.append("A meal has no valid foods.")
                continue
            for food in foods:
                portion = food.get("portion_g")
                if not food.get("food_name") or not number(portion) or portion <= 0:
                    failures.append("A food has no valid name or portion.")
                serving = food.get("serving") or {}
                low = food.get("min_serving_g", serving.get("min_g"))
                high = food.get("max_serving_g", serving.get("max_g"))
                if not number(low) or not number(high) or not number(portion) or not 0 <= low <= portion <= high or high <= 0:
                    failures.append("A food has missing or violated serving limits.")
                nutrition = food.get("nutrition") or {}
                for nutrient in ("calories", "protein_g", "carbs_g", "fat_g", "fiber_g"):
                    value = nutrition.get(nutrient, nutrition.get("carbohydrates_g") if nutrient == "carbs_g" else None)
                    if not number(value) or value < 0:
                        failures.append("A food has missing or invalid nutrition fields.")
                if profile:
                    _, excluded = FoodFilter().filter_foods([food], profile)
                    if excluded:
                        failures.append("A selected food violates recorded dietary restrictions.")
    if not lifestyle.get("recommendations"):
        failures.append("Lifestyle recommendations are unavailable.")
    return list(dict.fromkeys(failures))
