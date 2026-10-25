package com.example.vedaahar.network

import com.example.vedaahar.network.dto.TokenRefreshRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException

class TokenRefreshAuthenticator(
    private val tokenStore: TokenStorage,
    private val authApi: AuthApi
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? = synchronized(RefreshLock) {
        val failedAccess = response.request.header("Authorization")?.removePrefix("Bearer ")
        val currentAccess = tokenStore.getAccessToken()?.takeIf { it.isNotBlank() }
        // Never create a session for an anonymous request or revive a logout.
        if (failedAccess.isNullOrBlank() || currentAccess == null) return null
        if (response.responseCount() >= 2) {
            if (currentAccess == failedAccess) tokenStore.clear()
            return null
        }
        // Another request may already have refreshed the same session.
        if (currentAccess != failedAccess) return retry(response, currentAccess)
        val refreshToken = tokenStore.getRefreshToken()?.takeIf { it.isNotBlank() }
        if (refreshToken == null) {
            tokenStore.clear()
            return null
        }

        // IO failures propagate to the repository's visible network error path.
        // Temporary outages must not erase a valid refresh token.
        val refreshResponse = runBlocking { authApi.refresh(TokenRefreshRequest(refreshToken)) }
        if (tokenStore.getRefreshToken() != refreshToken || tokenStore.getAccessToken() != currentAccess) {
            return null
        }
        if (!refreshResponse.isSuccessful) {
            if (refreshResponse.code() in setOf(400, 401, 403)) {
                tokenStore.clear()
                return null
            }
            throw IOException("Session refresh service unavailable (HTTP ${refreshResponse.code()}).")
        }
        val newAccessToken = refreshResponse.body()?.access
        if (newAccessToken.isNullOrBlank()) {
            tokenStore.clear()
            return null
        }

        tokenStore.saveTokens(accessToken = newAccessToken, refreshToken = refreshToken)
        retry(response, newAccessToken)
    }

    private fun retry(response: Response, accessToken: String): Request =
        response.request.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()

    companion object {
        // ApiClient creates separate profile/assessment clients sharing one store.
        private val RefreshLock = Any()
    }
}

private fun Response.responseCount(): Int {
    var result = 1
    var current = priorResponse
    while (current != null) {
        result++
        current = current.priorResponse
    }
    return result
}
