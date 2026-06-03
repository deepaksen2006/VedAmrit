package com.example.vedaahar.doctor.viewmodel

import androidx.lifecycle.ViewModel
import com.example.vedaahar.doctor.data.DoctorRepository
import com.example.vedaahar.doctor.domain.DoctorConsultation
import com.example.vedaahar.doctor.domain.DoctorDietPlanDraft
import com.example.vedaahar.doctor.domain.VerifiedDoctor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DoctorUiState(
    val isLoggedIn: Boolean = false,
    val isVerified: Boolean = true,
    val isOnline: Boolean = true,
    val fullName: String = "Dr. Meera Sharma",
    val specialization: String = "BAMS - Digestive Health",
    val clinicName: String = "Prakriti Ayurveda Clinic",
    val availability: String = "10:00 AM - 6:00 PM",
    val verifiedDoctors: List<VerifiedDoctor> = emptyList(),
    val consultations: List<DoctorConsultation> = emptyList(),
    val dietPlans: List<DoctorDietPlanDraft> = emptyList()
)

class DoctorViewModel(
    private val repository: DoctorRepository = DoctorRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        DoctorUiState(
            verifiedDoctors = repository.verifiedDoctors(),
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
