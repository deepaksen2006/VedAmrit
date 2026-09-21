package com.example.vedaahar.profile

import android.content.Context
import com.example.vedaahar.PatientProfileUiState
import org.json.JSONArray

object PatientProfileStore {
    private const val PrefName = "vedaahar_patient_profile"
    private const val FullName = "full_name"
    private const val Age = "age"
    private const val Gender = "gender"
    private const val HeightCm = "height_cm"
    private const val WeightKg = "weight_kg"
    private const val DietaryPreference = "dietary_preference"
    private const val Goal = "goal"
    private const val ActivityLevel = "activity_level"
    private const val Allergies = "allergies"
    private const val FoodIntolerances = "food_intolerances"
    private const val FoodsToAvoid = "foods_to_avoid"
    private const val HealthConditions = "health_conditions"
    private const val MedicineAllergies = "medicine_allergies"
    private const val CurrentMedicines = "current_medicines"
    private const val MedicationRestrictions = "medication_restrictions"
    private const val DoctorRestrictions = "doctor_restrictions"
    private const val PregnancyInformation = "pregnancy_information"

    fun save(context: Context, state: PatientProfileUiState) {
        context.getSharedPreferences(PrefName, Context.MODE_PRIVATE)
            .edit()
            .putString(FullName, state.fullName.trim())
            .putString(Age, state.age.trim())
            .putString(Gender, state.gender.trim())
            .putString(HeightCm, state.height.trim())
            .putString(WeightKg, state.weight.trim())
            .putString(DietaryPreference, state.dietaryPreference.trim())
            .putString(Goal, state.goal.trim())
            .putString(ActivityLevel, state.activityLevel.trim())
            .putString(Allergies, state.allergies.toJsonArrayString())
            .putString(FoodIntolerances, state.foodIntolerances.toJsonArrayString())
            .putString(FoodsToAvoid, state.foodsToAvoid.toJsonArrayString())
            .putString(HealthConditions, state.healthConditions.toJsonArrayString())
            .putString(MedicineAllergies, state.medicineAllergies.toJsonArrayString())
            .putString(CurrentMedicines, state.currentMedicines.toJsonArrayString())
            .putString(MedicationRestrictions, state.medicationRestrictions.toJsonArrayString())
            .putString(DoctorRestrictions, state.doctorRestrictions.toJsonArrayString())
            .putString(PregnancyInformation, state.pregnancyInformation.trim())
            .apply()
    }

    fun current(context: Context): StoredPatientProfile {
        val preferences = context.getSharedPreferences(PrefName, Context.MODE_PRIVATE)
        return StoredPatientProfile(
            fullName = preferences.getString(FullName, "").orEmpty(),
            age = preferences.getString(Age, "").orEmpty(),
            gender = preferences.getString(Gender, "").orEmpty(),
            heightCm = preferences.getString(HeightCm, "").orEmpty(),
            weightKg = preferences.getString(WeightKg, "").orEmpty(),
            dietaryPreference = preferences.getString(DietaryPreference, "").orEmpty(),
            goal = preferences.getString(Goal, "").orEmpty(),
            activityLevel = preferences.getString(ActivityLevel, "").orEmpty(),
            allergies = preferences.getStoredList(Allergies),
            foodIntolerances = preferences.getStoredList(FoodIntolerances),
            foodsToAvoid = preferences.getStoredList(FoodsToAvoid),
            healthConditions = preferences.getStoredList(HealthConditions),
            medicineAllergies = preferences.getStoredList(MedicineAllergies),
            currentMedicines = preferences.getStoredList(CurrentMedicines),
            medicationRestrictions = preferences.getStoredList(MedicationRestrictions),
            doctorRestrictions = preferences.getStoredList(DoctorRestrictions),
            pregnancyInformation = preferences.getString(PregnancyInformation, "").orEmpty()
        )
    }

    private fun List<String>.toJsonArrayString(): String {
        val array = JSONArray()
        forEach { item ->
            val cleaned = item.trim()
            if (cleaned.isNotBlank()) array.put(cleaned)
        }
        return array.toString()
    }

    private fun android.content.SharedPreferences.getStoredList(key: String): List<String>? {
        if (!contains(key)) return null
        val raw = getString(key, "[]").orEmpty()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length())
                .mapNotNull { index -> array.optString(index).trim().takeIf(String::isNotBlank) }
        }.getOrDefault(emptyList())
    }
}

data class StoredPatientProfile(
    val fullName: String,
    val age: String,
    val gender: String,
    val heightCm: String,
    val weightKg: String,
    val dietaryPreference: String = "",
    val goal: String = "",
    val activityLevel: String = "",
    val allergies: List<String>? = null,
    val foodIntolerances: List<String>? = null,
    val foodsToAvoid: List<String>? = null,
    val healthConditions: List<String>? = null,
    val medicineAllergies: List<String>? = null,
    val currentMedicines: List<String>? = null,
    val medicationRestrictions: List<String>? = null,
    val doctorRestrictions: List<String>? = null,
    val pregnancyInformation: String = ""
)
