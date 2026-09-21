package com.example.vedaahar.agentic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vedaahar.network.ApiClient
import com.example.vedaahar.network.AuthTokenStore
import com.example.vedaahar.network.dto.WellnessPlanResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class AgenticPlanUiState {
    data object Idle : AgenticPlanUiState()
    data object Loading : AgenticPlanUiState()
    data class Success(val plan: WellnessPlanResponse) : AgenticPlanUiState()
    data class Error(val message: String, val type: AgenticApiErrorType? = null) : AgenticPlanUiState()
}

class AgenticPlanViewModel(
    application: Application,
    private val repository: AgenticRepository = AgenticRepository(
        api = ApiClient.agenticApi(application),
        accessTokenProvider = { AuthTokenStore(application).getAccessToken() }
    )
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow<AgenticPlanUiState>(AgenticPlanUiState.Idle)
    val uiState: StateFlow<AgenticPlanUiState> = _uiState

    fun generatePlan() {
        if (_uiState.value is AgenticPlanUiState.Loading) return

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
                viewModelScope.launch {
                    when (val result = repository.generatePlan(mapping.request)) {
                        is AgenticApiResult.Success -> _uiState.value = AgenticPlanUiState.Success(result.value)
                        is AgenticApiResult.Error -> _uiState.value = AgenticPlanUiState.Error(result.message, result.type)
                    }
                }
            }
        }
    }

    fun reset() {
        _uiState.update { AgenticPlanUiState.Idle }
    }
}
