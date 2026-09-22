package com.example.vedaahar.agentic

import com.example.vedaahar.profile.StoredPatientProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgenticPlanRequestMapperTest {
    private val rawAssessment = """
        {
          "prakriti_result": {
            "scores": {"Vata": 18, "Pitta": 10, "Kapha": 4},
            "percentages": {"Vata": 56.2, "Pitta": 31.3, "Kapha": 12.5},
            "primaryDosha": "Vata",
            "secondaryDosha": "Pitta",
            "prakritiType": "Vata-Pitta",
            "borderline": false,
            "questionsAnswered": 21
          },
          "vikriti_result": {
            "answers": {"V1": "not_at_all"},
            "scores": {"Vata": 3, "Pitta": 19, "Kapha": 6},
            "severities": {"Vata": "Low", "Pitta": "Moderate", "Kapha": "Low"},
            "percentages": {"Vata": 10.7, "Pitta": 67.9, "Kapha": 21.4},
            "dominantDosha": "Pitta",
            "completedAtMillis": 1720000000000,
            "prakritiType": "Vata-Pitta"
          },
          "agni_result": {
            "answers": {"1": "mandagni"},
            "scores": {"Mandagni": 2, "Vishamagni": 1, "Samagni": 8, "Tikshnagni": 0},
            "dominantAgni": "Samagni",
            "mixedAgniTypes": [],
            "isMixed": false,
            "completedAtMillis": 1720000001000
          }
        }
    """.trimIndent()

    private fun completeProfile() = StoredPatientProfile(
        fullName = "Anaya Rao",
        age = "34",
        gender = "Female",
        heightCm = "164",
        weightKg = "58",
        dietaryPreference = "vegetarian",
        goal = "general wellness",
        activityLevel = "moderate",
        allergies = emptyList(),
        foodIntolerances = emptyList(),
        foodsToAvoid = emptyList(),
        healthConditions = emptyList(),
        medicineAllergies = emptyList(),
        currentMedicines = emptyList(),
        medicationRestrictions = emptyList(),
        doctorRestrictions = emptyList(),
        pregnancyInformation = "not_applicable"
    )

    @Test
    fun preservesCanonicalAssessmentValuesExactly() {
        val result = AgenticPlanRequestMapper.fromRawAssessmentAndProfile(rawAssessment, completeProfile())

        val request = result.successRequest()
        assertEquals("Vata-Pitta", request.assessment.prakriti["prakritiType"])
        assertEquals("Vata", request.assessment.prakriti["primaryDosha"])
        assertEquals("Pitta", request.assessment.vikriti["dominantDosha"])
        assertEquals("Samagni", request.assessment.agni["dominantAgni"])
        assertEquals(21, (request.assessment.prakriti["questionsAnswered"] as Number).toInt())
    }

    @Test
    fun mapsCompleteProfileToBackendContractFieldNames() {
        val result = AgenticPlanRequestMapper.fromRawAssessmentAndProfile(
            rawAssessment,
            completeProfile().copy(
                allergies = listOf("peanut"),
                foodIntolerances = listOf("lactose"),
                foodsToAvoid = listOf("curd at night"),
                healthConditions = listOf("acidity"),
                medicineAllergies = listOf("penicillin"),
                currentMedicines = listOf("vitamin d"),
                medicationRestrictions = listOf("avoid grapefruit"),
                doctorRestrictions = listOf("low sodium"),
                pregnancyInformation = "not_pregnant"
            )
        )

        val profile = result.successRequest().patientProfile
        assertEquals("Anaya Rao", profile["name"])
        assertEquals("34", profile["age"])
        assertEquals("female", profile["gender"])
        assertEquals("164", profile["height_cm"])
        assertEquals("58", profile["weight_kg"])
        assertEquals("vegetarian", profile["dietary_preference"])
        assertEquals(listOf("peanut"), profile["allergies"])
        assertEquals(listOf("vitamin d"), profile["current_medicines"])
        assertEquals("not_pregnant", profile["pregnancy_information"])
    }

    @Test
    fun passesCanonicalPrakritiThroughUnchanged() {
        val result = AgenticPlanRequestMapper.fromRawAssessmentAndProfile(rawAssessment, completeProfile())

        val prakriti = result.successRequest().assessment.prakriti
        assertEquals("Vata-Pitta", prakriti["prakritiType"])
        assertEquals("Vata", prakriti["primaryDosha"])
        assertEquals("Pitta", prakriti["secondaryDosha"])
    }

    @Test
    fun passesCanonicalVikritiThroughUnchanged() {
        val result = AgenticPlanRequestMapper.fromRawAssessmentAndProfile(rawAssessment, completeProfile())

        val vikriti = result.successRequest().assessment.vikriti
        assertEquals("Pitta", vikriti["dominantDosha"])
        assertEquals("Moderate", (vikriti["severities"] as Map<*, *>)["Pitta"])
        assertEquals("Vata-Pitta", vikriti["prakritiType"])
    }

    @Test
    fun passesCanonicalAgniThroughUnchanged() {
        val result = AgenticPlanRequestMapper.fromRawAssessmentAndProfile(rawAssessment, completeProfile())

        val agni = result.successRequest().assessment.agni
        assertEquals("Samagni", agni["dominantAgni"])
        assertEquals(false, agni["isMixed"])
        assertEquals(emptyList<Any?>(), agni["mixedAgniTypes"])
    }

    @Test
    fun doesNotRescoreQuestionnaireAnswers() {
        val contradictoryAssessment = rawAssessment.replace(
            "\"answers\": {\"1\": \"mandagni\"}",
            "\"answers\": {\"1\": \"mandagni\", \"2\": \"mandagni\", \"3\": \"mandagni\"}"
        )

        val result = AgenticPlanRequestMapper.fromRawAssessmentAndProfile(contradictoryAssessment, completeProfile())

        val agni = result.successRequest().assessment.agni
        assertEquals("Samagni", agni["dominantAgni"])
        assertEquals(8, ((agni["scores"] as Map<*, *>)["Samagni"] as Number).toInt())
    }

    @Test
    fun reportsMissingProfileDataWithoutInventingValues() {
        val result = AgenticPlanRequestMapper.fromRawAssessmentAndProfile(
            rawAssessment,
            StoredPatientProfile(fullName = "Anaya", age = "34", gender = "Female", heightCm = "164", weightKg = "58")
        )

        assertTrue(result is AgenticRequestMappingResult.MissingData)
        val missing = (result as AgenticRequestMappingResult.MissingData).missingFields
        assertTrue(missing.contains("patient_profile.dietary_preference"))
        assertTrue(missing.contains("patient_profile.allergies"))
        assertTrue(missing.contains("patient_profile.health_conditions"))
    }

    @Test
    fun acceptsExplicitlyEmptySafetyLists() {
        val result = AgenticPlanRequestMapper.fromRawAssessmentAndProfile(rawAssessment, completeProfile())

        val profile = result.successRequest().patientProfile
        assertEquals(emptyList<String>(), profile["allergies"])
        assertEquals(emptyList<String>(), profile["doctor_restrictions"])
    }

    @Test
    fun rejectsMalformedCanonicalAssessmentData() {
        val malformedAssessment = """
            {
              "prakriti_result": {"prakritiType": "Vata-Pitta"},
              "vikriti_result": {"dominantDosha": "Pitta", "scores": {"Pitta": 19}},
              "agni_result": {"dominantAgni": "Samagni", "scores": {"Samagni": 8}}
            }
        """.trimIndent()

        val result = AgenticPlanRequestMapper.fromRawAssessmentAndProfile(malformedAssessment, completeProfile())

        assertTrue(result is AgenticRequestMappingResult.MalformedData)
    }

    @Test
    fun rejectsMissingRequiredAssessmentFields() {
        val result = AgenticPlanRequestMapper.fromRawAssessmentAndProfile("{}", completeProfile())

        assertTrue(result is AgenticRequestMappingResult.MalformedData)
    }

    @Test
    fun rejectsMissingRequiredProfileFields() {
        val result = AgenticPlanRequestMapper.fromRawAssessmentAndProfile(
            rawAssessment,
            completeProfile().copy(dietaryPreference = "", allergies = null, pregnancyInformation = "")
        )

        assertTrue(result is AgenticRequestMappingResult.MissingData)
        val missing = (result as AgenticRequestMappingResult.MissingData).missingFields
        assertTrue(missing.contains("patient_profile.dietary_preference"))
        assertTrue(missing.contains("patient_profile.allergies"))
        assertTrue(missing.contains("patient_profile.pregnancy_information"))
    }

    @Test
    fun rejectsOutOfRangeProfileNumbers() {
        val result = AgenticPlanRequestMapper.fromRawAssessmentAndProfile(
            rawAssessment,
            completeProfile().copy(age = "121", heightCm = "49", weightKg = "301")
        )

        assertTrue(result is AgenticRequestMappingResult.MissingData)
        val missing = (result as AgenticRequestMappingResult.MissingData).missingFields
        assertTrue(missing.contains("patient_profile.age"))
        assertTrue(missing.contains("patient_profile.height_cm"))
        assertTrue(missing.contains("patient_profile.weight_kg"))
    }

    private fun AgenticRequestMappingResult.successRequest() =
        when (this) {
            is AgenticRequestMappingResult.Success -> request
            else -> error("Expected success but was $this")
        }
}
