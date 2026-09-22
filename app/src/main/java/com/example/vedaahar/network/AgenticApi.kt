package com.example.vedaahar.network

import com.example.vedaahar.network.dto.AgenticPlanRequest
import com.example.vedaahar.network.dto.AssessmentResultResponse
import com.example.vedaahar.network.dto.CanonicalAssessmentRequest
import com.example.vedaahar.network.dto.FinalReportResponse
import com.example.vedaahar.network.dto.WellnessPlanResponse
import com.example.vedaahar.network.dto.WellnessPlanStatusResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AgenticApi {
    @POST("api/agentic/assessments/")
    suspend fun persistAssessment(@Body request: CanonicalAssessmentRequest): Response<AssessmentResultResponse>

    @GET("api/agentic/assessments/latest/")
    suspend fun latestAssessment(): Response<AssessmentResultResponse>

    @POST("api/agentic/plans/")
    suspend fun generatePlan(@Body request: AgenticPlanRequest): Response<WellnessPlanResponse>

    @GET("api/agentic/plans/")
    suspend fun plans(): Response<List<WellnessPlanResponse>>

    @GET("api/agentic/plans/{id}/")
    suspend fun plan(@Path("id") id: Long): Response<WellnessPlanResponse>

    @GET("api/agentic/plans/{id}/status/")
    suspend fun planStatus(@Path("id") id: Long): Response<WellnessPlanStatusResponse>

    @GET("api/agentic/plans/{id}/report/")
    suspend fun finalReport(@Path("id") id: Long): Response<FinalReportResponse>
}
