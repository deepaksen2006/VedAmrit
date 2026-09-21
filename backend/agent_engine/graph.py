from __future__ import annotations

from typing import Any, Literal, TypedDict

from langgraph.graph import END, START, StateGraph

from agent_engine.agents.conditions import condition_agent
from agent_engine.agents.diet_planning import diet_planning_agent
from agent_engine.agents.final_response import final_response_agent
from agent_engine.agents.food_retrieval import food_retrieval_agent
from agent_engine.agents.lifestyle import lifestyle_agent
from agent_engine.agents.nutrition import nutrition_agent
from agent_engine.agents.safety import safety_fact_checker_agent
from agent_engine.assessment_adapter import canonical_assessment_state_agent
from agent_engine.services.diet_presentation import diet_presentation_agent


class DietPlanningState(TypedDict, total=False):
    """Agent state built from canonical VedAmrit Android assessment results."""

    agent2_output: dict[str, Any]
    diet_timetable: dict[str, Any]
    doctor_review: dict[str, Any]
    review_validation: dict[str, Any]
    approved_snapshot: dict[str, Any]
    final_report: dict[str, Any]
    final_report_version: int
    final_report_id: str
    session_id: str
    patient_id: str
    assessment_report: dict[str, Any]
    assessment_pdf_path: str
    workflow_status: str
    user_input: dict[str, Any]
    patient_profile: dict[str, Any]
    prakriti_result: dict[str, Any]
    vikriti_result: dict[str, Any]
    agni_result: dict[str, Any]
    ayurvedic_assessment: dict[str, Any]
    constitution: str
    assessment_source: str
    plan_days: int
    food_dataset_path: str
    report_output_dir: str
    rag_status: dict[str, Any]
    rag_available: bool
    nutrition_context: dict[str, Any]
    condition_context: dict[str, Any]
    food_candidates: list[dict[str, Any]]
    food_retrieval_context: dict[str, Any]
    meal_plan: dict[str, Any]
    lifestyle_plan: dict[str, Any]
    validation_result: dict[str, Any]
    final_response: str
    errors: list[str]


def review_response_agent(state: DietPlanningState) -> dict[str, Any]:
    validation_result = state.get("validation_result", {})
    response_lines = [
        "PLAN REVIEW REQUIRED",
        "",
        "A final diet plan was not shown because one or more validation checks require review.",
        "",
        "Validation checks:",
    ]
    for check_name, status in validation_result.get("checks", {}).items():
        response_lines.append(f"- {check_name}: {status}")
    warnings = validation_result.get("warnings", [])
    failed_checks = validation_result.get("failed_checks", [])
    if warnings:
        response_lines.extend(["", "Warnings:"])
        response_lines.extend(f"- {warning}" for warning in warnings)
    if failed_checks:
        response_lines.extend(["", "Failed checks:"])
        response_lines.extend(f"- {failed_check}" for failed_check in failed_checks)
    response_lines.extend(
        [
            "",
            "Please review the missing safety information or modify the profile before generating a plan.",
        ]
    )
    return {"final_response": "\n".join(response_lines)}


def route_after_safety(state: DietPlanningState) -> Literal["lifestyle_agent", "review_response"]:
    validation_result = state.get("validation_result", {})
    if validation_result.get("status") in {"PASS", "REVIEW_REQUIRED"}:
        return "lifestyle_agent"
    return "review_response"


def assessment_report_node(state: DietPlanningState) -> dict[str, Any]:
    from agent_engine.services.report_workflow import TwoReportWorkflow
    from agent_engine.services.two_report_pdf import TwoReportPDFGenerator

    working = dict(state)
    flow = TwoReportWorkflow(working)
    path = TwoReportPDFGenerator(
        output_dir=working.get("report_output_dir", "reports")
    ).generate_assessment_report(
        working,
        f"AI_Assessment_Report_{flow.session_id}.pdf",
    )
    return {
        "session_id": flow.session_id,
        "patient_id": working["patient_id"],
        "assessment_report": working["assessment_report"],
        "assessment_pdf_path": path,
        "workflow_status": working["workflow_status"],
    }


def pending_doctor_review_node(state: DietPlanningState) -> dict[str, Any]:
    from agent_engine.services.report_workflow import TwoReportWorkflow

    working = dict(state)
    TwoReportWorkflow(working).begin_doctor_review()
    return {
        "review_validation": working["review_validation"],
        "workflow_status": working["workflow_status"],
    }


builder = StateGraph(DietPlanningState)
builder.add_node("canonical_assessment_state", canonical_assessment_state_agent)
builder.add_node("assessment_report", assessment_report_node)
builder.add_node("nutrition_agent", nutrition_agent)
builder.add_node("condition_agent", condition_agent)
builder.add_node("food_retrieval_agent", food_retrieval_agent)
builder.add_node("diet_planning_agent", diet_planning_agent)
builder.add_node("diet_presentation", diet_presentation_agent)
builder.add_node("safety_fact_checker_agent", safety_fact_checker_agent)
builder.add_node("lifestyle_agent", lifestyle_agent)
builder.add_node("final_response", final_response_agent)
builder.add_node("review_response", review_response_agent)
builder.add_node("pending_doctor_review", pending_doctor_review_node)

builder.add_edge(START, "canonical_assessment_state")
builder.add_edge("canonical_assessment_state", "assessment_report")
builder.add_edge("assessment_report", "nutrition_agent")
builder.add_edge("nutrition_agent", "condition_agent")
builder.add_edge("condition_agent", "food_retrieval_agent")
builder.add_edge("food_retrieval_agent", "diet_planning_agent")
builder.add_edge("diet_planning_agent", "diet_presentation")
builder.add_edge("diet_presentation", "safety_fact_checker_agent")
builder.add_conditional_edges(
    "safety_fact_checker_agent",
    route_after_safety,
    {
        "lifestyle_agent": "lifestyle_agent",
        "review_response": "review_response",
    },
)
builder.add_edge("lifestyle_agent", "final_response")
builder.add_edge("final_response", "pending_doctor_review")
builder.add_edge("review_response", "pending_doctor_review")
builder.add_edge("pending_doctor_review", END)

graph = builder.compile()
