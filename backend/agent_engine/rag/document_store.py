from typing import Any
from agent_engine.rag.models import DocumentChunk, FoodKnowledgeRecord, LifestyleKnowledgeRecord


CLASSICAL_LIFESTYLE_KNOWLEDGE: list[dict[str, Any]] = [
    {
        "topic": "Dinacharya",
        "category": "Morning Routine",
        "dosha_suitability": {"Vata": "High", "Pitta": "High", "Kapha": "High"},
        "recommendations": [
            "Wake up during Brahma Muhurta (approx. 45 mins before sunrise) to harness morning Vata energy.",
            "Perform Kavala/Gandusha (oil pulling with sesame or coconut oil) to strengthen teeth, gums, and taste buds.",
            "Practice Jivha Nirlekhana (tongue scraping) to remove Ama (accumulated toxins).",
            "Drink a glass of warm water to stimulate peristalsis and Agni."
        ],
        "source_title": "Charaka Samhita, Sutrasthana Ch. 5 (Matrasiteeya)",
        "source_reference": "Charaka Samhita 1.5.78-88"
    },
    {
        "topic": "Meal Timing",
        "category": "Digestion & Agni",
        "dosha_suitability": {"Vata": "High", "Pitta": "High", "Kapha": "High"},
        "recommendations": [
            "Eat lunch as the largest meal of the day between 12:00 PM and 1:30 PM when Pitta and Agni are naturally strongest.",
            "Keep dinner light and eat at least 2 to 3 hours before sleep to prevent sluggish digestion.",
            "Avoid snacking between meals unless genuine hunger is present to allow previous meal digestion (Koshtha Agni)."
        ],
        "source_title": "Ashtanga Hridayam, Sutrasthana Ch. 8 (Matrashiteeya)",
        "source_reference": "Ashtanga Hridayam 1.8.1-12"
    },
    {
        "topic": "Sleep Routine",
        "category": "Nidra",
        "dosha_suitability": {"Vata": "High", "Pitta": "High", "Kapha": "Moderate"},
        "recommendations": [
            "Maintain consistent sleep schedule, sleeping around 10:00 PM before Pitta hours begin.",
            "For Vata dominance: Apply warm sesame oil to soles of feet (Padabhyanga) for calm sleep.",
            "For Pitta dominance: Keep room cool and avoid mentally stimulating work before bedtime.",
            "For Kapha dominance: Avoid daytime napping (Diva Swapna) as it exacerbates Kapha and lethargy."
        ],
        "source_title": "Charaka Samhita, Sutrasthana Ch. 21 (Ashtau Ninditiya)",
        "source_reference": "Charaka Samhita 1.21.35-51"
    },
    {
        "topic": "Activity & Exercise",
        "category": "Vyayama",
        "dosha_suitability": {"Vata": "Moderate", "Pitta": "Moderate", "Kapha": "High"},
        "recommendations": [
            "Vata types: Gentle, grounding exercise (Yogasanas, walking, mild stretching). Avoid overexertion.",
            "Pitta types: Moderate intensity activities (swimming, cycling) in cool environments. Avoid exercising in direct noon heat.",
            "Kapha types: Vigorous, stimulating exercise (brisk running, active sports) to reduce excess Kapha heaviness.",
            "Exercise up to half capacity (Ardhashakti) indicated by light perspiration on forehead/armpits."
        ],
        "source_title": "Sushruta Samhita, Chikitsasthana Ch. 24",
        "source_reference": "Sushruta Samhita 4.24.38-46"
    },
    {
        "topic": "Stress Management",
        "category": "Manas / Mental Balance",
        "dosha_suitability": {"Vata": "High", "Pitta": "High", "Kapha": "High"},
        "recommendations": [
            "Practice Pranayama (Nadi Shodhana for Vata/Pitta, Bhastrika/Kapalabhati for Kapha).",
            "Daily Dhyana (mindfulness meditation) for 15-20 minutes to calm the nervous system.",
            "Abhyanga (self-massage with warm oil appropriate for your Prakriti) to soothe Ojas and lower cortisol."
        ],
        "source_title": "Charaka Samhita, Sharirasthana Ch. 1",
        "source_reference": "Charaka Samhita 4.1.134-142"
    },
    {
        "topic": "Seasonal Guidance",
        "category": "Ritucharya",
        "dosha_suitability": {"Vata": "High", "Pitta": "High", "Kapha": "High"},
        "recommendations": [
            "Adapt routine according to seasonal transitions (Ritu Sandhi).",
            "In Cold/Dry weather (Hemanta/Shishira): Favor warm, unctuous foods and warm oil massage for Vata.",
            "In Hot weather (Greeshma): Favor cooling, sweet, hydrating foods and shade for Pitta.",
            "In Spring (Vasanta): Favor light, dry, warm foods and active detoxification for Kapha."
        ],
        "source_title": "Ashtanga Hridayam, Sutrasthana Ch. 3 (Ritucharya)",
        "source_reference": "Ashtanga Hridayam 1.3.1-45"
    }
]


class KnowledgeStore:
    """In-memory RAG knowledge store with classical metadata and retrieval support."""

    def __init__(self):
        self.lifestyle_records: list[LifestyleKnowledgeRecord] = []
        self._load_lifestyle_knowledge()

    def _load_lifestyle_knowledge(self):
        for item in CLASSICAL_LIFESTYLE_KNOWLEDGE:
            record = LifestyleKnowledgeRecord(
                topic=item["topic"],
                category=item["category"],
                recommendations=item["recommendations"],
                dosha_suitability=item["dosha_suitability"],
                source_title=item["source_title"],
                source_reference=item["source_reference"],
            )
            self.lifestyle_records.append(record)

    def get_lifestyle_recommendations(
        self,
        prakriti: str,
        goal: str = "",
    ) -> list[dict[str, Any]]:
        """Retrieve lifestyle knowledge filtered by Prakriti and goal."""
        results = []
        prakriti_primary = prakriti.split("-")[0] if "-" in prakriti else prakriti

        for record in self.lifestyle_records:
            suitability = record.dosha_suitability.get(prakriti_primary, "Moderate")
            results.append({
                "topic": record.topic,
                "category": record.category,
                "recommendations": record.recommendations,
                "suitability": suitability,
                "source_title": record.source_title,
                "source_reference": record.source_reference,
            })
        return results
