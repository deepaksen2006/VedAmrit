package com.example.vedaahar.auth

import com.example.vedaahar.agentic.AgenticApiErrorType
import com.example.vedaahar.agentic.AgenticApiResult
import com.example.vedaahar.network.AuthApi
import com.example.vedaahar.network.TokenStorage
import com.example.vedaahar.network.dto.AuthRequest
import com.example.vedaahar.network.dto.AuthResponse
import com.example.vedaahar.network.dto.TokenRefreshRequest
import com.google.gson.JsonSyntaxException
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

    suspend fun refreshSession(): Boolean {
        val refresh = tokenStore.getRefreshToken() ?: return false
        return runCatching {
            val response = authApi.refresh(TokenRefreshRequest(refresh))
            if (response.isSuccessful) {
                val access = response.body()?.access
                if (access.isNullOrBlank()) {
                    tokenStore.clear()
                    false
                } else {
                    tokenStore.saveTokens(accessToken = access, refreshToken = refresh)
                    true
                }
            } else {
                tokenStore.clear()
                false
            }
        }.getOrElse {
            false
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
                if (saveSession) {
                    tokenStore.saveTokens(body.access, body.refresh)
                }
                AgenticApiResult.Success(body)
            } else {
                AgenticApiResult.Error(response.errorType(), response.errorMessage())
            }
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
        return when (code()) {
            400 -> "The email or password is invalid, or the account already exists."
            401 -> "Incorrect email or password."
            403 -> "Authentication was rejected by the backend."
            in 500..599 -> "The authentication service returned HTTP ${code()}. Check Django and PostgreSQL."
            else -> "Authentication failed with HTTP ${code()}."
        }
    }
}
