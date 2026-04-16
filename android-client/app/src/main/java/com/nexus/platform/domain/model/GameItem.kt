package com.nexus.platform.domain.model

import java.io.Serializable

data class GameItem(
    val id: String,
    val name: String,
    val description: String,
    val iconUrl: String,
    val downloadUrl: String,
    val version: String,
    val md5: String = "",
    val category: String = "",
    val requiresOnline: Boolean = false,
    val localizedNames: Map<String, String> = emptyMap(),
    val localizedDescriptions: Map<String, String> = emptyMap()
) : Serializable
