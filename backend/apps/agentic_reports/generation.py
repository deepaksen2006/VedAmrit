"""Durable recovery for synchronous generation, using existing AgentRun JSON."""
from django.db import transaction
from django.contrib.auth import get_user_model
from rest_framework.exceptions import APIException
from .assessment_sync import content_version, AssessmentConflict
from .models import AgentRun, AgentRunStatus


def profile_payload(profile):
    fields = ("name", "age", "gender", "dietary_preference", "goal", "activity_level", "allergies",
              "foods_to_avoid", "health_conditions", "current_medicines", "medicine_allergies",
              "medication_restrictions", "doctor_restrictions")
    result = {field: getattr(profile, field) for field in fields}
    result.update(height_cm=float(profile.height_cm) if profile.height_cm is not None else None,
                  weight_kg=float(profile.weight_kg) if profile.weight_kg is not None else None,
                  food_intolerances=profile.intolerances, pregnancy_information=profile.pregnancy_status)
    return result


def profile_version(profile):
    return content_version(profile_payload(profile))


def inputs_current(plan):
    from .assessment_sync import assessment_version
    from .models import AssessmentResult, PatientProfile
    latest = AssessmentResult.objects.filter(user_id=plan.user_id).order_by("-created_at", "-pk").first()
    profile = PatientProfile.objects.filter(user_id=plan.user_id).first()
    runtime = (plan.agent_run.request_payload.get("runtime") or {}) if plan.agent_run_id else {}
    return bool(latest and profile and latest.pk == plan.assessment_id
                and assessment_version(latest) == runtime.get("assessment_version")
                and profile_version(profile) == runtime.get("profile_version"))


class GenerationPending(APIException):
    status_code = 202
    default_detail = "Generation is already running. Check generation status instead of starting another run."


@transaction.atomic
def claim_generation(user, assessment, engine_payload, generation_id):
    get_user_model().objects.select_for_update().get(pk=user.pk)
    fingerprint = content_version({key: engine_payload.get(key, 7 if key == "plan_days" else None) for key in ("patient_profile", "assessment", "plan_days")})
    if generation_id:
        existing = AgentRun.objects.filter(user=user, request_payload__runtime__generation_id=str(generation_id)).first()
        if existing:
            if existing.request_payload["runtime"].get("generation_fingerprint") != fingerprint:
                raise AssessmentConflict("This generation ID belongs to different patient inputs. Reload profile/assessments before generating again.")
            return existing, False
    engine_payload["runtime"].update(generation_id=str(generation_id) if generation_id else None, generation_fingerprint=fingerprint)
    return AgentRun.objects.create(user=user, assessment=assessment, status=AgentRunStatus.RUNNING, request_payload=engine_payload), True


def generation_status(run):
    plan = getattr(run, "wellness_plan", None)
    runtime = run.request_payload.get("runtime") or {}
    return {"generation_id": runtime.get("generation_id"), "status": run.status, "plan_id": plan.pk if plan else None,
            "assessment_id": run.assessment_id, "assessment_version": runtime.get("assessment_version"),
            "profile_version": runtime.get("profile_version"), "error_code": run.error_code,
            "message": "Generation failed. Review the profile/assessment and start a new attempt." if run.status == AgentRunStatus.FAILED else ""}
