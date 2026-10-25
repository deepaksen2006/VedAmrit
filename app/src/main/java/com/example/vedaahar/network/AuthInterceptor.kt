package com.example.vedaahar.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

internal data class RequestSession(val refreshToken: String?)

class AuthInterceptor(
    private val tokenProvider: () -> String?,
    private val refreshTokenProvider: () -> String? = { null },
    private val expectedPatientKey: String? = null,
    private val patientKeyForToken: (String?) -> String? = { null }
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider()
        if (expectedPatientKey != null && patientKeyForToken(token) != expectedPatientKey) {
            throw IOException("Patient session changed. Sign in again before syncing.")
        }
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
