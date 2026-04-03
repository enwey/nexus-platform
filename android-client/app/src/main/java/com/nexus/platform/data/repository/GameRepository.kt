package com.nexus.platform.data.repository

import android.content.Context
import com.nexus.platform.BuildConfig
import com.nexus.platform.data.local.AuthSessionStore
import com.nexus.platform.data.remote.PlatformBackendApi
import com.nexus.platform.domain.model.DiscoverHomeSnapshot
import com.nexus.platform.domain.model.GameItem
import com.nexus.platform.domain.model.LibraryHomeSnapshot

class GameRepository(context: Context) {
    private val backendApi = PlatformBackendApi(context)
    private val authSessionStore = AuthSessionStore(context)

    suspend fun getApprovedGames(): List<GameItem> {
        return if (BuildConfig.USE_MOCK_DATA) {
            listOf(
                GameItem(
                    id = "demo-space-runner",
                    name = "Space Runner",
                    description = "Fast-paced endless runner demo.",
                    iconUrl = "",
                    downloadUrl = "",
                    version = "1.0.0"
                ),
                GameItem(
                    id = "demo-tower-defense",
                    name = "Tower Defense",
                    description = "Strategy defense demo with waves.",
                    iconUrl = "",
                    downloadUrl = "",
                    version = "1.2.0"
                ),
                GameItem(
                    id = "demo-puzzle-quest",
                    name = "Puzzle Quest",
                    description = "Casual puzzle demo for UI preview.",
                    iconUrl = "",
                    downloadUrl = "",
                    version = "0.9.5"
                ),
                GameItem(
                    id = "demo-racing",
                    name = "Neon Racing",
                    description = "Arcade racing demo screen.",
                    iconUrl = "",
                    downloadUrl = "",
                    version = "2.1.3"
                )
            )
        } else {
            backendApi.getApprovedGames()
        }
    }

    suspend fun getLibraryHome(): LibraryHomeSnapshot? {
        if (BuildConfig.USE_MOCK_DATA) {
            return null
        }
        return backendApi.getLibraryHome()
    }

    suspend fun getDiscoverGames(category: String? = null): List<GameItem> {
        return if (BuildConfig.USE_MOCK_DATA) {
            emptyList()
        } else {
            backendApi.getDiscoverGames(category = category)
        }
    }

    suspend fun getDiscoverHome(): DiscoverHomeSnapshot? {
        if (BuildConfig.USE_MOCK_DATA) {
            return null
        }
        return backendApi.getDiscoverHome()
    }

    suspend fun markPlayed(appId: String) {
        if (BuildConfig.USE_MOCK_DATA) {
            return
        }
        if (authSessionStore.get() == null) {
            return
        }
        backendApi.markPlayed(appId)
    }

    suspend fun markShared(appId: String) {
        if (BuildConfig.USE_MOCK_DATA) {
            return
        }
        if (authSessionStore.get() == null) {
            return
        }
        backendApi.markShared(appId)
    }
}
