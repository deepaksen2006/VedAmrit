package com.example.vedaahar.assessment

import com.example.vedaahar.agentic.AgenticApiErrorType
import com.example.vedaahar.agentic.AgenticApiResult
import com.example.vedaahar.network.AgenticApi
import com.example.vedaahar.network.dto.AgenticPlanRequest
import com.example.vedaahar.network.dto.AssessmentResultResponse
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

class AssessmentRepositoryTest {
    @Test
    fun persistsCanonicalAssessmentWithoutChangingValues() = runTest {
        val api = FakeAgenticApi()
        val repository = AssessmentRepository(api, accessTokenProvider = { "jwt" })

        val result = repository.persistCanonicalAssessment(rawAssessment())

        assertTrue(result is AgenticApiResult.Success)
        assertEquals(1, api.persistCalls)
        val sent = api.lastRequest?.assessmentProfile.orEmpty()
        val prakriti = sent["prakriti_result"] as Map<*, *>
        val vikriti = sent["vikriti_result"] as Map<*, *>
        val agni = sent["agni_result"] as Map<*, *>
        assertEquals("Vata-Pitta", prakriti["prakritiType"])
        assertEquals("Pitta", vikriti["dominantDosha"])
        assertEquals("Samagni", agni["dominantAgni"])
    }

    @Test
    fun returnsAuthenticationRequiredWhenJwtMissing() = runTest {
        val api = FakeAgenticApi()
        val repository = AssessmentRepository(api, accessTokenProvider = { null })

        val result = repository.persistCanonicalAssessment(rawAssessment())

        assertTrue(result is AgenticApiResult.Error)
        assertEquals(AgenticApiErrorType.AuthenticationRequired, (result as AgenticApiResult.Error).type)
        assertEquals(0, api.persistCalls)
    }

    @Test
    fun returnsParseErrorForMalformedLocalAssessmentJson() = runTest {
        val api = FakeAgenticApi()
        val repository = AssessmentRepository(api, accessTokenProvider = { "jwt" })

        val result = repository.persistCanonicalAssessment("{bad")

        assertTrue(result is AgenticApiResult.Error)
        assertEquals(AgenticApiErrorType.Parse, (result as AgenticApiResult.Error).type)
        assertEquals(0, api.persistCalls)
    }

    @Test
    fun mapsBackendBadRequest() = runTest {
        val api = FakeAgenticApi(Response.error(400, "{}".toResponseBody(null)))
        val repository = AssessmentRepository(api, accessTokenProvider = { "jwt" })

        val result = repository.persistCanonicalAssessment(rawAssessment())

        assertTrue(result is AgenticApiResult.Error)
        assertEquals(AgenticApiErrorType.BadRequest, (result as AgenticApiResult.Error).type)
    }

    private fun rawAssessment() = """
        {
          "prakriti_result": {
            "scores": {"Vata": 18, "Pitta": 10, "Kapha": 4},
            "prakritiType": "Vata-Pitta",
            "primaryDosha": "Vata"
          },
          "vikriti_result": {
            "scores": {"Vata": 3, "Pitta": 19, "Kapha": 6},
            "dominantDosha": "Pitta"
          },
          "agni_result": {
            "scores": {"Mandagni": 2, "Samagni": 8},
            "dominantAgni": "Samagni"
          }
        }
    """.trimIndent()

    private class FakeAgenticApi(
        private val persistResponse: Response<AssessmentResultResponse> = Response.success(
            AssessmentResultResponse(
                id = 1L,
                prakritiResult = emptyMap(),
                vikritiResult = emptyMap(),
                agniResult = emptyMap(),
                canonicalPayload = emptyMap(),
                createdAt = null,
                updatedAt = null
            )
        )
    ) : AgenticApi {
        override suspend fun generationStatus(id: String): Response<com.example.vedaahar.network.dto.GenerationStatusResponse> = Response.error(404, "".toResponseBody())

        var persistCalls = 0
            private set
        var lastRequest: CanonicalAssessmentRequest? = null
            private set

        override suspend fun persistAssessment(request: CanonicalAssessmentRequest): Response<AssessmentResultResponse> {
            persistCalls += 1
            lastRequest = request
            return persistResponse
        }

        override suspend fun latestAssessment(): Response<AssessmentResultResponse> = throw NotImplementedError()
        override suspend fun generatePlan(request: AgenticPlanRequest): Response<WellnessPlanResponse> = throw NotImplementedError()
        override suspend fun plans(): Response<List<WellnessPlanResponse>> = throw NotImplementedError()
        override suspend fun plan(id: Long): Response<WellnessPlanResponse> = throw NotImplementedError()
        override suspend fun planStatus(id: Long): Response<WellnessPlanStatusResponse> = throw NotImplementedError()
        override suspend fun finalReport(id: Long): Response<FinalReportResponse> = throw NotImplementedError()
    }
}
