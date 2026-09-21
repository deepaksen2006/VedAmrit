"""Structured report records used to keep AI drafts and doctor content separate."""
from __future__ import annotations

from dataclasses import asdict, dataclass, field
from datetime import datetime, timezone
from enum import StrEnum
from typing import Any
from uuid import uuid4


class ReportType(StrEnum):
    AI_ASSESSMENT = "AI_ASSESSMENT"
    FINAL_PERSONALIZED = "FINAL_PERSONALIZED"


class WorkflowStatus(StrEnum):
    DRAFT = "DRAFT"
    AI_ASSESSMENT_GENERATED = "AI_ASSESSMENT_GENERATED"
    AGENT_3_RECOMMENDATIONS_GENERATED = "AGENT_3_RECOMMENDATIONS_GENERATED"
    UNDER_DOCTOR_REVIEW = "UNDER_DOCTOR_REVIEW"
    PENDING_DOCTOR_REVIEW = "PENDING_DOCTOR_REVIEW"
    REJECTED = "REJECTED"
    CHANGES_REQUIRED = "CHANGES_REQUIRED"
    DOCTOR_APPROVED = "DOCTOR_APPROVED"
    FINALIZED = "FINALIZED"
    SENT_TO_PATIENT = "SENT_TO_PATIENT"


class DoctorDecision(StrEnum):
    APPROVE = "APPROVE"
    REQUEST_CHANGES = "REQUEST_CHANGES"
    REJECT = "REJECT"


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat()


@dataclass
class ReportMetadata:
    patient_id: str
    session_id: str
    report_type: ReportType
    report_id: str = field(default_factory=lambda: str(uuid4()))
    report_version: int = 1
    status: WorkflowStatus = WorkflowStatus.DRAFT
    created_at: str = field(default_factory=utc_now)
    updated_at: str = field(default_factory=utc_now)
    approval_timestamp: str | None = None

    def as_dict(self) -> dict[str, Any]:
        return asdict(self)
