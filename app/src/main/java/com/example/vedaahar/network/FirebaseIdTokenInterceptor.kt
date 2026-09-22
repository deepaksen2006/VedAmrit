package com.example.vedaahar.network

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

class FirebaseIdTokenInterceptor(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val firebaseUser = firebaseAuth.currentUser
        val token = firebaseUser?.let { user ->
            runCatching {
                Tasks.await(user.getIdToken(false), TokenTimeoutSeconds, TimeUnit.SECONDS).token
            }.getOrNull()
        }

        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }

    private companion object {
        private const val TokenTimeoutSeconds = 10L
    }
}
