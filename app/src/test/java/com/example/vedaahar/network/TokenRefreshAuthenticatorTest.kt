package com.example.vedaahar.network

import com.example.vedaahar.network.dto.AuthRequest
import com.example.vedaahar.network.dto.AuthResponse
import com.example.vedaahar.network.dto.TokenRefreshRequest
import com.example.vedaahar.network.dto.TokenRefreshResponse
import kotlinx.coroutines.runBlocking
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Response

class TokenRefreshAuthenticatorTest {
    @Test
    fun refreshesTokenAndRetriesOriginalRequestOnce() {
        val storage = FakeTokenStorage(access = "expired", refresh = "refresh")
        val authenticator = TokenRefreshAuthenticator(
            tokenStore = storage,
            authApi = FakeAuthApi(Response.success(TokenRefreshResponse("fresh-access")))
        )

        val retry = authenticator.authenticate(null, unauthorizedResponse())

        assertEquals("fresh-access", storage.access)
        assertEquals("Bearer fresh-access", retry?.header("Authorization"))
    }

    @Test
    fun failedRefreshClearsTokensAndDoesNotRetry() {
        val storage = FakeTokenStorage(access = "expired", refresh = "bad-refresh")
        val authenticator = TokenRefreshAuthenticator(
            tokenStore = storage,
            authApi = FakeAuthApi(Response.error(401, "{}".toResponseBody(null)))
        )

        val retry = authenticator.authenticate(null, unauthorizedResponse())

        assertNull(retry)
        assertNull(storage.access)
        assertNull(storage.refresh)
    }

    private fun unauthorizedResponse(): okhttp3.Response {
        val request = Request.Builder()
            .url("http://localhost/api/agentic/plans/")
            .header("Authorization", "Bearer expired")
            .build()
        return okhttp3.Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("{}".toResponseBody(null))
            .build()
    }

    private class FakeTokenStorage(
        var access: String?,
        var refresh: String?
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
        private val refreshResponse: Response<TokenRefreshResponse>
    ) : AuthApi {
        override suspend fun register(request: AuthRequest): Response<AuthResponse> = throw NotImplementedError()
        override suspend fun login(request: AuthRequest): Response<AuthResponse> = throw NotImplementedError()
        override suspend fun refresh(request: TokenRefreshRequest): Response<TokenRefreshResponse> = runBlocking { refreshResponse }
    }
}
