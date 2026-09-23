package com.example.vedaahar.network

import com.example.vedaahar.network.dto.TokenRefreshRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenRefreshAuthenticator(
    private val tokenStore: TokenStorage,
    private val authApi: AuthApi
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.responseCount() >= 2) return null
        val refreshToken = tokenStore.getRefreshToken() ?: return null

        val newAccessToken = runBlocking {
            runCatching {
                val refreshResponse = authApi.refresh(TokenRefreshRequest(refreshToken))
                if (refreshResponse.isSuccessful) refreshResponse.body()?.access else null
            }.getOrNull()
        }

        if (newAccessToken.isNullOrBlank()) {
            tokenStore.clear()
            return null
        }

        tokenStore.saveTokens(accessToken = newAccessToken, refreshToken = refreshToken)
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccessToken")
            .build()
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
