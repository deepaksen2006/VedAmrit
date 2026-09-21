package com.example.vedaahar.network

import com.example.vedaahar.network.dto.AuthRequest
import com.example.vedaahar.network.dto.AuthResponse
import com.example.vedaahar.network.dto.TokenRefreshRequest
import com.example.vedaahar.network.dto.TokenRefreshResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/auth/register/")
    suspend fun register(@Body request: AuthRequest): Response<AuthResponse>

    @POST("api/auth/login/")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

    @POST("api/auth/token/refresh/")
    suspend fun refresh(@Body request: TokenRefreshRequest): Response<TokenRefreshResponse>
}
