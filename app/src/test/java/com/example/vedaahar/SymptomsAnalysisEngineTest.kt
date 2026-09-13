package com.example.vedaahar

import com.example.vedaahar.symptoms.emptySymptomsInput
import com.example.vedaahar.symptoms.generateSymptomsAnalysisResult
import com.example.vedaahar.symptoms.symptomFeatures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SymptomsAnalysisEngineTest {

    @Test
    fun createsAllThirtyOneBinaryFeaturesInOrder() {
        val input = emptySymptomsInput()

        assertEquals(31, input.size)
        assertEquals(symptomFeatures.map { it.key }, input.keys.toList())
        assertEquals(0, input.getValue("upper_abdomain_pain"))
    }

    @Test
    fun reportsOnlySymptomsSelectedAsYes() {
        val input = emptySymptomsInput() + mapOf(
            "acidity" to 1,
            "headache" to 1,
            "nausea" to 1,
            "abdominal_bloating" to 1,
            "heartburn" to 1
        )

        val result = generateSymptomsAnalysisResult(input, completedAtMillis = 789L)

        assertEquals(listOf("Acidity", "Headache", "Nausea", "Abdominal Bloating", "Heartburn"), result.reportedSymptoms.map { it.label })
        assertEquals(789L, result.completedAtMillis)
    }

    @Test
    fun noSymptomsProducesNoPossibleCondition() {
        val result = generateSymptomsAnalysisResult(emptySymptomsInput())

        assertEquals(emptyList<String>(), result.reportedSymptoms.map { it.label })
        assertNull(result.possibleCondition)
    }

    @Test
    fun invalidBinaryValueIsRejected() {
        assertThrowsIllegalArgument {
            generateSymptomsAnalysisResult(emptySymptomsInput() + ("acidity" to 2))
        }
    }

    @Test
    fun missingFeatureIsRejected() {
        assertThrowsIllegalArgument {
            generateSymptomsAnalysisResult(emptySymptomsInput() - "belching")
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
