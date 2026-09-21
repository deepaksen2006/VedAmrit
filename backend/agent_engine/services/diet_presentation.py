"""Read-only timetable preparation. No meals, days, or recommendations are generated."""
from copy import deepcopy

from agent_engine.services.report_validation import selected_diet


def compact_diet_timetable(patient_state):
    diet = selected_diet(patient_state)
    days = diet.get("days", [])
    columns = list(dict.fromkeys(meal["meal"] for day in days
                                for meal in day.get("meals", []) if meal.get("meal")))
    common = {key: deepcopy(diet[key]) for key in
              ("foods_to_prefer", "foods_to_avoid", "dietary_restrictions", "meal_timing", "general_notes", "daily_target")
              if key in diet and diet[key] not in (None, [], {}, "")}
    # Lift only identical day-level notes present on every supplied day.
    shared_day_keys = []
    for key in ("notes", "general_notes", "meal_timing", "dietary_restrictions"):
        if days and key in days[0] and all(day.get(key) == days[0][key] for day in days):
            if key not in common or common[key] == days[0][key]:
                common[key] = deepcopy(days[0][key])
                shared_day_keys.append(key)
    rows = []
    for day in days:
        cells = {category: [] for category in columns}
        for meal in day.get("meals", []):
            category = meal.get("meal")
            if not category:
                raise ValueError("A supplied meal is missing its category.")
            # Preserve portions and all recommendation details, including multiple meals
            # sharing one label; consumers can display food/portion text compactly.
            cells[category].append(deepcopy({key: value for key, value in meal.items() if key != "meal"}))
        rows.append({"day": day.get("day"), "cells": cells,
                     "details": deepcopy({key: value for key, value in day.items()
                                          if key not in {"day", "meals", *shared_day_keys}})})
    return {"columns": ["Day", *columns], "rows": rows, "common": common}


def diet_presentation_agent(patient_state):
    return {"diet_timetable": compact_diet_timetable(patient_state)}
