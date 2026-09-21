from __future__ import annotations

from typing import Any
from copy import deepcopy
import csv
from pathlib import Path

NUTRIENTS = ("calories", "protein_g", "carbohydrates_g", "fat_g", "fiber_g")
_DISPLAY_ALIASES = {
    "carbohydrates_g": ("carbohydrates_g", "carbs_g"),
    "fiber_g": ("fiber_g", "fibre_g"),
}


def _number(value: Any) -> float | None:
    if isinstance(value, bool):
        return None
    try:
        number = float(value)
    except (TypeError, ValueError):
        return None
    return number if number == number and abs(number) != float("inf") else None


def _nutrient_values(data: dict[str, Any]) -> dict[str, float | None]:
    values: dict[str, float | None] = {}
    for nutrient in NUTRIENTS:
        aliases = _DISPLAY_ALIASES.get(nutrient, (nutrient,))
        values[nutrient] = next(
            (number for key in aliases if (number := _number(data.get(key))) is not None),
            None,
        )
    return values


def _sum_items(items: list[dict[str, Any]]) -> tuple[dict[str, float], list[str]]:
    totals = {nutrient: 0.0 for nutrient in NUTRIENTS}
    missing: list[str] = []
    for index, item in enumerate(items, start=1):
        if not str(item.get("food_name") or item.get("name") or "").strip():
            missing.append(f"Food {index}: missing food name.")
        portion = _number(item.get("portion_g"))
        if portion is None or portion <= 0:
            missing.append(f"Food {index}: missing or invalid portion size.")
        values = _nutrient_values(item.get("nutrition", item))
        for nutrient, value in values.items():
            if value is None or value < 0:
                missing.append(f"Food {index}: missing or invalid {nutrient}.")
                totals[nutrient] = None
            else:
                if totals[nutrient] is not None:
                    totals[nutrient] += value
    return {key: round(value, 1) if value is not None else None for key, value in totals.items()}, missing


def _matches(left: dict[str, Any], right: dict[str, float]) -> bool:
    for nutrient, expected in right.items():
        actual = _nutrient_values(left).get(nutrient)
        if actual is None or round(actual, 1) != expected:
            return False
    return True


def selected_diet(state: dict[str, Any]) -> dict[str, Any]:
    """Read-only contract adapter outside the trusted diet service."""
    edited = (state.get("doctor_review") or {}).get("edited_diet_plan")
    payload = edited if edited is not None else state.get("agent2_output") or state.get("meal_plan") or state.get("diet_plan") or {}
    diet = deepcopy(payload.get("diet_plan", payload))
    if "days" not in diet:
        meals = [{"meal": name, **meal} for name, meal in diet.items() if isinstance(meal, dict) and "foods" in meal]
        diet = {"days": [{"day": 1, "meals": meals}]} if meals else {"days": []}
    return diet


def _assessment_status(result: dict[str, Any], required: tuple[str, ...]) -> str:
    if not result:
        return "Review Required"
    if any(key not in result for key in required):
        return "Review Required"
    return "Preliminary AI-based Assessment - Doctor Verification Required"


def _items(value: Any) -> set[str]:
    if isinstance(value, str):
        value = value.split(",")
    if not isinstance(value, (list, tuple, set)):
        return set()
    return {str(item).strip().lower() for item in value if str(item).strip()}


def validate_report_state(state: dict[str, Any]) -> dict[str, Any]:
    """Create one normalized, printable report snapshot and validation result."""
    profile = state.get("patient_profile") or state.get("user_input") or {}
    diet = selected_diet(state)
    days = diet.get("days") or []
    warnings: list[str] = []
    failed_checks: list[str] = []

    calculated_days: list[dict[str, Any]] = []
    source_path = Path(state.get("food_dataset_path") or Path(__file__).resolve().parents[1] / "data" / "demo_foods.csv")
    sources = {}
    try:
        with source_path.open(encoding="utf-8-sig", newline="") as handle:
            sources = {row["food_id"]: row for row in csv.DictReader(handle)}
    except (OSError, KeyError, csv.Error):
        warnings.append("NUTRITION REVIEW REQUIRED: source nutrition dataset unavailable.")
    plan_total = {nutrient: 0.0 for nutrient in NUTRIENTS}
    for day in days:
        calculated_meals: list[dict[str, Any]] = []
        day_total = {nutrient: 0.0 for nutrient in NUTRIENTS}
        for meal in day.get("meals", []):
            for food in meal.get("foods") or []:
                for nutrient, value in _nutrient_values(food.get("nutrition", food)).items():
                    if value is not None and value < 0:
                        failed_checks.append(f"Negative nutrient value: {food.get('food_name')} {nutrient}.")
                source = sources.get(str(food.get("food_id")))
                portion = _number(food.get("portion_g"))
                if source is None or portion is None:
                    warnings.append(f"NUTRITION REVIEW REQUIRED: cannot verify portion nutrition for {food.get('food_name') or 'unnamed food'} against source.")
                    continue
                if str(source.get("food_name", "")).lower() != str(food.get("food_name", "")).lower():
                    failed_checks.append("Food name does not match source food identifier.")
                actual = _nutrient_values(food.get("nutrition", food))
                for nutrient, column in (("calories", "calories_per_100g"), ("protein_g", "protein_g"), ("carbohydrates_g", "carbs_g"), ("fat_g", "fat_g"), ("fiber_g", "fiber_g")):
                    base = _number(source.get(column))
                    if base is None:
                        warnings.append(f"Source {nutrient} unavailable for {food.get('food_name')}.")
                    elif actual[nutrient] is not None and abs(round(base * portion / 100, 1) - actual[nutrient]) > .11:
                        failed_checks.append(f"Portion nutrition mismatch: {food.get('food_name')} {nutrient}.")
                # Source ingredients remain validation metadata, not edits to Agent 2.
                food["ingredients"] = source.get("ingredients", "").split("|")
                food["allergens"] = source.get("allergens", "").split("|")
                food["diet_type"] = source.get("diet_type", "")
                food["avoid_or_limit_when"] = source.get("avoid_or_limit_when", "").split("|")
            calculated, missing = _sum_items(meal.get("foods") or [])
            warnings.extend(
                f"Day {day.get('day', 'unknown')} {meal.get('meal', 'meal')}: {issue}"
                for issue in missing
            )
            stored = meal.get("meal_total") or {}
            if stored and not _matches(stored, calculated):
                failed_checks.append(
                    f"Meal total mismatch in day {day.get('day', 'unknown')} {meal.get('meal', 'meal')}."
                )
            calculated_meals.append({**meal, "meal_total": calculated})
            for nutrient in NUTRIENTS:
                day_total[nutrient] = day_total[nutrient] + calculated[nutrient] if day_total[nutrient] is not None and calculated[nutrient] is not None else None
        day_total = {key: round(value, 1) if value is not None else None for key, value in day_total.items()}
        stored_day = day.get("daily_total") or {}
        if stored_day and not _matches(stored_day, day_total):
            failed_checks.append(f"Daily total mismatch in day {day.get('day', 'unknown')}.")
        calculated_days.append({**day, "meals": calculated_meals, "daily_total": day_total})
        for nutrient in NUTRIENTS:
            plan_total[nutrient] = plan_total[nutrient] + day_total[nutrient] if plan_total[nutrient] is not None and day_total[nutrient] is not None else None

    plan_total = {key: round(value, 1) if value is not None else None for key, value in plan_total.items()}
    if not days:
        warnings.append("No detailed meal plan is available.")
    if not profile:
        failed_checks.append("Patient profile is missing.")
    for field in ("allergies", "medicine_allergies", "food_intolerances", "foods_to_avoid", "health_conditions", "medication_restrictions", "pregnancy_information", "significant_dietary_restrictions"):
        if profile.get(field) in (None, "", [], {}):
            warnings.append(f"REVIEW REQUIRED: {field.replace('_', ' ')} is unknown; record an explicit none or not applicable when confirmed.")

    prakriti = state.get("prakriti_result") or {}
    vikriti = state.get("vikriti_result") or {}
    agni = state.get("agni_result") or {}
    assessment_checks = {
        "prakriti": _assessment_status(prakriti, ("scores", "primary_dosha", "secondary_dosha")),
        "vikriti": _assessment_status(vikriti, ("scores", "dominant_dosha")),
        "agni": _assessment_status(agni, ("category_scores", "status")),
    }
    for name, status in assessment_checks.items():
        if status == "Review Required":
            failed_checks.append(f"{name.title()} assessment is incomplete.")

    allergies = _items(profile.get("allergies")) | _items(profile.get("food_intolerances"))
    exclusions = _items(profile.get("foods_to_avoid", profile.get("food_exclusions", [])))
    exclusions |= _items((state.get("doctor_review") or {}).get("food_restrictions"))
    exclusions |= _items(profile.get("medication_restrictions"))
    exclusions |= _items(profile.get("doctor_restrictions"))
    conditions = _items(profile.get("health_conditions")) - {"none", "not applicable"}
    for day in calculated_days:
        for meal in day.get("meals", []):
            for food in meal.get("foods", []):
                food_name = str(food.get("food_name") or food.get("name") or "").strip().lower()
                ingredients = _items(food.get("ingredients")) | _items(food.get("allergens"))
                if conditions & _items(food.get("avoid_or_limit_when")):
                    failed_checks.append(f"Condition-related food restriction: {food_name}.")
                preference = str(profile.get("dietary_preference") or "").lower()
                food_type = str(food.get("diet_type") or "").lower()
                if preference == "vegan" and food_type and food_type != "vegan" or preference == "vegetarian" and food_type in {"non-vegetarian", "non_vegetarian"}:
                    failed_checks.append(f"Dietary preference conflict: {food_name}.")
                if any(token in food_name for token in (allergies | exclusions) - {"none", "not applicable"}) or allergies & ingredients or exclusions & ingredients:
                    failed_checks.append(f"Restricted food selected: {food_name}.")

    doctor_review = state.get("doctor_review") or {}
    lifestyle = doctor_review.get("edited_lifestyle_recommendations")
    if lifestyle is None:
        lifestyle = state.get("lifestyle_plan") or {}
    warnings.extend(lifestyle.get("safety_flags") or [])
    warnings.extend((state.get("validation_result") or {}).get("warnings") or [])
    for field in ("current_medicines", "doctor_restrictions"):
        if profile.get(field):
            warnings.append(f"REVIEW REQUIRED: doctor must reconcile patient-provided {field.replace('_', ' ')} with the plan.")
    for field in ("health_conditions", "medication_restrictions", "pregnancy_information", "symptom_severity", "significant_dietary_restrictions"):
        if profile.get(field) and str(profile[field]).lower() not in {"none", "not applicable"}:
            warnings.append(f"REVIEW REQUIRED: doctor must reconcile {field.replace('_', ' ')} with the diet and lifestyle draft.")
    medicine_allergies = _items(profile.get("medicine_allergies")) - {"none", "not applicable"}
    for medicine in doctor_review.get("medicines") or []:
        if any(allergy in str(medicine.get("name") or "").lower() for allergy in medicine_allergies):
            failed_checks.append("Medicine allergy conflict requires correction.")
    approved = doctor_review.get("approved") is True
    if failed_checks:
        report_status = "Changes Required"
    elif doctor_review:
        report_status = "Under Doctor Review"
    else:
        report_status = "AI Draft"
    status = "PASS" if not failed_checks and not warnings else "REVIEW_REQUIRED"
    weekly = []
    for offset in range(0, len(calculated_days), 7):
        totals = {}
        for nutrient in NUTRIENTS:
            values = [day["daily_total"][nutrient] for day in calculated_days[offset:offset + 7]]
            totals[nutrient] = round(sum(values), 1) if all(v is not None for v in values) else None
        weekly.append({"week": offset // 7 + 1, "days": len(calculated_days[offset:offset + 7]), "totals": totals})
    target = (state.get("agent2_output") or {}).get("nutrition_target") or diet.get("nutrition_target") or diet.get("daily_target") or {}
    target_values = _nutrient_values(target) if isinstance(target, dict) else {k: None for k in NUTRIENTS}
    target_comparison = [{"day": day.get("day"), "difference": {k: round(day["daily_total"][k] - target_values[k], 1) if day["daily_total"][k] is not None and target_values[k] is not None else None for k in NUTRIENTS}} for day in calculated_days]
    if diet.get("calculated_total") and not _matches(diet["calculated_total"], plan_total):
        failed_checks.append("Full-plan stored total mismatch.")
    for stored_week, calculated_week in zip(diet.get("weekly_totals") or [], weekly):
        if not _matches(stored_week.get("totals", stored_week), calculated_week["totals"]):
            failed_checks.append("Weekly stored total mismatch.")
    warnings = list(dict.fromkeys(warnings))
    failed_checks = list(dict.fromkeys(failed_checks))
    status = "PASS" if not failed_checks and not warnings else "REVIEW_REQUIRED"

    return {
        "profile": profile,
        "diet": {**diet, "days": calculated_days, "calculated_total": plan_total},
        "nutrition_total": plan_total,
        "weekly_totals": weekly,
        "target_versus_actual": target_comparison,
        "assessment_checks": assessment_checks,
        "status": status,
        "warnings": warnings,
        "failed_checks": failed_checks,
        "report_status": report_status,
        "report_version": str(state.get("report_version") or "1.0"),
        "generated_date": str(state.get("generated_date") or "Not provided"),
        "last_updated_date": str(state.get("last_updated_date") or "Not provided"),
        "can_finalize": False,  # Only the version-bound workflow can authorize finalization.
    }
