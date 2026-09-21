package com.example.vedaahar.auth

import com.example.vedaahar.agentic.AgenticApiErrorType
import com.example.vedaahar.agentic.AgenticApiResult
import com.example.vedaahar.network.AuthApi
import com.example.vedaahar.network.TokenStorage
import com.example.vedaahar.network.dto.AuthRequest
import com.example.vedaahar.network.dto.AuthResponse
import com.example.vedaahar.network.dto.TokenRefreshRequest
import retrofit2.Response
import java.io.IOException

class AuthRepository(
    private val authApi: AuthApi,
    private val tokenStore: TokenStorage
) {
    suspend fun login(identifier: String, password: String): AgenticApiResult<AuthResponse> {
        return authenticate { authApi.login(AuthRequest(identifier, password)) }
    }

    suspend fun register(identifier: String, password: String): AgenticApiResult<AuthResponse> {
        return authenticate { authApi.register(AuthRequest(identifier, password)) }
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

    private suspend fun authenticate(block: suspend () -> Response<AuthResponse>): AgenticApiResult<AuthResponse> {
        return try {
            val response = block()
            if (response.isSuccessful) {
                val body = response.body()
                if (body == null) {
                    AgenticApiResult.Error(AgenticApiErrorType.Parse, "Authentication response was empty.")
                } else {
                    tokenStore.saveTokens(body.access, body.refresh)
                    AgenticApiResult.Success(body)
                }
            } else {
                val type = if (response.code() in 400..499) AgenticApiErrorType.AuthenticationExpired else AgenticApiErrorType.BackendFailure
                AgenticApiResult.Error(type, "Invalid credentials or authentication service unavailable.")
            }
        } catch (_: IOException) {
            AgenticApiResult.Error(AgenticApiErrorType.Network, "Network is unavailable or the backend cannot be reached.")
        } catch (_: RuntimeException) {
            AgenticApiResult.Error(AgenticApiErrorType.Parse, "Authentication response could not be read.")
        }
    }
}
