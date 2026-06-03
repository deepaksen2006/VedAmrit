package com.example.vedaahar.dosha

import kotlin.math.abs
import kotlin.math.roundToInt

object DoshaScoringEngine {
    fun generateResult(
        questions: List<DoshaQuestion>,
        answers: Map<Int, String>
    ): DoshaResult {
        val scores = Dosha.entries.associateWith { 0 }.toMutableMap()

        questions.forEach { question ->
            val selectedOption = question.options.firstOrNull { it.id == answers[question.id] }
            selectedOption?.scores?.forEach { score ->
                scores[score.dosha] = scores.getValue(score.dosha) + score.points
            }
        }

        val total = scores.values.sum().coerceAtLeast(1)
        val percentages = scores.mapValues { (_, value) -> ((value.toFloat() / total.toFloat()) * 100f).roundToInt() }
        val ranked = Dosha.entries.sortedByDescending { scores.getValue(it) }
        val dominant = ranked.first()
        val secondary = ranked[1]
        val isDual = abs(scores.getValue(dominant) - scores.getValue(secondary)) <= 2

        val profileName = when {
            isDual -> dualProfileName(dominant, secondary)
            else -> dominant.displayName
        }

        return DoshaResult(
            profileName = profileName,
            profileType = when {
                isDual -> DoshaProfileType.Dual
                else -> DoshaProfileType.Single
            },
            dominant = dominant,
            secondary = if (isDual) secondary else null,
            scores = scores,
            percentages = percentages,
            description = descriptionFor(profileName)
        )
    }

    private fun dualProfileName(first: Dosha, second: Dosha): String {
        val pair = setOf(first, second)
        return when {
            pair == setOf(Dosha.Vata, Dosha.Pitta) -> "Vata-Pitta"
            pair == setOf(Dosha.Pitta, Dosha.Kapha) -> "Pitta-Kapha"
            else -> "Vata-Kapha"
        }
    }

    private fun descriptionFor(profileName: String): DoshaResultDescription {
        return descriptions[profileName] ?: descriptions.getValue("Vata")
    }

    private val descriptions = mapOf(
        "Vata" to DoshaResultDescription(
            overview = "Vata reflects air and space: movement, sensitivity, creativity, and quick adaptation.",
            personalityTraits = "Imaginative, expressive, curious, fast-thinking, and often energized by novelty.",
            digestionStyle = "Appetite and digestion can be irregular, with bloating or dryness when routines slip.",
            emotionalTendencies = "Emotionally quick, empathetic, and inspired, with anxiety or overwhelm during imbalance.",
            energyBehavior = "Energy arrives in bursts and benefits from steady pacing and recovery windows.",
            strengths = "Creative problem solving, flexibility, intuition, communication, and fresh ideas.",
            imbalanceRisks = "Dryness, constipation, restlessness, light sleep, scattered focus, and nervous fatigue.",
            lifestyleSuggestions = "Prioritize warmth, routine, oil massage, calming breathwork, consistent meals, and early sleep.",
            foodRecommendations = "Favor warm cooked meals, ghee, soups, stews, root vegetables, rice, oats, dates, and gentle spices."
        ),
        "Pitta" to DoshaResultDescription(
            overview = "Pitta reflects fire and water: transformation, intelligence, precision, and purposeful drive.",
            personalityTraits = "Focused, courageous, organized, direct, ambitious, and naturally analytical.",
            digestionStyle = "Strong appetite and quick digestion, with acidity or heat when meals are delayed.",
            emotionalTendencies = "Confident and decisive, with irritability, impatience, or criticism under stress.",
            energyBehavior = "High, goal-oriented energy that performs best with cooling pauses and balanced intensity.",
            strengths = "Leadership, clarity, discipline, strategy, metabolism, and strong follow-through.",
            imbalanceRisks = "Acidity, heat, skin sensitivity, inflammation, anger, burnout, and perfectionism.",
            lifestyleSuggestions = "Use cooling routines, time outdoors, non-competitive movement, mindful pauses, and softer evenings.",
            foodRecommendations = "Favor cooling foods, cucumber, leafy greens, coconut, sweet fruits, basmati rice, cilantro, and fennel."
        ),
        "Kapha" to DoshaResultDescription(
            overview = "Kapha reflects earth and water: stability, nourishment, endurance, and grounded presence.",
            personalityTraits = "Calm, loyal, patient, compassionate, steady, and reliable in relationships and work.",
            digestionStyle = "Digestion may be slower, with heaviness after rich, cold, or frequent meals.",
            emotionalTendencies = "Emotionally steady and caring, with attachment, lethargy, or resistance during imbalance.",
            energyBehavior = "Stable energy and strong stamina, but benefits from stimulation and active starts.",
            strengths = "Endurance, immunity, patience, emotional steadiness, consistency, and long-term commitment.",
            imbalanceRisks = "Sluggishness, congestion, weight gain, water retention, oversleeping, and low motivation.",
            lifestyleSuggestions = "Choose energizing movement, lighter dinners, dry brushing, variety, early rising, and mental stimulation.",
            foodRecommendations = "Favor warm light meals, legumes, bitter greens, millet, barley, ginger, black pepper, and honey in moderation."
        ),
        "Vata-Pitta" to DoshaResultDescription(
            overview = "Vata-Pitta blends creativity with precision: quick ideas, sharp insight, and active transformation.",
            personalityTraits = "Inventive, articulate, intense, curious, independent, and highly responsive to stimulation.",
            digestionStyle = "Can swing between irregular appetite and sharp hunger, with gas or acidity when stressed.",
            emotionalTendencies = "Inspired and decisive, but prone to worry, impatience, and mental overdrive.",
            energyBehavior = "Fast and bright energy that needs grounding, cooling, and deliberate recovery.",
            strengths = "Innovation, communication, strategy, learning speed, leadership, and adaptability.",
            imbalanceRisks = "Burnout, inflammation, anxiety, acidity, dry skin, disturbed sleep, and over-scheduling.",
            lifestyleSuggestions = "Keep regular meals, cooling breathwork, warm oiling, calm exercise, and screen-light boundaries at night.",
            foodRecommendations = "Favor warm but not spicy foods, rice, mung dal, sweet fruits, cooked vegetables, ghee, mint, and coriander."
        ),
        "Pitta-Kapha" to DoshaResultDescription(
            overview = "Pitta-Kapha combines drive with endurance: focused action, resilience, and steady ambition.",
            personalityTraits = "Organized, confident, loyal, practical, persuasive, and naturally managerial.",
            digestionStyle = "Generally strong digestion, but rich or oily foods may create heaviness and acidity.",
            emotionalTendencies = "Stable and confident, with stubbornness, intensity, or frustration under pressure.",
            energyBehavior = "Sustained energy with strong output, best balanced by lightness and cooling release.",
            strengths = "Leadership, stamina, reliability, strategic focus, immunity, and execution.",
            imbalanceRisks = "Heat, congestion, heaviness, inflammation, attachment to outcomes, and over-control.",
            lifestyleSuggestions = "Use brisk movement, cooling downtime, lighter routines, variety, and regular emotional decompression.",
            foodRecommendations = "Favor light cooling meals, greens, legumes, barley, pomegranate, apples, cilantro, and gentle bitter flavors."
        ),
        "Vata-Kapha" to DoshaResultDescription(
            overview = "Vata-Kapha blends sensitivity with steadiness: intuitive, caring, imaginative, and deeply restorative.",
            personalityTraits = "Gentle, creative, empathetic, reflective, supportive, and comfort-oriented.",
            digestionStyle = "Digestion can be slow and variable, with bloating, heaviness, or low appetite.",
            emotionalTendencies = "Compassionate and receptive, with worry, withdrawal, or emotional heaviness in imbalance.",
            energyBehavior = "Energy may alternate between restless bursts and low momentum, needing warmth and activation.",
            strengths = "Empathy, imagination, patience, intuition, loyalty, and nurturing presence.",
            imbalanceRisks = "Coldness, mucus, lethargy, constipation, anxious stagnation, and inconsistent habits.",
            lifestyleSuggestions = "Choose warm structure, daily movement, light stimulation, regular meals, and uplifting social rhythm.",
            foodRecommendations = "Favor warm light foods, soups, cooked greens, ginger, cumin, cloves, mung dal, and avoid cold heavy meals."
        ),
        "Tri-Doshic / Balanced Constitution" to DoshaResultDescription(
            overview = "A balanced tri-doshic pattern suggests Vata, Pitta, and Kapha are all meaningfully present.",
            personalityTraits = "Versatile, balanced, adaptive, thoughtful, and able to shift between creativity, focus, and steadiness.",
            digestionStyle = "Digestion may be generally balanced but changes with season, stress, travel, and sleep.",
            emotionalTendencies = "Emotionally adaptable, with imbalance showing differently depending on context and lifestyle load.",
            energyBehavior = "Energy is broad and responsive, thriving with seasonal tuning and consistent basics.",
            strengths = "Adaptability, resilience, balanced judgment, social range, and holistic self-awareness.",
            imbalanceRisks = "Symptoms may rotate: restlessness, heat, or heaviness depending on food, climate, and stress.",
            lifestyleSuggestions = "Follow seasonal routines, protect sleep, eat mindfully, move daily, and adjust intensity to your current state.",
            foodRecommendations = "Favor fresh seasonal meals, balanced spices, cooked whole foods, adequate hydration, and variety without extremes."
        )
    )
}
