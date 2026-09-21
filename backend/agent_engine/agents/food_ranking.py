from typing import Any


class FoodRanker:
    """
    Ranks already-filtered foods according to:
    - Vikriti
    - Prakriti
    - Agni
    - User goal
    """

    def rank_foods(
        self,
        foods: list[dict[str, Any]],
        ayurvedic_assessment: dict[str, Any],
        patient_profile: dict[str, Any],
    ) -> list[dict[str, Any]]:

        prakriti = ayurvedic_assessment.get(
            "prakriti", {}
        )

        vikriti = ayurvedic_assessment.get(
            "vikriti", {}
        )

        agni = ayurvedic_assessment.get(
            "agni", {}
        )

        primary_dosha = (
            prakriti.get("primary_dosha") or ""
        ).lower()

        secondary_dosha = (
            prakriti.get("secondary_dosha") or ""
        ).lower()

        dominant_vikriti = (
            vikriti.get("dominant_dosha") or ""
        ).lower()

        agni_status = (
            agni.get("status") or ""
        ).lower()

        goal = (
            patient_profile.get("goal") or ""
        ).lower()

        ranked_foods = []

        for food in foods:

            score = 0
            reasons = []

            # ------------------------------------------
            # IGNORE INGREDIENTS / CONDIMENTS AS MEALS
            # ------------------------------------------

            food_type = food.get(
                "food_type", ""
            ).lower()

            if food_type in [
                "spice",
                "oil",
                "ingredient",
                "condiment",
                "sweetener",
            ]:
                continue

            ayurvedic = food.get(
                "ayurvedic_attributes", {}
            )

            # ------------------------------------------
            # 1. VIKRITI
            # ------------------------------------------

            dosha_effect = ayurvedic.get(
                f"{dominant_vikriti}_effect",
                ""
            ).lower()

            if dosha_effect == "balancing":
                score += 5
                reasons.append(
                    f"supports {dominant_vikriti} balance"
                )

            elif dosha_effect == "soothing":
                score += 4
                reasons.append(
                    f"soothing for {dominant_vikriti}"
                )

            elif dosha_effect == "may increase":
                score -= 3
                reasons.append(
                    f"may increase {dominant_vikriti}"
                )

            # ------------------------------------------
            # 2. PRAKRITI
            # ------------------------------------------

            for dosha in [
                primary_dosha,
                secondary_dosha,
            ]:

                if not dosha:
                    continue

                effect = ayurvedic.get(
                    f"{dosha}_effect",
                    ""
                ).lower()

                if effect == "balancing":
                    score += 2
                    reasons.append(
                        f"suitable for {dosha} constitution"
                    )

                elif effect == "soothing":
                    score += 1

                elif effect == "may increase":
                    score -= 1

            # ------------------------------------------
            # 3. AGNI
            # ------------------------------------------

            agni_suitability = ayurvedic.get(
                "agni_suitability",
                ""
            ).lower()

            if agni_status and agni_status in agni_suitability:
                score += 2
                reasons.append(
                    f"suitable for {agni_status}"
                )

            # General digestive suitability
            if (
                "easy to digest"
                in agni_suitability
            ):
                score += 1

            # ------------------------------------------
            # 4. USER GOAL
            # ------------------------------------------

            best_for = [
                item.lower()
                for item in food.get(
                    "best_for", []
                )
            ]

            if goal:

                if goal in best_for:
                    score += 2
                    reasons.append(
                        f"matches goal: {goal}"
                    )

                elif (
                    "general wellness" in best_for
                    and goal == "general wellness"
                ):
                    score += 2
                    reasons.append(
                        "supports general wellness"
                    )

            # ------------------------------------------
            # FINAL RECORD
            # ------------------------------------------

            ranked_foods.append({
                **food,
                "ranking": {
                    "score": score,
                    "reasons": reasons,
                },
            })
        
        # Highest score first
        ranked_foods.sort(
            key=lambda food: food["ranking"]["score"],
            reverse=True,
        )

        return ranked_foods