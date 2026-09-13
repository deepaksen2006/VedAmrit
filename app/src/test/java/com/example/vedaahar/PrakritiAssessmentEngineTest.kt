package com.example.vedaahar

import com.example.vedaahar.prakriti.PrakritiDosha
import com.example.vedaahar.prakriti.calculatePrakritiPercentages
import com.example.vedaahar.prakriti.generatePrakritiResult
import com.example.vedaahar.prakriti.prakritiQuestions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrakritiAssessmentEngineTest {

    @Test
    fun pureVataDominant() {
        val result = generatePrakritiResult(answers(a = 21))
        assertEquals("Vata", result.prakritiType)
        assertEquals("Vata", result.primaryDosha)
        assertEquals(21, result.scores.getValue(PrakritiDosha.Vata))
        assertFalse(result.borderline)
    }

    @Test
    fun purePittaDominant() {
        val result = generatePrakritiResult(answers(b = 21))
        assertEquals("Pitta", result.prakritiType)
        assertEquals("Pitta", result.primaryDosha)
    }

    @Test
    fun pureKaphaDominant() {
        val result = generatePrakritiResult(answers(c = 21))
        assertEquals("Kapha", result.prakritiType)
        assertEquals("Kapha", result.primaryDosha)
    }

    @Test
    fun vataPittaDual() {
        val result = generatePrakritiResult(answers(a = 9, b = 8, c = 4))
        assertEquals("Vata-Pitta", result.prakritiType)
        assertEquals("Vata", result.primaryDosha)
        assertEquals("Pitta", result.secondaryDosha)
    }

    @Test
    fun pittaVataDual() {
        val result = generatePrakritiResult(answers(a = 8, b = 9, c = 4))
        assertEquals("Pitta-Vata", result.prakritiType)
        assertEquals("Pitta", result.primaryDosha)
        assertEquals("Vata", result.secondaryDosha)
    }

    @Test
    fun vataKaphaDual() {
        val result = generatePrakritiResult(answers(a = 9, b = 4, c = 8))
        assertEquals("Vata-Kapha", result.prakritiType)
    }

    @Test
    fun kaphaVataDual() {
        val result = generatePrakritiResult(answers(a = 8, b = 4, c = 9))
        assertEquals("Kapha-Vata", result.prakritiType)
    }

    @Test
    fun pittaKaphaDual() {
        val result = generatePrakritiResult(answers(a = 3, b = 10, c = 8))
        assertEquals("Pitta-Kapha", result.prakritiType)
    }

    @Test
    fun kaphaPittaDual() {
        val result = generatePrakritiResult(answers(a = 3, b = 8, c = 10))
        assertEquals("Kapha-Pitta", result.prakritiType)
    }

    @Test
    fun tridoshicResult() {
        val result = generatePrakritiResult(answers(a = 8, b = 7, c = 6))
        assertEquals("Tridoshic", result.prakritiType)
        assertEquals("Balanced", result.primaryDosha)
        assertEquals(null, result.secondaryDosha)
    }

    @Test
    fun borderlineResult() {
        val result = generatePrakritiResult(answers(a = 11, b = 7, c = 3))
        assertEquals("Vata", result.prakritiType)
        assertEquals("Vata", result.primaryDosha)
        assertTrue(result.borderline)
    }

    @Test
    fun incompleteAnswersAreRejected() {
        assertThrowsIllegalArgument {
            generatePrakritiResult(mapOf(1 to "A", 2 to "B"))
        }
    }

    @Test
    fun invalidAnswersAreRejected() {
        assertThrowsIllegalArgument {
            generatePrakritiResult(prakritiQuestions.associate { it.id to "X" })
        }
    }

    @Test
    fun percentagesRoundToOneDecimalAndTotalApproximatelyOneHundred() {
        val percentages = calculatePrakritiPercentages(
            mapOf(
                PrakritiDosha.Vata to 9,
                PrakritiDosha.Pitta to 8,
                PrakritiDosha.Kapha to 4
            )
        )
        assertEquals(42.9, percentages.getValue(PrakritiDosha.Vata), 0.0)
        assertEquals(38.1, percentages.getValue(PrakritiDosha.Pitta), 0.0)
        assertEquals(19.0, percentages.getValue(PrakritiDosha.Kapha), 0.0)
        val total = percentages.values.sum()
        assertTrue(total in 99.9..100.1)
    }

    private fun answers(a: Int = 0, b: Int = 0, c: Int = 0): Map<Int, String> {
        require(a + b + c == prakritiQuestions.size)
        val selections = List(a) { "A" } + List(b) { "B" } + List(c) { "C" }
        return prakritiQuestions.mapIndexed { index, question -> question.id to selections[index] }.toMap()
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
