package com.nexus.platform.feature.library.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.platform.data.local.GameEngagementStore
import com.nexus.platform.domain.model.GameItem
import com.nexus.platform.domain.usecase.GetApprovedGamesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val games: List<GameItem> = emptyList(),
    val currentPlayingGame: GameItem? = null,
    val recentGames: List<GameItem> = emptyList(),
    val myGames: List<GameItem> = emptyList()
)

private const val ERROR_LOAD_GAMES_FAILED = "__error_load_games_failed__"

class LibraryViewModel(
    private val getApprovedGamesUseCase: GetApprovedGamesUseCase,
    private val engagementStore: GameEngagementStore
) : ViewModel() {
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    fun load() {
        _uiState.update { it.copy(loading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { getApprovedGamesUseCase() }
                .onSuccess { games ->
                    val display = buildDisplayData(games)
                    _uiState.update {
                        it.copy(
                            loading = false,
                            games = games,
                            currentPlayingGame = display.currentPlaying,
                            recentGames = display.recent,
                            myGames = display.myGames
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            errorMessage = e.message ?: ERROR_LOAD_GAMES_FAILED
                        )
                    }
                }
        }
    }

    fun markPlayed(game: GameItem) {
        engagementStore.markPlayed(game.id)
        val games = _uiState.value.games
        if (games.isNotEmpty()) {
            val display = buildDisplayData(games)
            _uiState.update {
                it.copy(
                    currentPlayingGame = display.currentPlaying,
                    recentGames = display.recent,
                    myGames = display.myGames
                )
            }
        }
    }

    fun refreshLocalOrder() {
        val games = _uiState.value.games
        if (games.isEmpty()) {
            return
        }
        val display = buildDisplayData(games)
        _uiState.update {
            it.copy(
                currentPlayingGame = display.currentPlaying,
                recentGames = display.recent,
                myGames = display.myGames
            )
        }
    }

    private data class DisplayData(
        val currentPlaying: GameItem?,
        val recent: List<GameItem>,
        val myGames: List<GameItem>
    )

    private fun buildDisplayData(games: List<GameItem>): DisplayData {
        val gameMap = games.associateBy { it.id }
        val recent = engagementStore.getRecentPlayedGameIdsDesc()
            .mapNotNull { gameMap[it] }
        val myGames = engagementStore.getFavoriteGameIdsDesc()
            .mapNotNull { gameMap[it] }
        val current = engagementStore.getCurrentPlayingGameId()
            ?.let { gameMap[it] }
            ?: recent.firstOrNull()
        return DisplayData(
            currentPlaying = current,
            recent = recent,
            myGames = myGames
        )
    }
}
