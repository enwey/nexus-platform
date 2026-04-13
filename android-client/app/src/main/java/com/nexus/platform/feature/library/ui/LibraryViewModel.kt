package com.nexus.platform.feature.library.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.platform.data.local.GameCatalogCacheStore
import com.nexus.platform.data.local.GameEngagementStore
import com.nexus.platform.domain.model.DiscoverCategory
import com.nexus.platform.domain.model.DiscoverHeroCard
import com.nexus.platform.domain.model.GameItem
import com.nexus.platform.domain.model.LibraryHomeSnapshot
import com.nexus.platform.domain.usecase.GetApprovedGamesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class LibraryUiState(
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val games: List<GameItem> = emptyList(),
    val discoverGames: List<GameItem> = emptyList(),
    val discoverHero: DiscoverHeroCard? = null,
    val libraryTopBanner: DiscoverHeroCard? = null,
    val discoverCategories: List<DiscoverCategory> = emptyList(),
    val discoverNewbie: List<GameItem> = emptyList(),
    val discoverEveryone: List<GameItem> = emptyList(),
    val currentPlayingGame: GameItem? = null,
    val recentGames: List<GameItem> = emptyList(),
    val myGames: List<GameItem> = emptyList(),
    val newbieMustPlay: List<GameItem> = emptyList(),
    val everyonePlaying: List<GameItem> = emptyList(),
    val favoriteCount: Long = 0L,
    val shareCount: Long = 0L
)

private const val ERROR_LOAD_GAMES_FAILED = "__error_load_games_failed__"

class LibraryViewModel(
    private val getApprovedGamesUseCase: GetApprovedGamesUseCase,
    private val engagementStore: GameEngagementStore,
    private val catalogCacheStore: GameCatalogCacheStore
) : ViewModel() {
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    fun load() {
        _uiState.update { it.copy(loading = true, errorMessage = null) }
        val cachedGames = catalogCacheStore.loadGames()
        if (cachedGames.isNotEmpty()) {
            val cachedDisplay = buildDisplayData(cachedGames, serverHome = null)
            _uiState.update {
                it.copy(
                    loading = false,
                    games = cachedGames,
                    discoverGames = cachedGames,
                    currentPlayingGame = cachedDisplay.currentPlaying,
                    recentGames = cachedDisplay.recent,
                    myGames = cachedDisplay.myGames,
                    newbieMustPlay = cachedDisplay.newbieMustPlay,
                    everyonePlaying = cachedDisplay.everyonePlaying
                )
            }
        }
        viewModelScope.launch {
            runCatching { getApprovedGamesUseCase() }
                .onSuccess { games ->
                    val serverHome = runCatching { getApprovedGamesUseCase.getLibraryHome() }.getOrNull()
                    val discoverHome = runCatching { getApprovedGamesUseCase.getDiscoverHome() }.getOrNull()
                    val discoverGamesRaw = if (discoverHome != null) {
                        (discoverHome.rankedGames + discoverHome.newbieMustPlay + discoverHome.everyonePlaying)
                            .distinctBy { it.id }
                    } else {
                        runCatching { getApprovedGamesUseCase.getDiscoverGames(category = "all") }.getOrDefault(emptyList())
                    }
                    val discoverGames = if (discoverGamesRaw.isNotEmpty()) discoverGamesRaw else games
                    catalogCacheStore.saveGames(games + discoverGames)
                    val display = buildDisplayData(games, serverHome)
                    val discoverCategories = discoverHome?.categories.orEmpty().ifEmpty {
                        listOf(
                            DiscoverCategory(key = "all", label = "all"),
                            DiscoverCategory(key = "動作射擊", label = "動作射擊"),
                            DiscoverCategory(key = "休閒益智", label = "休閒益智"),
                            DiscoverCategory(key = "角色扮演", label = "角色扮演")
                        )
                    }
                    val coldstartNewbie = if (display.newbieMustPlay.isNotEmpty()) {
                        display.newbieMustPlay
                    } else {
                        discoverHome?.newbieMustPlay.orEmpty()
                    }
                    val coldstartEveryone = if (display.everyonePlaying.isNotEmpty()) {
                        display.everyonePlaying
                    } else {
                        discoverHome?.everyonePlaying.orEmpty()
                    }
                    val randomizedEveryone = coldstartEveryone
                        .take(50)
                        .shuffled(Random(System.currentTimeMillis()))
                    _uiState.update {
                        it.copy(
                            loading = false,
                            games = games,
                            discoverGames = discoverGames,
                            discoverHero = discoverHome?.hero,
                            libraryTopBanner = discoverHome?.libraryTopBanner,
                            discoverCategories = discoverCategories,
                            discoverNewbie = discoverHome?.newbieMustPlay ?: emptyList(),
                            discoverEveryone = discoverHome?.everyonePlaying ?: emptyList(),
                            currentPlayingGame = display.currentPlaying,
                            recentGames = display.recent,
                            myGames = display.myGames,
                            newbieMustPlay = coldstartNewbie,
                            everyonePlaying = randomizedEveryone,
                            favoriteCount = display.favoriteCount,
                            shareCount = display.shareCount
                        )
                    }
                }
                .onFailure { e ->
                    val discoverHome = runCatching { getApprovedGamesUseCase.getDiscoverHome() }.getOrNull()
                    val discoverGamesRaw = if (discoverHome != null) {
                        (discoverHome.rankedGames + discoverHome.newbieMustPlay + discoverHome.everyonePlaying)
                            .distinctBy { it.id }
                    } else {
                        runCatching { getApprovedGamesUseCase.getDiscoverGames(category = "all") }.getOrDefault(emptyList())
                    }
                    val cachedCatalogGames = catalogCacheStore.loadGames()
                    val fallbackGames = (discoverGamesRaw + cachedCatalogGames).distinctBy { it.id }
                    val display = buildDisplayData(fallbackGames, serverHome = null)
                    val discoverCategories = discoverHome?.categories.orEmpty().ifEmpty {
                        listOf(
                            DiscoverCategory(key = "all", label = "all"),
                            DiscoverCategory(key = "動作射擊", label = "動作射擊"),
                            DiscoverCategory(key = "休閒益智", label = "休閒益智"),
                            DiscoverCategory(key = "角色扮演", label = "角色扮演")
                        )
                    }
                    val coldstartNewbie = if (display.newbieMustPlay.isNotEmpty()) {
                        display.newbieMustPlay
                    } else {
                        discoverHome?.newbieMustPlay.orEmpty()
                    }
                    val coldstartEveryone = if (display.everyonePlaying.isNotEmpty()) {
                        display.everyonePlaying
                    } else {
                        discoverHome?.everyonePlaying.orEmpty()
                    }
                    val randomizedEveryone = coldstartEveryone
                        .take(50)
                        .shuffled(Random(System.currentTimeMillis()))
                    _uiState.update {
                        it.copy(
                            loading = false,
                            errorMessage = if (fallbackGames.isEmpty()) (e.message ?: ERROR_LOAD_GAMES_FAILED) else null,
                            games = fallbackGames,
                            discoverGames = fallbackGames,
                            discoverHero = discoverHome?.hero,
                            libraryTopBanner = discoverHome?.libraryTopBanner,
                            discoverCategories = discoverCategories,
                            discoverNewbie = discoverHome?.newbieMustPlay ?: emptyList(),
                            discoverEveryone = discoverHome?.everyonePlaying ?: emptyList(),
                            currentPlayingGame = display.currentPlaying,
                            recentGames = display.recent,
                            myGames = display.myGames,
                            newbieMustPlay = coldstartNewbie,
                            everyonePlaying = randomizedEveryone,
                            favoriteCount = display.favoriteCount,
                            shareCount = display.shareCount
                        )
                    }
                }
        }
    }

    fun markPlayed(game: GameItem) {
        engagementStore.markPlayed(game.id)
        viewModelScope.launch {
            runCatching { getApprovedGamesUseCase.markPlayed(game.id) }
        }
        val games = _uiState.value.games
        if (games.isNotEmpty()) {
            val display = buildDisplayData(games, serverHome = null)
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
        val display = buildDisplayData(games, serverHome = null)
        _uiState.update {
            it.copy(
                currentPlayingGame = display.currentPlaying,
                recentGames = display.recent,
                myGames = display.myGames
            )
        }
    }

    fun toggleMyGame(gameId: String) {
        val nowFavorite = engagementStore.toggleFavorite(gameId)
        refreshLocalOrder()
        viewModelScope.launch {
            runCatching { getApprovedGamesUseCase.setFavorite(gameId, nowFavorite) }
                .onSuccess { synced ->
                    if (!synced) {
                        engagementStore.toggleFavorite(gameId)
                    }
                }
                .onFailure {
                    engagementStore.toggleFavorite(gameId)
                }
            refreshLocalOrder()
        }
    }

    fun loadDiscoverByCategory(category: String) {
        viewModelScope.launch {
            val normalized = category.ifBlank { "all" }
            val result = runCatching {
                getApprovedGamesUseCase.getDiscoverGames(category = normalized)
            }
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(discoverGames = result.getOrDefault(emptyList()))
                }
            } else {
                _uiState.update {
                    it.copy(discoverGames = emptyList())
                }
            }
        }
    }

    private data class DisplayData(
        val currentPlaying: GameItem?,
        val recent: List<GameItem>,
        val myGames: List<GameItem>,
        val newbieMustPlay: List<GameItem>,
        val everyonePlaying: List<GameItem>,
        val favoriteCount: Long,
        val shareCount: Long
    )

    private fun buildDisplayData(games: List<GameItem>, serverHome: LibraryHomeSnapshot?): DisplayData {
        if (serverHome != null) {
            val merged = (games + serverHome.recentGames + serverHome.myGames + listOfNotNull(serverHome.currentPlayingGame))
                .associateBy { it.id }

            val localRecentIds = engagementStore.getRecentPlayedGameIdsDesc()
            val localFavoriteIds = engagementStore.getFavoriteGameIdsDesc()

            val recentIds = linkedSetOf<String>().apply {
                addAll(serverHome.recentGames.map { it.id })
                addAll(localRecentIds)
            }
            val myGameIds = linkedSetOf<String>().apply {
                addAll(serverHome.myGames.map { it.id })
                addAll(localFavoriteIds)
            }

            val recent = recentIds.mapNotNull { merged[it] }
            val myGames = myGameIds.mapNotNull { merged[it] }
            val current = serverHome.currentPlayingGame?.let { merged[it.id] }
                ?: engagementStore.getCurrentPlayingGameId()?.let { merged[it] }
                ?: recent.firstOrNull()
            return DisplayData(
                currentPlaying = current,
                recent = recent,
                myGames = myGames,
                newbieMustPlay = serverHome.newbieMustPlay.mapNotNull { merged[it.id] },
                everyonePlaying = serverHome.everyonePlaying.mapNotNull { merged[it.id] },
                favoriteCount = serverHome.favoriteCount,
                shareCount = serverHome.shareCount
            )
        }

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
            myGames = myGames,
            newbieMustPlay = emptyList(),
            everyonePlaying = emptyList(),
            favoriteCount = myGames.size.toLong(),
            shareCount = 0L
        )
    }
}
