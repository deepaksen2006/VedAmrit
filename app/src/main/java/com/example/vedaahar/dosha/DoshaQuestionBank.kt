package com.example.vedaahar.dosha

object DoshaQuestionBank {
    val questions = listOf(
        q(
            id = 1,
            title = "Body Frame",
            category = QuestionCategory.Physical,
            a = "Thin, bony and small framed. Hardly gain weight",
            b = "Medium built. Can gain or lose weight easily",
            c = "Large built. Gain weight easily but difficult to lose"
        ),
        q(
            id = 2,
            title = "Walk & Talk",
            category = QuestionCategory.Physical,
            a = "Fast walk and talk",
            b = "Moderate and determined walk",
            c = "Slow and steady walk"
        ),
        q(
            id = 3,
            title = "Weather Reaction",
            category = QuestionCategory.Physical,
            a = "Enjoy warm climate but feel uncomfortable in cool weather",
            b = "Enjoy cool weather and dislike warm climate",
            c = "Comfortable most of the year but prefer summer and spring. Do not like damp climate"
        ),
        q(
            id = 4,
            title = "Sweating",
            category = QuestionCategory.Physical,
            a = "Sweat little. Minimal body odour",
            b = "Sweat a lot. Medium body odour",
            c = "Sweat moderately but sweat a lot when working hard. Strong body odour"
        ),
        q(
            id = 5,
            title = "Appetite",
            category = QuestionCategory.Physical,
            a = "Irregular appetite. Sometimes hungry, sometimes not",
            b = "Strong and sharp appetite. Always feel hungry",
            c = "Decent appetite. Tendency to eat for comfort and taste"
        ),
        q(
            id = 6,
            title = "Skin",
            category = QuestionCategory.Physical,
            a = "Normal to dry, rough, thin and cool. Dryness, dullness and wrinkles",
            b = "Normal to oily, soft, reddish, sensitive and warm. Inflammation issues",
            c = "Normal to oily, soft, thick and cool. Excessive oily, itching, fungal infections"
        ),
        q(
            id = 7,
            title = "Hair",
            category = QuestionCategory.Physical,
            a = "Rough, dry and wavy. Split ends easily",
            b = "Normal, straight, thin and brownish",
            c = "Thick, curly and oily. Hair colour tends to be darker"
        ),
        q(
            id = 8,
            title = "Lips & Teeth",
            category = QuestionCategory.Physical,
            a = "Thin dry lips. Teeth somewhat uneven and need constant care",
            b = "Medium soft lips. Teeth medium sized but cavity tendency",
            c = "Large smooth lips. Teeth well formed, aligned and require less care"
        ),
        q(
            id = 9,
            title = "Eyes",
            category = QuestionCategory.Physical,
            a = "Small eyes. Dry and sleepy eyes, blink a lot",
            b = "Medium eyes. Often reddish eyes",
            c = "Big attractive eyes with thick eyelashes"
        ),
        q(
            id = 10,
            title = "General Signs",
            category = QuestionCategory.Physical,
            a = "Cracking sound in joints. Small forehead. Nails crack easily",
            b = "Black moles on body. Medium forehead. Nails pink and soft",
            c = "Heavy thighs/hips, large forehead. Nails wide and whitish"
        ),
        q(
            id = 11,
            title = "Memory",
            category = QuestionCategory.MentalEmotional,
            a = "Quick to learn but quick to forget. Good short-term memory",
            b = "Average learning speed. Once learnt, never forgets",
            c = "Slow to learn but remembers long time. Good long-term memory"
        ),
        q(
            id = 12,
            title = "Mind",
            category = QuestionCategory.MentalEmotional,
            a = "Mind gets restless easily",
            b = "Mind gets impatient or aggressive easily",
            c = "Mind remains cool and calm, mostly unruffled"
        ),
        q(
            id = 13,
            title = "Mind on Actions",
            category = QuestionCategory.MentalEmotional,
            a = "Overthinking",
            b = "Quick implementation",
            c = "Lazy implementation. Tendency to procrastinate"
        ),
        q(
            id = 14,
            title = "Sleep Quality",
            category = QuestionCategory.MentalEmotional,
            a = "Light and disturbed sleep. Wake up easily in morning",
            b = "Moderate but regular sleep. Can go back to sleep easily",
            c = "Deep and heavy sleep. Cannot easily wake up in morning"
        ),
        q(
            id = 15,
            title = "Emotional Nature",
            category = QuestionCategory.MentalEmotional,
            a = "Worry a lot. Often nervous and anxious",
            b = "Often irritable, angry and impatient",
            c = "Loving and caring. Takes a lot to become angry"
        )
    )

    private fun q(
        id: Int,
        title: String,
        category: QuestionCategory,
        a: String,
        b: String,
        c: String
    ) = DoshaQuestion(
        id = id,
        title = title,
        category = category,
        whyWeAsk = "This helps map your natural Vata, Pitta, and Kapha tendencies across body, mind, and daily rhythm.",
        options = listOf(
            o("a", a, Dosha.Vata),
            o("b", b, Dosha.Pitta),
            o("c", c, Dosha.Kapha)
        )
    )

    private fun o(id: String, label: String, dosha: Dosha) =
        DoshaOption(id = id, title = label, description = dosha.displayName, scores = listOf(DoshaScore(dosha, 1)))
}
