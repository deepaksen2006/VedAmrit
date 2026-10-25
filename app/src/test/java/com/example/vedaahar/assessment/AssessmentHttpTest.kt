package com.example.vedaahar.assessment

import com.example.vedaahar.agentic.AgenticApiErrorType
import com.example.vedaahar.agentic.AgenticApiResult
import com.example.vedaahar.network.*
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64
import java.util.concurrent.TimeUnit

class AssessmentHttpTest {
    private val raw = """{"prakriti_result":{"scores":{"Vata":10},"prakritiType":"Vata"},"vikriti_result":{"scores":{"Pitta":8},"dominantDosha":"Pitta"},"agni_result":{"scores":{"Samagni":6},"dominantAgni":"Samagni"}}"""
    private fun token(id: Int) = "header.${Base64.getUrlEncoder().withoutPadding().encodeToString("{\"user_id\":$id}".toByteArray())}.signature"
    private class Tokens(var access: String) : TokenStorage {
        override fun getAccessToken() = access
        override fun getRefreshToken() = "test-refresh"
        override fun hasTokens() = true
        override fun saveTokens(accessToken: String, refreshToken: String?) { access = accessToken }
        override fun clear() { access = "" }
    }

    @Test fun canonicalPostUsesBearerAllResultsAndPersistentSubmissionId() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(201).setHeader("Content-Type", "application/json")
                .setBody("""{"id":7,"assessment_version":"${"a".repeat(64)}","prakriti_result":{},"vikriti_result":{},"agni_result":{}}"""))
            val tokens = Tokens(token(1))
            val api = ApiClient.createAuthenticatedApi(server.url("/").toString(), tokens, AgenticApi::class.java)
            val result = AssessmentRepository(api, tokens::getAccessToken).persistCanonicalAssessment(raw, "2136f3f4-47a7-4da6-9a86-017b62f74650")
            assertTrue(result is AgenticApiResult.Success)
            val request = requireNotNull(server.takeRequest(3, TimeUnit.SECONDS))
            assertEquals("/api/agentic/assessments/", request.path)
            assertEquals("Bearer ${token(1)}", request.getHeader("Authorization"))
            val body = JSONObject(request.body.readUtf8())
            assertEquals("2136f3f4-47a7-4da6-9a86-017b62f74650", body.getString("submission_id"))
            assertEquals(3, body.getJSONObject("assessment_profile").length())
        } finally { server.shutdown() }
    }

    @Test fun boundAssessmentClientNeverUsesAnotherPatientsJwt() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            val url = server.url("/").toString()
            val tokens = Tokens(token(1))
            val api = ApiClient.createAuthenticatedApi(url, tokens, AgenticApi::class.java, PatientSession.keyFor(tokens.access, url))
            tokens.access = token(2)
            val result = AssessmentRepository(api, tokens::getAccessToken).persistCanonicalAssessment(raw, "2136f3f4-47a7-4da6-9a86-017b62f74650") as AgenticApiResult.Error
            assertEquals(AgenticApiErrorType.Network, result.type)
            assertEquals(0, server.requestCount)
        } finally { server.shutdown() }
    }
}
