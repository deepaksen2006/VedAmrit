from __future__ import annotations

import uuid

from django.conf import settings
from django.core.exceptions import ValidationError
from django.db import models
from django.utils import timezone

from apps.doctors.models import DoctorProfile


class AgentRunStatus(models.TextChoices):
    PENDING = "PENDING", "Pending"
    RUNNING = "RUNNING", "Running"
    COMPLETED = "COMPLETED", "Completed"
    FAILED = "FAILED", "Failed"


class ValidationStatus(models.TextChoices):
    PASS = "PASS", "Pass"
    REVIEW_REQUIRED = "REVIEW_REQUIRED", "Review required"
    FAIL = "FAIL", "Fail"


class DoctorReviewStatus(models.TextChoices):
    PENDING = "PENDING", "Pending"
    APPROVED = "APPROVED", "Approved"
    CHANGES_REQUESTED = "CHANGES_REQUESTED", "Changes requested"
    REJECTED = "REJECTED", "Rejected"


class AssessmentResult(models.Model):
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.PROTECT,
        related_name="agentic_assessment_results",
    )
    prakriti_result = models.JSONField()
    vikriti_result = models.JSONField()
    agni_result = models.JSONField()
    canonical_payload = models.JSONField(default=dict, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["-created_at"]
        indexes = [
            models.Index(fields=["user", "-created_at"]),
        ]

    def __str__(self) -> str:
        return f"AssessmentResult {self.pk} for user {self.user_id}"


class AgentRun(models.Model):
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.PROTECT,
        related_name="agentic_runs",
    )
    assessment = models.ForeignKey(
        AssessmentResult,
        on_delete=models.PROTECT,
        related_name="agent_runs",
    )
    status = models.CharField(
        max_length=20,
        choices=AgentRunStatus.choices,
        default=AgentRunStatus.PENDING,
        db_index=True,
    )
    started_at = models.DateTimeField(auto_now_add=True)
    completed_at = models.DateTimeField(blank=True, null=True)
    error_code = models.CharField(max_length=120, blank=True)
    error_message = models.TextField(blank=True)
    error_details = models.JSONField(default=dict, blank=True)
    rag_status = models.JSONField(default=dict, blank=True)
    request_payload = models.JSONField(default=dict, blank=True)
    response_payload = models.JSONField(default=dict, blank=True)

    class Meta:
        ordering = ["-started_at"]
        indexes = [
            models.Index(fields=["user", "-started_at"]),
            models.Index(fields=["assessment", "status"]),
        ]

    def mark_completed(self, response_payload: dict, rag_status: dict | None = None) -> None:
        self.status = AgentRunStatus.COMPLETED
        self.completed_at = timezone.now()
        self.response_payload = response_payload
        self.rag_status = rag_status or {}
        self.save(update_fields=["status", "completed_at", "response_payload", "rag_status"])

    def mark_failed(
        self,
        *,
        code: str,
        message: str,
        details: dict | None = None,
        response_payload: dict | None = None,
        rag_status: dict | None = None,
    ) -> None:
        self.status = AgentRunStatus.FAILED
        self.completed_at = timezone.now()
        self.error_code = code
        self.error_message = message
        self.error_details = details or {}
        self.response_payload = response_payload or {}
        self.rag_status = rag_status or {}
        self.save(
            update_fields=[
                "status",
                "completed_at",
                "error_code",
                "error_message",
                "error_details",
                "response_payload",
                "rag_status",
            ]
        )

    def __str__(self) -> str:
        return f"AgentRun {self.pk} ({self.status})"


class WellnessPlan(models.Model):
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.PROTECT,
        related_name="wellness_plans",
    )
    assessment = models.ForeignKey(
        AssessmentResult,
        on_delete=models.PROTECT,
        related_name="wellness_plans",
    )
    agent_run = models.OneToOneField(
        AgentRun,
        on_delete=models.PROTECT,
        related_name="wellness_plan",
        blank=True,
        null=True,
    )
    lineage_id = models.UUIDField(default=uuid.uuid4, db_index=True)
    parent_plan = models.ForeignKey(
        "self",
        on_delete=models.PROTECT,
        related_name="revisions",
        blank=True,
        null=True,
    )
    version = models.PositiveIntegerField(default=1)
    diet_plan = models.JSONField(default=dict, blank=True)
    lifestyle_plan = models.JSONField(default=dict, blank=True)
    assessment_summary = models.JSONField(default=dict, blank=True)
    validation = models.JSONField(default=dict, blank=True)
    validation_status = models.CharField(
        max_length=24,
        choices=ValidationStatus.choices,
        default=ValidationStatus.REVIEW_REQUIRED,
        db_index=True,
    )
    workflow_status = models.CharField(max_length=80, blank=True)
    doctor_status = models.CharField(
        max_length=24,
        choices=DoctorReviewStatus.choices,
        default=DoctorReviewStatus.PENDING,
        db_index=True,
    )
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["lineage_id", "-version"]
        constraints = [
            models.UniqueConstraint(
                fields=["lineage_id", "version"],
                name="unique_wellness_plan_lineage_version",
            ),
        ]
        indexes = [
            models.Index(fields=["user", "-created_at"]),
            models.Index(fields=["assessment", "lineage_id"]),
            models.Index(fields=["doctor_status", "validation_status"]),
        ]

    def __str__(self) -> str:
        return f"WellnessPlan {self.lineage_id} v{self.version}"


class DoctorReview(models.Model):
    wellness_plan = models.ForeignKey(
        WellnessPlan,
        on_delete=models.PROTECT,
        related_name="doctor_reviews",
    )
    doctor = models.ForeignKey(
        DoctorProfile,
        on_delete=models.PROTECT,
        related_name="agentic_reviews",
    )
    decision = models.CharField(
        max_length=24,
        choices=DoctorReviewStatus.choices,
        default=DoctorReviewStatus.PENDING,
        db_index=True,
    )
    feedback = models.TextField(blank=True)
    comments = models.TextField(blank=True)
    reviewed_at = models.DateTimeField(blank=True, null=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-created_at"]
        indexes = [
            models.Index(fields=["doctor", "-created_at"]),
            models.Index(fields=["wellness_plan", "decision"]),
        ]

    def __str__(self) -> str:
        return f"DoctorReview {self.pk} ({self.decision})"


class FinalReport(models.Model):
    wellness_plan = models.OneToOneField(
        WellnessPlan,
        on_delete=models.PROTECT,
        related_name="final_report_record",
    )
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.PROTECT,
        related_name="final_wellness_reports",
    )
    report_path = models.CharField(max_length=500)
    report_metadata = models.JSONField(default=dict, blank=True)
    finalized_at = models.DateTimeField(default=timezone.now)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-finalized_at"]
        indexes = [
            models.Index(fields=["user", "-finalized_at"]),
        ]

    def clean(self) -> None:
        if self.wellness_plan.doctor_status != DoctorReviewStatus.APPROVED:
            raise ValidationError("Final report requires doctor_status=APPROVED.")
        if self.user_id and self.wellness_plan.user_id and self.user_id != self.wellness_plan.user_id:
            raise ValidationError("Final report user must match the wellness plan user.")

    def save(self, *args, **kwargs):
        self.full_clean()
        return super().save(*args, **kwargs)

    def __str__(self) -> str:
        return f"FinalReport for WellnessPlan {self.wellness_plan_id}"
