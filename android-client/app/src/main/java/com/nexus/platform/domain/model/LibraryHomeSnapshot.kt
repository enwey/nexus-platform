package com.nexus.platform.domain.model

data class LibraryHomeSnapshot(
    val currentPlayingGame: GameItem? = null,
    val recentGames: List<GameItem> = emptyList(),
    val myGames: List<GameItem> = emptyList(),
    val newbieMustPlay: List<GameItem> = emptyList(),
    val everyonePlaying: List<GameItem> = emptyList(),
    val favoriteCount: Long = 0L,
    val shareCount: Long = 0L
)
