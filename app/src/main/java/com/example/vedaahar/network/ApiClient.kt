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
        return createAgenticApi(BuildConfig.AGENTIC_API_BASE_URL, tokenStore)
    }

    fun createAgenticApi(
        baseUrl: String,
        tokenStore: AuthTokenStore
    ): AgenticApi {
        val authApi = createAuthApi(baseUrl)
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor { tokenStore.getAccessToken() })
            .authenticator(TokenRefreshAuthenticator(tokenStore, authApi))
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AgenticApi::class.java)
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
}
