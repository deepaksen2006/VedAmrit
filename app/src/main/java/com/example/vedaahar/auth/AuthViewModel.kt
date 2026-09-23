package com.example.vedaahar.auth

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vedaahar.BuildConfig
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
    private val localSession by lazy { LocalDevelopmentSessionStore(application) }
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    fun restoreSession(onAuthenticated: () -> Unit, onUnauthenticated: () -> Unit = {}) {
        if (_uiState.value is AuthUiState.Loading) return
        if (BuildConfig.LOCAL_NO_AUTH_MODE) {
            if (localSession.isActive()) {
                _uiState.value = AuthUiState.Authenticated
                onAuthenticated()
            } else {
                _uiState.value = AuthUiState.Idle
                onUnauthenticated()
            }
            return
        }
        if (!repository.isSessionAvailable()) {
            _uiState.value = AuthUiState.Idle
            onUnauthenticated()
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            if (repository.refreshSession()) {
                _uiState.value = AuthUiState.Authenticated
                onAuthenticated()
            } else {
                _uiState.value = AuthUiState.Idle
                onUnauthenticated()
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
        localSession.clear()
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
        if (BuildConfig.LOCAL_NO_AUTH_MODE) {
            localSession.start()
            _uiState.value = AuthUiState.Authenticated
            onAuthenticated()
            return
        }
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

private class LocalDevelopmentSessionStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PrefName, Context.MODE_PRIVATE)

    fun isActive(): Boolean = preferences.getBoolean(IsActive, false)

    fun start() {
        preferences.edit().putBoolean(IsActive, true).apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    companion object {
        private const val PrefName = "vedamrit_local_no_auth_session"
        private const val IsActive = "is_active"
    }
}
