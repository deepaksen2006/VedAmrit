package com.example.vedaahar.auth

import com.example.vedaahar.agentic.AgenticApiErrorType
import com.example.vedaahar.agentic.AgenticApiResult
import com.example.vedaahar.network.AuthApi
import com.example.vedaahar.network.TokenStorage
import com.example.vedaahar.network.dto.AuthRequest
import com.example.vedaahar.network.dto.AuthResponse
import com.example.vedaahar.network.dto.TokenRefreshRequest
import com.google.gson.JsonSyntaxException
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException

class AuthRepository(
    private val authApi: AuthApi,
    private val tokenStore: TokenStorage
) {
    suspend fun login(email: String, password: String): AgenticApiResult<AuthResponse> {
        return authenticate(saveSession = true) {
            authApi.login(AuthRequest(email.trim(), password))
        }
    }

    suspend fun register(email: String, password: String): AgenticApiResult<AuthResponse> {
        return authenticate(saveSession = false) {
            authApi.register(AuthRequest(email.trim(), password))
        }
    }

    suspend fun refreshSession(): AgenticApiResult<Unit> {
        val refresh = tokenStore.getRefreshToken()?.takeIf { it.isNotBlank() }
        if (refresh == null) {
            tokenStore.clear()
            return AgenticApiResult.Error(AgenticApiErrorType.AuthenticationRequired, "Please sign in to continue.")
        }
        return try {
            val response = authApi.refresh(TokenRefreshRequest(refresh))
            if (response.isSuccessful) {
                val access = response.body()?.access
                if (access.isNullOrBlank()) {
                    tokenStore.clear()
                    AgenticApiResult.Error(AgenticApiErrorType.Parse, "The server returned an invalid session. Please sign in again.")
                } else {
                    tokenStore.saveTokens(accessToken = access, refreshToken = refresh)
                    AgenticApiResult.Success(Unit)
                }
            } else {
                if (response.code() in setOf(400, 401, 403)) {
                    tokenStore.clear()
                    AgenticApiResult.Error(AgenticApiErrorType.AuthenticationExpired, "Your session expired. Please sign in again.")
                } else {
                    AgenticApiResult.Error(response.errorType(), "Could not restore your session. Please retry signing in when Django is available.")
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: SocketTimeoutException) {
            AgenticApiResult.Error(AgenticApiErrorType.Timeout, "Session restore timed out. Check your connection and try again.")
        } catch (_: IOException) {
            AgenticApiResult.Error(AgenticApiErrorType.Network, "Could not reach Django to restore your session. Check your connection and try again.")
        } catch (_: RuntimeException) {
            AgenticApiResult.Error(AgenticApiErrorType.Parse, "The server session response could not be read. Please sign in again.")
        }
    }

    fun isSessionAvailable(): Boolean = tokenStore.hasTokens()

    fun logout() {
        tokenStore.clear()
    }

    private suspend fun authenticate(
        saveSession: Boolean,
        block: suspend () -> Response<AuthResponse>
    ): AgenticApiResult<AuthResponse> {
        return try {
            val response = block()
            if (response.isSuccessful) {
                val body = response.body()
                    ?: return AgenticApiResult.Error(
                        AgenticApiErrorType.Parse,
                        "Authentication response was empty."
                    )
                if (body.access.isNullOrBlank() || body.refresh.isNullOrBlank()) {
                    return AgenticApiResult.Error(
                        AgenticApiErrorType.Parse,
                        "Authentication response did not contain valid session tokens. Please try again."
                    )
                }
                if (saveSession) {
                    tokenStore.saveTokens(body.access, body.refresh)
                }
                AgenticApiResult.Success(body)
            } else {
                AgenticApiResult.Error(response.errorType(), response.errorMessage())
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: SocketTimeoutException) {
            AgenticApiResult.Error(
                AgenticApiErrorType.Timeout,
                "The authentication request timed out. Check that Django is running and reachable from your phone."
            )
        } catch (_: ConnectException) {
            AgenticApiResult.Error(
                AgenticApiErrorType.Network,
                "Could not connect to Django. Check the API IP address, server, and Windows Firewall."
            )
        } catch (_: IOException) {
            AgenticApiResult.Error(
                AgenticApiErrorType.Network,
                "Network is unavailable or the backend cannot be reached."
            )
        } catch (_: JsonSyntaxException) {
            AgenticApiResult.Error(
                AgenticApiErrorType.Parse,
                "Authentication response could not be read."
            )
        } catch (_: RuntimeException) {
            AgenticApiResult.Error(
                AgenticApiErrorType.Unknown,
                "Authentication failed. Please try again."
            )
        }
    }

    private fun <T> Response<T>.errorType(): AgenticApiErrorType {
        return when (code()) {
            400 -> AgenticApiErrorType.BadRequest
            401 -> AgenticApiErrorType.AuthenticationExpired
            403 -> AgenticApiErrorType.AuthorizationDenied
            in 500..599 -> AgenticApiErrorType.BackendFailure
            else -> AgenticApiErrorType.Unknown
        }
    }

    private fun <T> Response<T>.errorMessage(): String {
        if (code() in setOf(400, 401, 403)) {
            val validationMessage = runCatching {
                val json = JsonParser.parseString(errorBody()?.string().orEmpty()).asJsonObject
                listOf("detail", "non_field_errors", "identifier", "password").mapNotNull { field ->
                    val value = json.get(field) ?: return@mapNotNull null
                    val message = if (value.isJsonArray) {
                        value.asJsonArray.filter { it.isJsonPrimitive }.joinToString(" ") { it.asString }
                    } else if (value.isJsonPrimitive) value.asString else ""
                    if (message.isBlank()) null else when (field) {
                        "identifier" -> "Email: $message"
                        "password" -> "Password: $message"
                        else -> message
                    }
                }.joinToString(" ").take(400)
            }.getOrNull()
            if (!validationMessage.isNullOrBlank()) return validationMessage
        }
        return when (code()) {
            400 -> "The email or password is invalid, or the account already exists."
            401 -> "Incorrect email or password."
            403 -> "Authentication was rejected by the backend."
            in 500..599 -> "The authentication service returned HTTP ${code()}. Check Django and PostgreSQL."
            else -> "Authentication failed with HTTP ${code()}."
        }
    }
}
