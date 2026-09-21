from agent_engine.services.diet_target import calculate_diet_target


def test_adult():

    patient = {
        "age": 20,
        "gender": "female",
        "height_cm": 160,
        "weight_kg": 55,
        "goal": "general wellness",
    }

    result = calculate_diet_target(patient)

    print("\n==============================")
    print("ADULT TARGET")
    print("==============================")

    print(result)


def test_minor():

    patient = {
        "age": 12,
        "gender": "female",
        "height_cm": 145,
        "weight_kg": 40,
        "goal": "general wellness",
    }

    result = calculate_diet_target(patient)

    print("\n==============================")
    print("UNDER 18 TARGET")
    print("==============================")

    print(result)


if __name__ == "__main__":

    test_adult()
    test_minor()