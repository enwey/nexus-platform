package com.nexus.platform.domain.model

data class GameRuntimeProfile(
    val appId: String = "",
    val gameName: String = "",
    val studioName: String = "",
    val playerCountText: String = "",
    val runtimeBannerUrl: String = "",
    val runtimeLogoUrl: String = "",
    val shareTitle: String = "",
    val shareSubtitle: String = "",
    val shareImageUrl: String = ""
)
