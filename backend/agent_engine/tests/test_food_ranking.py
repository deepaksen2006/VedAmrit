from agent_engine.services.food_repository import FoodRepository
from agent_engine.agents.food_filter import FoodFilter
from agent_engine.agents.food_ranking import FoodRanker


def main():

    # ------------------------------------------
    # LOAD FOOD DATA
    # ------------------------------------------

    repository = FoodRepository()

    foods = repository.get_all_foods()

    # ------------------------------------------
    # PATIENT
    # ------------------------------------------

    patient_profile = {
        "name": "Demo User",
        "age": 20,
        "gender": "female",
        "dietary_preference": "vegan",
        "allergies": ["onion"],
        "foods_to_avoid": ["capsicum"],
        "health_conditions": [],
        "goal": "general wellness",
    }

    # ------------------------------------------
    # FILTER
    # ------------------------------------------

    food_filter = FoodFilter()

    allowed_foods, excluded_foods = (
        food_filter.filter_foods(
            foods,
            patient_profile,
        )
    )

    # ------------------------------------------
    # AGENT 1 RESULT
    # ------------------------------------------

    ayurvedic_assessment = {

        "prakriti": {
            "primary_dosha": "Pitta",
            "secondary_dosha": "Vata",
            "constitution": "Pitta-Vata",
        },

        "vikriti": {
            "dominant_dosha": "Pitta",
        },

        "agni": {
            "status": "Mandagni",
        },
    }

    # ------------------------------------------
    # RANK
    # ------------------------------------------

    ranker = FoodRanker()

    ranked_foods = ranker.rank_foods(
        allowed_foods,
        ayurvedic_assessment,
        patient_profile,
    )

    # ------------------------------------------
    # DISPLAY
    # ------------------------------------------

    print("\n==============================")
    print("FOOD RANKING TEST")
    print("==============================")

    print(
        "\nTotal safe foods:",
        len(allowed_foods)
    )

    print("\nTOP 15 FOODS:")
    print("------------------------------")

    for index, food in enumerate(
        ranked_foods[:15],
        start=1
    ):

        print(
            f"{index}. "
            f"{food['food_name']} "
            f"→ Score: "
            f"{food['ranking']['score']}"
        )

        if food["ranking"]["reasons"]:
            print(
                "   Reasons:",
                "; ".join(
                    food["ranking"]["reasons"]
                )
            )


if __name__ == "__main__":
    main()