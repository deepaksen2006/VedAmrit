package com.example.vedaahar.network

import android.content.Context
import com.example.vedaahar.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    fun authApi(): AuthApi = createAuthApi(BuildConfig.AGENTIC_API_BASE_URL)

    fun agenticApi(context: Context): AgenticApi {
        val tokenStore = AuthTokenStore(context)
        return createAuthenticatedApi(BuildConfig.AGENTIC_API_BASE_URL, tokenStore, AgenticApi::class.java,
            PatientSession.currentKey(context))
    }

    fun profileApi(context: Context): ProfileApi {
        val tokenStore = AuthTokenStore(context)
        return createAuthenticatedApi(BuildConfig.AGENTIC_API_BASE_URL, tokenStore, ProfileApi::class.java,
            PatientSession.currentKey(context))
    }

    fun createAuthApi(baseUrl: String): AuthApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }

    fun <T> createAuthenticatedApi(
        baseUrl: String,
        tokenStore: TokenStorage,
        service: Class<T>,
        expectedPatientKey: String? = null
    ): T {
        val authApi = createAuthApi(baseUrl)
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(
                tokenProvider = { tokenStore.getAccessToken() },
                refreshTokenProvider = { tokenStore.getRefreshToken() },
                expectedPatientKey = expectedPatientKey,
                patientKeyForToken = { PatientSession.keyFor(it, baseUrl) }
            ))
            .authenticator(TokenRefreshAuthenticator(tokenStore, authApi))
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(service)
    }
}
