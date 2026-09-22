package com.example.vedaahar.network

import com.example.vedaahar.network.dto.PatientProfileDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.PUT

interface ProfileApi {
    @GET("api/profile/")
    suspend fun profile(): Response<PatientProfileDto>

    @PUT("api/profile/")
    suspend fun putProfile(@Body profile: PatientProfileDto): Response<PatientProfileDto>

    @PATCH("api/profile/")
    suspend fun patchProfile(@Body profile: PatientProfileDto): Response<PatientProfileDto>
}
