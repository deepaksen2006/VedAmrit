from agent_engine.services.food_repository import FoodRepository
from agent_engine.agents.food_filter import FoodFilter
from agent_engine.agents.food_ranking import FoodRanker
from agent_engine.agents.diet_meal_planner import DietMealPlanner
from agent_engine.services.diet_target import calculate_diet_target
from agent_engine.services.portion_calculator import PortionCalculator
from agent_engine.agents.diet_validation import DietValidator


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


# 5. Build meals
planner = DietMealPlanner()

meal_plan = planner.build_day(
    ranked_foods,
    daily_target
)


# 6. Calculate portions
portion_calculator = PortionCalculator()

portion_plan = portion_calculator.calculate_portions(
    meal_plan
)


# 7. Validate
validator = DietValidator()

validation_result = validator.validate(
    portion_plan,
    patient_profile
)


# 8. Print result
print("\n==============================")
print("DIET VALIDATION")
print("==============================")

print(
    f"\nOverall valid: "
    f"{validation_result['valid']}"
)


for meal_name, meal in validation_result["meals"].items():

    validation = meal["validation"]

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
        f"Calorie difference: "
        f"{meal['calorie_difference']}"
    )

    print(
        f"Valid: "
        f"{validation['valid']}"
    )

    if validation["issues"]:

        print("Issues:")

        for issue in validation["issues"]:
            print(f" - {issue}")

    else:

        print("Issues: None")