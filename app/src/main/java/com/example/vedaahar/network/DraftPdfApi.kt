package com.example.vedaahar.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming

interface DraftPdfApi {
    @Streaming
    @GET("api/agentic/plans/{id}/draft-pdf/")
    suspend fun download(@Path("id") id: Long): Response<ResponseBody>
}

