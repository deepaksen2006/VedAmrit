from typing import Any

from agent_engine.agents.diet_meal_planner import DietMealPlanner
from agent_engine.services.portion_calculator import PortionCalculator
from agent_engine.agents.diet_validation import DietValidator


class DietReplanner:

    def __init__(self):
        self.meal_planner = DietMealPlanner()
        self.portion_calculator = PortionCalculator()
        self.validator = DietValidator()

    def replan(
        self,
        ranked_foods: list[dict[str, Any]],
        daily_target: dict[str, Any],
        patient_profile: dict[str, Any],
    ) -> dict[str, Any]:

        best_plan = None
        best_validation = None
        best_error = float("inf")

        # Try several starting positions in the ranked list
        for start_index in range(0, 10):

            alternative_foods = ranked_foods[start_index:]

            meal_plan = self.meal_planner.build_day(
                alternative_foods,
                daily_target
            )

            portion_plan = self.portion_calculator.calculate_portions(
                meal_plan
            )

            validation = self.validator.validate(
                portion_plan,
                patient_profile
            )

            # Calculate total calorie error
            total_error = 0

            for meal in portion_plan.values():

                target = meal.get("target_calories")

                actual = meal.get("actual_calories", 0)

                if target is not None:
                    total_error += abs(actual - target)

            # Keep the best plan found
            if total_error < best_error:

                best_error = total_error
                best_plan = portion_plan
                best_validation = validation

            # Stop immediately if a valid plan is found
            if validation["valid"]:

                return {
                    "plan": portion_plan,
                    "validation": validation,
                    "replanned": start_index > 0,
                }

        # Return the closest plan if no completely valid plan exists
        return {
            "plan": best_plan,
            "validation": best_validation,
            "replanned": True,
        }

    def replan_days(
        self,
        ranked_foods: list[dict[str, Any]],
        daily_target: float,
        plan_days: int,
        patient_profile: dict[str, Any],
        build_day,
        validate_days,
        max_attempts: int = 3,
    ) -> dict[str, Any]:
        """Try alternative ranked-food starting positions for Agent 2."""
        best_plan = None
        best_validation = None
        best_key = (True, float("inf"))

        attempt_count = min(max_attempts, max(1, len(ranked_foods)))

        for start_index in range(attempt_count):
            candidate_days = [
                build_day(day_number, start_index)
                for day_number in range(1, plan_days + 1)
            ]
            validation = validate_days(
                candidate_days,
                patient_profile,
                daily_target,
            )
            total_error = sum(
                abs(item["daily_calorie_difference"])
                for item in validation.get("daily_summaries", [])
            )
            candidate_key = (not bool(validation.get("valid")), total_error)

            if best_plan is None or candidate_key < best_key:
                best_plan = candidate_days
                best_validation = validation
                best_key = candidate_key

            if validation.get("valid"):
                return {
                    "plan": candidate_days,
                    "validation": validation,
                    "replanned": start_index > 0,
                }

        return {
            "plan": best_plan or [],
            "validation": best_validation or {},
            "replanned": True,
        }