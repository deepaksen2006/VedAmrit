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

class AuthRepositoryTest {
    @Test
    fun successfulLoginStoresAccessAndRefreshTokens() = runTest {
        val storage = FakeTokenStorage()
        val repository = AuthRepository(FakeAuthApi(loginResponse = Response.success(authResponse())), storage)

        val result = repository.login("patient@example.com", "password")

        assertTrue(result is AgenticApiResult.Success)
        assertEquals("access-token", storage.access)
        assertEquals("refresh-token", storage.refresh)
    }

    @Test
    fun incorrectCredentialsReturnErrorAndDoNotStoreTokens() = runTest {
        val storage = FakeTokenStorage()
        val repository = AuthRepository(FakeAuthApi(loginResponse = error(400)), storage)

        val result = repository.login("patient@example.com", "wrong")

        assertTrue(result is AgenticApiResult.Error)
        assertEquals(AgenticApiErrorType.AuthenticationExpired, (result as AgenticApiResult.Error).type)
        assertNull(storage.access)
        assertNull(storage.refresh)
    }

    @Test
    fun registrationStoresTokensWhenBackendReturnsJwt() = runTest {
        val storage = FakeTokenStorage()
        val repository = AuthRepository(FakeAuthApi(registerResponse = Response.success(authResponse())), storage)

        val result = repository.register("new@example.com", "password")

        assertTrue(result is AgenticApiResult.Success)
        assertEquals("access-token", storage.access)
        assertEquals("refresh-token", storage.refresh)
    }

    @Test
    fun refreshSessionStoresNewAccessToken() = runTest {
        val storage = FakeTokenStorage(refresh = "refresh-token")
        val repository = AuthRepository(FakeAuthApi(refreshResponse = Response.success(TokenRefreshResponse("new-access"))), storage)

        val refreshed = repository.refreshSession()

        assertTrue(refreshed)
        assertEquals("new-access", storage.access)
        assertEquals("refresh-token", storage.refresh)
    }

    @Test
    fun failedRefreshClearsTokens() = runTest {
        val storage = FakeTokenStorage(access = "old", refresh = "bad-refresh")
        val repository = AuthRepository(FakeAuthApi(refreshResponse = Response.error(401, "{}".toResponseBody(null))), storage)

        val refreshed = repository.refreshSession()

        assertFalse(refreshed)
        assertNull(storage.access)
        assertNull(storage.refresh)
    }

    @Test
    fun logoutClearsSession() {
        val storage = FakeTokenStorage(access = "access", refresh = "refresh")
        val repository = AuthRepository(FakeAuthApi(), storage)

        repository.logout()

        assertFalse(storage.hasTokens())
    }

    @Test
    fun sessionRestorationReflectsStoredTokens() {
        val repository = AuthRepository(FakeAuthApi(), FakeTokenStorage(access = "access", refresh = "refresh"))

        assertTrue(repository.isSessionAvailable())
    }

    private fun authResponse() = AuthResponse(
        access = "access-token",
        refresh = "refresh-token",
        user = AuthUserDto(id = 1L, username = "patient@example.com", email = "patient@example.com")
    )

    private fun error(code: Int): Response<AuthResponse> = Response.error(code, "{}".toResponseBody(null))

    private class FakeTokenStorage(
        var access: String? = null,
        var refresh: String? = null
    ) : TokenStorage {
        override fun getAccessToken(): String? = access
        override fun getRefreshToken(): String? = refresh
        override fun hasTokens(): Boolean = !access.isNullOrBlank() && !refresh.isNullOrBlank()
        override fun saveTokens(accessToken: String, refreshToken: String?) {
            access = accessToken
            if (refreshToken != null) refresh = refreshToken
        }
        override fun clear() {
            access = null
            refresh = null
        }
    }

    private class FakeAuthApi(
        private val loginResponse: Response<AuthResponse> = Response.success(AuthResponse("a", "r", null)),
        private val registerResponse: Response<AuthResponse> = Response.success(AuthResponse("a", "r", null)),
        private val refreshResponse: Response<TokenRefreshResponse> = Response.success(TokenRefreshResponse("new"))
    ) : AuthApi {
        override suspend fun register(request: AuthRequest): Response<AuthResponse> = registerResponse
        override suspend fun login(request: AuthRequest): Response<AuthResponse> = loginResponse
        override suspend fun refresh(request: TokenRefreshRequest): Response<TokenRefreshResponse> = refreshResponse
    }
}
