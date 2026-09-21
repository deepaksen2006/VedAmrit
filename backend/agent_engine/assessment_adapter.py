from __future__ import annotations

from copy import deepcopy
from typing import Any


def _string_keyed_scores(value: Any) -> dict[str, Any]:
    if not isinstance(value, dict):
        return {}
    return {str(key): score for key, score in value.items()}


def adapt_prakriti_result(result: dict[str, Any]) -> dict[str, Any]:
    """Normalize canonical Android Prakriti output for the agent pipeline."""
    if not isinstance(result, dict):
        return {}
    raw = deepcopy(result or {})
    primary = raw.get("primary_dosha") or raw.get("primaryDosha")
    secondary = raw.get("secondary_dosha") or raw.get("secondaryDosha")
    constitution = raw.get("constitution") or raw.get("prakritiType") or primary
    return {
        **raw,
        "scores": _string_keyed_scores(raw.get("scores")),
        "percentages": _string_keyed_scores(raw.get("percentages")),
        "primary_dosha": primary,
        "secondary_dosha": secondary,
        "constitution": constitution,
        "assessment_method": raw.get("assessment_method", "canonical_android_prakriti_result"),
    }


def adapt_vikriti_result(result: dict[str, Any]) -> dict[str, Any]:
    """Normalize canonical Android Vikriti output for the agent pipeline."""
    if not isinstance(result, dict):
        return {}
    raw = deepcopy(result or {})
    dominant = raw.get("dominant_dosha") or raw.get("dominantDosha")
    return {
        **raw,
        "scores": _string_keyed_scores(raw.get("scores")),
        "severities": _string_keyed_scores(raw.get("severities")),
        "percentages": _string_keyed_scores(raw.get("percentages")),
        "dominant_dosha": dominant,
        "assessment_method": raw.get("assessment_method", "canonical_android_vikriti_result"),
    }


def adapt_agni_result(result: dict[str, Any]) -> dict[str, Any]:
    """Normalize canonical Android Agni output for the agent pipeline."""
    if not isinstance(result, dict):
        return {}
    raw = deepcopy(result or {})
    status = raw.get("status") or raw.get("dominantAgni") or raw.get("displayResult")
    scores = raw.get("category_scores") or raw.get("scores")
    percentages = raw.get("category_percentages") or raw.get("percentages") or {}
    return {
        **raw,
        "category_scores": _string_keyed_scores(scores),
        "category_percentages": _string_keyed_scores(percentages),
        "status": status,
        "assessment_method": raw.get("assessment_method", "canonical_android_agni_result"),
    }


def build_canonical_state(
    *,
    patient_profile: dict[str, Any],
    prakriti_result: dict[str, Any],
    vikriti_result: dict[str, Any],
    agni_result: dict[str, Any],
    plan_days: int = 7,
    extra_state: dict[str, Any] | None = None,
) -> dict[str, Any]:
    """Build an agent state from already-calculated Android assessment results."""
    state = deepcopy(extra_state or {})
    prakriti = adapt_prakriti_result(prakriti_result)
    vikriti = adapt_vikriti_result(vikriti_result)
    agni = adapt_agni_result(agni_result)
    state.update(
        {
            "patient_profile": deepcopy(patient_profile or {}),
            "prakriti_result": prakriti,
            "vikriti_result": vikriti,
            "agni_result": agni,
            "constitution": prakriti.get("constitution"),
            "ayurvedic_assessment": {
                "patient_profile": deepcopy(patient_profile or {}),
                "prakriti": prakriti,
                "vikriti": vikriti,
                "agni": agni,
            },
            "plan_days": plan_days,
            "assessment_source": "android_canonical_results",
        }
    )
    return state


def canonical_assessment_state_agent(state: dict[str, Any]) -> dict[str, Any]:
    """LangGraph node that validates and normalizes supplied canonical results."""
    profile = state.get("patient_profile") or state.get("user_input") or {}
    prakriti = adapt_prakriti_result(state.get("prakriti_result") or {})
    vikriti = adapt_vikriti_result(state.get("vikriti_result") or {})
    agni = adapt_agni_result(state.get("agni_result") or {})
    missing = []
    if not prakriti.get("constitution"):
        missing.append("prakriti_result.constitution/prakritiType")
    if not vikriti.get("dominant_dosha"):
        missing.append("vikriti_result.dominant_dosha/dominantDosha")
    if not agni.get("status"):
        missing.append("agni_result.status/dominantAgni")
    if missing:
        raise ValueError("Canonical Android assessment result is incomplete: " + ", ".join(missing))
    return {
        "patient_profile": profile,
        "prakriti_result": prakriti,
        "vikriti_result": vikriti,
        "agni_result": agni,
        "constitution": prakriti.get("constitution"),
        "ayurvedic_assessment": {
            "patient_profile": profile,
            "prakriti": prakriti,
            "vikriti": vikriti,
            "agni": agni,
        },
        "assessment_source": "android_canonical_results",
    }
