"""Compatibility entry point for the single personalized Lifestyle RAG agent."""
from agent_engine.agents.lifestyle import lifestyle_agent


class LifestyleAgent:
    def generate_lifestyle_plan(self, patient_profile, ayurvedic_assessment):
        state = {"patient_profile": patient_profile,
                 "prakriti_result": ayurvedic_assessment.get("prakriti", {}),
                 "vikriti_result": ayurvedic_assessment.get("vikriti", {}),
                 "agni_result": ayurvedic_assessment.get("agni", {})}
        plan = lifestyle_agent(state)["lifestyle_plan"]
        return {"agent": "Personalized Lifestyle RAG Agent", "lifestyle_plan": plan,
                "status": "AI-GENERATED - DOCTOR REVIEW REQUIRED"}
