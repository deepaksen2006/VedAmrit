package com.example.vedaahar.dosha

enum class Dosha(val displayName: String) {
    Vata("Vata"),
    Pitta("Pitta"),
    Kapha("Kapha")
}

enum class QuestionCategory(val title: String) {
    Physical("Physical (Sharirik Lakshan)"),
    MentalEmotional("Mental & Emotional Attributes"),
    Digestive("Digestive System (Agni & Gut)"),
    Lifestyle("Lifestyle & Routine"),
    Metabolism("Metabolism & Energy"),
    Diet("Diet & Food Habits")
}

data class DoshaScore(
    val dosha: Dosha,
    val points: Int
)

data class DoshaOption(
    val id: String,
    val title: String,
    val description: String,
    val scores: List<DoshaScore>
)

data class DoshaQuestion(
    val id: Int,
    val title: String,
    val category: QuestionCategory,
    val whyWeAsk: String,
    val options: List<DoshaOption>
)

enum class DoshaProfileType {
    Single,
    Dual,
    Balanced
}

data class DoshaResult(
    val profileName: String,
    val profileType: DoshaProfileType,
    val dominant: Dosha,
    val secondary: Dosha?,
    val scores: Map<Dosha, Int>,
    val percentages: Map<Dosha, Int>,
    val description: DoshaResultDescription
)

data class DoshaResultDescription(
    val overview: String,
    val personalityTraits: String,
    val digestionStyle: String,
    val emotionalTendencies: String,
    val energyBehavior: String,
    val strengths: String,
    val imbalanceRisks: String,
    val lifestyleSuggestions: String,
    val foodRecommendations: String
)

data class SavedDoshaAssessment(
    val profileName: String,
    val dominantName: String,
    val overview: String,
    val percentages: Map<Dosha, Int>,
    val savedAtMillis: Long
)

data class DoshaAssessmentUiState(
    val questions: List<DoshaQuestion> = DoshaQuestionBank.questions,
    val answers: Map<Int, String> = emptyMap(),
    val currentQuestionIndex: Int = 0,
    val result: DoshaResult? = null,
    val savedAtMillis: Long? = null,
    val previousSavedResult: SavedDoshaAssessment? = null,
    val savedAssessmentCount: Int = 0
) {
    val currentQuestion: DoshaQuestion
        get() = questions[currentQuestionIndex]

    val selectedOptionId: String?
        get() = answers[currentQuestion.id]

    val progress: Float
        get() = (currentQuestionIndex + 1).toFloat() / questions.size.toFloat()

    val answeredCount: Int
        get() = answers.size

    val canGoPrevious: Boolean
        get() = currentQuestionIndex > 0

    val isLastQuestion: Boolean
        get() = currentQuestionIndex == questions.lastIndex
}
