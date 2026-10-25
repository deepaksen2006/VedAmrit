package com.example.vedaahar.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
    data object AccountCreated : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(
    application: Application,
    private val repository: AuthRepository = AuthRepository(
        authApi = ApiClient.authApi(),
        tokenStore = AuthTokenStore(application)
    )
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    fun restoreSession(onAuthenticated: () -> Unit, onUnauthenticated: () -> Unit = {}) {
        if (_uiState.value is AuthUiState.Loading) return
        if (!repository.isSessionAvailable()) {
            _uiState.value = AuthUiState.Idle
            onUnauthenticated()
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            when (val result = repository.refreshSession()) {
                is AgenticApiResult.Success -> {
                    _uiState.value = AuthUiState.Authenticated
                    onAuthenticated()
                }
                is AgenticApiResult.Error -> {
                    _uiState.value = AuthUiState.Error(result.message)
                    onUnauthenticated()
                }
            }
        }
    }

    fun login(identifier: String, password: String, onAuthenticated: () -> Unit) {
        authenticate(identifier, password, register = false, onAuthenticated = onAuthenticated)
    }

    fun register(identifier: String, password: String, onAuthenticated: () -> Unit = {}) {
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
            val result = if (register) {
                repository.register(identifier, password)
            } else {
                repository.login(identifier, password)
            }
            when (result) {
                is AgenticApiResult.Success -> {
                    if (register) {
                        _uiState.value = AuthUiState.AccountCreated
                    } else {
                        _uiState.value = AuthUiState.Authenticated
                        onAuthenticated()
                    }
                }
                is AgenticApiResult.Error -> {
                    _uiState.value = AuthUiState.Error(result.message)
                }
            }
        }
    }

    class Factory(
        private val application: Application,
        private val repository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                return AuthViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
