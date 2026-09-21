package com.example.vedaahar.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vedaahar.agentic.AgenticApiResult
import com.example.vedaahar.network.ApiClient
import com.example.vedaahar.network.AuthTokenStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data object Authenticated : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(
    application: Application,
    private val repository: AuthRepository = AuthRepository(
        authApi = ApiClient.authApi(),
        tokenStore = AuthTokenStore(application)
    )
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow<AuthUiState>(
        if (repository.isSessionAvailable()) AuthUiState.Authenticated else AuthUiState.Idle
    )
    val uiState: StateFlow<AuthUiState> = _uiState

    fun restoreSession(onAuthenticated: () -> Unit) {
        if (!repository.isSessionAvailable()) {
            _uiState.value = AuthUiState.Idle
            return
        }
        viewModelScope.launch {
            if (repository.refreshSession()) {
                _uiState.value = AuthUiState.Authenticated
                onAuthenticated()
            } else {
                _uiState.value = AuthUiState.Idle
            }
        }
    }

    fun login(identifier: String, password: String, onAuthenticated: () -> Unit) {
        authenticate(identifier, password, register = false, onAuthenticated = onAuthenticated)
    }

    fun register(identifier: String, password: String, onAuthenticated: () -> Unit) {
        authenticate(identifier, password, register = true, onAuthenticated = onAuthenticated)
    }

    fun logout() {
        repository.logout()
        _uiState.value = AuthUiState.Idle
    }

    private fun authenticate(
        identifier: String,
        password: String,
        register: Boolean,
        onAuthenticated: () -> Unit
    ) {
        if (_uiState.value is AuthUiState.Loading) return
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = if (register) repository.register(identifier, password) else repository.login(identifier, password)
            when (result) {
                is AgenticApiResult.Success -> {
                    _uiState.value = AuthUiState.Authenticated
                    onAuthenticated()
                }
                is AgenticApiResult.Error -> {
                    _uiState.value = AuthUiState.Error(result.message)
                }
            }
        }
    }
}
