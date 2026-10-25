from typing import Any

from agent_engine.services.food_restrictions import matches_food_restriction
from agent_engine.agents.food_retrieval import DAIRY_KEYWORDS, NON_VEG_KEYWORDS


class FoodFilter:
    """
    Applies hard dietary constraints before food ranking.

    Hard constraints:
    1. Dietary preference
    2. Allergies
    3. Foods explicitly avoided
    """

    def filter_foods(
        self,
        foods: list[dict[str, Any]],
        patient_profile: dict[str, Any],
    ) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:

        dietary_preference = (
            patient_profile.get("dietary_preference", "")
            .strip()
            .lower()
        )

        allergies = self._normalize_list(
            patient_profile.get("allergies", [])
        )

        foods_to_avoid = self._normalize_list(
            patient_profile.get("foods_to_avoid", [])
        )
        foods_to_avoid += self._normalize_list(patient_profile.get("food_intolerances", []))
        foods_to_avoid += self._normalize_list(patient_profile.get("food_exclusions", []))

        allowed_foods = []
        excluded_foods = []

        for food in foods:

            reason = self._check_food(
                food=food,
                dietary_preference=dietary_preference,
                allergies=allergies,
                foods_to_avoid=foods_to_avoid,
            )

            if reason:
                excluded_foods.append({
                    "food_id": food.get("food_id"),
                    "food_name": food["food_name"],
                    "reason": reason,
                })
            else:
                allowed_foods.append(food)

        return allowed_foods, excluded_foods

    def _check_food(
        self,
        food: dict[str, Any],
        dietary_preference: str,
        allergies: list[str],
        foods_to_avoid: list[str],
    ) -> str | None:

        # --------------------------------------------------
        # 1. DIETARY PREFERENCE
        # --------------------------------------------------

        food_diet = food.get("diet_type", "").lower()

        if dietary_preference == "vegan":
            if food_diet != "vegan":
                return "Not suitable for vegan diet"
            if any(matches_food_restriction(food, item) for item in DAIRY_KEYWORDS | NON_VEG_KEYWORDS):
                return "Animal-derived ingredient is not suitable for vegan diet"

        elif dietary_preference == "vegetarian":
            if food_diet not in ["vegan", "vegetarian"]:
                return "Not suitable for vegetarian diet"
            if any(matches_food_restriction(food, item) for item in NON_VEG_KEYWORDS):
                return "Animal ingredient is not suitable for vegetarian diet"

        # --------------------------------------------------
        # 2. ALLERGIES
        # --------------------------------------------------

        for allergy in allergies:
            if matches_food_restriction(food, allergy):
                return f"Food contains allergen or related ingredient: {allergy}"

        # --------------------------------------------------
        # 3. FOODS TO AVOID
        # --------------------------------------------------

        for avoided in foods_to_avoid:
            if matches_food_restriction(food, avoided):
                return f"Food contains avoided item or related ingredient: {avoided}"

        return None

    @staticmethod
    def _normalize_list(value: Any) -> list[str]:

        if value is None:
            return []

        if isinstance(value, str):
            return [
                item.strip().lower()
                for item in value.split(",")
                if item.strip()
            ]

        if isinstance(value, list):
            return [
                str(item).strip().lower()
                for item in value
                if str(item).strip()
            ]

        return []
