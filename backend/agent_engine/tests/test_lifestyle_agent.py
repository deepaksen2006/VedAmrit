from agent_engine.agents.lifestyle_agent import LifestyleAgent


patient_profile = {
    "id": "P001",
    "name": "Demo User",
    "age": 19,
    "gender": "female",
    "blood_group": "O+",
    "height_cm": 154,
    "weight_kg": 55,
    "dietary_preference": "vegetarian",
    "allergies": [],
    "foods_to_avoid": [],
    "health_conditions": [],
    "goal": "general wellness",
}


ayurvedic_assessment = {
    "prakriti": {
        "primary_dosha": "Vata",
        "secondary_dosha": "Pitta",
        "constitution": "Vata-Pitta",
    },

    "vikriti": {
        "dominant_dosha": "Vata",
    },

    "agni": {
        "status": "Vishamagni",
    },
}


agent = LifestyleAgent()

result = agent.generate_lifestyle_plan(
    patient_profile,
    ayurvedic_assessment,
)


print("\n===================================")
print("AYURGENIX - AGENT 3")
print("===================================\n")

print("Agent:", result["agent"])
print("Status:", result["status"])

print("\n--- Lifestyle Plan ---\n")

for category, recommendations in result["lifestyle_plan"].items():

    print(f"\n### {category.upper()}")

    for recommendation in recommendations:
        print(f"- {recommendation}")
print("\n--- RAG EVIDENCE ---")

for i, source in enumerate(
    result.get("rag_evidence", []),
    start=1
):
    print(f"\nSOURCE {i}")
    print("Book:", source["source_book"])
    print("Page:", source["page"])
    print("Author:", source["author"])
    import sys
    content_snippet = source["content"][:500]
    try:
        print(content_snippet)
    except UnicodeEncodeError:
        safe = content_snippet.encode(sys.stdout.encoding or "utf-8", errors="replace").decode(sys.stdout.encoding or "utf-8")
        print(safe)