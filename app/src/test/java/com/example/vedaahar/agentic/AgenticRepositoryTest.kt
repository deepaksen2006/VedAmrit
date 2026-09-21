package com.example.vedaahar.agentic

import com.example.vedaahar.network.AgenticApi
import com.example.vedaahar.network.dto.AgenticPlanRequest
import com.example.vedaahar.network.dto.CanonicalAssessmentDto
import com.example.vedaahar.network.dto.FinalReportResponse
import com.example.vedaahar.network.dto.WellnessPlanResponse
import com.example.vedaahar.network.dto.WellnessPlanStatusResponse
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class AgenticRepositoryTest {
    private val request = AgenticPlanRequest(
        patientProfile = mapOf("fullName" to "Anaya"),
        assessment = CanonicalAssessmentDto(emptyMap(), emptyMap(), emptyMap())
    )

    @Test
    fun returnsAuthenticationRequiredWhenTokenMissing() = runTest {
        val repository = AgenticRepository(FakeAgenticApi(Response.success(plan())), accessTokenProvider = { null })

        val result = repository.generatePlan(request)

        assertTrue(result is AgenticApiResult.Error)
        assertEquals(AgenticApiErrorType.AuthenticationRequired, (result as AgenticApiResult.Error).type)
    }

    @Test
    fun parsesSuccessfulGeneratePlanResponse() = runTest {
        val repository = AgenticRepository(FakeAgenticApi(Response.success(plan())), accessTokenProvider = { "jwt" })

        val result = repository.generatePlan(request)

        assertTrue(result is AgenticApiResult.Success)
        assertEquals(42L, (result as AgenticApiResult.Success).value.id)
    }

    @Test
    fun mapsBadRequestUnauthorizedAndServerFailure() = runTest {
        val badRequest = AgenticRepository(FakeAgenticApi(error(400)), accessTokenProvider = { "jwt" }).generatePlan(request)
        val unauthorized = AgenticRepository(FakeAgenticApi(error(401)), accessTokenProvider = { "jwt" }).generatePlan(request)
        val server = AgenticRepository(FakeAgenticApi(error(500)), accessTokenProvider = { "jwt" }).generatePlan(request)

        assertEquals(AgenticApiErrorType.BadRequest, (badRequest as AgenticApiResult.Error).type)
        assertEquals(AgenticApiErrorType.AuthenticationExpired, (unauthorized as AgenticApiResult.Error).type)
        assertEquals(AgenticApiErrorType.BackendFailure, (server as AgenticApiResult.Error).type)
    }

    private fun error(code: Int): Response<WellnessPlanResponse> {
        return Response.error(code, "{}".toResponseBody(null))
    }

    private fun plan() = WellnessPlanResponse(
        id = 42L,
        assessmentId = 1L,
        agentRunId = 2L,
        lineageId = "lineage",
        parentPlanId = null,
        version = 1,
        assessmentSummary = mapOf("source" to "android_canonical_results"),
        dietPlan = mapOf("duration_days" to 7),
        lifestylePlan = mapOf("recommendations" to emptyList<Any>()),
        validation = mapOf("status" to "PASS"),
        validationStatus = "PASS",
        validationWarnings = emptyList(),
        ragStatus = mapOf("status" to "available"),
        doctorStatus = "PENDING",
        workflowStatus = "PENDING_DOCTOR_REVIEW",
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z"
    )

    private class FakeAgenticApi(
        private val generateResponse: Response<WellnessPlanResponse>
    ) : AgenticApi {
        override suspend fun generatePlan(request: AgenticPlanRequest): Response<WellnessPlanResponse> = generateResponse
        override suspend fun plans(): Response<List<WellnessPlanResponse>> = Response.success(emptyList())
        override suspend fun plan(id: Long): Response<WellnessPlanResponse> = generateResponse
        override suspend fun planStatus(id: Long): Response<WellnessPlanStatusResponse> = throw NotImplementedError()
        override suspend fun finalReport(id: Long): Response<FinalReportResponse> = throw NotImplementedError()
    }
}
