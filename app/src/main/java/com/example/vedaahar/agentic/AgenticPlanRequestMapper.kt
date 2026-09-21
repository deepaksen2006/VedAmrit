package com.example.vedaahar.agentic

import android.content.Context
import com.example.vedaahar.assessment.AssessmentProfileStore
import com.example.vedaahar.network.dto.AgenticPlanRequest
import com.example.vedaahar.network.dto.CanonicalAssessmentDto
import com.example.vedaahar.profile.PatientProfileStore
import com.example.vedaahar.profile.StoredPatientProfile
import org.json.JSONArray
import org.json.JSONObject

sealed class AgenticRequestMappingResult {
    data class Success(val request: AgenticPlanRequest) : AgenticRequestMappingResult()
    data class MissingData(val missingFields: List<String>) : AgenticRequestMappingResult()
    data class MalformedData(val reason: String) : AgenticRequestMappingResult()
}

object AgenticPlanRequestMapper {
    private val SupportedGenders = setOf("male", "female", "other")
    private val SupportedDietaryPreferences = setOf("vegetarian", "vegan", "non-vegetarian")
    private val SupportedGoals = setOf("general wellness", "weight loss", "weight gain", "weight management", "digestion")
    private val SupportedActivityLevels = setOf("sedentary", "low", "moderate", "high")

    fun fromStoredState(context: Context): AgenticRequestMappingResult {
        val rawAssessment = AssessmentProfileStore.currentRaw(context)
            ?: return AgenticRequestMappingResult.MissingData(
                listOf(
                    "assessment.prakriti",
                    "assessment.vikriti",
                    "assessment.agni"
                )
            )
        return fromRawAssessmentAndProfile(
            rawAssessment = rawAssessment,
            profile = PatientProfileStore.current(context)
        )
    }

    fun fromRawAssessmentAndProfile(
        rawAssessment: String,
        profile: StoredPatientProfile
    ): AgenticRequestMappingResult {
        val missing = mutableListOf<String>()
        val gender = profile.gender.trim().lowercase()
        val dietaryPreference = profile.dietaryPreference.trim().lowercase()
        val age = profile.age.trim().toIntOrNull()
        val heightCm = profile.heightCm.trim().toFloatOrNull()
        val weightKg = profile.weightKg.trim().toFloatOrNull()

        if (profile.fullName.isBlank()) missing += "patient_profile.name"
        if (age?.let { it in 1..120 } != true) missing += "patient_profile.age"
        if (gender !in SupportedGenders) missing += "patient_profile.gender"
        if (heightCm?.let { it in 50f..250f } != true) missing += "patient_profile.height_cm"
        if (weightKg?.let { it in 10f..300f } != true) missing += "patient_profile.weight_kg"

        if (dietaryPreference !in SupportedDietaryPreferences) missing += "patient_profile.dietary_preference"
        if (profile.goal.isBlank()) missing += "patient_profile.goal"
        if (profile.goal.trim().lowercase() !in SupportedGoals) missing += "patient_profile.goal"
        if (profile.activityLevel.trim().lowercase() !in SupportedActivityLevels) missing += "patient_profile.activity_level"
        if (profile.allergies == null) missing += "patient_profile.allergies"
        if (profile.foodIntolerances == null) missing += "patient_profile.food_intolerances"
        if (profile.foodsToAvoid == null) missing += "patient_profile.foods_to_avoid"
        if (profile.healthConditions == null) missing += "patient_profile.health_conditions"
        if (profile.medicineAllergies == null) missing += "patient_profile.medicine_allergies"
        if (profile.medicationRestrictions == null) missing += "patient_profile.medication_restrictions"
        if (profile.doctorRestrictions == null) missing += "patient_profile.doctor_restrictions"
        if (profile.pregnancyInformation.isBlank()) missing += "patient_profile.pregnancy_information"

        if (missing.isNotEmpty()) {
            return AgenticRequestMappingResult.MissingData(missing.distinct())
        }

        return runCatching {
            val root = JSONObject(rawAssessment)
            val prakriti = root.optJSONObject("prakriti_result")
                ?: error("assessment.prakriti_result is missing")
            val vikriti = root.optJSONObject("vikriti_result")
                ?: error("assessment.vikriti_result is missing")
            val agni = root.optJSONObject("agni_result")
                ?: error("assessment.agni_result is missing")
            validateCanonicalAssessment(prakriti, vikriti, agni)

            AgenticRequestMappingResult.Success(
                AgenticPlanRequest(
                    patientProfile = profile.toPatientProfilePayload(
                        gender = gender,
                        dietaryPreference = dietaryPreference
                    ),
                    assessment = CanonicalAssessmentDto(
                        prakriti = prakriti.toKotlinMap(),
                        vikriti = vikriti.toKotlinMap(),
                        agni = agni.toKotlinMap()
                    )
                )
            )
        }.getOrElse { error ->
            AgenticRequestMappingResult.MalformedData(error.message ?: "Stored assessment data is malformed.")
        }
    }
}

private fun validateCanonicalAssessment(
    prakriti: JSONObject,
    vikriti: JSONObject,
    agni: JSONObject
) {
    requireCanonicalField(
        result = prakriti,
        fieldOptions = listOf("prakritiType", "constitution", "primaryDosha", "primary_dosha"),
        label = "assessment.prakriti.prakritiType"
    )
    requireCanonicalField(
        result = vikriti,
        fieldOptions = listOf("dominantDosha", "dominant_dosha"),
        label = "assessment.vikriti.dominantDosha"
    )
    requireCanonicalField(
        result = agni,
        fieldOptions = listOf("dominantAgni", "status", "displayResult"),
        label = "assessment.agni.dominantAgni"
    )
    listOf(
        "assessment.prakriti.scores" to prakriti,
        "assessment.vikriti.scores" to vikriti,
        "assessment.agni.scores" to agni
    ).forEach { (label, result) ->
        if (result.optJSONObject("scores") == null) error("$label must be present as an object")
    }
}

private fun requireCanonicalField(
    result: JSONObject,
    fieldOptions: List<String>,
    label: String
) {
    val hasValue = fieldOptions.any { field ->
        val value = result.opt(field)
        value != null && value != JSONObject.NULL && value.toString().isNotBlank()
    }
    if (!hasValue) error("$label is missing")
}

private fun StoredPatientProfile.toPatientProfilePayload(
    gender: String,
    dietaryPreference: String
): Map<String, Any> {
    val payload = mutableMapOf<String, Any>(
        "name" to fullName.trim(),
        "age" to age.trim(),
        "gender" to gender,
        "height_cm" to heightCm.trim(),
        "weight_kg" to weightKg.trim(),
        "dietary_preference" to dietaryPreference,
        "goal" to goal.trim().lowercase(),
        "activity_level" to activityLevel.trim().lowercase(),
        "allergies" to allergies.orEmpty(),
        "food_intolerances" to foodIntolerances.orEmpty(),
        "foods_to_avoid" to foodsToAvoid.orEmpty(),
        "health_conditions" to healthConditions.orEmpty(),
        "medicine_allergies" to medicineAllergies.orEmpty(),
        "medication_restrictions" to medicationRestrictions.orEmpty(),
        "doctor_restrictions" to doctorRestrictions.orEmpty(),
        "pregnancy_information" to pregnancyInformation.trim()
    )
    if (currentMedicines != null) {
        payload["current_medicines"] = currentMedicines
    }
    return payload
}

private fun JSONObject.toKotlinMap(): Map<String, Any?> {
    return keys().asSequence().associateWith { key ->
        when (val value = get(key)) {
            is JSONObject -> value.toKotlinMap()
            is JSONArray -> value.toKotlinList()
            JSONObject.NULL -> null
            else -> value
        }
    }
}

private fun JSONArray.toKotlinList(): List<Any?> {
    return (0 until length()).map { index ->
        when (val value = get(index)) {
            is JSONObject -> value.toKotlinMap()
            is JSONArray -> value.toKotlinList()
            JSONObject.NULL -> null
            else -> value
        }
    }
}
