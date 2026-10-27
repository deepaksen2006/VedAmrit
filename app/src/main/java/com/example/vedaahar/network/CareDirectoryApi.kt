package com.example.vedaahar.network

import com.example.vedaahar.BuildConfig
import com.example.vedaahar.network.dto.CareDoctorDto
import com.example.vedaahar.network.dto.CareHospitalDto
import com.example.vedaahar.network.dto.CarePage
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/** Public, verified directory only. Task 4.2 can consume this without GPS or login. */
interface CareDirectoryApi {
    @GET("api/care/hospitals/nearby/")
    suspend fun nearbyHospitals(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radius_km") radiusKm: Double = 20.0,
        @Query("search") search: String? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): Response<CarePage<CareHospitalDto>>

    @GET("api/care/hospitals/")
    suspend fun hospitals(
        @Query("city") city: String? = null,
        @Query("search") search: String? = null,
        @Query("state") state: String? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): Response<CarePage<CareHospitalDto>>

    @GET("api/care/hospitals/{id}/")
    suspend fun hospital(@Path("id") id: Long): Response<CareHospitalDto>

    @GET("api/care/doctors/")
    suspend fun doctors(
        @Query("city") city: String? = null,
        @Query("search") search: String? = null,
        @Query("state") state: String? = null,
        @Query("specialization") specialization: String? = null,
        @Query("hospital_id") hospitalId: Long? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): Response<CarePage<CareDoctorDto>>

    @GET("api/care/doctors/{id}/")
    suspend fun doctor(@Path("id") id: Long): Response<CareDoctorDto>
}

object CareDirectoryClient {
    fun create(baseUrl: String = BuildConfig.AGENTIC_API_BASE_URL): CareDirectoryApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(CareDirectoryApi::class.java)
}
