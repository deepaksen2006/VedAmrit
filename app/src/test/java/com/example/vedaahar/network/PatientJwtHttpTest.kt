package com.example.vedaahar.network

import com.example.vedaahar.auth.AuthRepository
import com.example.vedaahar.agentic.AgenticApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** HTTP fixtures are confined to this test server; production always uses Django. */
class PatientJwtHttpTest {
    private lateinit var server: MockWebServer
    private lateinit var store: MemoryTokens

    @Before fun setUp() {
        server = MockWebServer()
        server.start()
        store = MemoryTokens()
    }

    @After fun tearDown() { server.shutdown() }

    private fun json(body: String, code: Int = 200) = MockResponse()
        .setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

    private fun profileApi() = ApiClient.createAuthenticatedApi(server.url("/").toString(), store, ProfileApi::class.java)
    private fun request() = requireNotNull(server.takeRequest(5, TimeUnit.SECONDS))

    @Test fun loginPersistsPairAndProfileRequestUsesAccessToken() = runTest {
        server.enqueue(json("""{"access":"access-one","refresh":"refresh-one","user":{"id":1}}"""))
        server.enqueue(json("""{"name":"HTTP patient"}"""))
        val repository = AuthRepository(ApiClient.createAuthApi(server.url("/").toString()), store)
        assertTrue(repository.login(" patient@example.com ", "test-only-password") is AgenticApiResult.Success)
        assertTrue(profileApi().profile().isSuccessful)
        val login = request()
        assertEquals("/api/auth/login/", login.path)
        assertNull(login.getHeader("Authorization"))
        assertTrue(login.body.readUtf8().contains("\"identifier\":\"patient@example.com\""))
        assertEquals("Bearer access-one", request().getHeader("Authorization"))
        assertEquals("refresh-one", store.getRefreshToken())
    }

    @Test fun registrationUsesDjangoContractAndKeepsExistingSignInFlow() = runTest {
        server.enqueue(json("""{"access":"registration-access","refresh":"registration-refresh"}""", 201))
        val repository = AuthRepository(ApiClient.createAuthApi(server.url("/").toString()), store)
        assertTrue(repository.register("new@example.com", "test-only-password") is AgenticApiResult.Success)
        val registration = request()
        assertEquals("/api/auth/register/", registration.path)
        assertTrue(registration.body.readUtf8().contains("new@example.com"))
        assertFalse(store.hasTokens())
    }

    @Test fun unauthorizedRequestRefreshesWithoutBearerAndRetriesWithNewAccess() = runTest {
        store.saveTokens("old-access", "refresh-one")
        server.enqueue(json("{}", 401))
        server.enqueue(json("""{"access":"new-access"}"""))
        server.enqueue(json("""{"name":"HTTP patient"}"""))
        assertTrue(profileApi().profile().isSuccessful)
        assertEquals("Bearer old-access", request().getHeader("Authorization"))
        val refresh = request()
        assertEquals("/api/auth/token/refresh/", refresh.path)
        assertNull(refresh.getHeader("Authorization"))
        assertEquals("{\"refresh\":\"refresh-one\"}", refresh.body.readUtf8())
        assertEquals("Bearer new-access", request().getHeader("Authorization"))
        assertEquals("new-access", store.getAccessToken())
        assertEquals("refresh-one", store.getRefreshToken())
    }

    @Test fun anonymous401DoesNotAttemptRefreshOrInventSession() = runTest {
        server.enqueue(json("{}", 401))
        assertEquals(401, profileApi().profile().code())
        assertNull(request().getHeader("Authorization"))
        assertEquals(1, server.requestCount)
        assertFalse(store.hasTokens())
    }

    @Test fun rejectedRefreshClearsTokensAndDoesNotLoop() = runTest {
        store.saveTokens("old-access", "expired-refresh")
        server.enqueue(json("{}", 401))
        server.enqueue(json("{}", 401))
        assertEquals(401, profileApi().profile().code())
        assertEquals(2, server.requestCount)
        assertFalse(store.hasTokens())
    }

    @Test fun repeated401AfterRefreshEndsSessionAfterOneRetry() = runTest {
        store.saveTokens("old-access", "refresh-one")
        server.enqueue(json("{}", 401))
        server.enqueue(json("""{"access":"new-access"}"""))
        server.enqueue(json("{}", 401))
        assertEquals(401, profileApi().profile().code())
        assertEquals(3, server.requestCount)
        assertFalse(store.hasTokens())
    }

    @Test fun refreshOutagePreservesTokensAndPropagatesNetworkFailure() = runTest {
        store.saveTokens("old-access", "refresh-one")
        server.enqueue(json("{}", 401))
        server.enqueue(json("{}", 503))
        try {
            profileApi().profile()
            fail("An unavailable refresh service must surface a network failure")
        } catch (_: IOException) {
            assertEquals("old-access", store.getAccessToken())
            assertEquals("refresh-one", store.getRefreshToken())
        }
        assertEquals(2, server.requestCount)
    }

    @Test fun concurrent401sAcrossClientsRefreshOnlyOnce() = runTest {
        store.saveTokens("old-access", "refresh-one")
        val initialRequests = CountDownLatch(2)
        val refreshCalls = AtomicInteger()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.path == "/api/auth/token/refresh/") {
                    refreshCalls.incrementAndGet()
                    return json("""{"access":"new-access"}""")
                }
                if (request.getHeader("Authorization") == "Bearer old-access") {
                    initialRequests.countDown()
                    if (!initialRequests.await(5, TimeUnit.SECONDS)) return json("{}", 500)
                    return json("{}", 401)
                }
                return json("""{"name":"HTTP patient"}""")
            }
        }
        val first = profileApi()
        val second = profileApi()
        val responses = listOf(
            async(Dispatchers.IO) { first.profile() },
            async(Dispatchers.IO) { second.profile() }
        ).awaitAll()
        assertTrue(responses.all { it.isSuccessful })
        assertEquals(1, refreshCalls.get())
        assertEquals(5, server.requestCount)
    }

    @Test fun inFlight401AfterLogoutDoesNotRestoreSession() = runTest {
        store.saveTokens("old-access", "refresh-one")
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                store.clear()
                return json("{}", 401)
            }
        }
        assertEquals(401, profileApi().profile().code())
        assertEquals(1, server.requestCount)
        assertFalse(store.hasTokens())
    }

    @Test fun previousPatients401IsNotRetriedUnderNewLogin() = runTest {
        store.saveTokens("old-access", "refresh-one")
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                store.saveTokens("other-patient-access", "other-patient-refresh")
                return json("{}", 401)
            }
        }
        assertEquals(401, profileApi().profile().code())
        assertEquals(1, server.requestCount)
        assertEquals("other-patient-access", store.getAccessToken())
    }

    private class MemoryTokens : TokenStorage {
        @Volatile private var access: String? = null
        @Volatile private var refresh: String? = null
        override fun getAccessToken() = access
        override fun getRefreshToken() = refresh
        override fun hasTokens() = !access.isNullOrBlank() && !refresh.isNullOrBlank()
        override fun saveTokens(accessToken: String, refreshToken: String?) {
            access = accessToken
            if (refreshToken != null) refresh = refreshToken
        }
        override fun clear() { access = null; refresh = null }
    }
}
