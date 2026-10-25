package com.example.vedaahar.auth

import android.app.Application
import com.example.vedaahar.network.AuthApi
import com.example.vedaahar.network.TokenStorage
import com.example.vedaahar.network.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val api = RecordingAuthApi()
    private val tokens = MemoryTokens()
    private fun viewModel() = AuthViewModel(Application(), AuthRepository(api, tokens))

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun debugLoginCallsBackendBeforeEnteringAuthenticatedState() {
        val model = viewModel()
        var navigated = false
        model.login("patient@example.com", "test-only-password") { navigated = true }
        assertEquals(1, api.loginCalls)
        assertEquals(AuthUiState.Authenticated, model.uiState.value)
        assertTrue(tokens.hasTokens())
        assertTrue(navigated)
    }

    @Test fun registrationCallsBackendAndRequiresExistingSignInStep() {
        val model = viewModel()
        var navigated = false
        model.register("patient@example.com", "test-only-password") { navigated = true }
        assertEquals(1, api.registerCalls)
        assertEquals(AuthUiState.AccountCreated, model.uiState.value)
        assertFalse(tokens.hasTokens())
        assertFalse(navigated)
    }

    @Test fun rejectedLoginShowsErrorAndNeverNavigates() {
        api.rejectLogin = true
        val model = viewModel()
        var navigated = false
        model.login("patient@example.com", "incorrect") { navigated = true }
        assertTrue(model.uiState.value is AuthUiState.Error)
        assertFalse(navigated)
        assertFalse(tokens.hasTokens())
    }

    @Test fun restoreRequiresBackendRefresh() {
        tokens.saveTokens("old-access", "test-refresh")
        val model = viewModel()
        var authenticated = false
        model.restoreSession(onAuthenticated = { authenticated = true })
        assertEquals(1, api.refreshCalls)
        assertEquals("new-access", tokens.getAccessToken())
        assertTrue(authenticated)
    }

    @Test fun restoreNetworkErrorIsVisibleAndRetainsTokensForRetry() {
        tokens.saveTokens("old-access", "test-refresh")
        api.offline = true
        val model = viewModel()
        var authenticated = false
        var unauthenticated = false
        model.restoreSession({ authenticated = true }, { unauthenticated = true })
        assertTrue(model.uiState.value is AuthUiState.Error)
        assertFalse(authenticated)
        assertTrue(unauthenticated)
        assertTrue(tokens.hasTokens())
    }

    @Test fun missingSessionDoesNotRestoreLocalDevelopmentLogin() {
        val model = viewModel()
        var unauthenticated = false
        model.restoreSession({ fail("No JWT session exists") }, { unauthenticated = true })
        assertEquals(AuthUiState.Idle, model.uiState.value)
        assertEquals(0, api.refreshCalls)
        assertTrue(unauthenticated)
    }

    private class RecordingAuthApi : AuthApi {
        var loginCalls = 0
        var registerCalls = 0
        var refreshCalls = 0
        var rejectLogin = false
        var offline = false
        override suspend fun register(request: AuthRequest): Response<AuthResponse> {
            registerCalls++
            return Response.success(AuthResponse("test-access", "test-refresh", null))
        }
        override suspend fun login(request: AuthRequest): Response<AuthResponse> {
            loginCalls++
            return if (rejectLogin) Response.error(400, "{}".toResponseBody(null))
            else Response.success(AuthResponse("test-access", "test-refresh", null))
        }
        override suspend fun refresh(request: TokenRefreshRequest): Response<TokenRefreshResponse> {
            refreshCalls++
            if (offline) throw IOException("test offline")
            return Response.success(TokenRefreshResponse("new-access"))
        }
    }

    private class MemoryTokens : TokenStorage {
        private var access: String? = null
        private var refresh: String? = null
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
