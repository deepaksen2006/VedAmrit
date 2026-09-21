from __future__ import annotations

from typing import Any

_retriever: AyurvedaRetriever | None = None


def _get_retriever() -> AyurvedaRetriever:
    """Delay model loading until Agent 3 actually needs RAG retrieval."""
    from agent_engine.rag.retriever import AyurvedaRetriever

    global _retriever
    if _retriever is None:
        _retriever = AyurvedaRetriever()
    return _retriever


def _document_to_dict(document) -> dict[str, Any]:
    """
    Convert a LangChain Document into a simple dictionary
    that can be passed to Agent 3 / final response.
    """

    return {
        "content": document.page_content,
        "metadata": document.metadata or {},
    }


def get_food_ayurvedic_rag_evidence(
    food_name: str,
    rasa: str | None = None,
    virya: str | None = None,
    vipaka: str | None = None,
) -> dict[str, Any]:
    """
    Retrieve Ayurvedic evidence about a food from the existing
    Chroma knowledge base.
    """

    query_parts = [f"Ayurvedic properties of {food_name}"]

    if rasa:
        query_parts.append(f"Rasa: {rasa}")

    if virya:
        query_parts.append(f"Virya: {virya}")

    if vipaka:
        query_parts.append(f"Vipaka: {vipaka}")

    query = ". ".join(query_parts)

    documents = _get_retriever().search(query, k=5)

    return {
        "query": query,
        "evidence": [
            _document_to_dict(document)
            for document in documents
        ],
    }


def get_lifestyle_rag_evidence(
    primary_dosha: str,
    goal: str = "",
    health_conditions: list[str] | None = None,
    patient_context: str = "",
) -> list[dict[str, Any]]:
    """
    Retrieve evidence-backed lifestyle and wellness information
    for Agent 3 from the existing Ayurvedic Chroma knowledge base.

    This function does not diagnose disease or prescribe medicine.
    """

    query_parts = [
        f"Ayurvedic lifestyle and daily routine guidance for {primary_dosha} dosha"
    ]
    if patient_context:
        query_parts.append("Patient-specific routine context: " + patient_context)

    if goal:
        query_parts.append(f"Goal: {goal}")

    if health_conditions:
        query_parts.append(
            "Health conditions/context: "
            + ", ".join(health_conditions)
        )

    query = ". ".join(query_parts)

    documents = _get_retriever().search(query, k=5)

    return [
        _document_to_dict(document)
        for document in documents
    ]
