from __future__ import annotations

from rest_framework import serializers

from apps.agentic_reports.models import (
    AgentRun,
    AssessmentResult,
    DoctorReview,
    DoctorReviewStatus,
    FinalReport,
    WellnessPlan,
)


class AgenticPlanGenerationRequestSerializer(serializers.Serializer):
    patient_profile = serializers.DictField()
    assessment = serializers.DictField(required=False)
    assessments = serializers.DictField(required=False)
    assessment_profile = serializers.DictField(required=False)
    plan_days = serializers.IntegerField(required=False, min_value=1, max_value=7)
    runtime = serializers.DictField(required=False)
    user_id = serializers.IntegerField(required=False, write_only=True)
    doctor_id = serializers.IntegerField(required=False, write_only=True)

    def validate(self, attrs):
        if not any(key in attrs for key in ("assessment", "assessments", "assessment_profile")):
            raise serializers.ValidationError("Canonical assessment data is required.")
        attrs.pop("user_id", None)
        attrs.pop("doctor_id", None)
        return attrs


class AssessmentResultSerializer(serializers.ModelSerializer):
    class Meta:
        model = AssessmentResult
        fields = [
            "id",
            "prakriti_result",
            "vikriti_result",
            "agni_result",
            "created_at",
            "updated_at",
        ]
        read_only_fields = fields


class ValidationSerializer(serializers.Serializer):
    status = serializers.CharField(required=False)
    warnings = serializers.ListField(required=False)
    failed_checks = serializers.ListField(required=False)
    issues = serializers.ListField(required=False)


class DoctorReviewSerializer(serializers.ModelSerializer):
    doctor_id = serializers.IntegerField(source="doctor.id", read_only=True)
    doctor_name = serializers.CharField(source="doctor.full_name", read_only=True)

    class Meta:
        model = DoctorReview
        fields = [
            "id",
            "doctor_id",
            "doctor_name",
            "decision",
            "feedback",
            "comments",
            "reviewed_at",
            "created_at",
        ]
        read_only_fields = fields


class WellnessPlanSerializer(serializers.ModelSerializer):
    assessment_id = serializers.IntegerField(source="assessment.id", read_only=True)
    agent_run_id = serializers.IntegerField(source="agent_run.id", read_only=True)
    rag_status = serializers.SerializerMethodField()
    validation_warnings = serializers.SerializerMethodField()

    class Meta:
        model = WellnessPlan
        fields = [
            "id",
            "assessment_id",
            "agent_run_id",
            "lineage_id",
            "parent_plan_id",
            "version",
            "assessment_summary",
            "diet_plan",
            "lifestyle_plan",
            "validation",
            "validation_status",
            "validation_warnings",
            "rag_status",
            "doctor_status",
            "workflow_status",
            "created_at",
            "updated_at",
        ]
        read_only_fields = fields

    def get_rag_status(self, obj: WellnessPlan):
        return (obj.agent_run.rag_status if obj.agent_run_id else {}) or {}

    def get_validation_warnings(self, obj: WellnessPlan):
        validation = obj.validation or {}
        return validation.get("warnings") or validation.get("safety_flags") or []


class WellnessPlanListSerializer(serializers.ModelSerializer):
    assessment_id = serializers.IntegerField(source="assessment.id", read_only=True)
    agent_run_id = serializers.IntegerField(source="agent_run.id", read_only=True)

    class Meta:
        model = WellnessPlan
        fields = [
            "id",
            "assessment_id",
            "agent_run_id",
            "lineage_id",
            "parent_plan_id",
            "version",
            "validation_status",
            "doctor_status",
            "workflow_status",
            "created_at",
            "updated_at",
        ]
        read_only_fields = fields


class WellnessPlanStatusSerializer(serializers.ModelSerializer):
    agent_run_status = serializers.CharField(source="agent_run.status", read_only=True)
    rag_status = serializers.SerializerMethodField()

    class Meta:
        model = WellnessPlan
        fields = [
            "id",
            "version",
            "agent_run_status",
            "validation_status",
            "doctor_status",
            "workflow_status",
            "rag_status",
            "created_at",
            "updated_at",
        ]
        read_only_fields = fields

    def get_rag_status(self, obj: WellnessPlan):
        return (obj.agent_run.rag_status if obj.agent_run_id else {}) or {}


class DoctorDecisionRequestSerializer(serializers.Serializer):
    feedback = serializers.CharField(required=False, allow_blank=True)
    comments = serializers.CharField(required=False, allow_blank=True)


class DoctorChangesRequestSerializer(DoctorDecisionRequestSerializer):
    feedback = serializers.CharField(required=True, allow_blank=False)


class FinalReportSerializer(serializers.ModelSerializer):
    report_reference = serializers.SerializerMethodField()

    class Meta:
        model = FinalReport
        fields = [
            "id",
            "wellness_plan_id",
            "report_reference",
            "report_metadata",
            "finalized_at",
            "created_at",
        ]
        read_only_fields = fields

    def get_report_reference(self, obj: FinalReport):
        request = self.context.get("request")
        path = f"/api/agentic/plans/{obj.wellness_plan_id}/report/"
        return request.build_absolute_uri(path) if request else path
