"""Shared food restriction matching for allergies and patient exclusions."""

from typing import Any


RESTRICTION_GROUPS = {
    "dairy": {
        "milk", "dairy", "cow milk", "buffalo milk", "goat milk",
        "cream", "butter", "ghee", "cheese", "paneer", "malai",
        "curd", "yogurt", "dahi", "raita", "lassi", "milkshake",
    },
}


def normalize_term(value: Any) -> str:
    return " ".join(str(value or "").strip().lower().replace("-", " ").split())


def restriction_terms(value: Any) -> set[str]:
    term = normalize_term(value)
    terms = {term} if term else set()
    for group in RESTRICTION_GROUPS.values():
        if term in group or any(item in term or term in item for item in group):
            terms.update(group)
    return terms


def food_restriction_text(food: dict[str, Any]) -> set[str]:
    values = [food.get("food_name", "")]
    for field in ("ingredients", "allergens"):
        field_value = food.get(field, [])
        if isinstance(field_value, str):
            values.extend(field_value.replace("|", ",").split(","))
        else:
            values.extend(field_value or [])

    return {normalize_term(value) for value in values if normalize_term(value)}


def matches_food_restriction(food: dict[str, Any], restriction: Any) -> bool:
    food_terms = food_restriction_text(food)
    restricted_terms = restriction_terms(restriction)
    return any(
        food_term == restricted_term
        or food_term in restricted_term
        or restricted_term in food_term
        for food_term in food_terms
        for restricted_term in restricted_terms
    )