package com.example.vedaahar.agentic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vedaahar.network.dto.FinalReportResponse
import com.example.vedaahar.network.dto.WellnessPlanResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AgenticPlanUiState {
    data object Idle : AgenticPlanUiState()
    data object Loading : AgenticPlanUiState()
    data class Success(
        val plan: WellnessPlanResponse,
        val refreshing: Boolean = false,
        val report: FinalReportResponse? = null,
        val notice: String? = null
    ) : AgenticPlanUiState()
    data class Error(val message: String, val type: AgenticApiErrorType? = null) : AgenticPlanUiState()
}

class AgenticPlanViewModel(
    application: Application,
    private val repository: AgenticPlanGateway,
    initialLatestPlanId: Long? = AgenticPlanStore.latestPlanId(application)
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow<AgenticPlanUiState>(AgenticPlanUiState.Idle)
    val uiState: StateFlow<AgenticPlanUiState> = _uiState
    private val _latestPlanId = MutableStateFlow(initialLatestPlanId)
    val latestPlanId: StateFlow<Long?> = _latestPlanId
    private var requestInFlight = false

    fun generatePlan() {
        if (requestInFlight) return

        when (val mapping = AgenticPlanRequestMapper.fromStoredState(getApplication())) {
            is AgenticRequestMappingResult.MissingData -> {
                _uiState.value = AgenticPlanUiState.Error(
                    "Cannot generate plan yet. Missing: ${mapping.missingFields.joinToString(", ")}"
                )
            }
            is AgenticRequestMappingResult.MalformedData -> {
                _uiState.value = AgenticPlanUiState.Error(mapping.reason)
            }
            is AgenticRequestMappingResult.Success -> {
                _uiState.value = AgenticPlanUiState.Loading
                requestInFlight = true
                viewModelScope.launch {
                    when (val result = repository.generatePlan(mapping.request)) {
                        is AgenticApiResult.Success -> setCurrentPlan(result.value)
                        is AgenticApiResult.Error -> _uiState.value = AgenticPlanUiState.Error(result.message, result.type)
                    }
                    requestInFlight = false
                }
            }
        }
    }

    fun refreshCurrentPlan() {
        if (requestInFlight) return
        val planId = (_uiState.value as? AgenticPlanUiState.Success)?.plan?.id ?: _latestPlanId.value
        if (planId == null) {
            _uiState.value = AgenticPlanUiState.Error("No generated wellness plan has been saved yet.")
            return
        }

        val current = _uiState.value
        _uiState.value = if (current is AgenticPlanUiState.Success) {
            current.copy(refreshing = true, notice = null)
        } else {
            AgenticPlanUiState.Loading
        }
        requestInFlight = true
        viewModelScope.launch {
            when (val result = repository.plan(planId)) {
                is AgenticApiResult.Success -> setCurrentPlan(result.value)
                is AgenticApiResult.Error -> {
                    _uiState.value = if (current is AgenticPlanUiState.Success) {
                        current.copy(refreshing = false, notice = result.message)
                    } else {
                        AgenticPlanUiState.Error(result.message, result.type)
                    }
                }
            }
            requestInFlight = false
        }
    }

    fun loadFinalReport() {
        if (requestInFlight) return
        val current = _uiState.value as? AgenticPlanUiState.Success ?: return
        val planId = current.plan.id
        requestInFlight = true
        _uiState.value = current.copy(refreshing = true, notice = null)
        viewModelScope.launch {
            when (val result = repository.finalReport(planId)) {
                is AgenticApiResult.Success -> _uiState.value = current.copy(
                    refreshing = false,
                    report = result.value,
                    notice = "Final report is available."
                )
                is AgenticApiResult.Error -> _uiState.value = current.copy(
                    refreshing = false,
                    notice = result.message
                )
            }
            requestInFlight = false
        }
    }

    fun reset() {
        _uiState.value = AgenticPlanUiState.Idle
    }

    private fun setCurrentPlan(plan: WellnessPlanResponse) {
        AgenticPlanStore.saveLatestPlanId(getApplication(), plan.id)
        _latestPlanId.value = plan.id
        _uiState.value = AgenticPlanUiState.Success(plan)
    }

    class Factory(
        private val application: Application,
        private val repository: AgenticPlanGateway,
        private val latestPlanIdProvider: () -> Long? = { AgenticPlanStore.latestPlanId(application) }
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AgenticPlanViewModel::class.java)) {
                return AgenticPlanViewModel(application, repository, latestPlanIdProvider()) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
