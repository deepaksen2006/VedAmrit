package com.example.vedaahar.vikriti

import kotlin.math.round

enum class VikritiDosha(val displayName: String) {
    Vata("Vata"),
    Pitta("Pitta"),
    Kapha("Kapha")
}

data class VikritiAnswerOption(
    val id: String,
    val label: String,
    val points: Int
)

data class VikritiQuestion(
    val id: String,
    val text: String,
    val dosha: VikritiDosha
)

data class VikritiResult(
    val answers: Map<String, String>,
    val scores: Map<VikritiDosha, Int>,
    val severities: Map<VikritiDosha, String>,
    val percentages: Map<VikritiDosha, Double>,
    val dominantDosha: String,
    val completedAtMillis: Long,
    val prakritiType: String
)

val vikritiAnswerOptions = listOf(
    VikritiAnswerOption("not_at_all", "Not at all", 1),
    VikritiAnswerOption("somewhat", "Somewhat / Occasionally", 3),
    VikritiAnswerOption("very_often", "Very often", 5)
)

val vikritiQuestions = listOf(
    VikritiQuestion(
        id = "V1",
        text = "Recently, have you felt unusually restless, anxious, worried, or unable to settle your thoughts?",
        dosha = VikritiDosha.Vata
    ),
    VikritiQuestion(
        id = "V2",
        text = "Recently, has your sleep been light, disturbed, irregular, or difficult to return to after waking?",
        dosha = VikritiDosha.Vata
    ),
    VikritiQuestion(
        id = "V3",
        text = "Recently, have you noticed dryness in your skin, lips, hair, throat, or stools?",
        dosha = VikritiDosha.Vata
    ),
    VikritiQuestion(
        id = "V4",
        text = "Recently, have you had a lasting dry cough, often needed to clear your throat, had repeated throat or voice problems, or found it hard to express yourself?",
        dosha = VikritiDosha.Vata
    ),
    VikritiQuestion(
        id = "V5",
        text = "Recently, have you experienced bloating, gas, constipation, abdominal tightness, or irregular appetite?",
        dosha = VikritiDosha.Vata
    ),
    VikritiQuestion(
        id = "V6",
        text = "Recently, have you felt cold easily, had cold hands or feet, tremors, twitches, or variable energy?",
        dosha = VikritiDosha.Vata
    ),
    VikritiQuestion(
        id = "V7",
        text = "Recently, have you had aches, cracking joints, stiffness that moves around, or discomfort that changes location?",
        dosha = VikritiDosha.Vata
    ),
    VikritiQuestion(
        id = "P1",
        text = "Recently, have you felt overheated, flushed, easily irritated, impatient, or quick to anger?",
        dosha = VikritiDosha.Pitta
    ),
    VikritiQuestion(
        id = "P2",
        text = "Recently, have you had acidity, heartburn, sour burps, burning sensations, or loose stools?",
        dosha = VikritiDosha.Pitta
    ),
    VikritiQuestion(
        id = "P3",
        text = "Recently, have you noticed increased sweating, strong body odor, heat rashes, redness, or skin sensitivity?",
        dosha = VikritiDosha.Pitta
    ),
    VikritiQuestion(
        id = "P4",
        text = "Recently, have you felt intense hunger or thirst, become uncomfortable when meals are delayed, or craved cold foods and drinks?",
        dosha = VikritiDosha.Pitta
    ),
    VikritiQuestion(
        id = "P5",
        text = "Recently, have you had burning eyes, light sensitivity, headaches around the temples, or sharp inflammatory discomfort?",
        dosha = VikritiDosha.Pitta
    ),
    VikritiQuestion(
        id = "P6",
        text = "Recently, have you been overly critical, competitive, perfectionistic, or mentally intense?",
        dosha = VikritiDosha.Pitta
    ),
    VikritiQuestion(
        id = "P7",
        text = "Recently, have you noticed yellowish coating, bitter taste, mouth ulcers, or a feeling of excess internal heat?",
        dosha = VikritiDosha.Pitta
    ),
    VikritiQuestion(
        id = "K1",
        text = "Recently, have you felt heavy, sluggish, sleepy during the day, or slow to get moving?",
        dosha = VikritiDosha.Kapha
    ),
    VikritiQuestion(
        id = "K2",
        text = "Recently, have you experienced congestion, mucus, sinus heaviness, cough with phlegm, or frequent throat coating?",
        dosha = VikritiDosha.Kapha
    ),
    VikritiQuestion(
        id = "K3",
        text = "Recently, has your digestion felt slow with low appetite, nausea, fullness after small meals, or heaviness after eating?",
        dosha = VikritiDosha.Kapha
    ),
    VikritiQuestion(
        id = "K4",
        text = "Recently, have you noticed water retention, puffiness, swelling, or a tendency to gain weight easily?",
        dosha = VikritiDosha.Kapha
    ),
    VikritiQuestion(
        id = "K5",
        text = "Recently, have you felt emotionally dull, unmotivated, attached, resistant to change, or mentally foggy?",
        dosha = VikritiDosha.Kapha
    ),
    VikritiQuestion(
        id = "K6",
        text = "Recently, have you craved sweets, dairy, fried foods, cold foods, or heavy meals more than usual?",
        dosha = VikritiDosha.Kapha
    ),
    VikritiQuestion(
        id = "K7",
        text = "Recently, have you felt chest heaviness, slow circulation, excessive salivation, or a damp feeling in the body?",
        dosha = VikritiDosha.Kapha
    )
)

fun calculateVikritiScores(answers: Map<String, String>): Map<VikritiDosha, Int> {
    validateVikritiAnswers(answers)
    val optionPoints = vikritiAnswerOptions.associate { it.id to it.points }
    return VikritiDosha.entries.associateWith { dosha ->
        vikritiQuestions
            .filter { it.dosha == dosha }
            .sumOf { question -> optionPoints.getValue(answers.getValue(question.id)) }
    }
}

fun calculateVikritiPercentages(scores: Map<VikritiDosha, Int>): Map<VikritiDosha, Double> {
    return VikritiDosha.entries.associateWith { dosha ->
        val score = scores.getValue(dosha)
        round((((score - 7).coerceIn(0, 28) / 28.0) * 100.0) * 10.0) / 10.0
    }
}

fun classifyVikritiSeverity(score: Int): String {
    return when (score) {
        in 7..13 -> "Low"
        in 14..20 -> "Mild"
        in 21..27 -> "Moderate"
        in 28..35 -> "High"
        else -> throw IllegalArgumentException("Vikriti score must be between 7 and 35.")
    }
}

fun generateVikritiResult(
    answers: Map<String, String>,
    prakritiType: String,
    completedAtMillis: Long = System.currentTimeMillis()
): VikritiResult {
    val scores = calculateVikritiScores(answers)
    val severities = VikritiDosha.entries.associateWith { dosha -> classifyVikritiSeverity(scores.getValue(dosha)) }
    val dominantDosha = scores.maxWith(compareBy<Map.Entry<VikritiDosha, Int>> { it.value }.thenByDescending { -it.key.ordinal }).key
    return VikritiResult(
        answers = answers,
        scores = scores,
        severities = severities,
        percentages = calculateVikritiPercentages(scores),
        dominantDosha = dominantDosha.displayName,
        completedAtMillis = completedAtMillis,
        prakritiType = prakritiType
    )
}

fun validateVikritiAnswers(answers: Map<String, String>) {
    val requiredQuestionIds = vikritiQuestions.map { it.id }.toSet()
    val validOptionIds = vikritiAnswerOptions.map { it.id }.toSet()
    require(answers.keys == requiredQuestionIds) { "All 21 Vikriti questions must be answered exactly once." }
    require(answers.values.all { it in validOptionIds }) { "Vikriti answers contain an invalid option." }
}
