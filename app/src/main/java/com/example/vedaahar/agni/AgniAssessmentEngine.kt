package com.example.vedaahar.agni

enum class AgniType(val displayName: String, val subtitle: String) {
    Mandagni("Mandagni", "Slow Digestive Pattern"),
    Vishamagni("Vishamagni", "Irregular Digestive Pattern"),
    Samagni("Samagni", "Balanced Digestive Pattern"),
    Tikshnagni("Tikshnagni", "Intense Digestive Pattern")
}

data class AgniOption(
    val id: String,
    val text: String,
    val type: AgniType
)

data class AgniQuestion(
    val id: Int,
    val text: String,
    val options: List<AgniOption>
)

data class AgniResult(
    val answers: Map<Int, String>,
    val scores: Map<AgniType, Int>,
    val dominantAgni: String,
    val mixedAgniTypes: List<String>,
    val isMixed: Boolean,
    val completedAtMillis: Long
) {
    val resultTitle: String
        get() = if (isMixed) "Mixed Agni Pattern" else dominantAgni

    val displayResult: String
        get() = if (isMixed) "Mixed Agni - ${mixedAgniTypes.joinToString(" + ")}" else dominantAgni
}

val agniQuestions = listOf(
    aq(
        id = 1,
        text = "Which option best describes how well you digest food?",
        mandagni = "You cannot digest even small amounts of food",
        vishamagni = "Your ability to digest food changes; sometimes you can and sometimes you cannot",
        samagni = "You can digest almost all foods when you eat the right amount",
        tikshnagni = "You can digest almost any food easily, even large amounts"
    ),
    aq(
        id = 2,
        text = "How long after a meal does it usually take before you feel hungry again?",
        mandagni = "About 8 hours",
        vishamagni = "The timing is not consistent",
        samagni = "6 to 8 hours",
        tikshnagni = "Less than 6 hours"
    ),
    aq(
        id = 3,
        text = "How does your digestion change when your routine is disturbed, such as by irregular meals, poor sleep, or emotional stress?",
        mandagni = "Your digestion is disturbed by even a small change",
        vishamagni = "Your digestion is disturbed by major changes",
        samagni = "Your digestion is not affected much",
        tikshnagni = "Your digestion is disturbed at first but adjusts later"
    ),
    aq(
        id = 4,
        text = "How many meals do you usually eat each day?",
        mandagni = "Fewer than 2 meals a day",
        vishamagni = "The number changes between 1 and 4 meals a day",
        samagni = "Usually 2 to 3 meals a day",
        tikshnagni = "Almost always more than 3 meals a day"
    ),
    aq(
        id = 5,
        text = "How long can you usually wait after you feel hungry before eating?",
        mandagni = "More than 2 hours",
        vishamagni = "Sometimes up to 1 hour, but sometimes less than 1 hour",
        samagni = "About 1 to 2 hours",
        tikshnagni = "It is very difficult to wait when hungry"
    ),
    aq(
        id = 6,
        text = "How much food do you usually eat at each meal?",
        mandagni = "Usually small meals",
        vishamagni = "Sometimes large and sometimes small meals",
        samagni = "Neither very small nor very large meals",
        tikshnagni = "Usually large meals"
    ),
    aq(
        id = 7,
        text = "How long does it usually take you to digest a heavy meal?",
        mandagni = "Usually longer than normal",
        vishamagni = "The time varies",
        samagni = "A normal amount of time",
        tikshnagni = "Quite quickly"
    ),
    AgniQuestion(
        id = 8,
        text = "What are your bowel habits usually like?",
        options = listOf(
            AgniOption("mandagni", "You tend to have constipation", AgniType.Mandagni),
            AgniOption("vishamagni", "Your stools are sometimes hard and sometimes soft", AgniType.Vishamagni),
            AgniOption("samagni", "Your stools are normal, neither hard nor soft", AgniType.Samagni)
        )
    ),
    aq(
        id = 9,
        text = "How regular are your usual meal times?",
        mandagni = "You usually eat after the scheduled time",
        vishamagni = "You usually eat before or after the scheduled time",
        samagni = "You usually eat exactly at the scheduled time",
        tikshnagni = "You usually eat before the scheduled time"
    ),
    aq(
        id = 10,
        text = "How do you usually feel after your food has been fully digested?",
        mandagni = "You often feel heaviness in your abdomen or body",
        vishamagni = "You sometimes feel slight heaviness",
        samagni = "You mostly feel light",
        tikshnagni = "You feel light quite soon after eating"
    ),
    aq(
        id = 11,
        text = "What do you usually feel when you see foods that you like?",
        mandagni = "You do not want to eat even when hungry",
        vishamagni = "Sometimes you want to eat and sometimes you do not",
        samagni = "You want to eat the food",
        tikshnagni = "You want to eat almost any food, whether you like it or not"
    )
)

fun calculateAgniScores(answers: Map<Int, String>): Map<AgniType, Int> {
    validateAgniAnswers(answers)
    val questionsById = agniQuestions.associateBy { it.id }
    val scores = AgniType.entries.associateWith { 0 }.toMutableMap()
    answers.forEach { (questionId, optionId) ->
        val option = questionsById.getValue(questionId).options.first { it.id == optionId }
        scores[option.type] = scores.getValue(option.type) + 1
    }
    return scores
}

fun generateAgniResult(
    answers: Map<Int, String>,
    completedAtMillis: Long = System.currentTimeMillis()
): AgniResult {
    val scores = calculateAgniScores(answers)
    val highest = scores.values.maxOrNull() ?: 0
    val topTypes = AgniType.entries.filter { scores.getValue(it) == highest }
    val mixed = topTypes.size > 1
    return AgniResult(
        answers = answers,
        scores = scores,
        dominantAgni = if (mixed) "Mixed Agni Pattern" else topTypes.first().displayName,
        mixedAgniTypes = if (mixed) topTypes.map { it.displayName } else emptyList(),
        isMixed = mixed,
        completedAtMillis = completedAtMillis
    )
}

fun validateAgniAnswers(answers: Map<Int, String>) {
    val requiredQuestionIds = agniQuestions.map { it.id }.toSet()
    require(answers.keys == requiredQuestionIds) { "All 11 Agni questions must be answered exactly once." }
    val validOptionsByQuestion = agniQuestions.associate { question -> question.id to question.options.map { it.id }.toSet() }
    require(answers.all { (questionId, optionId) -> optionId in validOptionsByQuestion.getValue(questionId) }) {
        "Agni answers contain an invalid option."
    }
}

private fun aq(
    id: Int,
    text: String,
    mandagni: String,
    vishamagni: String,
    samagni: String,
    tikshnagni: String
): AgniQuestion {
    return AgniQuestion(
        id = id,
        text = text,
        options = listOf(
            AgniOption("mandagni", mandagni, AgniType.Mandagni),
            AgniOption("vishamagni", vishamagni, AgniType.Vishamagni),
            AgniOption("samagni", samagni, AgniType.Samagni),
            AgniOption("tikshnagni", tikshnagni, AgniType.Tikshnagni)
        )
    )
}
