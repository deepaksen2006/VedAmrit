from agent_engine.services.food_repository import FoodRepository
from agent_engine.agents.food_filter import FoodFilter
from agent_engine.agents.food_ranking import FoodRanker
from agent_engine.agents.diet_meal_planner import DietMealPlanner
from agent_engine.services.diet_target import calculate_diet_target
from agent_engine.services.portion_calculator import PortionCalculator


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


# 1. Load food dataset
repository = FoodRepository()
foods = repository.get_all_foods()


# 2. Filter foods
food_filter = FoodFilter()

allowed_foods, excluded_foods = food_filter.filter_foods(
    foods,
    patient_profile
)


# 3. Rank foods
ranker = FoodRanker()

ranked_foods = ranker.rank_foods(
    allowed_foods,
    ayurvedic_assessment,
    patient_profile
)


# 4. Calculate nutrition target
daily_target = calculate_diet_target(patient_profile)


# 5. Build meals
planner = DietMealPlanner()

meal_plan = planner.build_day(
    ranked_foods,
    daily_target
)


# 6. Calculate portions
portion_calculator = PortionCalculator()

final_plan = portion_calculator.calculate_portions(
    meal_plan
)


# 7. Print result
print("\n==============================")
print("FINAL PORTION PLAN")
print("==============================")


for meal_name, meal_data in final_plan.items():

    print(f"\n{meal_name.upper()}")

    print(
        f"Target calories: "
        f"{meal_data['target_calories']}"
    )

    for food in meal_data["foods"]:

        print(
            f" - {food['food_name']} "
            f"| Portion: {food['portion_g']} g "
            f"| Calories: {food['calories']}"
        )