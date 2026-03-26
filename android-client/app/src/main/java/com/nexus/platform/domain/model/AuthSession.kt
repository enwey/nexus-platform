package com.nexus.platform.domain.model

data class AuthSession(
    val accessToken: String,
    val refreshToken: String
)
