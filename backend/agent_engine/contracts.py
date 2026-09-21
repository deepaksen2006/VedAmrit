from __future__ import annotations

from copy import deepcopy
from dataclasses import dataclass
from typing import Any, Mapping


SUPPORTED_DIETARY_PREFERENCES = {
    "vegetarian": "vegetarian",
    "veg": "vegetarian",
    "pure veg": "vegetarian",
    "vegan": "vegan",
    "plant-based": "vegan",
    "non-vegetarian": "non-vegetarian",
    "non vegetarian": "non-vegetarian",
    "non-veg": "non-vegetarian",
    "nonveg": "non-vegetarian",
}

SUPPORTED_GENDERS = {"male", "female", "other"}

CORE_PATIENT_FIELDS = (
    "name",
    "age",
    "gender",
    "height_cm",
    "weight_kg",
    "dietary_preference",
    "goal",
    "activity_level",
)

REQUIRED_EXPLICIT_CONTEXT_FIELDS = (
    "allergies",
    "food_intolerances",
    "foods_to_avoid",
    "health_conditions",
    "medicine_allergies",
    "medication_restrictions",
    "doctor_restrictions",
    "pregnancy_information",
)


@dataclass(frozen=True)
class AgentEngineInputError(Exception):
    code: str
    message: str
    field: str | None = None
    details: dict[str, Any] | None = None

    def as_dict(self) -> dict[str, Any]:
        payload = {"code": self.code, "message": self.message}
        if self.field:
            payload["field"] = self.field
        if self.details:
            payload["details"] = deepcopy(self.details)
        return payload


@dataclass(frozen=True)
class AgentEngineRequest:
    patient_profile: dict[str, Any]
    prakriti_result: dict[str, Any]
    vikriti_result: dict[str, Any]
    agni_result: dict[str, Any]
    plan_days: int
    runtime: dict[str, Any]


def _as_mapping(value: Any, field: str) -> dict[str, Any]:
    if not isinstance(value, Mapping):
        raise AgentEngineInputError(
            "malformed_payload",
            f"{field} must be a JSON object.",
            field,
        )
    return dict(value)


def _missing(field: str) -> AgentEngineInputError:
    return AgentEngineInputError(
        "missing_required_field",
        f"Required field is missing: {field}",
        field,
    )


def _number(value: Any, field: str, minimum: float, maximum: float) -> float:
    if isinstance(value, bool):
        raise AgentEngineInputError("invalid_number", f"{field} must be numeric.", field)
    try:
        parsed = float(value)
    except (TypeError, ValueError) as exc:
        raise AgentEngineInputError("invalid_number", f"{field} must be numeric.", field) from exc
    if not (minimum <= parsed <= maximum):
        raise AgentEngineInputError(
            "invalid_number_range",
            f"{field} must be between {minimum:g} and {maximum:g}.",
            field,
            {"minimum": minimum, "maximum": maximum},
        )
    return parsed


def _integer(value: Any, field: str, minimum: int, maximum: int) -> int:
    parsed = _number(value, field, minimum, maximum)
    if int(parsed) != parsed:
        raise AgentEngineInputError("invalid_integer", f"{field} must be an integer.", field)
    return int(parsed)


def _list(value: Any, field: str) -> list[str]:
    if not isinstance(value, list):
        raise AgentEngineInputError(
            "invalid_list",
            f"{field} must be a JSON array. Send [] when there are no known values.",
            field,
        )
    return [str(item).strip() for item in value if str(item).strip()]


def _normalize_patient_profile(profile: Mapping[str, Any]) -> dict[str, Any]:
    raw = dict(profile)

    # Match current Android UI names while exposing backend field names.
    if "name" not in raw and "fullName" in raw:
        raw["name"] = raw["fullName"]
    if "height_cm" not in raw and "height" in raw:
        raw["height_cm"] = raw["height"]
    if "weight_kg" not in raw and "weight" in raw:
        raw["weight_kg"] = raw["weight"]

    for field in CORE_PATIENT_FIELDS:
        if field not in raw or raw[field] in (None, "", [], {}):
            raise _missing(f"patient_profile.{field}")

    for field in REQUIRED_EXPLICIT_CONTEXT_FIELDS:
        if field not in raw:
            raise _missing(f"patient_profile.{field}")

    age = _integer(raw["age"], "patient_profile.age", 1, 120)
    height_cm = _number(raw["height_cm"], "patient_profile.height_cm", 50, 250)
    weight_kg = _number(raw["weight_kg"], "patient_profile.weight_kg", 10, 300)

    gender = str(raw["gender"]).strip().lower()
    if gender not in SUPPORTED_GENDERS:
        raise AgentEngineInputError(
            "unsupported_gender",
            "patient_profile.gender must be Male, Female, or Other.",
            "patient_profile.gender",
        )

    preference_key = str(raw["dietary_preference"]).strip().lower()
    dietary_preference = SUPPORTED_DIETARY_PREFERENCES.get(preference_key)
    if not dietary_preference:
        raise AgentEngineInputError(
            "unsupported_dietary_preference",
            "patient_profile.dietary_preference must be Vegetarian, Vegan, or Non-vegetarian.",
            "patient_profile.dietary_preference",
        )

    normalized = deepcopy(raw)
    normalized.update(
        {
            "name": str(raw["name"]).strip(),
            "age": age,
            "gender": gender,
            "height_cm": height_cm,
            "weight_kg": weight_kg,
            "dietary_preference": dietary_preference,
            "goal": str(raw["goal"]).strip(),
            "activity_level": str(raw["activity_level"]).strip(),
            "allergies": _list(raw["allergies"], "patient_profile.allergies"),
            "food_intolerances": _list(raw["food_intolerances"], "patient_profile.food_intolerances"),
            "foods_to_avoid": _list(raw["foods_to_avoid"], "patient_profile.foods_to_avoid"),
            "health_conditions": _list(raw["health_conditions"], "patient_profile.health_conditions"),
            "medicine_allergies": _list(raw["medicine_allergies"], "patient_profile.medicine_allergies"),
            "medication_restrictions": _list(raw["medication_restrictions"], "patient_profile.medication_restrictions"),
            "doctor_restrictions": _list(raw["doctor_restrictions"], "patient_profile.doctor_restrictions"),
        }
    )
    return normalized


def _assessment_block(payload: Mapping[str, Any]) -> dict[str, Any]:
    block = (
        payload.get("assessment")
        or payload.get("assessments")
        or payload.get("assessment_profile")
        or payload
    )
    return _as_mapping(block, "assessment")


def _extract_assessment_result(block: Mapping[str, Any], name: str) -> dict[str, Any]:
    aliases = (name, f"{name}_result")
    for alias in aliases:
        if alias in block:
            value = block[alias]
            if not isinstance(value, Mapping):
                raise AgentEngineInputError(
                    "malformed_canonical_assessment",
                    f"assessment.{alias} must be the canonical Android JSON object.",
                    f"assessment.{alias}",
                )
            return dict(value)
    raise AgentEngineInputError(
        f"missing_{name}",
        f"Canonical Android {name.title()} result is required.",
        f"assessment.{name}",
    )


def _require_assessment_field(result: Mapping[str, Any], field_options: tuple[str, ...], label: str) -> None:
    if not any(result.get(field) not in (None, "", [], {}) for field in field_options):
        raise AgentEngineInputError(
            "missing_required_assessment_field",
            f"Canonical Android assessment data is missing {label}.",
            label,
            {"accepted_fields": list(field_options)},
        )


def _validate_assessments(
    prakriti: Mapping[str, Any],
    vikriti: Mapping[str, Any],
    agni: Mapping[str, Any],
) -> None:
    _require_assessment_field(prakriti, ("prakritiType", "constitution", "primaryDosha", "primary_dosha"), "assessment.prakriti.prakritiType")
    _require_assessment_field(vikriti, ("dominantDosha", "dominant_dosha"), "assessment.vikriti.dominantDosha")
    _require_assessment_field(agni, ("dominantAgni", "status", "displayResult"), "assessment.agni.dominantAgni")

    for name, result in (("prakriti", prakriti), ("vikriti", vikriti), ("agni", agni)):
        if "scores" not in result or not isinstance(result.get("scores"), Mapping):
            raise AgentEngineInputError(
                "malformed_canonical_assessment",
                f"assessment.{name}.scores must be present as an object.",
                f"assessment.{name}.scores",
            )


def _plan_days(value: Any) -> int:
    if value in (None, ""):
        return 7
    parsed = _integer(value, "plan_days", 1, 7)
    return parsed


def parse_agent_engine_request(payload: Mapping[str, Any]) -> AgentEngineRequest:
    """Validate the public backend contract without rescoring Android assessments."""
    root = _as_mapping(payload, "payload")
    profile = root.get("patient_profile") or root.get("profile")
    if profile is None:
        raise _missing("patient_profile")
    patient_profile = _normalize_patient_profile(_as_mapping(profile, "patient_profile"))

    assessment = _assessment_block(root)
    prakriti = _extract_assessment_result(assessment, "prakriti")
    vikriti = _extract_assessment_result(assessment, "vikriti")
    agni = _extract_assessment_result(assessment, "agni")
    _validate_assessments(prakriti, vikriti, agni)

    runtime = root.get("runtime") or {}
    runtime = _as_mapping(runtime, "runtime")
    plan_days = _plan_days(root.get("plan_days", runtime.get("plan_days")))

    return AgentEngineRequest(
        patient_profile=patient_profile,
        prakriti_result=deepcopy(prakriti),
        vikriti_result=deepcopy(vikriti),
        agni_result=deepcopy(agni),
        plan_days=plan_days,
        runtime=runtime,
    )
