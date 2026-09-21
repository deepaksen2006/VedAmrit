from dataclasses import dataclass, field
from typing import Any


@dataclass
class DocumentChunk:
    doc_id: str
    content: str
    metadata: dict[str, Any] = field(default_factory=dict)
    source_title: str = "Ayurvedic Knowledge Base"
    source_type: str = "Classical Reference"
    source_reference: str = ""


@dataclass
class FoodKnowledgeRecord:
    food_name: str
    rasa: list[str]
    guna: list[str]
    virya: str
    vipaka: str
    vata_effect: str
    pitta_effect: str
    kapha_effect: str
    indications: list[str]
    contraindications: list[str]
    source: str = "Ayurvedic Food Knowledge Base"


@dataclass
class LifestyleKnowledgeRecord:
    topic: str
    category: str
    recommendations: list[str]
    dosha_suitability: dict[str, str]
    source_title: str
    source_reference: str
