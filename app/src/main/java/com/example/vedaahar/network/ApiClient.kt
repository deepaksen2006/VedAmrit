package com.example.vedaahar.network

import android.content.Context
import com.example.vedaahar.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    fun agenticApi(context: Context): AgenticApi {
        return createAgenticApi(BuildConfig.AGENTIC_API_BASE_URL)
    }

    fun profileApi(context: Context): ProfileApi {
        return createAuthenticatedApi(BuildConfig.AGENTIC_API_BASE_URL, ProfileApi::class.java)
    }

    fun createAgenticApi(
        baseUrl: String
    ): AgenticApi = createAuthenticatedApi(baseUrl, AgenticApi::class.java)

    fun <T> createAuthenticatedApi(
        baseUrl: String,
        service: Class<T>
    ): T {
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(FirebaseIdTokenInterceptor())
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(service)
    }
}
