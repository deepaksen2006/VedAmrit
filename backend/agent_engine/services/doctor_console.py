"""Local doctor review interface; it does not authenticate a clinical identity."""
from copy import deepcopy
import json
from pathlib import Path

from agent_engine.services.report_validation import validate_report_state
from agent_engine.services.report_workflow import final_content


def review_content(state, review):
    """Provide editable structured content and show the exact result before sign-off."""
    def edit(label, current):
        print(f"\n{label}:\n{json.dumps(current, indent=2, ensure_ascii=True)}")
        while True:
            value = input(f"Replacement {label} JSON (blank keeps current): ").strip()
            if not value:
                return deepcopy(current)
            try:
                parsed = json.loads(value)
                if not isinstance(parsed, dict):
                    raise ValueError("Expected a JSON object")
                return parsed
            except (ValueError, TypeError) as error:
                print(f"Invalid edit: {error}")

    print("\nPatient information (original questionnaire):")
    for key, value in (state.get("patient_profile") or {}).items():
        if key not in {"season", "current_season", "seasonal_guidance"}:
            print(f"{key.replace('_', ' ').title()}: {value}")
    for key in ("prakriti_result", "vikriti_result", "agni_result"):
        print(f"\nOriginal {key}: {state.get(key) or {}}")
    print("Doctor-confirmed assessment fields are stored separately from these AI results.")
    review["edited_diet_plan"] = edit("diet plan (add/remove foods or change portions)", state.get("agent2_output") or {"diet_plan": state.get("meal_plan") or {}})
    review["edited_lifestyle_recommendations"] = edit("lifestyle recommendations", state.get("lifestyle_plan") or {})
    review["medicines"] = []
    while input("Add a doctor-entered medicine/prescription? (yes/no): ").strip().lower() == "yes":
        medicine = {key: input(f"Medicine {key}: ").strip() or None for key in ("name", "dosage", "frequency", "duration", "instructions")}
        medicine["source"] = "DOCTOR_ENTERED"
        review["medicines"].append(medicine)
    print("\nAI-GENERATED — DOCTOR REVIEW REQUIRED\n")
    validation = validate_report_state({**state, "doctor_review": review})
    state["review_validation"] = validation
    for label in ("failed_checks", "warnings"):
        print(f"\n{label.replace('_', ' ').title()}:")
        for message in validation[label]:
            print(f"- {message}")
    warnings = validation["warnings"]
    review["validation_reviewed"] = not warnings or input("Have you reviewed every displayed validation warning? (yes/no): ").strip().lower() == "yes"
    while True:
        decision = input("Decision (APPROVE / REQUEST_CHANGES / REJECT): ").strip().upper()
        if decision in {"APPROVE", "REQUEST_CHANGES", "REJECT"}:
            break
        print("Choose one of the three displayed decisions.")
    review["decision"] = decision
    review["approved"] = decision == "APPROVE"
    if decision == "REQUEST_CHANGES":
        review["change_requests"] = input("Specific changes requested: ").strip() or None
    elif decision == "REJECT":
        review["rejection_reason"] = input("Reason for rejection: ").strip() or None
    if decision == "APPROVE":
        review["signature"] = input("Typed doctor name approving this exact content: ").strip() or None
    return review


def save_session(state, directory="reports"):
    """Persist locally using a session-specific, atomically replaced JSON record."""
    root = Path(directory)
    root.mkdir(parents=True, exist_ok=True)
    path = root / f"session_{state['session_id']}.json"
    temporary = path.with_suffix(".tmp")
    temporary.write_text(json.dumps(state, indent=2, ensure_ascii=True, allow_nan=False), encoding="utf-8")
    temporary.replace(path)
    return path


def main():
    """Resume a saved local session for a later doctor review."""
    import argparse
    from agent_engine.main import get_doctor_review
    from agent_engine.services.report_workflow import TwoReportWorkflow
    from agent_engine.services.two_report_pdf import TwoReportPDFGenerator
    parser = argparse.ArgumentParser(description="Resume a local doctor review")
    parser.add_argument("session_file", type=Path)
    args = parser.parse_args()
    state = json.loads(args.session_file.read_text(encoding="utf-8"))
    for key in ("answers", "questionnaire_answers"):
        if key in state:
            state[key] = {int(k): v for k, v in state[key].items()}
    flow = TwoReportWorkflow(state)
    try:
        flow.begin_doctor_review()
    except PermissionError as error:
        print(str(error))
        return
    review = get_doctor_review(state)
    try:
        if review:
            flow.record_decision(review)
        if review.get("decision") == "APPROVE":
            path = TwoReportPDFGenerator(str(args.session_file.parent)).generate_final_report(state, f"Final_Personalized_Wellness_Report_{flow.session_id}.pdf")
            print(f"DOCTOR APPROVED: {path}")
        else:
            print("Report 2 is unavailable for this decision.")
    except (ValueError, PermissionError) as error:
        flow.request_changes(review)
        print(f"DOCTOR VERIFICATION REQUIRED: {error}")
    save_session(state, str(args.session_file.parent))


if __name__ == "__main__":
    main()
