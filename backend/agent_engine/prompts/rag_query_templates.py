"""
RAG Query Templates for AyurGenix

This module provides structured, evidence-grounded query templates for the AyurGenix
RAG (Retrieval-Augmented Generation) system. All queries follow the 5-component structure:

1. Ayurvedic Concept
2. Dosha/Prakriti/Vikriti Context
3. Agni Context (when relevant)
4. Application Area
5. Required Evidence Type

Following RAG_PROTOCOL.md, queries are specific (never broad), contextual, and designed
to retrieve evidence from approved classical sources before generating recommendations.

Agents:
- Agent 1: Assessment (Prakriti, Vikriti, Agni)
- Agent 2: Personalized Diet Planning (Foods, Rasa, Guna, Virya, Vipaka, Agni)
- Agent 3: Lifestyle & Wellness (Dinacharya, Sleep, Exercise, Stress, Ritucharya)
"""

from typing import Dict, Optional, Literal
from dataclasses import dataclass


@dataclass
class RetrievalConfig:
    """Configuration for RAG retrieval parameters."""
    k: int = 5  # Default: retrieve top-5 relevant chunks
    max_k: int = 10  # Maximum allowable k value
    min_k: int = 3  # Minimum for narrow queries


class QueryTemplates:
    """
    Structured query template factory for AyurGenix RAG retrieval.

    Each template is specific, contextual, and follows the 5-component structure.
    Templates are method-based to enable dynamic context injection and validation.

    Usage:
        query = QueryTemplates.query_for(
            agent="Agent 1",
            topic="Prakriti Assessment",
            context={"vata_score": 42, "pitta_score": 35, "kapha_score": 23}
        )
    """

    # =========================================================================
    # AGENT 1: ASSESSMENT — Prakriti, Vikriti, Agni Assessment
    # =========================================================================

    @staticmethod
    def agent1_prakriti_assessment(context: Optional[Dict] = None) -> Dict:
        """
        Agent 1 Query: Prakriti (Constitutional Type) Assessment

        5-Component Structure:
        1. Concept: Prakriti assessment and characteristics
        2. Dosha Context: Constitutional dosha tendencies
        3. Agni Context: Natural digestive capacity
        4. Application: Assessment and diagnosis
        5. Evidence: Classical definitions from Ayurvedic texts

        Returns:
            dict: Query configuration with template, k value, and retrieval scope
        """
        return {
            "agent": "Agent 1",
            "topic": "Prakriti Assessment",
            "query": (
                "Prakriti (constitutional type) assessment: Vata, Pitta, and Kapha "
                "constitutional characteristics, behavioral traits, physical features, "
                "natural agni (digestive capacity), and validated assessment criteria "
                "according to classical Ayurvedic texts including dosha-specific "
                "psychological and physiological markers."
            ),
            "k": 5,
            "scope": [
                "Prakriti definition and characteristics",
                "Vata, Pitta, Kapha constitutional features",
                "Assessment methodologies",
                "Natural agni by prakriti",
            ],
            "exclude": [
                "Disease treatment",
                "Medicines and pharmaceuticals",
                "Surgery and Panchakarma",
            ],
        }

    @staticmethod
    def agent1_vikriti_assessment(context: Optional[Dict] = None) -> Dict:
        """
        Agent 1 Query: Vikriti (Current Imbalance) Assessment

        5-Component Structure:
        1. Concept: Vikriti assessment and dosha aggravation signs
        2. Dosha Context: Current dosha imbalance indicators
        3. Agni Context: Impact on digestive balance
        4. Application: Imbalance diagnosis and root cause
        5. Evidence: Classical symptoms and modern validated indicators

        Args:
            context: Optional dict with detected vikriti patterns

        Returns:
            dict: Query configuration for vikriti-specific retrieval
        """
        vikriti_type = (
            context.get("primary_vikriti", "unknown") if context else "unknown"
        )
        return {
            "agent": "Agent 1",
            "topic": "Vikriti Assessment",
            "query": (
                f"Vikriti (current imbalance) assessment for {vikriti_type} dosha aggravation: "
                "signs of dosha imbalance, behavioral changes, physical symptoms, "
                "digestive disturbances, and diagnostic indicators that distinguish "
                "current vikriti from natural prakriti according to Charaka Samhita, "
                "Sushruta Samhita, and Ashtanga Hridaya."
            ),
            "k": 5,
            "scope": [
                "Vikriti definition and characteristics",
                "Dosha aggravation signs",
                "Imbalance indicators",
                "Assessment methods",
            ],
            "exclude": [
                "Disease diagnosis",
                "Medical treatment",
                "Pharmaceutical interventions",
                "Panchakarma procedures",
            ],
        }

    @staticmethod
    def agent1_agni_assessment(context: Optional[Dict] = None) -> Dict:
        """
        Agent 1 Query: Agni (Digestive Fire) Assessment

        5-Component Structure:
        1. Concept: Agni types and assessment
        2. Dosha Context: Agni balance by dosha
        3. Agni Context: Samagni, Mandagni, Tikshnagni, Vishamagni
        4. Application: Agni evaluation and classification
        5. Evidence: Classical agni indicators and characteristics

        Args:
            context: Optional dict with digestive observations

        Returns:
            dict: Query configuration for agni-specific retrieval
        """
        return {
            "agent": "Agent 1",
            "topic": "Agni Assessment",
            "query": (
                "Agni (digestive fire) types and assessment: Samagni (balanced digestion), "
                "Mandagni (weak digestion), Tikshnagni (excessive digestion), and Vishamagni (variable digestion). "
                "Agni characteristics by prakriti (Vata, Pitta, Kapha), diagnostic indicators of each agni type, "
                "agni-dependent food suitability, and methods to assess digestive capacity according to "
                "classical texts."
            ),
            "k": 5,
            "scope": [
                "Agni types definition",
                "Agni characteristics by prakriti",
                "Assessment indicators",
                "Agni balance principles",
            ],
            "exclude": [
                "Disease-specific agni management",
                "Pharmaceutical treatments",
                "Panchakarma procedures",
            ],
        }

    # =========================================================================
    # AGENT 2: PERSONALIZED DIET — Food Properties and Dietary Guidance
    # =========================================================================

    @staticmethod
    def agent2_dosha_balancing_foods(context: Optional[Dict] = None) -> Dict:
        """
        Agent 2 Query: Dosha-Balancing Dietary Principles

        5-Component Structure:
        1. Concept: Food properties that balance dosha
        2. Dosha Context: Vata, Pitta, or Kapha imbalance (from vikriti)
        3. Agni Context: Food suitability for agni type
        4. Application: Dietary guidance for dosha balance
        5. Evidence: Rasa, Guna, Virya, Vipaka, and dosha-food effects

        Args:
            context: Dict with primary_vikriti, agni_status, dietary_preference

        Returns:
            dict: Query configuration for dosha-specific food retrieval
        """
        vikriti = context.get("primary_vikriti", "Vata") if context else "Vata"
        agni = context.get("agni_status", "Samagni") if context else "Samagni"

        return {
            "agent": "Agent 2",
            "topic": "Dosha-Balancing Foods",
            "query": (
                f"Foods and dietary principles for balancing {vikriti} Vikriti with {agni} agni: "
                "Ayurvedic food properties (Rasa, Guna, Virya, Vipaka), dosha-pacifying foods, "
                "food selection by Rasa and Guna, foods that strengthen digestive capacity, "
                "seasonal food adjustments, and food incompatibilities. Include specific guidance on "
                "which food properties reduce {vikriti} aggravation while supporting {agni} agni strength."
            ),
            "k": 5,
            "scope": [
                "Food properties by Rasa, Guna, Virya, Vipaka",
                "Dosha-food interactions",
                "Agni-compatible foods",
                "Dietary principles for balance",
                "Food incompatibilities",
            ],
            "exclude": [
                "Disease-specific diets",
                "Pharmaceutical supplements",
                "Panchakarma protocols",
                "Medicinal food preparations",
            ],
        }

    @staticmethod
    def agent2_rasa_guidance(context: Optional[Dict] = None) -> Dict:
        """
        Agent 2 Query: Rasa (Taste) Dietary Guidance

        5-Component Structure:
        1. Concept: Six Rasa (tastes) and their effects
        2. Dosha Context: Rasa balance for current vikriti
        3. Agni Context: Rasa suitability for agni type
        4. Application: Meal composition by Rasa
        5. Evidence: Classical Rasa properties and dosha effects

        Args:
            context: Dict with vikriti and agni information

        Returns:
            dict: Query configuration for Rasa-specific guidance
        """
        vikriti = context.get("primary_vikriti", "Vata") if context else "Vata"
        return {
            "agent": "Agent 2",
            "topic": "Rasa (Taste) Guidance",
            "query": (
                f"Six Rasa (tastes) for {vikriti} Vikriti management: Sweet, Sour, Salty, Pungent, Bitter, Astringent. "
                "Properties of each Rasa, dosha effects, how Rasa balance supports digestion, "
                "Rasa suitability by agni type, seasonal Rasa adjustments, and optimal Rasa combination "
                "in meals according to classical Ayurvedic dietary principles."
            ),
            "k": 5,
            "scope": [
                "Rasa properties and definitions",
                "Dosha effects of each Rasa",
                "Rasa balance principles",
                "Rasa and agni interaction",
            ],
            "exclude": [
                "Disease treatment through Rasa",
                "Medicinal Rasa preparations",
            ],
        }

    @staticmethod
    def agent2_guna_guidance(context: Optional[Dict] = None) -> Dict:
        """
        Agent 2 Query: Guna (Quality) Dietary Guidance

        5-Component Structure:
        1. Concept: Twenty Guna (qualities) and food properties
        2. Dosha Context: Guna balance for vikriti management
        3. Agni Context: Guna suitability for digestive strength
        4. Application: Food selection by Guna
        5. Evidence: Classical Guna properties and effects

        Args:
            context: Dict with vikriti and agni

        Returns:
            dict: Query configuration for Guna-specific guidance
        """
        vikriti = context.get("primary_vikriti", "Vata") if context else "Vata"
        return {
            "agent": "Agent 2",
            "topic": "Guna (Quality) Guidance",
            "query": (
                f"Twenty Guna (qualities) for {vikriti} Vikriti dietary management: Heavy/Light, "
                "Slow/Sharp, Cold/Hot, Oily/Dry, Smooth/Rough, Dense/Liquid, Soft/Hard, Stable/Mobile, "
                "Subtle/Gross, Clear/Cloudy. How Guna properties balance dosha, food selection by Guna, "
                "optimal Guna combinations, and Guna impact on agni strength and digestion."
            ),
            "k": 5,
            "scope": [
                "Guna definitions and properties",
                "Dosha-Guna interactions",
                "Guna and agni effects",
                "Food Guna characteristics",
            ],
            "exclude": [
                "Disease-specific Guna therapy",
                "Panchakarma applications",
            ],
        }

    @staticmethod
    def agent2_virya_guidance(context: Optional[Dict] = None) -> Dict:
        """
        Agent 2 Query: Virya (Potency) Dietary Guidance

        5-Component Structure:
        1. Concept: Virya (heating vs cooling) and food potency
        2. Dosha Context: Virya effects on dosha imbalance
        3. Agni Context: Virya interaction with agni type
        4. Application: Food selection by Virya
        5. Evidence: Classical Virya properties and effects

        Args:
            context: Dict with vikriti and agni

        Returns:
            dict: Query configuration for Virya guidance
        """
        vikriti = context.get("primary_vikriti", "Vata") if context else "Vata"
        return {
            "agent": "Agent 2",
            "topic": "Virya (Potency) Guidance",
            "query": (
                f"Virya (heating/cooling potency) for {vikriti} Vikriti management: "
                "Ushna Virya (heating), Sheeta Virya (cooling), and neutral foods. "
                "How Virya properties affect dosha, temperature effects on digestion, "
                "seasonal Virya adjustments, Virya effects on agni strength, and optimal "
                "Virya balance in daily meals."
            ),
            "k": 5,
            "scope": [
                "Virya heating/cooling properties",
                "Dosha effects of Virya",
                "Virya and agni interaction",
                "Seasonal Virya balance",
            ],
            "exclude": [
                "Disease-specific Virya protocols",
            ],
        }

    @staticmethod
    def agent2_vipaka_guidance(context: Optional[Dict] = None) -> Dict:
        """
        Agent 2 Query: Vipaka (Post-Digestive Effect) Guidance

        5-Component Structure:
        1. Concept: Vipaka and food's final effect after digestion
        2. Dosha Context: Vipaka effects on vikriti
        3. Agni Context: Vipaka and digestive metabolism
        4. Application: Food selection by Vipaka
        5. Evidence: Classical Vipaka properties and effects

        Args:
            context: Dict with vikriti and agni

        Returns:
            dict: Query configuration for Vipaka guidance
        """
        vikriti = context.get("primary_vikriti", "Vata") if context else "Vata"
        return {
            "agent": "Agent 2",
            "topic": "Vipaka (Post-Digestive Effect) Guidance",
            "query": (
                f"Vipaka (post-digestive taste/effect) for {vikriti} Vikriti management: "
                "Sweet Vipaka, Sour Vipaka, Pungent Vipaka, and their long-term dosha effects. "
                "How Vipaka differs from Rasa, Vipaka effects on tissue metabolism, "
                "Vipaka impact on agni balance, and optimal Vipaka selection for sustained wellness."
            ),
            "k": 5,
            "scope": [
                "Vipaka definition and types",
                "Vipaka dosha effects",
                "Vipaka and metabolism",
                "Long-term dietary effects",
            ],
            "exclude": [
                "Disease management through Vipaka",
            ],
        }

    @staticmethod
    def agent2_agni_compatible_foods(context: Optional[Dict] = None) -> Dict:
        """
        Agent 2 Query: Agni-Compatible Food Guidance

        5-Component Structure:
        1. Concept: Food suitability based on agni type
        2. Dosha Context: Prakriti and vikriti agni tendency
        3. Agni Context: Specific agni type (Mandagni, Samagni, Tikshnagni, Vishamagni)
        4. Application: Food selection for agni support
        5. Evidence: Classical agni-food compatibility principles

        Args:
            context: Dict with agni_status (Mandagni, Samagni, Tikshnagni, Vishamagni)

        Returns:
            dict: Query configuration for agni-compatible food retrieval
        """
        agni = context.get("agni_status", "Samagni") if context else "Samagni"
        return {
            "agent": "Agent 2",
            "topic": "Agni-Compatible Foods",
            "query": (
                f"Food suitability for {agni} (digestive capacity): "
                "Foods that strengthen, maintain, or balance digestive fire without overwhelming or dampening. "
                "Meal composition for {agni}, food preparation methods, portion sizes, and meal timing. "
                "Foods to favor and foods to avoid based on current agni strength, and how to gradually "
                "improve agni capacity through dietary choices."
            ),
            "k": 5,
            "scope": [
                "Agni-food interactions",
                "Foods for Mandagni (weak digestion)",
                "Foods for Tikshnagni (excess digestion)",
                "Foods for Vishamagni (variable digestion)",
                "Meal timing and agni",
            ],
            "exclude": [
                "Digestive diseases and medical conditions",
                "Panchakarma agni preparation",
                "Medicinal agni treatments",
            ],
        }

    @staticmethod
    def agent2_food_combinations(context: Optional[Dict] = None) -> Dict:
        """
        Agent 2 Query: Food Incompatibilities and Combinations

        5-Component Structure:
        1. Concept: Ahara Virudha (incompatible food combinations)
        2. Dosha Context: How incompatibilities aggravate vikriti
        3. Agni Context: Impact on digestive burden
        4. Application: Safe meal composition
        5. Evidence: Classical food combination principles

        Returns:
            dict: Query configuration for food compatibility guidance
        """
        return {
            "agent": "Agent 2",
            "topic": "Food Combinations & Incompatibilities",
            "query": (
                "Ahara Virudha (incompatible food combinations) and compatible food pairings: "
                "Foods that should not be eaten together, adverse combinations, digestive strain from "
                "incompatible mixtures, safe food pairing principles, and how incompatibilities affect dosha balance. "
                "Optimal meal composition with compatible foods, meal sequencing, and traditional food "
                "combinations that enhance digestion and nutrient absorption."
            ),
            "k": 5,
            "scope": [
                "Incompatible food combinations",
                "Compatible pairings",
                "Digestion and combinations",
                "Traditional meal composition",
            ],
            "exclude": [
                "Disease treatment through food combinations",
            ],
        }

    # =========================================================================
    # AGENT 3: LIFESTYLE & WELLNESS — Dinacharya, Sleep, Exercise, Stress, Ritucharya
    # =========================================================================

    @staticmethod
    def agent3_dinacharya_guidance(context: Optional[Dict] = None) -> Dict:
        """
        Agent 3 Query: Dinacharya (Daily Routine) Guidance

        5-Component Structure:
        1. Concept: Dinacharya and daily wellness routines
        2. Dosha Context: Routine adjustments for prakriti/vikriti
        3. Agni Context: Meal timing and digestive rhythm
        4. Application: Personalized daily schedule
        5. Evidence: Classical Dinacharya principles

        Args:
            context: Dict with primary_dosha, agni_status

        Returns:
            dict: Query configuration for Dinacharya guidance
        """
        primary_dosha = (
            context.get("primary_dosha", "Vata") if context else "Vata"
        )
        return {
            "agent": "Agent 3",
            "topic": "Dinacharya (Daily Routine)",
            "query": (
                f"Dinacharya (daily routine) for {primary_dosha} prakriti: "
                "Optimal waking time, morning cleansing practices (oil massage, tongue scraping, bathing), "
                "physical activity timing, meal schedule, work and rest cycles, evening routine, and sleep timing. "
                "How Dinacharya supports dosha balance, improves digestion, and maintains wellness throughout the day "
                "according to Charaka Samhita and Ashtanga Hridaya."
            ),
            "k": 5,
            "scope": [
                "Dinacharya principles",
                "Daily routine by dosha",
                "Morning and evening practices",
                "Meal timing and digestion",
            ],
            "exclude": [
                "Disease treatment",
                "Pharmaceutical preparations",
                "Surgery",
                "Panchakarma procedures",
            ],
        }

    @staticmethod
    def agent3_sleep_guidance(context: Optional[Dict] = None) -> Dict:
        """
        Agent 3 Query: Sleep and Rest Guidance

        5-Component Structure:
        1. Concept: Sleep quality and wellness
        2. Dosha Context: Sleep patterns by prakriti/vikriti
        3. Agni Context: Sleep and digestive rest
        4. Application: Sleep optimization
        5. Evidence: Classical sleep principles

        Args:
            context: Dict with primary_dosha

        Returns:
            dict: Query configuration for sleep guidance
        """
        primary_dosha = (
            context.get("primary_dosha", "Vata") if context else "Vata"
        )
        return {
            "agent": "Agent 3",
            "topic": "Sleep & Rest",
            "query": (
                f"Sleep and rest for {primary_dosha} prakriti: "
                "Optimal sleep duration, sleep quality principles, bedtime routine, sleep environment, "
                "napping practices, and rest patterns. How sleep supports dosha balance, tissue regeneration, "
                "mental clarity, and immunity. Causes of sleep disturbance and how to naturally improve sleep "
                "quality through daily practices and dietary choices."
            ),
            "k": 5,
            "scope": [
                "Sleep quality and duration",
                "Sleep by dosha",
                "Bedtime practices",
                "Sleep and recovery",
            ],
            "exclude": [
                "Sleep disorders and medical treatment",
                "Sleep medications",
            ],
        }

    @staticmethod
    def agent3_exercise_guidance(context: Optional[Dict] = None) -> Dict:
        """
        Agent 3 Query: Exercise and Physical Activity Guidance

        5-Component Structure:
        1. Concept: Vyayama (exercise) principles
        2. Dosha Context: Exercise type and intensity by prakriti/vikriti
        3. Agni Context: Exercise and digestive capacity
        4. Application: Personalized exercise routine
        5. Evidence: Classical Vyayama principles

        Args:
            context: Dict with primary_dosha, agni_status

        Returns:
            dict: Query configuration for exercise guidance
        """
        primary_dosha = (
            context.get("primary_dosha", "Vata") if context else "Vata"
        )
        agni = context.get("agni_status", "Samagni") if context else "Samagni"
        return {
            "agent": "Agent 3",
            "topic": "Exercise & Physical Activity",
            "query": (
                f"Vyayama (exercise) for {primary_dosha} prakriti with {agni} agni: "
                "Optimal exercise types, duration, intensity, and timing. Exercise benefits for dosha balance, "
                "muscle and bone health, circulation, and mental clarity. How exercise affects digestion and agni, "
                "proper exercise intensity to avoid overexertion, seasonal activity adjustments, and recovery practices "
                "according to classical Ayurvedic wellness principles."
            ),
            "k": 5,
            "scope": [
                "Exercise types and effects",
                "Exercise by dosha",
                "Optimal exercise timing",
                "Exercise and agni",
                "Recovery practices",
            ],
            "exclude": [
                "Disease treatment through exercise",
                "Therapeutic rehabilitation",
            ],
        }

    @staticmethod
    def agent3_meal_timing_guidance(context: Optional[Dict] = None) -> Dict:
        """
        Agent 3 Query: Meal Timing and Digestive Rhythm

        5-Component Structure:
        1. Concept: Meal timing and digestive coordination
        2. Dosha Context: Meal timing by prakriti/vikriti
        3. Agni Context: Agni rhythm and meal scheduling
        4. Application: Optimal meal times
        5. Evidence: Classical meal timing principles

        Args:
            context: Dict with agni_status, primary_dosha

        Returns:
            dict: Query configuration for meal timing guidance
        """
        agni = context.get("agni_status", "Samagni") if context else "Samagni"
        return {
            "agent": "Agent 3",
            "topic": "Meal Timing & Digestion",
            "query": (
                f"Meal timing for {agni} agni and digestive rhythm: "
                "Optimal breakfast, lunch, and dinner times, meal frequency, fasting principles, and meal size. "
                "How meal timing supports agni strength, prevents digestive burden, and maintains dosha balance. "
                "Seasonal meal time adjustments, effects of irregular eating, and coordinating meals with daily activities "
                "and rest cycles according to Ayurvedic wellness principles."
            ),
            "k": 5,
            "scope": [
                "Meal timing principles",
                "Optimal meal frequency",
                "Agni and meal timing",
                "Seasonal timing adjustments",
            ],
            "exclude": [
                "Fasting for disease treatment",
                "Medical dietary protocols",
            ],
        }

    @staticmethod
    def agent3_stress_management_guidance(context: Optional[Dict] = None) -> Dict:
        """
        Agent 3 Query: Stress Management and Mental Wellness

        5-Component Structure:
        1. Concept: Stress effects on dosha and overall wellness
        2. Dosha Context: Dosha-specific stress patterns
        3. Agni Context: How stress affects digestion
        4. Application: Stress reduction practices
        5. Evidence: Classical stress management principles

        Args:
            context: Dict with primary_dosha

        Returns:
            dict: Query configuration for stress management
        """
        primary_dosha = (
            context.get("primary_dosha", "Vata") if context else "Vata"
        )
        return {
            "agent": "Agent 3",
            "topic": "Stress Management & Mental Wellness",
            "query": (
                f"Stress management and mental wellness for {primary_dosha} prakriti: "
                "How stress affects dosha imbalance, daily practices for mental calm, meditation and mindfulness principles, "
                "breathing techniques (Pranayama), emotional balance, and preventive wellness practices. "
                "Impact of stress on agni and digestion, lifestyle practices to reduce anxiety, and sustainable "
                "mental wellness according to classical Ayurvedic wisdom."
            ),
            "k": 5,
            "scope": [
                "Stress and dosha effects",
                "Mental wellness practices",
                "Meditation and mindfulness",
                "Breathing techniques",
                "Emotional balance",
            ],
            "exclude": [
                "Mental health disorders",
                "Psychiatric medications",
                "Clinical anxiety/depression treatment",
            ],
        }

    @staticmethod
    def agent3_ritucharya_guidance(context: Optional[Dict] = None) -> Dict:
        """
        Agent 3 Query: Ritucharya (Seasonal Routine) Guidance

        5-Component Structure:
        1. Concept: Seasonal adjustments and Ritucharya principles
        2. Dosha Context: How seasons affect each dosha
        3. Agni Context: Seasonal agni changes
        4. Application: Seasonal lifestyle and diet
        5. Evidence: Classical Ritucharya principles

        Args:
            context: Dict with current_season or primary_dosha

        Returns:
            dict: Query configuration for Ritucharya guidance
        """
        season = context.get("current_season", "all") if context else "all"
        season_text = (
            f"for {season} season" if season != "all" else "throughout the year"
        )
        return {
            "agent": "Agent 3",
            "topic": "Ritucharya (Seasonal Routine)",
            "query": (
                f"Ritucharya (seasonal routine) {season_text}: "
                "How each season (Vasanta-Spring, Grisma-Summer, Sharad-Autumn, Hemanta-Winter, Shita-Early Winter) "
                "affects dosha balance, seasonal dietary adjustments, activity modifications, sleep and rest patterns, "
                "seasonal clothing and bathing practices. How to maintain wellness by aligning daily life with seasonal changes, "
                "seasonal agni variations, and preventive practices for each season according to Charaka Samhita "
                "and Ashtanga Hridaya."
            ),
            "k": 5,
            "scope": [
                "Ritucharya principles",
                "Seasonal dosha effects",
                "Seasonal dietary guidance",
                "Seasonal activity adjustments",
                "Seasonal sleep and rest",
            ],
            "exclude": [
                "Seasonal disease prevention",
                "Disease-specific seasonal protocols",
            ],
        }

    # =========================================================================
    # FACTORY METHOD: Dynamic Query Generation
    # =========================================================================

    @staticmethod
    def query_for(
        agent: Literal["Agent 1", "Agent 2", "Agent 3"],
        topic: str,
        context: Optional[Dict] = None,
    ) -> Optional[Dict]:
        """
        Factory method to retrieve the appropriate query template.

        This method enables dynamic query selection based on agent and topic,
        with context injection for personalization.

        Args:
            agent: "Agent 1", "Agent 2", or "Agent 3"
            topic: Specific topic within agent domain (e.g., "Prakriti Assessment")
            context: Optional dictionary with patient/assessment context for injection

        Returns:
            dict: Query configuration with template, k value, scope, and exclusions
            None: If agent/topic combination not found

        Examples:
            # Agent 1: Prakriti Assessment
            query = QueryTemplates.query_for(
                agent="Agent 1",
                topic="Prakriti Assessment"
            )

            # Agent 2: Diet for Pitta Vikriti with Tikshnagni
            query = QueryTemplates.query_for(
                agent="Agent 2",
                topic="Dosha-Balancing Foods",
                context={"primary_vikriti": "Pitta", "agni_status": "Tikshnagni"}
            )

            # Agent 3: Dinacharya for Vata Prakriti
            query = QueryTemplates.query_for(
                agent="Agent 3",
                topic="Dinacharya (Daily Routine)",
                context={"primary_dosha": "Vata"}
            )
        """
        # Agent 1 Templates
        if agent == "Agent 1":
            if topic == "Prakriti Assessment":
                return QueryTemplates.agent1_prakriti_assessment(context)
            elif topic == "Vikriti Assessment":
                return QueryTemplates.agent1_vikriti_assessment(context)
            elif topic == "Agni Assessment":
                return QueryTemplates.agent1_agni_assessment(context)

        # Agent 2 Templates
        elif agent == "Agent 2":
            if topic == "Dosha-Balancing Foods":
                return QueryTemplates.agent2_dosha_balancing_foods(context)
            elif topic == "Rasa (Taste) Guidance":
                return QueryTemplates.agent2_rasa_guidance(context)
            elif topic == "Guna (Quality) Guidance":
                return QueryTemplates.agent2_guna_guidance(context)
            elif topic == "Virya (Potency) Guidance":
                return QueryTemplates.agent2_virya_guidance(context)
            elif topic == "Vipaka (Post-Digestive Effect) Guidance":
                return QueryTemplates.agent2_vipaka_guidance(context)
            elif topic == "Agni-Compatible Foods":
                return QueryTemplates.agent2_agni_compatible_foods(context)
            elif topic == "Food Combinations & Incompatibilities":
                return QueryTemplates.agent2_food_combinations(context)

        # Agent 3 Templates
        elif agent == "Agent 3":
            if topic == "Dinacharya (Daily Routine)":
                return QueryTemplates.agent3_dinacharya_guidance(context)
            elif topic == "Sleep & Rest":
                return QueryTemplates.agent3_sleep_guidance(context)
            elif topic == "Exercise & Physical Activity":
                return QueryTemplates.agent3_exercise_guidance(context)
            elif topic == "Meal Timing & Digestion":
                return QueryTemplates.agent3_meal_timing_guidance(context)
            elif topic == "Stress Management & Mental Wellness":
                return QueryTemplates.agent3_stress_management_guidance(context)
            elif topic == "Ritucharya (Seasonal Routine)":
                return QueryTemplates.agent3_ritucharya_guidance(context)

        # Topic not found
        return None

    @staticmethod
    def list_available_topics(agent: str) -> list:
        """
        List all available topics for a given agent.

        Args:
            agent: "Agent 1", "Agent 2", or "Agent 3"

        Returns:
            list: Available topic names for the agent
        """
        topics = {
            "Agent 1": [
                "Prakriti Assessment",
                "Vikriti Assessment",
                "Agni Assessment",
            ],
            "Agent 2": [
                "Dosha-Balancing Foods",
                "Rasa (Taste) Guidance",
                "Guna (Quality) Guidance",
                "Virya (Potency) Guidance",
                "Vipaka (Post-Digestive Effect) Guidance",
                "Agni-Compatible Foods",
                "Food Combinations & Incompatibilities",
            ],
            "Agent 3": [
                "Dinacharya (Daily Routine)",
                "Sleep & Rest",
                "Exercise & Physical Activity",
                "Meal Timing & Digestion",
                "Stress Management & Mental Wellness",
                "Ritucharya (Seasonal Routine)",
            ],
        }
        return topics.get(agent, [])
