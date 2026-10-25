package com.example.vedaahar.agentic

import android.app.Application
import com.example.vedaahar.network.dto.*
import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class AgenticPlanViewModelTest {
    private val request = AgenticPlanRequest(emptyMap(), CanonicalAssessmentDto(emptyMap(), emptyMap(), emptyMap()),
        planDays = 7, assessmentId = 7, assessmentVersion = "a".repeat(64), profileVersion = "b".repeat(64))
    private fun plan() = Gson().fromJson("""{"id":42,"version":1,"inputs_current":true,"assessment_id":7,"assessment_version":"${"a".repeat(64)}","profile_version":"${"b".repeat(64)}","doctor_status":"PENDING"}""", WellnessPlanResponse::class.java)
    private val session = FakeSession()
    private val gateway = FakeGateway()
    private fun vm() = AgenticPlanViewModel(Application(), gateway, null, session)
    @Before fun before() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun after() { Dispatchers.resetMain() }

    @Test fun successfulGenerationSavesExactPlanAndDurableAttempt() = runTest {
        val vm = vm()
        vm.generatePlan()
        assertEquals(42L, session.saved)
        assertEquals("attempt-1", gateway.requests.single().generationId)
        assertTrue(vm.uiState.value is AgenticPlanUiState.Success)
    }

    @Test fun repeatedTapsDoNotStartParallelRequests() = runTest {
        val wait = CompletableDeferred<Unit>()
        gateway.generate = { wait.await(); AgenticApiResult.Success(plan()) }
        val vm = vm()
        vm.generatePlan(); vm.generatePlan()
        assertEquals(1, gateway.requests.size)
        wait.complete(Unit)
        assertEquals(42L, session.saved)
    }

    @Test fun timeoutRecoversCommittedPlanWithoutAnotherPost() = runTest {
        gateway.generate = { AgenticApiResult.Error(AgenticApiErrorType.Timeout, "timeout") }
        gateway.status = AgenticApiResult.Success(status("COMPLETED", 42))
        val vm = vm(); vm.generatePlan()
        assertTrue(vm.uiState.value is AgenticPlanUiState.Success)
        assertEquals(1, gateway.requests.size)
        assertEquals(42L, session.saved)
    }

    @Test fun pendingAttemptSurvivesViewModelRecreation() = runTest {
        gateway.generate = { AgenticApiResult.Error(AgenticApiErrorType.GenerationRunning, "running") }
        gateway.status = AgenticApiResult.Success(status("RUNNING", null))
        val first = vm(); first.generatePlan()
        assertTrue(first.uiState.value is AgenticPlanUiState.Pending)
        gateway.status = AgenticApiResult.Success(status("COMPLETED", 42))
        val reopened = vm(); reopened.restoreLatestPlan()
        assertTrue(reopened.uiState.value is AgenticPlanUiState.Success)
        assertEquals(1, gateway.requests.size)
    }

    @Test fun offlineRetryReusesSameAttemptId() = runTest {
        gateway.generate = { AgenticApiResult.Error(AgenticApiErrorType.Network, "offline") }
        vm().generatePlan()
        gateway.generate = { AgenticApiResult.Success(plan()) }
        val reopened = vm(); reopened.generatePlan()
        assertEquals(listOf("attempt-1", "attempt-1"), gateway.requests.map { it.generationId })
    }

    @Test fun expiredJwtRemainsVisibleAndDoesNotLoseAttempt() = runTest {
        gateway.generate = { AgenticApiResult.Error(AgenticApiErrorType.AuthenticationExpired, "Sign in again") }
        val vm = vm(); vm.generatePlan()
        assertEquals(AgenticApiErrorType.AuthenticationExpired, (vm.uiState.value as AgenticPlanUiState.Error).type)
        assertNotNull(session.attempt)
        assertNull(session.saved)
    }

    @Test fun expiredRefreshClearsSessionAndStillPromptsSignIn() = runTest {
        gateway.generate = { session.patient = null; AgenticApiResult.Error(AgenticApiErrorType.AuthenticationExpired, "expired") }
        val vm = vm(); vm.generatePlan()
        val error = vm.uiState.value as AgenticPlanUiState.Error
        assertEquals(AgenticApiErrorType.AuthenticationExpired, error.type)
        assertTrue(error.message.contains("Sign in again"))
        assertNull(session.saved)
    }

    @Test fun accountSwitchDuringResponseDoesNotDisplayOrSaveOtherPatientPlan() = runTest {
        gateway.generate = { session.patient = "other"; AgenticApiResult.Success(plan()) }
        val vm = vm(); vm.generatePlan()
        assertTrue(vm.uiState.value is AgenticPlanUiState.Error)
        assertNull(session.saved)
    }

    @Test fun profileOrAssessmentChangeDuringResponseRejectsStalePlan() = runTest {
        gateway.generate = { session.input = request.copy(profileVersion = "c".repeat(64)); AgenticApiResult.Success(plan()) }
        val vm = vm(); vm.generatePlan()
        assertTrue(vm.uiState.value is AgenticPlanUiState.Error)
        assertNull(session.saved)
    }

    @Test fun restoreFiltersServerListByExactAssessmentAndProfileVersion() = runTest {
        gateway.list = listOf(plan().copy(id = 99, profileVersion = "stale"), plan())
        val vm = vm(); vm.restoreLatestPlan()
        assertEquals(42L, session.saved)
        assertTrue(gateway.requests.isEmpty())
    }

    @Test fun serverMarksPlanStaleEvenIfLocalVersionHasNotChanged() = runTest {
        gateway.generate = { AgenticApiResult.Success(plan().copy(inputsCurrent = false)) }
        val vm = vm(); vm.generatePlan()
        assertTrue(vm.uiState.value is AgenticPlanUiState.Error)
        assertNull(session.saved)
    }

    @Test fun acknowledgedAttemptDoesNotMaskNewerServerPlanOnReopen() = runTest {
        vm().generatePlan()
        gateway.list = listOf(plan().copy(id = 55), plan())
        vm().restoreLatestPlan()
        assertEquals(55L, session.saved)
        assertEquals(1, gateway.requests.size)
    }

    @Test fun failedGenerationRequiresExplicitNewAttempt() = runTest {
        gateway.generate = { AgenticApiResult.Error(AgenticApiErrorType.BackendFailure, "failed") }
        gateway.status = AgenticApiResult.Success(status("FAILED", null))
        val vm = vm(); vm.generatePlan()
        assertNull(session.attempt)
        assertTrue(vm.uiState.value is AgenticPlanUiState.Error)
    }

    private fun status(state: String, id: Long?) = GenerationStatusResponse("attempt-1", state, id,
        request.assessmentId, request.assessmentVersion, request.profileVersion, "Generation failed")
    private inner class FakeSession : PlanSession {
        var patient: String? = "patient"
        var input = request
        var attempt: String? = null
        var saved: Long? = null
        var completed = false
        override fun owner() = patient
        override fun request() = AgenticRequestMappingResult.Success(input)
        override fun generation(signature: String) = attempt.takeUnless { completed }
        override fun generationFor(signature: String): String { attempt = attempt ?: "attempt-1"; return attempt!! }
        override fun clearGeneration() { attempt = null }
        override fun savePlan(id: Long) { saved = id; completed = true }
    }
    private inner class FakeGateway : AgenticPlanGateway {
        val requests = mutableListOf<AgenticPlanRequest>()
        var generate: suspend () -> AgenticApiResult<WellnessPlanResponse> = { AgenticApiResult.Success(plan()) }
        var status: AgenticApiResult<GenerationStatusResponse> = AgenticApiResult.Error(AgenticApiErrorType.NotFound, "not found")
        var list = listOf<WellnessPlanResponse>()
        override suspend fun generatePlan(request: AgenticPlanRequest): AgenticApiResult<WellnessPlanResponse> { requests += request; return generate() }
        override suspend fun generationStatus(id: String) = status
        override suspend fun plans() = AgenticApiResult.Success(list)
        override suspend fun plan(id: Long) = AgenticApiResult.Success(plan().copy(id = id))
        override suspend fun planStatus(id: Long): AgenticApiResult<WellnessPlanStatusResponse> = error("unused")
        override suspend fun finalReport(id: Long): AgenticApiResult<FinalReportResponse> = error("unused")
    }
}
