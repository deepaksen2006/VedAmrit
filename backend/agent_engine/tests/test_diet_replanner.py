from agent_engine.services.food_repository import FoodRepository
from agent_engine.agents.food_filter import FoodFilter
from agent_engine.agents.food_ranking import FoodRanker
from agent_engine.services.diet_target import calculate_diet_target
from agent_engine.agents.diet_replanner import DietReplanner


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


# 1. Load foods
repository = FoodRepository()

foods = repository.get_all_foods()


# 2. Filter
food_filter = FoodFilter()

allowed_foods, excluded_foods = food_filter.filter_foods(
    foods,
    patient_profile
)


# 3. Rank
ranker = FoodRanker()

ranked_foods = ranker.rank_foods(
    allowed_foods,
    ayurvedic_assessment,
    patient_profile
)


# 4. Nutrition target
daily_target = calculate_diet_target(
    patient_profile
)


# 5. Re-plan
replanner = DietReplanner()

result = replanner.replan(
    ranked_foods,
    daily_target,
    patient_profile
)


# 6. Print result
print("\n==============================")
print("FINAL REPLANNED DIET")
print("==============================")

print(
    f"\nReplanned: "
    f"{result['replanned']}"
)

print(
    f"Overall valid: "
    f"{result['validation']['valid']}"
)


for meal_name, meal in result["plan"].items():

    print(f"\n{meal_name.upper()}")

    print(
        f"Target calories: "
        f"{meal['target_calories']}"
    )

    print(
        f"Actual calories: "
        f"{meal['actual_calories']}"
    )

    print(
        f"Difference: "
        f"{meal['calorie_difference']}"
    )

    for food in meal["foods"]:

        print(
            f" - {food['food_name']} "
            f"| {food['portion_g']} g "
            f"| {food['calories']} kcal"
        )

    validation = result["validation"]["meals"][meal_name]["validation"]

    print(
        f"Valid: {validation['valid']}"
    )

    if validation["issues"]:

        print("Issues:")

        for issue in validation["issues"]:
            print(f" - {issue}")

    else:

        print("Issues: None")
    
