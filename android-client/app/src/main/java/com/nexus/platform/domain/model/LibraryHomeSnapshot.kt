package com.nexus.platform.domain.model

data class LibraryHomeSnapshot(
    val currentPlayingGame: GameItem? = null,
    val recentGames: List<GameItem> = emptyList(),
    val myGames: List<GameItem> = emptyList()
)
