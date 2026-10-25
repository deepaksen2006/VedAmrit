package com.example.vedaahar.network

import okhttp3.Interceptor
import okhttp3.Response

internal data class RequestSession(val refreshToken: String?)

class AuthInterceptor(
    private val tokenProvider: () -> String?,
    private val refreshTokenProvider: () -> String? = { null }
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider()
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .tag(RequestSession::class.java, RequestSession(refreshTokenProvider()))
                .build()
        }
        return chain.proceed(request)
    }
}
