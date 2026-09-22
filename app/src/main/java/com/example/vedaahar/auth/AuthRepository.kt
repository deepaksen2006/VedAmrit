package com.example.vedaahar.auth

import com.example.vedaahar.agentic.AgenticApiErrorType
import com.example.vedaahar.agentic.AgenticApiResult
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApiNotAvailableException
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AuthRepository(
    private val gateway: FirebaseAuthGateway = FirebaseAuthGateway.Live()
) {
    constructor(firebaseAuth: FirebaseAuth) : this(FirebaseAuthGateway.Live(firebaseAuth))

    suspend fun login(email: String, password: String): AgenticApiResult<Unit> {
        return authenticate {
            gateway.signIn(email.trim(), password)
        }
    }

    suspend fun register(email: String, password: String): AgenticApiResult<Unit> {
        return authenticate {
            gateway.createAccount(email.trim(), password)
        }
    }

    fun isSessionAvailable(): Boolean = gateway.hasCurrentUser()

    fun logout() {
        gateway.signOut()
    }

    private suspend fun authenticate(block: suspend () -> Unit): AgenticApiResult<Unit> {
        return try {
            block()
            AgenticApiResult.Success(Unit)
        } catch (exception: Exception) {
            exception.toAuthError()
        }
    }

    private fun Exception.toAuthError(): AgenticApiResult.Error {
        return when (this) {
            is FirebaseAuthUserCollisionException -> AgenticApiResult.Error(
                AgenticApiErrorType.BadRequest,
                "An account already exists for this email."
            )
            is FirebaseAuthWeakPasswordException -> AgenticApiResult.Error(
                AgenticApiErrorType.BadRequest,
                reason?.takeIf { it.isNotBlank() } ?: "Use a stronger password."
            )
            is FirebaseAuthInvalidUserException -> AgenticApiResult.Error(
                AgenticApiErrorType.AuthenticationExpired,
                "No Firebase account exists for this email."
            )
            is FirebaseAuthInvalidCredentialsException -> AgenticApiResult.Error(
                AgenticApiErrorType.AuthenticationExpired,
                firebaseCredentialMessage()
            )
            is FirebaseNetworkException -> AgenticApiResult.Error(
                AgenticApiErrorType.Network,
                "Network is unavailable. Check your connection and try again."
            )
            is FirebaseApiNotAvailableException -> AgenticApiResult.Error(
                AgenticApiErrorType.BackendFailure,
                "Firebase Authentication is not available on this device."
            )
            is FirebaseException -> AgenticApiResult.Error(
                AgenticApiErrorType.BackendFailure,
                "Firebase Authentication is not configured correctly."
            )
            else -> AgenticApiResult.Error(
                AgenticApiErrorType.Unknown,
                "Authentication failed. Please try again."
            )
        }
    }

    private fun FirebaseAuthInvalidCredentialsException.firebaseCredentialMessage(): String {
        return when (errorCode) {
            "ERROR_INVALID_EMAIL" -> "Enter a valid email address."
            "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> "Incorrect email or password."
            else -> "Incorrect email or password."
        }
    }
}

interface FirebaseAuthGateway {
    suspend fun signIn(email: String, password: String)
    suspend fun createAccount(email: String, password: String)
    fun hasCurrentUser(): Boolean
    fun signOut()

    class Live(
        private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    ) : FirebaseAuthGateway {
        override suspend fun signIn(email: String, password: String) {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
        }

        override suspend fun createAccount(email: String, password: String) {
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        }

        override fun hasCurrentUser(): Boolean = firebaseAuth.currentUser != null

        override fun signOut() {
            firebaseAuth.signOut()
        }
    }
}

private suspend fun <T> Task<T>.await(): T {
    return suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(
                    task.exception ?: IllegalStateException("Firebase task failed.")
                )
            }
        }
    }
}
