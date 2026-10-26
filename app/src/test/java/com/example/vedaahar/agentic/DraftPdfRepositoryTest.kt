package com.example.vedaahar.agentic

import com.example.vedaahar.network.ApiClient
import com.example.vedaahar.network.DraftPdfApi
import com.example.vedaahar.network.TokenStorage
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import okio.Buffer

class DraftPdfRepositoryTest {
    private val tokens = object : TokenStorage {
        override fun getAccessToken() = "fixture-access"
        override fun getRefreshToken(): String? = null
        override fun hasTokens() = true
        override fun saveTokens(accessToken: String, refreshToken: String?) {}
        override fun clear() {}
    }
    private fun api(server: MockWebServer) = ApiClient.createAuthenticatedApi(server.url("/").toString(),
        tokens, DraftPdfApi::class.java)

    @Test fun authenticatesDownloadsBytesAndUsesPersistedPlanEndpoint() = runTest {
        val server = MockWebServer()
        try {
            server.enqueue(MockResponse().setHeader("Content-Type", "application/pdf")
                .setBody(Buffer().write("%PDF-1.4\nfixture\n%%EOF".toByteArray())))
            val result = DraftPdfRepository(api(server)) { "fixture-access" }.download(42)
            assertTrue(result is AgenticApiResult.Success)
            val request = server.takeRequest()
            assertEquals("/api/agentic/plans/42/draft-pdf/", request.path)
            assertEquals("GET", request.method)
            assertEquals("Bearer fixture-access", request.getHeader("Authorization"))
            assertEquals("1", request.getHeader("X-VedAmrit-Draft-Preview"))
        } finally { server.shutdown() }
    }

    @Test fun clinicalReviewReasonIsVisibleInsteadOfSavingErrorJson() = runTest {
        val server = MockWebServer()
        try {
            server.enqueue(MockResponse().setResponseCode(409).setHeader("Content-Type", "application/json")
                .setBody("""{"detail":"PDF blocked","reasons":["Doctor must review the safety warning."]}"""))
            val result = DraftPdfRepository(api(server)) { "fixture-access" }.download(42) as AgenticApiResult.Error
            assertEquals(AgenticApiErrorType.ReportNotReady, result.type)
            assertTrue(result.message.contains("Doctor must review"))
        } finally { server.shutdown() }
    }

    @Test fun transportFailuresRemainActionable() = runTest {
        for ((failure, expected) in listOf(java.net.SocketTimeoutException() to AgenticApiErrorType.Timeout,
            java.io.IOException() to AgenticApiErrorType.Network)) {
            val api = object : DraftPdfApi {
                override suspend fun download(id: Long): retrofit2.Response<okhttp3.ResponseBody> = throw failure
            }
            val result = DraftPdfRepository(api) { "fixture-access" }.download(42) as AgenticApiResult.Error
            assertEquals(expected, result.type)
            assertTrue(result.message.contains("retry", ignoreCase = true))
        }
    }

    @Test fun missingExpiredJwtAndNonPdfResponsesCannotBecomeDocuments() = runTest {
        val server = MockWebServer()
        try {
            val repo = DraftPdfRepository(api(server)) { null }
            assertEquals(AgenticApiErrorType.AuthenticationRequired,
                (repo.download(42) as AgenticApiResult.Error).type)
            assertEquals(0, server.requestCount)
            server.enqueue(MockResponse().setResponseCode(401))
            val signed = DraftPdfRepository(api(server)) { "fixture-access" }
            assertEquals(AgenticApiErrorType.AuthenticationExpired,
                (signed.download(42) as AgenticApiResult.Error).type)
            server.enqueue(MockResponse().setHeader("Content-Type", "text/html").setBody("<html>Error</html>"))
            assertEquals(AgenticApiErrorType.Parse, (signed.download(42) as AgenticApiResult.Error).type)
            server.enqueue(MockResponse().setHeader("Content-Type", "application/pdf").setBody("bad file"))
            assertEquals(AgenticApiErrorType.Parse, (signed.download(42) as AgenticApiResult.Error).type)
        } finally { server.shutdown() }
    }
}

