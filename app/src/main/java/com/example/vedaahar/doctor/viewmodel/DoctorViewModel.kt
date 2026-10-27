package com.example.vedaahar.doctor.viewmodel

import androidx.lifecycle.ViewModel
import com.example.vedaahar.doctor.data.DoctorRepository
import com.example.vedaahar.doctor.domain.DoctorConsultation
import com.example.vedaahar.doctor.domain.DoctorDietPlanDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DoctorUiState(
    val isLoggedIn: Boolean = false,
    val isVerified: Boolean = false,
    val isOnline: Boolean = false,
    val fullName: String = "Doctor account",
    val specialization: String = "",
    val clinicName: String = "",
    val availability: String = "",
    val consultations: List<DoctorConsultation> = emptyList(),
    val dietPlans: List<DoctorDietPlanDraft> = emptyList()
)

class DoctorViewModel(
    private val repository: DoctorRepository = DoctorRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        DoctorUiState(
            consultations = repository.consultations(),
            dietPlans = repository.recentDietPlans()
        )
    )
    val uiState: StateFlow<DoctorUiState> = _uiState.asStateFlow()

    fun login() {
        _uiState.value = _uiState.value.copy(isLoggedIn = true)
    }

    fun register() {
        _uiState.value = _uiState.value.copy(isLoggedIn = true, isVerified = false)
    }

    fun toggleOnline() {
        _uiState.value = _uiState.value.copy(isOnline = !_uiState.value.isOnline)
    }

    fun updateAvailability(value: String) {
        _uiState.value = _uiState.value.copy(availability = value)
    }
}
