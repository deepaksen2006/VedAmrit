package com.example.vedaahar.network

import android.content.Context
import android.content.SharedPreferences

interface TokenStorage {
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun hasTokens(): Boolean
    fun saveTokens(accessToken: String, refreshToken: String? = null)
    fun clear()
}

class AuthTokenStore internal constructor(private val preferences: SharedPreferences) : TokenStorage {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PrefName, Context.MODE_PRIVATE)
    )

    override fun getAccessToken(): String? = preferences.getString(AccessToken, null)

    override fun getRefreshToken(): String? = preferences.getString(RefreshToken, null)

    override fun hasTokens(): Boolean = !getAccessToken().isNullOrBlank() && !getRefreshToken().isNullOrBlank()

    override fun saveTokens(accessToken: String, refreshToken: String?) {
        require(accessToken.isNotBlank()) { "Access token must not be blank." }
        require(refreshToken == null || refreshToken.isNotBlank()) { "Refresh token must not be blank." }
        preferences.edit()
            .putString(AccessToken, accessToken)
            .apply {
                if (refreshToken != null) putString(RefreshToken, refreshToken)
            }
            .apply()
    }

    override fun clear() {
        preferences.edit().clear().apply()
    }

    companion object {
        private const val PrefName = "vedaahar_django_jwt"
        private const val AccessToken = "access_token"
        private const val RefreshToken = "refresh_token"
    }
}
