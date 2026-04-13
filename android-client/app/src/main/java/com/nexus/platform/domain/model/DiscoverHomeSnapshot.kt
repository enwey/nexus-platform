package com.nexus.platform.domain.model

data class DiscoverHeroCard(
    val appId: String = "",
    val title: String,
    val subtitle: String = "",
    val coverUrl: String = "",
    val badgeText: String = ""
)

data class DiscoverCategory(
    val key: String,
    val label: String
)

data class DiscoverHomeSnapshot(
    val hero: DiscoverHeroCard? = null,
    val libraryTopBanner: DiscoverHeroCard? = null,
    val categories: List<DiscoverCategory> = emptyList(),
    val rankedGames: List<GameItem> = emptyList(),
    val newbieMustPlay: List<GameItem> = emptyList(),
    val everyonePlaying: List<GameItem> = emptyList()
)
