from typing import Any


def final_response_agent(state: dict[str, Any]) -> dict[str, Any]:
    return {"final_response": "DOCTOR VERIFICATION REQUIRED\nAI-GENERATED — DOCTOR REVIEW REQUIRED\nReview assessment, diet and lifestyle drafts in the doctor workflow."}
