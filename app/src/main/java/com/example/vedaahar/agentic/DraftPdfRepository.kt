package com.example.vedaahar.agentic

import com.example.vedaahar.network.DraftPdfApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

class DraftPdfRepository(private val api: DraftPdfApi, private val token: () -> String?) {
    suspend fun download(id: Long): AgenticApiResult<ByteArray> = withContext(Dispatchers.IO) {
        if (token().isNullOrBlank()) return@withContext AgenticApiResult.Error(
            AgenticApiErrorType.AuthenticationRequired, "Sign in to download your personalized PDF.")
        try {
            val response = api.download(id)
            if (!response.isSuccessful) {
                val detail = runCatching {
                    val json = JSONObject(response.errorBody()?.string().orEmpty())
                    val reasons = json.optJSONArray("reasons")
                    if (reasons != null) (0 until reasons.length()).joinToString("\n") { reasons.getString(it) }
                    else json.optString("detail").takeIf { it.isNotBlank() }
                }.getOrNull()
                return@withContext AgenticApiResult.Error(when (response.code()) {
                    401 -> AgenticApiErrorType.AuthenticationExpired
                    403 -> AgenticApiErrorType.AuthorizationDenied
                    409 -> AgenticApiErrorType.ReportNotReady
                    else -> AgenticApiErrorType.BackendFailure
                }, if (response.code() == 401) "Your session expired. Sign in again." else detail
                    ?: "PDF download failed (HTTP ${response.code()}). Refresh the plan and retry.")
            }
            val body = response.body() ?: throw IOException("Empty PDF response.")
            body.use {
                if (body.contentType()?.let { it.type == "application" && it.subtype == "pdf" } != true)
                    return@withContext AgenticApiResult.Error(AgenticApiErrorType.Parse, "The server did not return a PDF.")
                val output = ByteArrayOutputStream()
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (output.size() + count > MAX_BYTES) throw IOException("PDF exceeds the supported size.")
                        output.write(buffer, 0, count)
                    }
                }
                val data = output.toByteArray()
                if (!isPdf(data)) return@withContext AgenticApiResult.Error(AgenticApiErrorType.Parse, "The downloaded file is not a valid PDF response.")
                AgenticApiResult.Success(data)
            }
        } catch (_: java.net.SocketTimeoutException) {
            AgenticApiResult.Error(AgenticApiErrorType.Timeout, "PDF download timed out. Retry; this does not generate a new AI plan.")
        } catch (_: IOException) {
            AgenticApiResult.Error(AgenticApiErrorType.Network, "PDF could not be downloaded. Check connectivity and available storage, then retry.")
        }
    }

    companion object {
        const val MAX_BYTES = 20 * 1024 * 1024
        fun isPdf(data: ByteArray) = data.size >= 8 && data.take(5).toByteArray().contentEquals("%PDF-".toByteArray())
    }
}

