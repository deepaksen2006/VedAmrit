"""Immutable assessment references and patient-serialized, idempotent submissions."""
from hashlib import sha256
import json

from django.contrib.auth import get_user_model
from django.db import transaction
from rest_framework.exceptions import APIException, ValidationError

from .models import AssessmentResult


class AssessmentConflict(APIException):
    status_code = 409
    default_detail = "Assessment changed. Sync or restore the current assessment before generating a plan."
    default_code = "assessment_conflict"


def assessment_content(assessment):
    return {
        "prakriti": assessment.prakriti_result,
        "vikriti": assessment.vikriti_result,
        "agni": assessment.agni_result,
    }


def content_version(content):
    # Stable across object key order; this is a content version, not a plan version.
    def normalize(value):
        if isinstance(value, dict):
            return {key: normalize(item) for key, item in value.items() if item is not None}
        if isinstance(value, list):
            return [normalize(item) for item in value]
        if isinstance(value, float) and value.is_integer():
            return int(value)
        return value
    return sha256(json.dumps(normalize(content), sort_keys=True, separators=(",", ":"), ensure_ascii=True, allow_nan=False).encode()).hexdigest()


def assessment_version(assessment):
    return content_version(assessment_content(assessment))


@transaction.atomic
def persist_assessment(user, data):
    # No schema change: serialize requests for this patient, including concurrent
    # retries after a committed response was lost. Never trust a submitted user_id.
    get_user_model().objects.select_for_update().get(pk=user.pk)
    submission_id = str(data.get("submission_id") or "")
    content = {key: data[f"{key}_result"] for key in ("prakriti", "vikriti", "agni")}
    version = content_version(content)
    if submission_id:
        existing = AssessmentResult.objects.filter(
            user=user, canonical_payload__sync__submission_id=submission_id
        ).first()
        if existing:
            if assessment_version(existing) != version:
                raise AssessmentConflict("This submission ID already belongs to different assessment content.")
            return existing, False
    canonical = dict(data["canonical_payload"])
    canonical["sync"] = {"submission_id": submission_id or None, "content_version": version}
    return AssessmentResult.objects.create(
        user=user, prakriti_result=content["prakriti"], vikriti_result=content["vikriti"],
        agni_result=content["agni"], canonical_payload=canonical,
    ), True


def select_assessment(user, payload):
    latest = AssessmentResult.objects.filter(user=user).order_by("-created_at", "-pk").first()
    selected_id = payload.get("assessment_id")
    selected_version = payload.get("assessment_version")
    if selected_id is not None:
        selected = AssessmentResult.objects.filter(user=user, pk=selected_id).first()
        if selected is None:
            raise ValidationError({"assessment_id": "Assessment not found for the authenticated patient."})
        if latest is None or latest.pk != selected.pk or assessment_version(selected) != selected_version:
            raise AssessmentConflict()
        latest = selected
    submitted = payload.get("assessment") or payload.get("assessments") or payload.get("assessment_profile")
    if latest and submitted:
        content = {key: submitted.get(key, submitted.get(f"{key}_result")) for key in ("prakriti", "vikriti", "agni")}
        if content_version(content) != assessment_version(latest):
            raise AssessmentConflict("Submitted assessment differs from the persisted assessment. Sync it first.")
    return latest
