package com.example.vedaahar.network.dto

data class AuthRequest(
    val identifier: String,
    val password: String
)

data class TokenRefreshRequest(
    val refresh: String
)

data class TokenRefreshResponse(
    val access: String
)

data class AuthUserDto(
    val id: Long,
    val username: String?,
    val email: String?
)

data class AuthResponse(
    val access: String,
    val refresh: String,
    val user: AuthUserDto?
)
