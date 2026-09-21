from typing import Any

from agent_engine.services.input_normalizer import (
    normalize_patient_profile,
    normalize_vikriti_result,
)


def prepare_diet_context(state: dict[str, Any]) -> dict[str, Any]:
    """
    Prepare the information required by Agent 2.

    Agent 2 receives:
    - Patient profile
    - Prakriti from Agent 1
    - Vikriti from Agent 1
    - Agni from Agent 1
    """

    # --------------------------------------------------
    # 1. PATIENT PROFILE
    # --------------------------------------------------

    raw_profile = (
        state.get("patient_profile")
        or state.get("user_input")
        or {}
    )

    patient_profile = normalize_patient_profile(raw_profile)

    # --------------------------------------------------
    # 2. PRAKRITI
    # --------------------------------------------------

    prakriti_result = state.get("prakriti_result") or {}

    prakriti = {
        "primary_dosha": prakriti_result.get(
            "primary_dosha"
        ),

        "secondary_dosha": prakriti_result.get(
            "secondary_dosha"
        ),

        "constitution": prakriti_result.get(
            "constitution"
        ),

        "scores": prakriti_result.get(
            "scores",
            {}
        ),

        "percentages": prakriti_result.get(
            "percentages",
            {}
        ),
    }

    # --------------------------------------------------
    # 3. VIKRITI
    # --------------------------------------------------

    raw_vikriti = state.get("vikriti_result") or {}

    vikriti = normalize_vikriti_result(
        raw_vikriti
    )

    # --------------------------------------------------
    # 4. AGNI
    # --------------------------------------------------

    agni_result = state.get("agni_result") or {}

    agni = {
        "status": agni_result.get(
            "status"
        ),

        "category_scores": agni_result.get(
            "category_scores",
            {}
        ),

        "category_percentages": agni_result.get(
            "category_percentages",
            {}
        ),
    }

    # --------------------------------------------------
    # 5. COMPLETE AYURVEDIC ASSESSMENT
    # --------------------------------------------------

    ayurvedic_assessment = {
        "prakriti": prakriti,
        "vikriti": vikriti,
        "agni": agni,
    }

    # --------------------------------------------------
    # 6. FINAL CONTEXT FOR AGENT 2
    # --------------------------------------------------

    return {
        "patient_profile": patient_profile,
        "ayurvedic_assessment": ayurvedic_assessment,

        # Convenient direct access
        "prakriti": prakriti,
        "vikriti": vikriti,
        "agni": agni,
    }