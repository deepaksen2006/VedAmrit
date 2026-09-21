package com.example.vedaahar

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

enum class PatientProfileField {
    FullName,
    Age,
    Gender,
    Height,
    Weight,
    DietaryPreference,
    Goal,
    ActivityLevel,
    Allergies,
    FoodIntolerances,
    FoodsToAvoid,
    HealthConditions,
    MedicineAllergies,
    CurrentMedicines,
    MedicationRestrictions,
    DoctorRestrictions,
    PregnancyInformation
}

data class PatientProfileUiState(
    val fullName: String = "",
    val age: String = "",
    val gender: String = "",
    val height: String = "",
    val weight: String = "",
    val dietaryPreference: String = "",
    val goal: String = "",
    val activityLevel: String = "",
    val allergies: List<String> = emptyList(),
    val foodIntolerances: List<String> = emptyList(),
    val foodsToAvoid: List<String> = emptyList(),
    val healthConditions: List<String> = emptyList(),
    val medicineAllergies: List<String> = emptyList(),
    val currentMedicines: List<String> = emptyList(),
    val medicationRestrictions: List<String> = emptyList(),
    val doctorRestrictions: List<String> = emptyList(),
    val pregnancyInformation: String = "",
    val touchedFields: Set<PatientProfileField> = emptySet(),
    val submitAttempts: Int = 0
) {
    val errors: Map<PatientProfileField, String> = PatientProfileValidators.validate(this)
    val requiredFields: List<PatientProfileField> = listOf(
        PatientProfileField.FullName,
        PatientProfileField.Age,
        PatientProfileField.Gender,
        PatientProfileField.Height,
        PatientProfileField.Weight,
        PatientProfileField.DietaryPreference,
        PatientProfileField.Goal,
        PatientProfileField.ActivityLevel,
        PatientProfileField.PregnancyInformation
    )
    val isFormValid: Boolean = requiredFields.none { errors.containsKey(it) }
    val firstInvalidField: PatientProfileField? = requiredFields.firstOrNull { errors.containsKey(it) }

    fun shouldShowError(field: PatientProfileField): Boolean {
        return errors[field] != null && (submitAttempts > 0 || touchedFields.contains(field))
    }

    fun isValid(field: PatientProfileField): Boolean {
        return !errors.containsKey(field) && touchedFields.contains(field) && valueFor(field).isNotBlank()
    }

    fun valueFor(field: PatientProfileField): String {
        return when (field) {
            PatientProfileField.FullName -> fullName
            PatientProfileField.Age -> age
            PatientProfileField.Gender -> gender
            PatientProfileField.Height -> height
            PatientProfileField.Weight -> weight
            PatientProfileField.DietaryPreference -> dietaryPreference
            PatientProfileField.Goal -> goal
            PatientProfileField.ActivityLevel -> activityLevel
            PatientProfileField.Allergies -> allergies.joinToString()
            PatientProfileField.FoodIntolerances -> foodIntolerances.joinToString()
            PatientProfileField.FoodsToAvoid -> foodsToAvoid.joinToString()
            PatientProfileField.HealthConditions -> healthConditions.joinToString()
            PatientProfileField.MedicineAllergies -> medicineAllergies.joinToString()
            PatientProfileField.CurrentMedicines -> currentMedicines.joinToString()
            PatientProfileField.MedicationRestrictions -> medicationRestrictions.joinToString()
            PatientProfileField.DoctorRestrictions -> doctorRestrictions.joinToString()
            PatientProfileField.PregnancyInformation -> pregnancyInformation
        }
    }
}

object PatientProfileOptions {
    val DietaryPreferences = listOf("vegetarian", "vegan", "non-vegetarian")
    val Goals = listOf("general wellness", "weight loss", "weight gain", "weight management", "digestion")
    val ActivityLevels = listOf("sedentary", "low", "moderate", "high")
    val PregnancyInformation = listOf("not_applicable", "not_pregnant", "pregnant", "prefer_not_to_say")
}

class PatientProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PatientProfileUiState())
    val uiState: StateFlow<PatientProfileUiState> = _uiState

    fun updateField(field: PatientProfileField, value: String) {
        _uiState.update { state ->
            val sanitized = sanitize(field, value)
            val touched = state.touchedFields + field
            when (field) {
                PatientProfileField.FullName -> state.copy(fullName = sanitized, touchedFields = touched)
                PatientProfileField.Age -> state.copy(age = sanitized, touchedFields = touched)
                PatientProfileField.Gender -> state.copy(gender = sanitized, touchedFields = touched)
                PatientProfileField.Height -> state.copy(height = sanitized, touchedFields = touched)
                PatientProfileField.Weight -> state.copy(weight = sanitized, touchedFields = touched)
                PatientProfileField.DietaryPreference -> state.copy(dietaryPreference = sanitized, touchedFields = touched)
                PatientProfileField.Goal -> state.copy(goal = sanitized, touchedFields = touched)
                PatientProfileField.ActivityLevel -> state.copy(activityLevel = sanitized, touchedFields = touched)
                PatientProfileField.PregnancyInformation -> state.copy(pregnancyInformation = sanitized, touchedFields = touched)
                PatientProfileField.Allergies,
                PatientProfileField.FoodIntolerances,
                PatientProfileField.FoodsToAvoid,
                PatientProfileField.HealthConditions,
                PatientProfileField.MedicineAllergies,
                PatientProfileField.CurrentMedicines,
                PatientProfileField.MedicationRestrictions,
                PatientProfileField.DoctorRestrictions -> state.copy(touchedFields = touched)
            }
        }
    }

    fun addListValue(field: PatientProfileField, value: String) {
        val sanitized = value.trim()
        if (sanitized.isBlank()) return

        _uiState.update { state ->
            val touched = state.touchedFields + field
            when (field) {
                PatientProfileField.Allergies -> state.copy(allergies = addUnique(state.allergies, sanitized), touchedFields = touched)
                PatientProfileField.FoodIntolerances -> state.copy(foodIntolerances = addUnique(state.foodIntolerances, sanitized), touchedFields = touched)
                PatientProfileField.FoodsToAvoid -> state.copy(foodsToAvoid = addUnique(state.foodsToAvoid, sanitized), touchedFields = touched)
                PatientProfileField.HealthConditions -> state.copy(healthConditions = addUnique(state.healthConditions, sanitized), touchedFields = touched)
                PatientProfileField.MedicineAllergies -> state.copy(medicineAllergies = addUnique(state.medicineAllergies, sanitized), touchedFields = touched)
                PatientProfileField.CurrentMedicines -> state.copy(currentMedicines = addUnique(state.currentMedicines, sanitized), touchedFields = touched)
                PatientProfileField.MedicationRestrictions -> state.copy(medicationRestrictions = addUnique(state.medicationRestrictions, sanitized), touchedFields = touched)
                PatientProfileField.DoctorRestrictions -> state.copy(doctorRestrictions = addUnique(state.doctorRestrictions, sanitized), touchedFields = touched)
                else -> state
            }
        }
    }

    fun removeListValue(field: PatientProfileField, value: String) {
        _uiState.update { state ->
            val touched = state.touchedFields + field
            when (field) {
                PatientProfileField.Allergies -> state.copy(allergies = state.allergies - value, touchedFields = touched)
                PatientProfileField.FoodIntolerances -> state.copy(foodIntolerances = state.foodIntolerances - value, touchedFields = touched)
                PatientProfileField.FoodsToAvoid -> state.copy(foodsToAvoid = state.foodsToAvoid - value, touchedFields = touched)
                PatientProfileField.HealthConditions -> state.copy(healthConditions = state.healthConditions - value, touchedFields = touched)
                PatientProfileField.MedicineAllergies -> state.copy(medicineAllergies = state.medicineAllergies - value, touchedFields = touched)
                PatientProfileField.CurrentMedicines -> state.copy(currentMedicines = state.currentMedicines - value, touchedFields = touched)
                PatientProfileField.MedicationRestrictions -> state.copy(medicationRestrictions = state.medicationRestrictions - value, touchedFields = touched)
                PatientProfileField.DoctorRestrictions -> state.copy(doctorRestrictions = state.doctorRestrictions - value, touchedFields = touched)
                else -> state
            }
        }
    }

    fun submit(): Boolean {
        var valid = false
        _uiState.update { state ->
            val submitted = state.copy(
                submitAttempts = state.submitAttempts + 1,
                touchedFields = state.touchedFields + state.requiredFields
            )
            valid = submitted.isFormValid
            submitted
        }
        return valid
    }

    private fun sanitize(field: PatientProfileField, value: String): String {
        return when (field) {
            PatientProfileField.Age -> value.filter(Char::isDigit)
            PatientProfileField.Height,
            PatientProfileField.Weight -> value.filter { it.isDigit() || it == '.' }.take(6)
            PatientProfileField.DietaryPreference,
            PatientProfileField.Goal,
            PatientProfileField.ActivityLevel,
            PatientProfileField.PregnancyInformation -> value.trim()
            else -> value
        }
    }

    private fun addUnique(existing: List<String>, value: String): List<String> {
        return if (existing.any { it.equals(value, ignoreCase = true) }) {
            existing
        } else {
            existing + value
        }
    }
}

object PatientProfileValidators {
    private val nameRegex = Regex("^[A-Za-z ]+$")

    fun validate(state: PatientProfileUiState): Map<PatientProfileField, String> {
        val errors = mutableMapOf<PatientProfileField, String>()

        if (state.fullName.trim().length < 3 || !nameRegex.matches(state.fullName.trim())) {
            errors[PatientProfileField.FullName] = "Enter a valid full name"
        }
        if (state.age.toIntOrNull()?.let { it in 1..120 } != true) {
            errors[PatientProfileField.Age] = "Enter a valid age"
        }
        if (state.gender !in listOf("Male", "Female", "Other")) {
            errors[PatientProfileField.Gender] = "Select a gender"
        }
        if (state.height.toFloatOrNull()?.let { it in 50f..250f } != true) {
            errors[PatientProfileField.Height] = "Enter valid height in cm"
        }
        if (state.weight.toFloatOrNull()?.let { it in 10f..300f } != true) {
            errors[PatientProfileField.Weight] = "Enter valid weight"
        }
        if (state.dietaryPreference !in PatientProfileOptions.DietaryPreferences) {
            errors[PatientProfileField.DietaryPreference] = "Select a dietary preference"
        }
        if (state.goal !in PatientProfileOptions.Goals) {
            errors[PatientProfileField.Goal] = "Select a wellness goal"
        }
        if (state.activityLevel !in PatientProfileOptions.ActivityLevels) {
            errors[PatientProfileField.ActivityLevel] = "Select an activity level"
        }
        if (state.pregnancyInformation !in PatientProfileOptions.PregnancyInformation) {
            errors[PatientProfileField.PregnancyInformation] = "Select pregnancy information"
        }

        return errors
    }
}
