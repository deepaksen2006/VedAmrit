package com.example.vedaahar.agentic

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vedaahar.network.dto.AgenticPlanRequest
import com.example.vedaahar.network.dto.FinalReportResponse
import com.example.vedaahar.network.dto.WellnessPlanResponse
import com.example.vedaahar.network.PatientSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AgenticPlanUiState {
    data object Idle : AgenticPlanUiState()
    data object Loading : AgenticPlanUiState()
    data class Pending(val message: String) : AgenticPlanUiState()
    data class Success(
        val plan: WellnessPlanResponse,
        val refreshing: Boolean = false,
        val report: FinalReportResponse? = null,
        val notice: String? = null
    ) : AgenticPlanUiState()
    data class Error(val message: String, val type: AgenticApiErrorType? = null) : AgenticPlanUiState()
}

// All durable state is scoped by authenticated patient and backend, as in Task 2.
interface PlanSession {
    fun owner(): String?
    fun request(): AgenticRequestMappingResult
    fun generation(signature: String): String?
    fun generationFor(signature: String): String
    fun clearGeneration()
    fun savePlan(id: Long)
}

class StoredPlanSession(private val context: Context) : PlanSession {
    override fun owner() = PatientSession.currentKey(context)
    override fun request() = AgenticPlanRequestMapper.fromStoredState(context)
    override fun generation(signature: String) = AgenticPlanStore.pendingGeneration(context, signature)
    override fun generationFor(signature: String) = AgenticPlanStore.generationFor(context, signature)
    override fun clearGeneration() = AgenticPlanStore.clearGeneration(context)
    override fun savePlan(id: Long) = AgenticPlanStore.saveLatestPlanId(context, id)
}

fun AgenticPlanRequest.inputSignature() = "$assessmentId:$assessmentVersion:$profileVersion:${planDays ?: 7}"
fun AgenticPlanRequest.matches(plan: WellnessPlanResponse) = plan.inputsCurrent && assessmentId == plan.assessmentId &&
    assessmentVersion == plan.assessmentVersion && profileVersion != null && profileVersion == plan.profileVersion

class AgenticPlanViewModel(
    application: Application,
    private val repository: AgenticPlanGateway,
    initialLatestPlanId: Long? = AgenticPlanStore.latestPlanId(application),
    private val session: PlanSession = StoredPlanSession(application)
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow<AgenticPlanUiState>(AgenticPlanUiState.Idle)
    val uiState: StateFlow<AgenticPlanUiState> = _uiState
    private val _latestPlanId = MutableStateFlow(initialLatestPlanId)
    val latestPlanId: StateFlow<Long?> = _latestPlanId
    private var requestInFlight = false

    fun generatePlan() = runOperation { request, owner ->
        val id = session.generationFor(request.inputSignature())
        when (val result = repository.generatePlan(request.copy(generationId = id))) {
            is AgenticApiResult.Success -> accept(result.value, request, owner)
            is AgenticApiResult.Error -> {
                if (result.type in setOf(AgenticApiErrorType.Timeout, AgenticApiErrorType.Network,
                        AgenticApiErrorType.GenerationRunning, AgenticApiErrorType.BackendFailure, AgenticApiErrorType.Parse)) {
                    recover(id, request, owner, result.message)
                } else fail(result)
            }
        }
    }

    fun restoreLatestPlan() = runOperation { request, owner ->
        val pending = session.generation(request.inputSignature())
        if (pending != null) {
            recover(pending, request, owner, "No completed generation found. Tap Generate to retry the saved attempt.")
        } else {
            when (val result = repository.plans()) {
                is AgenticApiResult.Success -> {
                    val latest = result.value.firstOrNull { request.matches(it) }
                    if (!current(request, owner)) changed()
                    else if (latest == null) _uiState.value = AgenticPlanUiState.Idle
                    else loadPlan(latest.id, request, owner)
                }
                is AgenticApiResult.Error -> fail(result)
            }
        }
    }

    fun refreshCurrentPlan() = restoreLatestPlan()

    private fun runOperation(block: suspend (AgenticPlanRequest, String?) -> Unit) {
        if (requestInFlight) return
        val owner = session.owner()
        val mapping = session.request()
        if (mapping !is AgenticRequestMappingResult.Success) {
            _uiState.value = AgenticPlanUiState.Error(when (mapping) {
                is AgenticRequestMappingResult.MissingData -> "Cannot load/generate plan. Missing: ${mapping.missingFields.joinToString()}."
                is AgenticRequestMappingResult.MalformedData -> mapping.reason
            })
            return
        }
        requestInFlight = true
        _uiState.value = AgenticPlanUiState.Loading
        viewModelScope.launch {
            try {
                block(mapping.request, owner)
                if (!current(mapping.request, owner)) changed()
            } catch (error: IllegalStateException) {
                _uiState.value = AgenticPlanUiState.Error(error.message ?: "Could not save the generation attempt.")
            } finally {
                requestInFlight = false
            }
        }
    }

    private suspend fun recover(id: String, request: AgenticPlanRequest, owner: String?, originalError: String) {
        when (val result = repository.generationStatus(id)) {
            is AgenticApiResult.Success -> {
                if (!current(request, owner)) { changed(); return }
                val status = result.value
                if (status.assessmentId != request.assessmentId || status.assessmentVersion != request.assessmentVersion ||
                    status.profileVersion != request.profileVersion) { changed(); return }
                when (status.status) {
                    "COMPLETED" -> if (status.planId != null) loadPlan(status.planId, request, owner)
                        else _uiState.value = AgenticPlanUiState.Error("Generation completed without a plan. Contact support.")
                    "FAILED" -> {
                        session.clearGeneration()
                        _uiState.value = AgenticPlanUiState.Error(status.message ?: "Generation failed. Review inputs and retry.")
                    }
                    "RUNNING", "PENDING" -> _uiState.value = AgenticPlanUiState.Pending("Generation is still running. Tap Refresh to check status; reopening the app will recover this attempt.")
                    else -> _uiState.value = AgenticPlanUiState.Error("The server returned an unknown generation status. Update the app/backend and retry Refresh.")
                }
            }
            is AgenticApiResult.Error -> fail(if (result.type == AgenticApiErrorType.NotFound)
                AgenticApiResult.Error(result.type, originalError) else result)
        }
    }

    private suspend fun loadPlan(id: Long, request: AgenticPlanRequest, owner: String?) {
        when (val result = repository.plan(id)) {
            is AgenticApiResult.Success -> accept(result.value, request, owner)
            is AgenticApiResult.Error -> fail(result)
        }
    }

    private fun current(request: AgenticPlanRequest, owner: String?): Boolean {
        val now = session.request() as? AgenticRequestMappingResult.Success
        return owner != null && owner == session.owner() && now?.request?.inputSignature() == request.inputSignature()
    }

    private fun accept(plan: WellnessPlanResponse, request: AgenticPlanRequest, owner: String?) {
        if (!current(request, owner) || !request.matches(plan)) { changed(); return }
        session.savePlan(plan.id)
        _latestPlanId.value = plan.id
        _uiState.value = AgenticPlanUiState.Success(plan)
    }

    private fun changed() {
        _latestPlanId.value = null
        _uiState.value = if (session.owner() == null) AgenticPlanUiState.Error(
            "Your Django session has expired. Sign in again to recover your saved generation attempt.",
            AgenticApiErrorType.AuthenticationExpired
        ) else AgenticPlanUiState.Error("Patient, health profile or assessment changed. Reload/save your profile, sync assessments and generate again.")
    }
    private fun fail(error: AgenticApiResult.Error) {
        _uiState.value = AgenticPlanUiState.Error(error.message, error.type)
    }

    fun loadFinalReport() {
        if (requestInFlight) return
        val current = _uiState.value as? AgenticPlanUiState.Success ?: return
        val owner = session.owner()
        requestInFlight = true
        _uiState.value = current.copy(refreshing = true)
        viewModelScope.launch {
            try {
                val result = repository.finalReport(current.plan.id)
                if (owner != session.owner()) { changed(); return@launch }
                _uiState.value = when (result) {
                    is AgenticApiResult.Success -> current.copy(report = result.value, notice = "Final report is available.")
                    is AgenticApiResult.Error -> current.copy(notice = result.message)
                }
            } finally { requestInFlight = false }
        }
    }

    fun reset() { _uiState.value = AgenticPlanUiState.Idle }

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
