package com.nexus.platform.domain.model

data class UserProfileDetail(
    val id: Long,
    val username: String,
    val email: String,
    val phone: String,
    val role: String,
    val displayName: String,
    val avatarUrl: String,
    val languageTag: String
)
