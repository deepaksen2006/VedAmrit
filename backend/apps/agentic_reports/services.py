from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from django.db import transaction
from django.utils import timezone

from agent_engine.contracts import parse_agent_engine_request
from agent_engine.integration_service import generate_personalized_plan

from apps.agentic_reports.models import (
    AgentRun,
    AgentRunStatus,
    AssessmentResult,
    DoctorReview,
    DoctorReviewStatus,
    FinalReport,
    ValidationStatus,
    WellnessPlan,
)
from apps.doctors.models import DoctorProfile


class AgentRunFailed(RuntimeError):
    def __init__(self, agent_run: AgentRun, message: str):
        super().__init__(message)
        self.agent_run = agent_run


@dataclass(frozen=True)
class WellnessPlanCreationResult:
    assessment: AssessmentResult
    agent_run: AgentRun
    wellness_plan: WellnessPlan


def _assessment_payload(payload: dict[str, Any]) -> dict[str, Any]:
    return (
        payload.get("assessment")
        or payload.get("assessments")
        or payload.get("assessment_profile")
        or {}
    )


def _validation_status(validation: dict[str, Any]) -> str:
    raw = str(
        validation.get("status")
        or validation.get("overall_status")
        or ValidationStatus.REVIEW_REQUIRED
    ).upper()
    if raw in {ValidationStatus.PASS, ValidationStatus.FAIL, ValidationStatus.REVIEW_REQUIRED}:
        return raw
    if raw in {"ERROR", "FAILED"}:
        return ValidationStatus.FAIL
    return ValidationStatus.REVIEW_REQUIRED


def _first_error(response: dict[str, Any]) -> dict[str, Any]:
    errors = ((response.get("workflow") or {}).get("errors") or [])
    return errors[0] if errors else {
        "code": "agent_execution_failed",
        "message": "Agent execution failed.",
    }


def create_wellness_plan(user, payload: dict[str, Any]) -> WellnessPlanCreationResult:
    request = parse_agent_engine_request(payload)
    assessment_block = _assessment_payload(payload)
    assessment = AssessmentResult.objects.create(
        user=user,
        prakriti_result=request.prakriti_result,
        vikriti_result=request.vikriti_result,
        agni_result=request.agni_result,
        canonical_payload=assessment_block,
    )
    agent_run = AgentRun.objects.create(
        user=user,
        assessment=assessment,
        status=AgentRunStatus.RUNNING,
        request_payload=payload,
    )

    try:
        response = generate_personalized_plan(payload)
    except Exception as exc:
        agent_run.mark_failed(
            code="agent_execution_exception",
            message=str(exc),
            details={"exception_class": exc.__class__.__name__},
        )
        raise AgentRunFailed(agent_run, str(exc)) from exc

    workflow = response.get("workflow") or {}
    rag_status = workflow.get("rag") or {}
    if workflow.get("status") == "error" or workflow.get("errors"):
        error = _first_error(response)
        agent_run.mark_failed(
            code=str(error.get("code", "agent_execution_failed")),
            message=str(error.get("message", "Agent execution failed.")),
            details=error.get("details") or {},
            response_payload=response,
            rag_status=rag_status,
        )
        raise AgentRunFailed(agent_run, agent_run.error_message)

    agent_run.mark_completed(response_payload=response, rag_status=rag_status)
    wellness_plan = WellnessPlan.objects.create(
        user=user,
        assessment=assessment,
        agent_run=agent_run,
        version=1,
        diet_plan=response.get("diet_plan") or {},
        lifestyle_plan=response.get("lifestyle_plan") or {},
        assessment_summary=response.get("assessment") or {},
        validation=response.get("validation") or {},
        validation_status=_validation_status(response.get("validation") or {}),
        workflow_status=str(workflow.get("workflow_status") or workflow.get("status") or ""),
        doctor_status=DoctorReviewStatus.PENDING,
    )
    return WellnessPlanCreationResult(
        assessment=assessment,
        agent_run=agent_run,
        wellness_plan=wellness_plan,
    )


@transaction.atomic
def create_plan_revision(
    *,
    parent_plan: WellnessPlan,
    agent_run: AgentRun | None = None,
    diet_plan: dict[str, Any] | None = None,
    lifestyle_plan: dict[str, Any] | None = None,
    assessment_summary: dict[str, Any] | None = None,
    validation: dict[str, Any] | None = None,
    workflow_status: str | None = None,
) -> WellnessPlan:
    latest = (
        WellnessPlan.objects.select_for_update()
        .filter(lineage_id=parent_plan.lineage_id)
        .order_by("-version")
        .first()
    )
    next_version = (latest.version if latest else parent_plan.version) + 1
    validation_data = validation if validation is not None else parent_plan.validation
    return WellnessPlan.objects.create(
        user=parent_plan.user,
        assessment=parent_plan.assessment,
        agent_run=agent_run,
        lineage_id=parent_plan.lineage_id,
        parent_plan=parent_plan,
        version=next_version,
        diet_plan=diet_plan if diet_plan is not None else parent_plan.diet_plan,
        lifestyle_plan=lifestyle_plan if lifestyle_plan is not None else parent_plan.lifestyle_plan,
        assessment_summary=assessment_summary if assessment_summary is not None else parent_plan.assessment_summary,
        validation=validation_data,
        validation_status=_validation_status(validation_data),
        workflow_status=workflow_status if workflow_status is not None else parent_plan.workflow_status,
        doctor_status=DoctorReviewStatus.PENDING,
    )


@transaction.atomic
def record_doctor_review(
    *,
    wellness_plan: WellnessPlan,
    doctor: DoctorProfile,
    decision: str,
    feedback: str = "",
    comments: str = "",
) -> DoctorReview:
    if decision not in DoctorReviewStatus.values:
        raise ValueError(f"Unsupported doctor review decision: {decision}")
    reviewed_at = timezone.now() if decision != DoctorReviewStatus.PENDING else None
    review = DoctorReview.objects.create(
        wellness_plan=wellness_plan,
        doctor=doctor,
        decision=decision,
        feedback=feedback,
        comments=comments,
        reviewed_at=reviewed_at,
    )
    wellness_plan.doctor_status = decision
    wellness_plan.save(update_fields=["doctor_status", "updated_at"])
    return review


@transaction.atomic
def finalize_report(
    *,
    wellness_plan: WellnessPlan,
    report_path: str,
    metadata: dict[str, Any] | None = None,
) -> FinalReport:
    if wellness_plan.doctor_status != DoctorReviewStatus.APPROVED:
        raise ValueError("Final report cannot be finalized before doctor approval.")
    return FinalReport.objects.create(
        wellness_plan=wellness_plan,
        user=wellness_plan.user,
        report_path=report_path,
        report_metadata=metadata or {},
    )
