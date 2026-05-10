package com.nexus.platform.feature.library.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.platform.data.local.GameCatalogCacheStore
import com.nexus.platform.data.local.GameEngagementStore
import com.nexus.platform.domain.model.DiscoverCategory
import com.nexus.platform.domain.model.DiscoverHeroCard
import com.nexus.platform.domain.model.DiscoverHomeSnapshot
import com.nexus.platform.domain.model.GameItem
import com.nexus.platform.domain.model.LibraryHomeSnapshot
import com.nexus.platform.domain.usecase.GetApprovedGamesUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
            runCatching { loadRemoteSnapshot() }
                .onSuccess { snapshot ->
                    catalogCacheStore.saveGames((snapshot.games + snapshot.discoverGames).distinctBy { it.id })
                    applySnapshot(snapshot)
                }
                .onFailure { e ->
                    val cachedCatalogGames = catalogCacheStore.loadGames()
                    val snapshot = loadFallbackSnapshot(cachedCatalogGames)
                    applySnapshot(
                        snapshot = snapshot,
                        errorMessage = if (snapshot.games.isEmpty()) (e.message ?: ERROR_LOAD_GAMES_FAILED) else null
                    )
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

    private data class RemoteSnapshot(
        val games: List<GameItem>,
        val discoverGames: List<GameItem>,
        val serverHome: LibraryHomeSnapshot?,
        val discoverHome: DiscoverHomeSnapshot?
    )

    private suspend fun loadRemoteSnapshot(): RemoteSnapshot = coroutineScope {
        val gamesDeferred = async { getApprovedGamesUseCase() }
        val libraryHomeDeferred = async { runCatching { getApprovedGamesUseCase.getLibraryHome() }.getOrNull() }
        val discoverHomeDeferred = async { runCatching { getApprovedGamesUseCase.getDiscoverHome() }.getOrNull() }

        val games = gamesDeferred.await()
        val serverHome = libraryHomeDeferred.await()
        val discoverHome = discoverHomeDeferred.await()
        val discoverGames = resolveDiscoverGames(discoverHome, fallbackGames = games)

        RemoteSnapshot(
            games = games,
            discoverGames = discoverGames,
            serverHome = serverHome,
            discoverHome = discoverHome
        )
    }

    private suspend fun loadFallbackSnapshot(cachedCatalogGames: List<GameItem>): RemoteSnapshot {
        val discoverHome = runCatching { getApprovedGamesUseCase.getDiscoverHome() }.getOrNull()
        val discoverGames = resolveDiscoverGames(
            discoverHome = discoverHome,
            fallbackGames = cachedCatalogGames
        )
        return RemoteSnapshot(
            games = discoverGames,
            discoverGames = discoverGames,
            serverHome = null,
            discoverHome = discoverHome
        )
    }

    private suspend fun resolveDiscoverGames(
        discoverHome: DiscoverHomeSnapshot?,
        fallbackGames: List<GameItem>
    ): List<GameItem> {
        val remoteDiscoverGames = discoverHome?.let {
            (it.rankedGames + it.newbieMustPlay + it.everyonePlaying)
                .distinctBy(GameItem::id)
        } ?: runCatching {
            getApprovedGamesUseCase.getDiscoverGames(
                category = "all",
                preferHomeSnapshot = false
            )
        }.getOrDefault(emptyList())

        return if (remoteDiscoverGames.isNotEmpty()) {
            remoteDiscoverGames
        } else {
            fallbackGames
        }
    }

    private fun defaultDiscoverCategories(): List<DiscoverCategory> {
        return listOf(
            DiscoverCategory(key = "all", label = "all"),
            DiscoverCategory(key = "動作射擊", label = "動作射擊"),
            DiscoverCategory(key = "休閒益智", label = "休閒益智"),
            DiscoverCategory(key = "角色扮演", label = "角色扮演")
        )
    }

    private fun applySnapshot(
        snapshot: RemoteSnapshot,
        errorMessage: String? = null
    ) {
        val display = buildDisplayData(snapshot.games, snapshot.serverHome)
        val discoverCategories = snapshot.discoverHome?.categories.orEmpty().ifEmpty {
            defaultDiscoverCategories()
        }
        val coldstartNewbie = if (display.newbieMustPlay.isNotEmpty()) {
            display.newbieMustPlay
        } else {
            snapshot.discoverHome?.newbieMustPlay.orEmpty()
        }
        val coldstartEveryone = if (display.everyonePlaying.isNotEmpty()) {
            display.everyonePlaying
        } else {
            snapshot.discoverHome?.everyonePlaying.orEmpty()
        }
        val randomizedEveryone = coldstartEveryone
            .take(50)
            .shuffled(Random(System.currentTimeMillis()))

        _uiState.update {
            it.copy(
                loading = false,
                errorMessage = errorMessage,
                games = snapshot.games,
                discoverGames = snapshot.discoverGames,
                discoverHero = snapshot.discoverHome?.hero,
                libraryTopBanner = snapshot.discoverHome?.libraryTopBanner,
                discoverCategories = discoverCategories,
                discoverNewbie = snapshot.discoverHome?.newbieMustPlay ?: emptyList(),
                discoverEveryone = snapshot.discoverHome?.everyonePlaying ?: emptyList(),
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
