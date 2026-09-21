from __future__ import annotations

from copy import deepcopy
import os
from pathlib import Path
import sqlite3
from typing import Any, Mapping

from agent_engine.assessment_adapter import build_canonical_state
from agent_engine.contracts import AgentEngineInputError, AgentEngineRequest, parse_agent_engine_request
from agent_engine.graph import graph
from agent_engine.rag import config as rag_config


PROJECT_ROOT = Path(__file__).resolve().parent
DEFAULT_FOOD_DATASET_PATH = PROJECT_ROOT / "data" / "demo_foods.csv"


def _error_response(
    errors: list[dict[str, Any]],
    *,
    assessment: dict[str, Any] | None = None,
    rag_status: dict[str, Any] | None = None,
) -> dict[str, Any]:
    return {
        "assessment": assessment,
        "diet_plan": None,
        "lifestyle_plan": None,
        "validation": {"status": "ERROR", "errors": deepcopy(errors)},
        "workflow": {
            "status": "error",
            "assessment_source": "android_canonical_results",
            "errors": deepcopy(errors),
            "rag": rag_status,
        },
    }


def _assessment_output(request: AgentEngineRequest) -> dict[str, Any]:
    return {
        "source": "android_canonical_results",
        "prakriti": deepcopy(request.prakriti_result),
        "vikriti": deepcopy(request.vikriti_result),
        "agni": deepcopy(request.agni_result),
    }


def _vectorstore_path() -> Path:
    return rag_config.vectorstore_dir()


def rag_availability_status() -> dict[str, Any]:
    path = _vectorstore_path()
    sqlite_path = path / "chroma.sqlite3"
    base = {
        "available": False,
        "vectorstore_path": str(path),
        "fallback": (
            "Agent 3 uses conservative non-RAG lifestyle draft text with "
            "doctor-review-required safety flags when retrieval is unavailable."
        ),
    }
    if not path.exists():
        return {
            **base,
            "status": "unavailable",
            "error": {
                "code": "rag_vector_store_unavailable",
                "message": "RAG vector store directory does not exist.",
            },
        }
    if not sqlite_path.exists():
        return {
            **base,
            "status": "unavailable",
            "error": {
                "code": "rag_vector_store_unavailable",
                "message": "RAG vector store is missing chroma.sqlite3.",
            },
        }
    try:
        with sqlite3.connect(sqlite_path) as connection:
            count = connection.execute("select count(*) from embeddings").fetchone()[0]
    except sqlite3.Error as exc:
        return {
            **base,
            "status": "unavailable",
            "error": {
                "code": "rag_vector_store_unavailable",
                "message": "RAG vector store is not readable.",
                "details": {"reason": str(exc)},
            },
        }
    if count <= 0:
        return {
            **base,
            "status": "unavailable",
            "error": {
                "code": "rag_vector_store_unavailable",
                "message": "RAG vector store has no embeddings.",
            },
        }
    return {
        "available": True,
        "status": "available",
        "vectorstore_path": str(path),
        "embedding_count": count,
        "error": None,
        "fallback": None,
    }


def _food_dataset_path(request: AgentEngineRequest) -> str:
    configured = (
        request.runtime.get("food_dataset_path")
        or os.getenv("AGENT_ENGINE_FOOD_DATASET_PATH")
        or DEFAULT_FOOD_DATASET_PATH
    )
    return str(configured)


def _workflow_state(request: AgentEngineRequest, rag_status: dict[str, Any]) -> dict[str, Any]:
    extra_state: dict[str, Any] = {
        "food_dataset_path": _food_dataset_path(request),
        "rag_status": deepcopy(rag_status),
        "rag_available": bool(rag_status.get("available")),
    }
    report_dir = request.runtime.get("report_dir") or os.getenv("AGENT_ENGINE_REPORT_DIR")
    if report_dir:
        extra_state["report_output_dir"] = str(report_dir)
    return build_canonical_state(
        patient_profile=request.patient_profile,
        prakriti_result=request.prakriti_result,
        vikriti_result=request.vikriti_result,
        agni_result=request.agni_result,
        plan_days=request.plan_days,
        extra_state=extra_state,
    )


def _service_response(
    request: AgentEngineRequest,
    result_state: Mapping[str, Any],
    rag_status: dict[str, Any],
) -> dict[str, Any]:
    agent2_output = result_state.get("agent2_output") or {}
    diet_plan = agent2_output.get("diet_plan") or result_state.get("meal_plan")
    validation = (
        result_state.get("review_validation")
        or result_state.get("validation_result")
        or agent2_output.get("validation")
        or {}
    )
    errors = result_state.get("errors") or []
    workflow_status = str(result_state.get("workflow_status") or "success").lower()
    status = "success"
    if errors:
        status = "error"
    elif "review" in workflow_status or (validation or {}).get("status") == "REVIEW_REQUIRED":
        status = "review_required"

    return {
        "assessment": _assessment_output(request),
        "diet_plan": deepcopy(diet_plan),
        "lifestyle_plan": deepcopy(result_state.get("lifestyle_plan")),
        "validation": deepcopy(validation),
        "workflow": {
            "status": status,
            "workflow_status": result_state.get("workflow_status"),
            "session_id": result_state.get("session_id"),
            "patient_id": result_state.get("patient_id"),
            "assessment_source": "android_canonical_results",
            "errors": deepcopy(errors),
            "rag": deepcopy(rag_status),
            "doctor_review_required": True,
            "final_response": result_state.get("final_response"),
        },
    }


def generate_personalized_plan(payload: Mapping[str, Any]) -> dict[str, Any]:
    """
    Public backend service for future REST views.

    The service accepts canonical Android assessment results and never recalculates
    Prakriti, Vikriti, or Agni from questionnaire answers.
    """
    request: AgentEngineRequest | None = None
    rag_status = rag_availability_status()
    try:
        request = parse_agent_engine_request(payload)
        state = _workflow_state(request, rag_status)
        result_state = graph.invoke(state)
        return _service_response(request, result_state, rag_status)
    except AgentEngineInputError as exc:
        return _error_response(
            [exc.as_dict()],
            assessment=_assessment_output(request) if request else None,
            rag_status=rag_status,
        )
    except Exception as exc:
        return _error_response(
            [
                {
                    "code": "agent_execution_failure",
                    "message": "Agent engine workflow failed before producing a personalized plan.",
                    "details": {"reason": str(exc)},
                }
            ],
            assessment=_assessment_output(request) if request else None,
            rag_status=rag_status,
        )
