package com.example.vedaahar.prakriti

import kotlin.math.abs
import kotlin.math.round

enum class PrakritiDosha(val displayName: String) {
    Vata("Vata"),
    Pitta("Pitta"),
    Kapha("Kapha")
}

data class PrakritiOption(
    val id: String,
    val text: String,
    val dosha: PrakritiDosha
)

data class PrakritiQuestion(
    val id: Int,
    val text: String,
    val options: List<PrakritiOption>
)

data class PrakritiClassification(
    val primaryDosha: String,
    val secondaryDosha: String?,
    val prakritiType: String,
    val borderline: Boolean
)

data class PrakritiResult(
    val scores: Map<PrakritiDosha, Int>,
    val percentages: Map<PrakritiDosha, Double>,
    val primaryDosha: String,
    val secondaryDosha: String?,
    val prakritiType: String,
    val borderline: Boolean,
    val questionsAnswered: Int
)

private val optionToDosha = mapOf(
    "A" to PrakritiDosha.Vata,
    "B" to PrakritiDosha.Pitta,
    "C" to PrakritiDosha.Kapha
)

val prakritiQuestions = listOf(
    q(1, "What is your skin usually like?", "Dry or rough skin that absorbs oil quickly.", "Soft, warm skin that may develop pigmentation, pimples, or moles.", "Oily, clear skin that absorbs oil slowly."),
    q(2, "What is your hair usually like?", "Dry, brittle, or rough hair that may have split ends.", "Medium-density, straight or slightly wavy hair that may gray or thin early.", "Thick, smooth hair that may be straight, wavy, or curly, with an oily scalp."),
    q(3, "How do you usually move?", "You move often, change position frequently, and find it hard to sit still.", "Your movements are coordinated and active.", "Your movements are calm and slow."),
    q(4, "How well can you continue physical activity?", "You have short bursts of energy but tire easily.", "You have strong drive and enjoy intense activity or competition.", "You have good stamina and can continue moderate activity for a long time."),
    q(5, "How often do you get infections, and how quickly do you recover?", "Your symptoms change or fluctuate quickly.", "You may have a strong fever or inflammation but recover quickly.", "You rarely get sick, but recovery is slow and congestion or mucus may occur."),
    q(6, "What is your appetite usually like?", "Your hunger changes from one meal to another.", "You have a strong appetite and may become irritable when hungry.", "You have a good appetite and can easily skip a meal."),
    q(7, "How much food do you usually eat at a meal?", "The amount varies, and you often prefer small portions.", "You digest food strongly and can manage larger portions.", "You digest food well, but it happens slowly and comfortably."),
    q(8, "How often do you feel thirsty?", "Your thirst is unpredictable, you drink in bursts, and you prefer warm drinks.", "You feel thirsty often and prefer cool drinks.", "You feel little thirst, can go a long time without drinking, and prefer warm drinks."),
    q(9, "How much do you usually sweat?", "You sweat little, and your sweat is usually without a strong smell.", "You sweat during exercise, and your sweat may have a noticeable smell.", "You mainly sweat during intense exercise, and your sweat is usually without a strong smell."),
    q(10, "How would you describe your memory and ability to remember things?", "You learn quickly but forget easily.", "You learn quickly and remember details well.", "You learn slowly but remember information for a long time."),
    q(11, "What is your sleep usually like?", "Your sleep is light or restless, you wake easily, and your thoughts may race.", "Your sleep is sound, but you can miss sleep when you are focused on work.", "Your sleep is deep and long, and it is difficult to give up sleep."),
    q(12, "How do you usually react to hot and cold temperatures?", "You prefer warmth and feel uncomfortable in the cold.", "You prefer cool surroundings and dislike too much heat or humidity.", "You prefer warmth slightly and tolerate cold fairly well."),
    q(13, "How do you usually begin a task or piece of work?", "You start with enthusiasm, but your motivation changes and you may lose interest.", "You are driven and goal-focused, and you take the lead.", "You start slowly and steadily and stay persistent once you begin."),
    q(14, "How quickly and strongly do your emotions change?", "Your emotions change often, and you become emotional or sensitive easily.", "Your emotions are strong and intense; you may become angry or frustrated.", "You are calm and stable, peaceful, content, and forgiving."),
    q(15, "How do you usually speak?", "You speak quickly, move between ideas, and may become distracted.", "You speak clearly, directly, sharply, and persuasively.", "You speak slowly and calmly and choose your words carefully."),
    q(16, "How do you usually feel and behave when competing with others?", "You dislike competition and find it stressful.", "You enjoy competition and perform well under pressure.", "You handle competition calmly and remain steady under pressure."),
    q(17, "What are your bowel movements usually like?", "They are irregular, with constipation, gas or bloating, and hard or dry stools.", "They are regular and frequent, but stools may sometimes be loose.", "They are slow and easy; stools may be heavy, sticky, and well formed."),
    q(18, "How easily do you usually lose weight?", "You lose weight easily.", "You can lose weight with focused diet and exercise.", "You need consistent effort, and weight loss is slower."),
    q(19, "What is your mind usually like during the day?", "Your mind is restless, easily distracted, and very active.", "Your mind is focused and sharp but may become agitated.", "Your mind is relaxed and unhurried, and you are not easily pressured."),
    q(20, "How would you describe your usual emotional nature?", "You may feel anxious or sensitive, and your emotions may be unpredictable.", "You may become angry or agitated quickly and have intense emotions.", "You are calm and slow to anger, but emotional detachment may be difficult."),
    q(21, "What tastes and types of food do you usually prefer?", "You prefer sweet, sour, salty, spicy, or hot foods and drinks.", "You prefer sour, salty, spicy, and strong flavors, along with cool drinks.", "You prefer sweet, salty, and calorie-rich foods, along with warm or sweet drinks.")
)

fun calculatePrakritiScore(answers: Map<Int, String>): Map<PrakritiDosha, Int> {
    validatePrakritiAnswers(answers)
    val scores = PrakritiDosha.entries.associateWith { 0 }.toMutableMap()
    answers.values.forEach { answer ->
        val dosha = optionToDosha.getValue(answer)
        scores[dosha] = scores.getValue(dosha) + 1
    }
    require(scores.values.sum() == prakritiQuestions.size) { "Scores must total ${prakritiQuestions.size}." }
    return scores
}

fun calculatePrakritiPercentages(scores: Map<PrakritiDosha, Int>): Map<PrakritiDosha, Double> {
    require(scores.values.sum() == prakritiQuestions.size) { "Scores must total ${prakritiQuestions.size}." }
    return PrakritiDosha.entries.associateWith { dosha ->
        round(((scores[dosha] ?: 0) * 100.0 / prakritiQuestions.size) * 10.0) / 10.0
    }
}

fun classifyPrakriti(scores: Map<PrakritiDosha, Int>): PrakritiClassification {
    require(scores.values.sum() == prakritiQuestions.size) { "Scores must total ${prakritiQuestions.size}." }
    val ranked = PrakritiDosha.entries.sortedWith(
        compareByDescending<PrakritiDosha> { scores[it] ?: 0 }.thenBy { it.ordinal }
    )
    val first = ranked[0]
    val second = ranked[1]
    val third = ranked[2]
    val highest = scores.getValue(first)
    val middle = scores.getValue(second)
    val lowest = scores.getValue(third)

    if (highest - lowest <= 3) {
        return PrakritiClassification(
            primaryDosha = "Balanced",
            secondaryDosha = null,
            prakritiType = "Tridoshic",
            borderline = false
        )
    }

    if (highest - middle <= 3) {
        return PrakritiClassification(
            primaryDosha = first.displayName,
            secondaryDosha = second.displayName,
            prakritiType = "${first.displayName}-${second.displayName}",
            borderline = false
        )
    }

    return PrakritiClassification(
        primaryDosha = first.displayName,
        secondaryDosha = null,
        prakritiType = first.displayName,
        borderline = abs(highest - middle) == 4
    )
}

fun generatePrakritiResult(answers: Map<Int, String>): PrakritiResult {
    val scores = calculatePrakritiScore(answers)
    val percentages = calculatePrakritiPercentages(scores)
    val classification = classifyPrakriti(scores)
    return PrakritiResult(
        scores = scores,
        percentages = percentages,
        primaryDosha = classification.primaryDosha,
        secondaryDosha = classification.secondaryDosha,
        prakritiType = classification.prakritiType,
        borderline = classification.borderline,
        questionsAnswered = answers.size
    )
}

fun validatePrakritiAnswers(answers: Map<Int, String>) {
    require(answers.size == prakritiQuestions.size) { "Exactly ${prakritiQuestions.size} answers are required." }
    val expectedIds = prakritiQuestions.map { it.id }.toSet()
    require(answers.keys == expectedIds) { "Every question must be answered once." }
    require(answers.values.all { it in optionToDosha.keys }) { "Every answer must be A, B, or C." }
}

private fun q(id: Int, text: String, a: String, b: String, c: String): PrakritiQuestion {
    return PrakritiQuestion(
        id = id,
        text = text,
        options = listOf(
            PrakritiOption("A", a, PrakritiDosha.Vata),
            PrakritiOption("B", b, PrakritiDosha.Pitta),
            PrakritiOption("C", c, PrakritiDosha.Kapha)
        )
    )
}
