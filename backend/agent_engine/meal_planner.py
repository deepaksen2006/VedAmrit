import pandas as pd


DOSHA_MEAL_GUIDANCE = {
    "Vata": {
        "breakfast_note": "Prefer warm, soft, nourishing foods.",
        "lunch_note": "Choose a warm, filling, freshly cooked lunch.",
        "dinner_note": "Choose a light but warm and grounding dinner.",
    },
    "Pitta": {
        "breakfast_note": "Prefer cooling, mild, and less spicy foods.",
        "lunch_note": "Choose a balanced meal; avoid excessive chilli, sourness, and oil.",
        "dinner_note": "Keep dinner cooling, mild, and easy to digest.",
    },
    "Kapha": {
        "breakfast_note": "Keep breakfast light, warm, and not overly sweet.",
        "lunch_note": "Choose warm, lightly spiced, high-fibre foods.",
        "dinner_note": "Keep dinner very light and avoid heavy or oily food.",
    },
}


def select_food_names(foods: pd.DataFrame) -> list[str]:
    if foods.empty or "Food_Item" not in foods.columns:
        return []

    return (
        foods["Food_Item"]
        .dropna()
        .astype(str)
        .str.strip()
        .loc[lambda values: values.ne("")]
        .drop_duplicates()
        .tolist()
    )


def create_meal_plan(
    foods: pd.DataFrame,
    primary_dosha: str,
    plan_days: int = 1,
) -> dict:
    """Create a 1-day or 7-day meal plan."""
    primary_dosha = primary_dosha.strip().title()

    if primary_dosha not in DOSHA_MEAL_GUIDANCE:
        raise ValueError("Primary dosha must be Vata, Pitta, or Kapha.")

    if plan_days not in {1, 7}:
        raise ValueError("Plan duration must be 1 or 7 days.")

    food_names = select_food_names(foods)

    if not food_names:
        raise ValueError("No compatible foods were found in the dataset.")

    guidance = DOSHA_MEAL_GUIDANCE[primary_dosha]
    daily_plans = []

    # Three food suggestions for each meal = nine choices daily.
    for day_number in range(1, plan_days + 1):
        start = ((day_number - 1) * 9) % len(food_names)

        selected = [
            food_names[(start + index) % len(food_names)]
            for index in range(9)
        ]

        daily_plans.append({
            "day": day_number,
            "breakfast": {
                "suggested_foods": selected[0:3],
                "guidance": guidance["breakfast_note"],
            },
            "lunch": {
                "suggested_foods": selected[3:6],
                "guidance": guidance["lunch_note"],
            },
            "dinner": {
                "suggested_foods": selected[6:9],
                "guidance": guidance["dinner_note"],
            },
        })

    return {
        "primary_dosha": primary_dosha,
        "plan_days": plan_days,
        "daily_plans": daily_plans,
    }


def format_meal_plan(meal_plan: dict) -> str:
    output = [
        f"{meal_plan['plan_days']}-Day Diet Plan for "
        f"{meal_plan['primary_dosha']} Constitution"
    ]

    for day_plan in meal_plan["daily_plans"]:
        breakfast = ", ".join(day_plan["breakfast"]["suggested_foods"])
        lunch = ", ".join(day_plan["lunch"]["suggested_foods"])
        dinner = ", ".join(day_plan["dinner"]["suggested_foods"])

        output.extend([
            f"\nDay {day_plan['day']}",
            f"Breakfast: {breakfast}",
            f"Tip: {day_plan['breakfast']['guidance']}",
            f"Lunch: {lunch}",
            f"Tip: {day_plan['lunch']['guidance']}",
            f"Dinner: {dinner}",
            f"Tip: {day_plan['dinner']['guidance']}",
        ])

    return "\n".join(output)