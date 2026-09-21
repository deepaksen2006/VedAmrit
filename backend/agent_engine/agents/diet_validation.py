from typing import Any


class DietValidator:

    def validate(
        self,
        diet_plan: dict[str, Any],
        patient_profile: dict[str, Any],
        daily_target_calories: float | None = None,
        days: list[dict[str, Any]] | None = None,
    ) -> dict[str, Any]:

        validated_meals = {}
        daily_summaries = []
        issues = []
        all_valid = True

        if days is not None:
            day_meals = {
                f"Day {day.get('day')} - {meal.get('meal')}" : meal
                for day in days
                for meal in day.get("meals", [])
            }
        else:
            day_meals = diet_plan

        for meal_name, meal in day_meals.items():

            target = meal.get("target_calories")
            actual = meal.get("actual_calories", 0)

            if target is None:
                calorie_valid = True
                difference = 0
            else:
                difference = actual - target

                # Allow 10% calorie deviation
                tolerance = target * 0.10

                calorie_valid = abs(difference) <= tolerance

            meal_issues = []

            if not calorie_valid:
                meal_issues.append(
                    f"Calorie target exceeded by {round(abs(difference), 1)} kcal"
                    if difference > 0
                    else
                    f"Calorie target short by {round(abs(difference), 1)} kcal"
                )

            # Check portions
            for food in meal.get("foods", []):

                portion = food.get("portion_g", 0)
                serving = food.get("serving", {})
                min_g = food.get(
                    "min_serving_g",
                    serving.get("min_g", 0),
                )
                max_g = food.get(
                    "max_serving_g",
                    serving.get("max_g", 0),
                )

                if portion < min_g:
                    meal_issues.append(
                        f"{food['food_name']} is below minimum serving"
                    )

                if portion > max_g:
                    meal_issues.append(
                        f"{food['food_name']} exceeds maximum serving"
                    )

            meal_valid = len(meal_issues) == 0

            if not meal_valid:
                all_valid = False
                issues.extend(meal_issues)

            validated_meals[meal_name] = {
                **meal,
                "validation": {
                    "valid": meal_valid,
                    "calorie_valid": calorie_valid,
                    "issues": meal_issues,
                },
            }

        if days is not None:
            target = float(daily_target_calories or 0)

            for day in days:
                actual = round(
                    sum(
                        float(meal.get("actual_calories", 0) or 0)
                        for meal in day.get("meals", [])
                    ),
                    1,
                )
                difference = round(actual - target, 1)
                tolerance = round(target * 0.10, 1)
                daily_valid = abs(difference) <= tolerance
                day_number = day.get("day")

                if not daily_valid:
                    direction = "exceed" if difference > 0 else "fall short"
                    issue = (
                        f"Day {day_number} daily calories "
                        f"{direction} target by {abs(difference):.1f} kcal"
                    )
                    validated_meals.setdefault(
                        f"Day {day_number}",
                        {"validation": {"issues": []}},
                    )
                    validated_meals[f"Day {day_number}"]["validation"][
                        "issues"
                    ].append(issue)
                    issues.append(issue)
                    all_valid = False

                daily_summaries.append({
                    "day": day_number,
                    "daily_target_calories": round(target, 1),
                    "daily_actual_calories": actual,
                    "daily_calorie_difference": difference,
                    "daily_calorie_tolerance": tolerance,
                    "daily_calorie_valid": daily_valid,
                })

        if daily_summaries:
            total_target = round(
                sum(item["daily_target_calories"] for item in daily_summaries),
                1,
            )
            total_actual = round(
                sum(item["daily_actual_calories"] for item in daily_summaries),
                1,
            )
            total_difference = round(total_actual - total_target, 1)
            daily_calorie_valid = all(
                item["daily_calorie_valid"] for item in daily_summaries
            )
        else:
            total_target = None
            total_actual = None
            total_difference = None
            daily_calorie_valid = True

        return {
            "valid": all_valid and daily_calorie_valid,
            "daily_target_calories": total_target,
            "daily_actual_calories": total_actual,
            "daily_calorie_difference": total_difference,
            "daily_calorie_tolerance": (
                round(total_target * 0.10, 1)
                if total_target is not None
                else None
            ),
            "daily_calorie_valid": daily_calorie_valid,
            "daily_summaries": daily_summaries,
            "issues": issues,
            "meals": validated_meals,
        }