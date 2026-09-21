from agent_engine.agents.diet_context import prepare_diet_context


def main():

    # Simulated output from Agent 1
    state = {

        "patient_profile": {
            "name": "Demo User",
            "age": 20,
            "gender": "female",
            "dietary_preference": "vegan",
            "allergies": ["onion"],
            "foods_to_avoid": ["capsicum"],
            "health_conditions": [],
            "goal": "general wellness",
        },

        "prakriti_result": {
            "scores": {
                "Vata": 6,
                "Pitta": 10,
                "Kapha": 5,
            },
            "percentages": {
                "Vata": 28.57,
                "Pitta": 47.62,
                "Kapha": 23.81,
            },
            "primary_dosha": "Pitta",
            "secondary_dosha": "Vata",
            "constitution": "Pitta-Vata",
        },

        "vikriti_result": {
            "Vata": 18,
            "Pitta": 25,
            "Kapha": 15,
            "dominant_dosha": "Pitta",
        },

        "agni_result": {
            "status": "Mandagni",
            "category_scores": {
                "Mandagni": 7,
                "Vishamagni": 2,
                "Samagni": 1,
                "Tikshnagni": 1,
            },
            "category_percentages": {
                "Mandagni": 63.64,
                "Vishamagni": 18.18,
                "Samagni": 9.09,
                "Tikshnagni": 9.09,
            },
        },
    }

    result = prepare_diet_context(state)

    print("\n==============================")
    print("DIET CONTEXT TEST")
    print("==============================")

    print("\nPATIENT:")
    print(result["patient_profile"])

    print("\nPRAKRITI:")
    print(result["prakriti"])

    print("\nVIKRITI:")
    print(result["vikriti"])

    print("\nAGNI:")
    print(result["agni"])


if __name__ == "__main__":
    main()