"""Deliberate patient-facing projection of a doctor-approved snapshot.

Never serialize arbitrary objects into report text. The detailed approved snapshot
and validation remain available to the internal review service.
"""
from agent_engine.services.diet_presentation import compact_diet_timetable


def text_values(value):
    """Only display text/scalars or lists thereof, never unknown dictionaries."""
    if isinstance(value, (str, int, float)) and not isinstance(value, bool):
        return str(value).strip()
    if isinstance(value, (list, tuple)):
        return "; ".join(filter(None, (text_values(item) for item in value)))
    return ""


def selected_fields(source, labels):
    return [(label, text) for key, label in labels
            if (text := text_values(source.get(key)))]


def patient_report_content(approved):
    profile = approved["patient_profile"]
    doctor = approved["doctor_review_data"]
    assessment = approved["assessment"]
    summary = []
    for key, label in (("prakriti", "Prakriti"), ("vikriti", "Vikriti"), ("agni", "Agni")):
        value = assessment.get(key)
        if isinstance(value, dict):
            value = next((value[name] for name in ("constitution", "dominant_dosha", "status", "primary_dosha") if value.get(name)), None)
        if text_values(value):
            summary.append((label, text_values(value)))
    summary += selected_fields(assessment, (("symptoms", "Symptoms reviewed"), ("doctor_assessment", "Doctor's assessment")))

    payload = approved["agent2_diet_data"]
    timetable = compact_diet_timetable({"diet_plan": payload.get("diet_plan", payload)})
    rows = []
    for row in timetable["rows"]:
        cells = []
        for category in timetable["columns"][1:]:
            lines = []
            for meal in row["cells"][category]:
                for food in meal.get("foods", []):
                    name = text_values(food.get("food_name") or food.get("name"))
                    portion = text_values(food.get("portion_g"))
                    if name:
                        lines.append(name + (f" - {portion} g" if portion else ""))
                    for key in ("instructions", "preparation", "notes"):
                        if text_values(food.get(key)):
                            lines.append(text_values(food[key]))
                for key in ("time", "timing", "instructions", "instruction", "notes"):
                    if text_values(meal.get(key)):
                        lines.append(text_values(meal[key]))
            cells.append("\n".join(lines))
        rows.append([f"Day {text_values(row['day'])}", *cells])
    common = selected_fields(timetable["common"], (
        ("foods_to_prefer", "Foods to prefer"), ("foods_to_avoid", "Foods to avoid"),
        ("dietary_restrictions", "Dietary restrictions"), ("meal_timing", "Meal timing"),
        ("general_notes", "General notes"), ("notes", "Notes")))
    # Day-specific instructions must survive compaction too.
    for row in timetable["rows"]:
        for label, value in selected_fields(row["details"], (("notes", "Notes"), ("general_notes", "Notes"), ("meal_timing", "Meal timing"), ("dietary_restrictions", "Restrictions"))):
            common.append((f"Day {row['day']} - {label}", value))
    common += selected_fields(profile, (("allergies", "Food allergies / intolerances"), ("food_intolerances", "Food intolerances"), ("allergy_reactions", "Reported reactions"), ("foods_to_avoid", "Foods avoided"), ("doctor_restrictions", "Existing doctor restrictions")))
    common += selected_fields(doctor, (("food_restrictions", "Doctor's food restrictions"),))
    # The standalone agent may supply these outside diet_plan. Extract names only.
    for key, label in (("priority_foods", "Foods to prefer"), ("excluded_foods", "Foods to avoid")):
        values = payload.get(key) or []
        if isinstance(values, list):
            names = [text_values(value.get("food_name") or value.get("name")) if isinstance(value, dict) else text_values(value) for value in values]
            if any(names):
                common.append((label, "; ".join(dict.fromkeys(filter(None, names)))))
    common = list(dict.fromkeys(common))

    lifestyle_payload = approved["agent3_lifestyle_data"]
    recommendations = lifestyle_payload.get("recommendations", lifestyle_payload.get("lifestyle_plan", {}).get("recommendations", []))
    labels = {"daily_routine": "Daily routine", "meal_timing": "Meal timing", "sleep": "Sleep",
              "activity": "Physical activity", "stress_management": "Stress management",
              "digestion": "Digestion", "wellness": "Wellness"}
    lifestyle = []
    for item in recommendations:
        category = item.get("category")
        if category not in labels:
            continue
        recommendation = text_values(item.get("personalized_recommendation"))
        if recommendation:
            # Agent 3 appends an internal restriction dictionary to this sentence.
            # Those restrictions are rendered separately as reviewed food guidance.
            safety = text_values(item.get("safety_consideration")).split(" Reviewing doctor must reconcile this suggestion with:", 1)[0]
            lifestyle.append({"label": labels[category], "recommendation": recommendation,
                              "safety": safety})
    medicines = []
    for medicine in doctor.get("medicines") or []:
        if medicine.get("source") == "DOCTOR_ENTERED":
            medicines.append([text_values(medicine.get(key)) for key in ("name", "dosage", "frequency", "duration", "instructions")])
    return {
        "patient": selected_fields(profile, (("name", "Name"), ("age", "Age"), ("gender", "Gender"),
            ("height_cm", "Height (cm)"), ("weight_kg", "Weight (kg)"), ("goal", "Wellness goal"),
            ("goal_details", "Goal details"), ("dietary_preference", "Diet type"), ("dietary_preference_details", "Diet details"))),
        "assessment": summary, "columns": timetable["columns"], "rows": rows,
        "food_guidance": common, "lifestyle": lifestyle, "medicines": medicines,
        "doctor": selected_fields(doctor, (("doctor_name", "Doctor name"), ("registration_number", "Registration number"),
            ("qualification", "Qualification / specialization"), ("clinic", "Clinic / hospital"),
            ("observations", "Doctor observations"), ("wellness_advice", "Doctor-approved advice"),
            ("final_comments", "Doctor comments"), ("follow_up_date", "Follow-up date"),
            ("follow_up_instructions", "Follow-up instructions"), ("progress_monitoring", "Progress monitoring"),
            ("review_datetime", "Review date / time"), ("approval_date", "Approval date / time"), ("decision", "Decision"))),
        "doctor_name": text_values(doctor.get("doctor_name")), "typed_name": text_values(doctor.get("signature")),
    }
