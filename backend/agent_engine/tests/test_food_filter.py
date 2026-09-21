from agent_engine.services.food_repository import FoodRepository
from agent_engine.agents.food_filter import FoodFilter


def main():

    # Load our demo dataset
    repository = FoodRepository()
    foods = repository.get_all_foods()

    # Example patient
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

    # Apply filters
    food_filter = FoodFilter()

    allowed_foods, excluded_foods = food_filter.filter_foods(
        foods,
        patient_profile,
    )

    print("\n==============================")
    print("FOOD FILTER TEST")
    print("==============================")

    print("\nTotal foods:", len(foods))
    print("Allowed foods:", len(allowed_foods))
    print("Excluded foods:", len(excluded_foods))

    print("\n------------------------------")
    print("ALLOWED FOODS")
    print("------------------------------")

    for food in allowed_foods:
        print("-", food["food_name"])

    print("\n------------------------------")
    print("EXCLUDED FOODS")
    print("------------------------------")

    for food in excluded_foods:
        print(
            f"- {food['food_name']} → {food['reason']}"
        )


if __name__ == "__main__":
    main()