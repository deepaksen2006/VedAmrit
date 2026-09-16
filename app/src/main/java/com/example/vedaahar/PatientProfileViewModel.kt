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
    Weight
}

data class PatientProfileUiState(
    val fullName: String = "",
    val age: String = "",
    val gender: String = "",
    val height: String = "",
    val weight: String = "",
    val touchedFields: Set<PatientProfileField> = emptySet(),
    val submitAttempts: Int = 0
) {
    val errors: Map<PatientProfileField, String> = PatientProfileValidators.validate(this)
    val requiredFields: List<PatientProfileField> = listOf(
        PatientProfileField.FullName,
        PatientProfileField.Age,
        PatientProfileField.Gender,
        PatientProfileField.Height,
        PatientProfileField.Weight
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
        }
    }
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
            else -> value
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

        return errors
    }
}
