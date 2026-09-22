from __future__ import annotations

from rest_framework import serializers

from apps.agentic_reports.models import (
    AgentRun,
    AssessmentResult,
    DoctorAssignment,
    DoctorReview,
    DoctorReviewStatus,
    FinalReport,
    PatientProfile,
    WellnessPlan,
)


class DoctorAssignmentSerializer(serializers.ModelSerializer):
    class Meta:
        model = DoctorAssignment
        fields = [
            "id",
            "patient_profile",
            "doctor",
            "active",
            "created_at",
            "ended_at",
        ]
        read_only_fields = ["id", "created_at"]

    def validate_doctor(self, doctor):
        if not doctor.is_verified:
            raise serializers.ValidationError("Only verified doctors can be assigned patients.")
        return doctor


class AgenticPlanGenerationRequestSerializer(serializers.Serializer):
    patient_profile = serializers.DictField(required=False, write_only=True)
    assessment = serializers.DictField(required=False)
    assessments = serializers.DictField(required=False)
    assessment_profile = serializers.DictField(required=False)
    plan_days = serializers.IntegerField(required=False, min_value=1, max_value=7)
    runtime = serializers.DictField(required=False)
    user_id = serializers.IntegerField(required=False, write_only=True)
    doctor_id = serializers.IntegerField(required=False, write_only=True)

    def validate(self, attrs):
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
            "canonical_payload",
            "created_at",
            "updated_at",
        ]
        read_only_fields = fields


def _canonical_assessment_block(attrs):
    return (
        attrs.get("assessment")
        or attrs.get("assessments")
        or attrs.get("assessment_profile")
        or attrs
    )


def _canonical_result(block, name):
    for key in (name, f"{name}_result"):
        value = block.get(key)
        if value is None:
            continue
        if not isinstance(value, dict):
            raise serializers.ValidationError({
                key: "Expected the canonical Android result as a JSON object."
            })
        return value
    raise serializers.ValidationError({
        name: f"Canonical Android {name.title()} result is required."
    })


def _require_any(result, fields, label):
    if not any(result.get(field) not in (None, "", [], {}) for field in fields):
        raise serializers.ValidationError({
            label: f"Canonical Android assessment data is missing {label}."
        })


def _validate_canonical_assessments(prakriti, vikriti, agni):
    _require_any(
        prakriti,
        ("prakritiType", "constitution", "primaryDosha", "primary_dosha"),
        "prakriti.prakritiType",
    )
    _require_any(vikriti, ("dominantDosha", "dominant_dosha"), "vikriti.dominantDosha")
    _require_any(agni, ("dominantAgni", "status", "displayResult"), "agni.dominantAgni")
    for name, result in (("prakriti", prakriti), ("vikriti", vikriti), ("agni", agni)):
        if not isinstance(result.get("scores"), dict):
            raise serializers.ValidationError({
                f"{name}.scores": "Canonical Android scores must be present as a JSON object."
            })


class CanonicalAssessmentRequestSerializer(serializers.Serializer):
    assessment = serializers.DictField(required=False)
    assessments = serializers.DictField(required=False)
    assessment_profile = serializers.DictField(required=False)
    prakriti = serializers.DictField(required=False)
    vikriti = serializers.DictField(required=False)
    agni = serializers.DictField(required=False)
    prakriti_result = serializers.DictField(required=False)
    vikriti_result = serializers.DictField(required=False)
    agni_result = serializers.DictField(required=False)
    symptoms_result = serializers.DictField(required=False)
    user_id = serializers.IntegerField(required=False, write_only=True)

    def validate(self, attrs):
        attrs.pop("user_id", None)
        block = _canonical_assessment_block(attrs)
        prakriti = _canonical_result(block, "prakriti")
        vikriti = _canonical_result(block, "vikriti")
        agni = _canonical_result(block, "agni")
        _validate_canonical_assessments(prakriti, vikriti, agni)
        attrs["prakriti_result"] = prakriti
        attrs["vikriti_result"] = vikriti
        attrs["agni_result"] = agni
        attrs["canonical_payload"] = block
        return attrs


class PatientProfileSerializer(serializers.ModelSerializer):
    user_id = serializers.IntegerField(source="user.id", read_only=True)
    food_intolerances = serializers.ListField(
        child=serializers.CharField(),
        allow_empty=True,
        allow_null=True,
        required=False,
        write_only=True,
    )
    pregnancy_information = serializers.CharField(
        allow_blank=True,
        required=False,
        write_only=True,
    )

    class Meta:
        model = PatientProfile
        fields = [
            "id",
            "user_id",
            "name",
            "age",
            "date_of_birth",
            "gender",
            "height_cm",
            "weight_kg",
            "dietary_preference",
            "goal",
            "activity_level",
            "allergies",
            "intolerances",
            "food_intolerances",
            "foods_to_avoid",
            "health_conditions",
            "current_medicines",
            "medicine_allergies",
            "medication_restrictions",
            "doctor_restrictions",
            "pregnancy_status",
            "pregnancy_information",
            "created_at",
            "updated_at",
        ]
        read_only_fields = ["id", "user_id", "created_at", "updated_at"]

    def _normalize_aliases(self, validated_data):
        if "food_intolerances" in validated_data:
            validated_data["intolerances"] = validated_data.pop("food_intolerances")
        if "pregnancy_information" in validated_data:
            validated_data["pregnancy_status"] = validated_data.pop("pregnancy_information")
        return validated_data

    def validate(self, attrs):
        list_fields = [
            "allergies",
            "intolerances",
            "food_intolerances",
            "foods_to_avoid",
            "health_conditions",
            "current_medicines",
            "medicine_allergies",
            "medication_restrictions",
            "doctor_restrictions",
        ]
        errors = {}
        for field in list_fields:
            if field not in attrs:
                continue
            value = attrs[field]
            if value is None:
                continue
            if not isinstance(value, list) or any(not isinstance(item, str) for item in value):
                errors[field] = "Expected a list of strings, [] for none, or null when not answered."
        if errors:
            raise serializers.ValidationError(errors)
        return attrs

    def create(self, validated_data):
        return super().create(self._normalize_aliases(validated_data))

    def update(self, instance, validated_data):
        return super().update(instance, self._normalize_aliases(validated_data))


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


def public_rag_status(raw_status):
    if not isinstance(raw_status, dict):
        return {}
    allowed_keys = {
        "available",
        "status",
        "embedding_count",
        "chunk_count",
        "chunks",
        "source_count",
        "sources",
        "fallback",
    }
    sanitized = {
        key: value
        for key, value in raw_status.items()
        if key in allowed_keys and value not in (None, "")
    }
    error = raw_status.get("error")
    if isinstance(error, dict):
        sanitized["error"] = {
            key: value
            for key, value in error.items()
            if key in {"code", "message"} and value not in (None, "")
        }
    return sanitized


class WellnessPlanSerializer(serializers.ModelSerializer):
    assessment_id = serializers.IntegerField(source="assessment.id", read_only=True)
    agent_run_id = serializers.IntegerField(source="agent_run.id", read_only=True)
    assignment_id = serializers.IntegerField(source="assignment.id", read_only=True)
    rag_status = serializers.SerializerMethodField()
    validation_warnings = serializers.SerializerMethodField()

    class Meta:
        model = WellnessPlan
        fields = [
            "id",
            "assessment_id",
            "agent_run_id",
            "assignment_id",
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
        return public_rag_status(obj.agent_run.rag_status if obj.agent_run_id else {})

    def get_validation_warnings(self, obj: WellnessPlan):
        validation = obj.validation or {}
        return validation.get("warnings") or validation.get("safety_flags") or []


class WellnessPlanListSerializer(serializers.ModelSerializer):
    assessment_id = serializers.IntegerField(source="assessment.id", read_only=True)
    agent_run_id = serializers.IntegerField(source="agent_run.id", read_only=True)
    assignment_id = serializers.IntegerField(source="assignment.id", read_only=True)

    class Meta:
        model = WellnessPlan
        fields = [
            "id",
            "assessment_id",
            "agent_run_id",
            "assignment_id",
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
        return public_rag_status(obj.agent_run.rag_status if obj.agent_run_id else {})


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
