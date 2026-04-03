package com.nexus.platform.domain.model

data class DiscoverHeroCard(
    val appId: String = "",
    val title: String,
    val subtitle: String = ""
)

data class DiscoverHomeSnapshot(
    val hero: DiscoverHeroCard? = null,
    val rankedGames: List<GameItem> = emptyList(),
    val newbieMustPlay: List<GameItem> = emptyList(),
    val everyonePlaying: List<GameItem> = emptyList()
)
