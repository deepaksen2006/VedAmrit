package com.example.vedaahar

import com.example.vedaahar.agni.AgniType
import com.example.vedaahar.agni.agniQuestions
import com.example.vedaahar.agni.generateAgniResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgniAssessmentEngineTest {

    @Test
    fun samagniDominatesByCount() {
        val result = generateAgniResult(answers(default = "samagni"), completedAtMillis = 456L)

        assertEquals("Samagni", result.dominantAgni)
        assertEquals("Samagni", result.displayResult)
        assertEquals(11, result.scores.getValue(AgniType.Samagni))
        assertEquals(456L, result.completedAtMillis)
        assertFalse(result.isMixed)
    }

    @Test
    fun detectsMixedAgniPatternOnHighestTie() {
        val selections = agniQuestions.associate { question ->
            question.id to when (question.id) {
                1, 2, 3, 4 -> "vishamagni"
                5, 6, 7, 8 -> "samagni"
                9, 10 -> "mandagni"
                else -> "tikshnagni"
            }
        }

        val result = generateAgniResult(selections)

        assertTrue(result.isMixed)
        assertEquals("Mixed Agni Pattern", result.dominantAgni)
        assertEquals(listOf("Vishamagni", "Samagni"), result.mixedAgniTypes)
        assertEquals("Mixed Agni - Vishamagni + Samagni", result.displayResult)
    }

    @Test
    fun questionEightHasThreeOptionsOnly() {
        assertEquals(3, agniQuestions.first { it.id == 8 }.options.size)
    }

    @Test
    fun incompleteAnswersAreRejected() {
        assertThrowsIllegalArgument {
            generateAgniResult(mapOf(1 to "samagni"))
        }
    }

    @Test
    fun invalidOptionForQuestionIsRejected() {
        val selections = answers(default = "samagni") + (8 to "tikshnagni")
        assertThrowsIllegalArgument {
            generateAgniResult(selections)
        }
    }

    private fun answers(default: String): Map<Int, String> {
        return agniQuestions.associate { it.id to default }
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
