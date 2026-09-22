package com.example.vedaahar.auth

import com.example.vedaahar.agentic.AgenticApiErrorType
import com.example.vedaahar.agentic.AgenticApiResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryTest {
    @Test
    fun successfulLoginUsesFirebaseAndStartsSession() = runTest {
        val gateway = FakeFirebaseAuthGateway()
        val repository = AuthRepository(gateway)

        val result = repository.login("patient@example.com", "password")

        assertTrue(result is AgenticApiResult.Success)
        assertEquals("patient@example.com", gateway.lastLoginEmail)
        assertTrue(repository.isSessionAvailable())
    }

    @Test
    fun successfulRegistrationUsesFirebaseAndStartsSession() = runTest {
        val gateway = FakeFirebaseAuthGateway()
        val repository = AuthRepository(gateway)

        val result = repository.register("new@example.com", "password")

        assertTrue(result is AgenticApiResult.Success)
        assertEquals("new@example.com", gateway.lastCreateEmail)
        assertTrue(repository.isSessionAvailable())
    }

    @Test
    fun firebaseFailureReturnsErrorResult() = runTest {
        val repository = AuthRepository(
            FakeFirebaseAuthGateway(loginError = RuntimeException("failed"))
        )

        val result = repository.login("patient@example.com", "password")

        assertTrue(result is AgenticApiResult.Error)
        assertEquals(AgenticApiErrorType.Unknown, (result as AgenticApiResult.Error).type)
    }

    @Test
    fun logoutSignsOutFirebase() {
        val gateway = FakeFirebaseAuthGateway(hasUser = true)
        val repository = AuthRepository(gateway)

        repository.logout()

        assertFalse(repository.isSessionAvailable())
        assertTrue(gateway.signedOut)
    }

    private class FakeFirebaseAuthGateway(
        private var hasUser: Boolean = false,
        private val loginError: Exception? = null,
        private val createError: Exception? = null
    ) : FirebaseAuthGateway {
        var lastLoginEmail: String? = null
            private set
        var lastCreateEmail: String? = null
            private set
        var signedOut: Boolean = false
            private set

        override suspend fun signIn(email: String, password: String) {
            loginError?.let { throw it }
            lastLoginEmail = email
            hasUser = true
        }

        override suspend fun createAccount(email: String, password: String) {
            createError?.let { throw it }
            lastCreateEmail = email
            hasUser = true
        }

        override fun hasCurrentUser(): Boolean = hasUser

        override fun signOut() {
            signedOut = true
            hasUser = false
        }
    }
}
