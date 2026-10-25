package com.example.vedaahar.auth

import com.example.vedaahar.agentic.AgenticApiErrorType
import com.example.vedaahar.agentic.AgenticApiResult
import com.example.vedaahar.network.AuthApi
import com.example.vedaahar.network.TokenStorage
import com.example.vedaahar.network.dto.AuthRequest
import com.example.vedaahar.network.dto.AuthResponse
import com.example.vedaahar.network.dto.AuthUserDto
import com.example.vedaahar.network.dto.TokenRefreshRequest
import com.example.vedaahar.network.dto.TokenRefreshResponse
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

class AuthRepositoryTest {
    @Test
    fun successfulLoginSavesDjangoJwtSession() = runTest {
        val tokenStore = FakeTokenStore()
        val api = FakeAuthApi(loginResponse = Response.success(authResponse()))
        val repository = AuthRepository(api, tokenStore)

        val result = repository.login("patient@example.com", "password")

        assertTrue(result is AgenticApiResult.Success)
        assertEquals("patient@example.com", api.lastLogin?.identifier)
        assertEquals("access-token", tokenStore.savedAccessToken)
        assertEquals("refresh-token", tokenStore.savedRefreshToken)
        assertTrue(repository.isSessionAvailable())
    }

    @Test
    fun successfulRegistrationDoesNotStartSession() = runTest {
        val tokenStore = FakeTokenStore()
        val api = FakeAuthApi(registerResponse = Response.success(authResponse()))
        val repository = AuthRepository(api, tokenStore)

        val result = repository.register("new@example.com", "password")

        assertTrue(result is AgenticApiResult.Success)
        assertEquals("new@example.com", api.lastRegister?.identifier)
        assertNull(tokenStore.savedAccessToken)
        assertNull(tokenStore.savedRefreshToken)
        assertFalse(repository.isSessionAvailable())
    }

    @Test
    fun duplicateOrInvalidAccountMapsToBadRequest() = runTest {
        val repository = AuthRepository(
            FakeAuthApi(registerResponse = Response.error(400, "{}".toResponseBody(null))),
            FakeTokenStore()
        )

        val result = repository.register("patient@example.com", "password")

        assertTrue(result is AgenticApiResult.Error)
        assertEquals(AgenticApiErrorType.BadRequest, (result as AgenticApiResult.Error).type)
    }

    @Test
    fun refreshSessionStoresNewAccessToken() = runTest {
        val tokenStore = FakeTokenStore(savedRefreshToken = "refresh-token")
        val api = FakeAuthApi(refreshResponse = Response.success(TokenRefreshResponse("new-access")))
        val repository = AuthRepository(api, tokenStore)

        val refreshed = repository.refreshSession()

        assertTrue(refreshed is AgenticApiResult.Success)
        assertEquals("new-access", tokenStore.savedAccessToken)
        assertEquals("refresh-token", tokenStore.savedRefreshToken)
    }

    @Test
    fun logoutClearsJwtSession() {
        val tokenStore = FakeTokenStore(savedAccessToken = "access-token", savedRefreshToken = "refresh-token")
        val repository = AuthRepository(FakeAuthApi(), tokenStore)

        repository.logout()

        assertFalse(repository.isSessionAvailable())
        assertTrue(tokenStore.cleared)
    }

    @Test
    fun invalidCredentialsExposeDjangoMessageAndDoNotSaveTokens() = runTest {
        val store = FakeTokenStore()
        val repository = AuthRepository(FakeAuthApi(loginResponse = Response.error(
            400, """{"non_field_errors":["Invalid credentials."]}""".toResponseBody(null)
        )), store)
        val result = repository.login("patient@example.com", "wrong") as AgenticApiResult.Error
        assertEquals("Invalid credentials.", result.message)
        assertFalse(store.hasTokens())
    }

    @Test
    fun registrationExposesFieldValidationErrors() = runTest {
        val repository = AuthRepository(FakeAuthApi(registerResponse = Response.error(
            400, """{"identifier":["An account already exists for this email."]}""".toResponseBody(null)
        )), FakeTokenStore())
        val result = repository.register("existing@example.com", "password") as AgenticApiResult.Error
        assertTrue(result.message.contains("already exists"))
    }

    @Test
    fun malformedSuccessfulLoginDoesNotStartSession() = runTest {
        val store = FakeTokenStore()
        val repository = AuthRepository(FakeAuthApi(loginResponse = Response.success(
            authResponse().copy(access = "")
        )), store)
        val result = repository.login("patient@example.com", "password") as AgenticApiResult.Error
        assertEquals(AgenticApiErrorType.Parse, result.type)
        assertFalse(store.hasTokens())
    }

    @Test
    fun loginNetworkErrorIsVisible() = runTest {
        val repository = AuthRepository(FakeAuthApi(loginError = IOException("offline")), FakeTokenStore())
        val result = repository.login("patient@example.com", "password") as AgenticApiResult.Error
        assertEquals(AgenticApiErrorType.Network, result.type)
        assertTrue(result.message.isNotBlank())
    }

    @Test
    fun rejectedRefreshClearsSessionAndRequiresSignIn() = runTest {
        val store = FakeTokenStore("expired", "expired-refresh")
        val repository = AuthRepository(FakeAuthApi(refreshResponse = Response.error(
            401, "{}".toResponseBody(null)
        )), store)
        val result = repository.refreshSession() as AgenticApiResult.Error
        assertEquals(AgenticApiErrorType.AuthenticationExpired, result.type)
        assertFalse(store.hasTokens())
    }

    @Test
    fun restoreNetworkFailurePreservesSessionForRetry() = runTest {
        val store = FakeTokenStore("access", "refresh")
        val repository = AuthRepository(FakeAuthApi(refreshError = IOException("offline")), store)
        val result = repository.refreshSession() as AgenticApiResult.Error
        assertEquals(AgenticApiErrorType.Network, result.type)
        assertTrue(store.hasTokens())
    }

    @Test
    fun restoreTimeoutIsVisibleAndPreservesSession() = runTest {
        val store = FakeTokenStore("access", "refresh")
        val repository = AuthRepository(FakeAuthApi(refreshError = SocketTimeoutException()), store)
        val result = repository.refreshSession() as AgenticApiResult.Error
        assertEquals(AgenticApiErrorType.Timeout, result.type)
        assertTrue(store.hasTokens())
    }

    @Test
    fun unavailableRefreshServicePreservesSession() = runTest {
        val store = FakeTokenStore("access", "refresh")
        val repository = AuthRepository(FakeAuthApi(refreshResponse = Response.error(
            503, "{}".toResponseBody(null)
        )), store)
        assertTrue(repository.refreshSession() is AgenticApiResult.Error)
        assertTrue(store.hasTokens())
    }

    @Test
    fun missingRefreshClearsPartialSession() = runTest {
        val store = FakeTokenStore(savedAccessToken = "access")
        val result = AuthRepository(FakeAuthApi(), store).refreshSession() as AgenticApiResult.Error
        assertEquals(AgenticApiErrorType.AuthenticationRequired, result.type)
        assertNull(store.savedAccessToken)
    }

    private fun authResponse() = AuthResponse(
        access = "access-token",
        refresh = "refresh-token",
        user = AuthUserDto(id = 1L, username = "patient@example.com", email = "patient@example.com")
    )

    private class FakeAuthApi(
        private val registerResponse: Response<AuthResponse> = Response.success(
            AuthResponse("access-token", "refresh-token", null)
        ),
        private val loginResponse: Response<AuthResponse> = Response.success(
            AuthResponse("access-token", "refresh-token", null)
        ),
        private val refreshResponse: Response<TokenRefreshResponse> = Response.success(
            TokenRefreshResponse("new-access-token")
        ),
        private val loginError: IOException? = null,
        private val refreshError: IOException? = null
    ) : AuthApi {
        var lastRegister: AuthRequest? = null
            private set
        var lastLogin: AuthRequest? = null
            private set

        override suspend fun register(request: AuthRequest): Response<AuthResponse> {
            lastRegister = request
            return registerResponse
        }

        override suspend fun login(request: AuthRequest): Response<AuthResponse> {
            lastLogin = request
            loginError?.let { throw it }
            return loginResponse
        }

        override suspend fun refresh(request: TokenRefreshRequest): Response<TokenRefreshResponse> {
            refreshError?.let { throw it }
            return refreshResponse
        }
    }

    private class FakeTokenStore(
        var savedAccessToken: String? = null,
        var savedRefreshToken: String? = null
    ) : TokenStorage {
        var cleared: Boolean = false
            private set

        override fun getAccessToken(): String? = savedAccessToken

        override fun getRefreshToken(): String? = savedRefreshToken

        override fun hasTokens(): Boolean = !savedAccessToken.isNullOrBlank() && !savedRefreshToken.isNullOrBlank()

        override fun saveTokens(accessToken: String, refreshToken: String?) {
            savedAccessToken = accessToken
            if (refreshToken != null) savedRefreshToken = refreshToken
        }

        override fun clear() {
            savedAccessToken = null
            savedRefreshToken = null
            cleared = true
        }
    }
}
