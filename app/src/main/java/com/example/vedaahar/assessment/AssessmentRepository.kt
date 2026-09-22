package com.example.vedaahar.assessment

import com.example.vedaahar.agentic.AgenticApiErrorType
import com.example.vedaahar.agentic.AgenticApiResult
import com.example.vedaahar.network.AgenticApi
import com.example.vedaahar.network.dto.AssessmentResultResponse
import com.example.vedaahar.network.dto.CanonicalAssessmentRequest
import com.google.gson.JsonSyntaxException
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

interface AssessmentGateway {
    suspend fun persistCanonicalAssessment(rawAssessmentProfile: String): AgenticApiResult<AssessmentResultResponse>
}

class AssessmentRepository(
    private val api: AgenticApi,
    private val accessTokenProvider: () -> String?
) : AssessmentGateway {
    override suspend fun persistCanonicalAssessment(rawAssessmentProfile: String): AgenticApiResult<AssessmentResultResponse> {
        if (accessTokenProvider().isNullOrBlank()) {
            return AgenticApiResult.Error(
                AgenticApiErrorType.AuthenticationRequired,
                "Sign in before syncing assessment results."
            )
        }
        val payload = runCatching {
            CanonicalAssessmentRequest(JSONObject(rawAssessmentProfile).toKotlinMap())
        }.getOrElse {
            return AgenticApiResult.Error(
                AgenticApiErrorType.Parse,
                "Stored assessment data is malformed."
            )
        }
        return try {
            val response = api.persistAssessment(payload)
            if (response.isSuccessful) {
                response.body()?.let { AgenticApiResult.Success(it) }
                    ?: AgenticApiResult.Error(AgenticApiErrorType.Parse, "The server returned an empty assessment response.")
            } else {
                AgenticApiResult.Error(response.errorType(), response.errorMessage())
            }
        } catch (_: SocketTimeoutException) {
            AgenticApiResult.Error(AgenticApiErrorType.Timeout, "Assessment sync timed out. It remains saved locally.")
        } catch (_: IOException) {
            AgenticApiResult.Error(AgenticApiErrorType.Network, "Network is unavailable. Assessment remains saved locally.")
        } catch (_: JsonSyntaxException) {
            AgenticApiResult.Error(AgenticApiErrorType.Parse, "The server assessment response could not be read.")
        }
    }
}

private fun JSONObject.toKotlinMap(): Map<String, Any?> {
    return keys().asSequence().associateWith { key ->
        when (val value = get(key)) {
            is JSONObject -> value.toKotlinMap()
            is JSONArray -> value.toKotlinList()
            JSONObject.NULL -> null
            else -> value
        }
    }
}

private fun JSONArray.toKotlinList(): List<Any?> {
    return (0 until length()).map { index ->
        when (val value = get(index)) {
            is JSONObject -> value.toKotlinMap()
            is JSONArray -> value.toKotlinList()
            JSONObject.NULL -> null
            else -> value
        }
    }
}

private fun <T> Response<T>.errorType(): AgenticApiErrorType {
    return when (code()) {
        400 -> AgenticApiErrorType.BadRequest
        401 -> AgenticApiErrorType.AuthenticationExpired
        403 -> AgenticApiErrorType.AuthorizationDenied
        404 -> AgenticApiErrorType.NotFound
        in 500..599 -> AgenticApiErrorType.BackendFailure
        else -> AgenticApiErrorType.Unknown
    }
}

private fun <T> Response<T>.errorMessage(): String {
    return when (code()) {
        400 -> "The canonical assessment payload is missing or malformed."
        401 -> "Your session expired. Please sign in again."
        403 -> "You are not allowed to save this assessment."
        in 500..599 -> "The backend could not save the assessment right now."
        else -> "Assessment sync failed with HTTP ${code()}."
    }
}
