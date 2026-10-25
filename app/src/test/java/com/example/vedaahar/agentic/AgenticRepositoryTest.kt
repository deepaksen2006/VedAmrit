package com.example.vedaahar.agentic

import com.example.vedaahar.network.AgenticApi
import com.example.vedaahar.network.dto.AgenticPlanRequest
import com.example.vedaahar.network.dto.AssessmentResultResponse
import com.example.vedaahar.network.dto.CanonicalAssessmentDto
import com.example.vedaahar.network.dto.CanonicalAssessmentRequest
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

    @Test
    fun mapsReportBlockedAndFinalReportAvailable() = runTest {
        val blocked = AgenticRepository(
            FakeAgenticApi(Response.success(plan()), finalReportResponse = Response.error(409, "{}".toResponseBody(null))),
            accessTokenProvider = { "jwt" }
        ).finalReport(42L)
        val available = AgenticRepository(
            FakeAgenticApi(Response.success(plan()), finalReportResponse = Response.success(finalReport())),
            accessTokenProvider = { "jwt" }
        ).finalReport(42L)

        assertEquals(AgenticApiErrorType.ReportNotReady, (blocked as AgenticApiResult.Error).type)
        assertTrue(blocked.message.contains("not ready"))
        assertTrue(available is AgenticApiResult.Success)
        assertEquals("/api/agentic/plans/42/report/", (available as AgenticApiResult.Success).value.reportReference)
    }

    @Test
    fun refreshPlanUsesGetPlanAndDoesNotGenerate() = runTest {
        val api = FakeAgenticApi(Response.success(plan()))
        val repository = AgenticRepository(api, accessTokenProvider = { "jwt" })

        val result = repository.plan(42L)

        assertTrue(result is AgenticApiResult.Success)
        assertEquals(0, api.generateCalls)
        assertEquals(1, api.planCalls)
    }

    @Test fun generatedPlanMustMatchExactAssessmentIdAndVersion() = runTest {
        val requested = request.copy(assessmentId = 1, assessmentVersion = "a".repeat(64))
        val matching = plan().copy(assessmentVersion = requested.assessmentVersion)
        val success = AgenticRepository(FakeAgenticApi(Response.success(matching)), { "jwt" }).generatePlan(requested)
        val wrongId = AgenticRepository(FakeAgenticApi(Response.success(matching.copy(assessmentId = 2))), { "jwt" }).generatePlan(requested)
        val wrongVersion = AgenticRepository(FakeAgenticApi(Response.success(matching.copy(assessmentVersion = "b".repeat(64)))), { "jwt" }).generatePlan(requested)
        assertTrue(success is AgenticApiResult.Success)
        assertTrue(wrongId is AgenticApiResult.Error)
        assertTrue(wrongVersion is AgenticApiResult.Error)
    }

    @Test fun acceptedRunningResponseIsNotTreatedAsCompletedPlan() = runTest {
        val api = FakeAgenticApi(Response.success(202, plan()))
        val result = AgenticRepository(api) { "jwt" }.generatePlan(request)
        assertEquals(AgenticApiErrorType.GenerationRunning, (result as AgenticApiResult.Error).type)
    }

    @Test fun staleProfileErrorKeepsActionableBackendDetail() = runTest {
        val api = FakeAgenticApi(Response.error(409, """{"detail":"Reload/save your health profile."}""".toResponseBody()))
        val result = AgenticRepository(api) { "jwt" }.generatePlan(request) as AgenticApiResult.Error
        assertEquals(AgenticApiErrorType.BadRequest, result.type)
        assertEquals("Reload/save your health profile.", result.message)
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

    private fun finalReport() = FinalReportResponse(
        id = 7L,
        wellnessPlanId = 42L,
        reportReference = "/api/agentic/plans/42/report/",
        reportMetadata = mapOf("status" to "available"),
        finalizedAt = "2026-01-01T00:00:00Z",
        createdAt = "2026-01-01T00:00:00Z"
    )

    private class FakeAgenticApi(
        private val generateResponse: Response<WellnessPlanResponse>,
        private val finalReportResponse: Response<FinalReportResponse> = Response.success(
            FinalReportResponse(
                id = null,
                wellnessPlanId = null,
                reportReference = null,
                reportMetadata = null,
                finalizedAt = null,
                createdAt = null
            )
        )
    ) : AgenticApi {
        override suspend fun generationStatus(id: String): Response<com.example.vedaahar.network.dto.GenerationStatusResponse> = Response.error(404, "".toResponseBody())

        var generateCalls = 0
            private set
        var planCalls = 0
            private set

        override suspend fun persistAssessment(request: CanonicalAssessmentRequest): Response<AssessmentResultResponse> {
            return Response.success(
                AssessmentResultResponse(
                    id = 1L,
                    prakritiResult = emptyMap(),
                    vikritiResult = emptyMap(),
                    agniResult = emptyMap(),
                    canonicalPayload = request.assessmentProfile,
                    createdAt = null,
                    updatedAt = null
                )
            )
        }

        override suspend fun latestAssessment(): Response<AssessmentResultResponse> = throw NotImplementedError()

        override suspend fun generatePlan(request: AgenticPlanRequest): Response<WellnessPlanResponse> {
            generateCalls += 1
            return generateResponse
        }
        override suspend fun plans(): Response<List<WellnessPlanResponse>> = Response.success(emptyList())
        override suspend fun plan(id: Long): Response<WellnessPlanResponse> {
            planCalls += 1
            return generateResponse
        }
        override suspend fun planStatus(id: Long): Response<WellnessPlanStatusResponse> = throw NotImplementedError()
        override suspend fun finalReport(id: Long): Response<FinalReportResponse> = finalReportResponse
    }
}
