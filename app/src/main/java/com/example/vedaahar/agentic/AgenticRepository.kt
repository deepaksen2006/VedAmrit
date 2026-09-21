package com.example.vedaahar.agentic

import com.example.vedaahar.network.AgenticApi
import com.example.vedaahar.network.dto.AgenticPlanRequest
import com.example.vedaahar.network.dto.FinalReportResponse
import com.example.vedaahar.network.dto.WellnessPlanResponse
import com.example.vedaahar.network.dto.WellnessPlanStatusResponse
import retrofit2.Response
import java.io.IOException

sealed class AgenticApiResult<out T> {
    data class Success<T>(val value: T) : AgenticApiResult<T>()
    data class Error(val type: AgenticApiErrorType, val message: String) : AgenticApiResult<Nothing>()
}

enum class AgenticApiErrorType {
    AuthenticationRequired,
    AuthenticationExpired,
    AuthorizationDenied,
    BadRequest,
    NotFound,
    ReportNotReady,
    BackendFailure,
    Network,
    Timeout,
    Parse,
    Unknown
}

class AgenticRepository(
    private val api: AgenticApi,
    private val accessTokenProvider: () -> String?
) {
    suspend fun generatePlan(request: AgenticPlanRequest): AgenticApiResult<WellnessPlanResponse> {
        if (accessTokenProvider().isNullOrBlank()) {
            return AgenticApiResult.Error(
                AgenticApiErrorType.AuthenticationRequired,
                "Django login is required before generating an AI wellness plan."
            )
        }
        return call { api.generatePlan(request) }
    }

    suspend fun plans(): AgenticApiResult<List<WellnessPlanResponse>> = authenticatedCall { api.plans() }

    suspend fun plan(id: Long): AgenticApiResult<WellnessPlanResponse> = authenticatedCall { api.plan(id) }

    suspend fun planStatus(id: Long): AgenticApiResult<WellnessPlanStatusResponse> = authenticatedCall { api.planStatus(id) }

    suspend fun finalReport(id: Long): AgenticApiResult<FinalReportResponse> = authenticatedCall { api.finalReport(id) }

    private suspend fun <T> authenticatedCall(block: suspend () -> Response<T>): AgenticApiResult<T> {
        if (accessTokenProvider().isNullOrBlank()) {
            return AgenticApiResult.Error(
                AgenticApiErrorType.AuthenticationRequired,
                "Django login is required."
            )
        }
        return call(block)
    }

    private suspend fun <T> call(block: suspend () -> Response<T>): AgenticApiResult<T> {
        return try {
            val response = block()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) AgenticApiResult.Success(body)
                else AgenticApiResult.Error(AgenticApiErrorType.Parse, "The server returned an empty response.")
            } else {
                response.toError()
            }
        } catch (_: java.net.SocketTimeoutException) {
            AgenticApiResult.Error(AgenticApiErrorType.Timeout, "The request timed out. Please try again.")
        } catch (_: IOException) {
            AgenticApiResult.Error(AgenticApiErrorType.Network, "Network is unavailable or the backend cannot be reached.")
        } catch (_: RuntimeException) {
            AgenticApiResult.Error(AgenticApiErrorType.Parse, "The server response could not be read.")
        }
    }

    private fun <T> Response<T>.toError(): AgenticApiResult.Error {
        val message = when (code()) {
            400 -> "The profile or assessment data is incomplete."
            401 -> "Your Django session has expired. Please sign in again."
            403 -> "You are not authorized to access this plan."
            404 -> "The requested wellness plan was not found."
            409 -> "The final report is not ready yet."
            in 500..599 -> "The AI wellness service is currently unavailable."
            else -> "Unexpected server response (${code()})."
        }
        val type = when (code()) {
            400 -> AgenticApiErrorType.BadRequest
            401 -> AgenticApiErrorType.AuthenticationExpired
            403 -> AgenticApiErrorType.AuthorizationDenied
            404 -> AgenticApiErrorType.NotFound
            409 -> AgenticApiErrorType.ReportNotReady
            in 500..599 -> AgenticApiErrorType.BackendFailure
            else -> AgenticApiErrorType.Unknown
        }
        return AgenticApiResult.Error(type, message)
    }
}
