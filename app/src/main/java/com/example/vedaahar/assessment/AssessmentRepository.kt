package com.example.vedaahar.assessment

import com.example.vedaahar.agentic.AgenticApiErrorType
import com.example.vedaahar.agentic.AgenticApiResult
import com.example.vedaahar.network.AgenticApi
import com.example.vedaahar.network.dto.AssessmentResultResponse
import com.example.vedaahar.network.dto.CanonicalAssessmentRequest
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

interface AssessmentGateway {
    suspend fun persistCanonicalAssessment(rawAssessmentProfile: String, submissionId: String? = null): AgenticApiResult<AssessmentResultResponse>
    suspend fun latestAssessment(): AgenticApiResult<AssessmentResultResponse>
}

class AssessmentRepository(
    private val api: AgenticApi,
    private val accessTokenProvider: () -> String?
) : AssessmentGateway {
    override suspend fun persistCanonicalAssessment(rawAssessmentProfile: String, submissionId: String?): AgenticApiResult<AssessmentResultResponse> {
        if (accessTokenProvider().isNullOrBlank()) {
            return AgenticApiResult.Error(
                AgenticApiErrorType.AuthenticationRequired,
                "Sign in before syncing assessment results."
            )
        }
        val payload = runCatching {
            val content = CanonicalAssessment.content(rawAssessmentProfile)
            CanonicalAssessmentRequest(content.mapKeys { "${it.key}_result" }, submissionId)
        }.getOrElse {
            return AgenticApiResult.Error(
                AgenticApiErrorType.Parse,
                "Stored assessment data is malformed."
            )
        }
        return call { api.persistAssessment(payload) }
    }

    override suspend fun latestAssessment(): AgenticApiResult<AssessmentResultResponse> {
        if (accessTokenProvider().isNullOrBlank()) return AgenticApiResult.Error(
            AgenticApiErrorType.AuthenticationRequired, "Sign in before restoring assessments."
        )
        return call { api.latestAssessment() }
    }

    private suspend fun call(request: suspend () -> Response<AssessmentResultResponse>): AgenticApiResult<AssessmentResultResponse> {
        return try {
            val response = request()
            if (response.isSuccessful) {
                response.body()?.let { AgenticApiResult.Success(it) }
                    ?: AgenticApiResult.Error(AgenticApiErrorType.Parse, "The server returned an empty assessment response.")
            } else {
                AgenticApiResult.Error(response.errorType(), response.errorMessage())
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: SocketTimeoutException) {
            AgenticApiResult.Error(AgenticApiErrorType.Timeout, "Assessment sync timed out. It remains saved locally.")
        } catch (_: IOException) {
            AgenticApiResult.Error(AgenticApiErrorType.Network, "Network is unavailable. Assessment remains saved locally.")
        } catch (_: RuntimeException) {
            AgenticApiResult.Error(AgenticApiErrorType.Parse, "The server assessment response could not be read.")
        }
    }
}

private fun <T> Response<T>.errorType(): AgenticApiErrorType {
    return when (code()) {
        400 -> AgenticApiErrorType.BadRequest
        401 -> AgenticApiErrorType.AuthenticationExpired
        403 -> AgenticApiErrorType.AuthorizationDenied
        404 -> AgenticApiErrorType.NotFound
        409 -> AgenticApiErrorType.BadRequest
        in 500..599 -> AgenticApiErrorType.BackendFailure
        else -> AgenticApiErrorType.Unknown
    }
}

private fun <T> Response<T>.errorMessage(): String {
    return when (code()) {
        400 -> "The canonical assessment payload is missing or malformed."
        401 -> "Your session expired. Please sign in again."
        403 -> "You are not allowed to save this assessment."
        409 -> "Assessment submission conflicts with server data. Local results are preserved; retry or restore the latest assessment."
        in 500..599 -> "The backend could not save the assessment right now."
        else -> "Assessment sync failed with HTTP ${code()}."
    }
}
