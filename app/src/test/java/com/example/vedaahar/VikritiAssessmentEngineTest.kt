package com.example.vedaahar

import com.example.vedaahar.vikriti.VikritiDosha
import com.example.vedaahar.vikriti.calculateVikritiPercentages
import com.example.vedaahar.vikriti.classifyVikritiSeverity
import com.example.vedaahar.vikriti.generateVikritiResult
import com.example.vedaahar.vikriti.vikritiQuestions
import org.junit.Assert.assertEquals
import org.junit.Test

class VikritiAssessmentEngineTest {

    @Test
    fun scoresEachDoshaIndependently() {
        val result = generateVikritiResult(
            answers(
                vata = "very_often",
                pitta = "somewhat",
                kapha = "not_at_all"
            ),
            prakritiType = "Vata-Pitta",
            completedAtMillis = 123L
        )

        assertEquals(35, result.scores.getValue(VikritiDosha.Vata))
        assertEquals(21, result.scores.getValue(VikritiDosha.Pitta))
        assertEquals(7, result.scores.getValue(VikritiDosha.Kapha))
        assertEquals("Vata", result.dominantDosha)
        assertEquals("Vata-Pitta", result.prakritiType)
        assertEquals(123L, result.completedAtMillis)
    }

    @Test
    fun appliesSeverityRanges() {
        assertEquals("Low", classifyVikritiSeverity(7))
        assertEquals("Low", classifyVikritiSeverity(13))
        assertEquals("Mild", classifyVikritiSeverity(14))
        assertEquals("Mild", classifyVikritiSeverity(20))
        assertEquals("Moderate", classifyVikritiSeverity(21))
        assertEquals("Moderate", classifyVikritiSeverity(27))
        assertEquals("High", classifyVikritiSeverity(28))
        assertEquals("High", classifyVikritiSeverity(35))
    }

    @Test
    fun percentagesDoNotNeedToSumToOneHundred() {
        val percentages = calculateVikritiPercentages(
            mapOf(
                VikritiDosha.Vata to 35,
                VikritiDosha.Pitta to 35,
                VikritiDosha.Kapha to 35
            )
        )

        assertEquals(100.0, percentages.getValue(VikritiDosha.Vata), 0.0)
        assertEquals(100.0, percentages.getValue(VikritiDosha.Pitta), 0.0)
        assertEquals(100.0, percentages.getValue(VikritiDosha.Kapha), 0.0)
    }

    @Test
    fun incompleteAnswersAreRejected() {
        assertThrowsIllegalArgument {
            generateVikritiResult(mapOf("V1" to "not_at_all"), prakritiType = "Pitta")
        }
    }

    @Test
    fun invalidAnswersAreRejected() {
        assertThrowsIllegalArgument {
            generateVikritiResult(vikritiQuestions.associate { it.id to "sometimes" }, prakritiType = "Kapha")
        }
    }

    private fun answers(vata: String, pitta: String, kapha: String): Map<String, String> {
        return vikritiQuestions.associate { question ->
            question.id to when (question.dosha) {
                VikritiDosha.Vata -> vata
                VikritiDosha.Pitta -> pitta
                VikritiDosha.Kapha -> kapha
            }
        }
    }

    private fun assertThrowsIllegalArgument(block: () -> Unit) {
        try {
            block()
        } catch (_: IllegalArgumentException) {
            return
        }
        throw AssertionError("Expected IllegalArgumentException")
    }
}
