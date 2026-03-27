package com.nexus.platform.domain.model

import java.io.Serializable

data class GameItem(
    val id: String,
    val name: String,
    val description: String,
    val iconUrl: String,
    val downloadUrl: String,
    val version: String,
    val md5: String = ""
) : Serializable
