from agent_engine.services.food_repository import FoodRepository


def main():
    repository = FoodRepository()

    foods = repository.get_all_foods()

    print("Number of foods:", len(foods))

    print("\nFirst food:")
    print(foods[0])

    print("\nFood names:")
    for food in foods[:10]:
        print("-", food["food_name"])


if __name__ == "__main__":
    main()