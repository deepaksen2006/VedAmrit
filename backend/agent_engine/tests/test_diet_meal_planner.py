from agent_engine.services.food_repository import FoodRepository
from agent_engine.agents.food_filter import FoodFilter
from agent_engine.agents.food_ranking import FoodRanker
from agent_engine.agents.diet_meal_planner import DietMealPlanner


def main():

    # ==========================================
    # LOAD FOODS
    # ==========================================

    repository = FoodRepository()

    foods = repository.get_all_foods()

    # ==========================================
    # PATIENT
    # ==========================================

    patient_profile = {
        "name": "Demo User",
        "age": 20,
        "gender": "female",
        "height_cm": 160,
        "weight_kg": 55,
        "dietary_preference": "vegan",
        "allergies": ["onion"],
        "foods_to_avoid": ["capsicum"],
        "health_conditions": [],
        "goal": "general wellness",
    }

    # ==========================================
    # FILTER
    # ==========================================

    food_filter = FoodFilter()

    allowed_foods, excluded_foods = (
        food_filter.filter_foods(
            foods,
            patient_profile,
        )
    )

    # ==========================================
    # AYURVEDIC ASSESSMENT
    # ==========================================

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

    # ==========================================
    # RANK
    # ==========================================

    ranker = FoodRanker()

    ranked_foods = ranker.rank_foods(
        allowed_foods,
        ayurvedic_assessment,
        patient_profile,
    )

    # ==========================================
    # NUTRITION TARGET
    # ==========================================

    daily_target = {
        "status": "calculated",
        "calories": 1772.4,
        "protein_g": 66.0,
        "carbs_g": 266.3,
        "fat_g": 49.2,
        "fiber_g": 30,
    }

    # ==========================================
    # MEAL PLANNER
    # ==========================================

    planner = DietMealPlanner()

    day_plan = planner.build_day(
        ranked_foods,
        daily_target,
    )

    # ==========================================
    # DISPLAY
    # ==========================================

    print("\n==============================")
    print("DAILY MEAL PLAN")
    print("==============================")

    for meal, data in day_plan.items():

        print(
            f"\n{meal.upper()}"
        )

        print(
            "Target calories:",
            data["target_calories"]
        )

        for food in data["foods"]:

            print(
                " -",
                food["food_name"],
                "| Score:",
                food["ranking"]["score"],
            )


if __name__ == "__main__":
    main()