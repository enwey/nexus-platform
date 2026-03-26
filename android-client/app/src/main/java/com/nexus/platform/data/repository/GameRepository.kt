package com.nexus.platform.data.repository

import android.content.Context
import com.nexus.platform.BuildConfig
import com.nexus.platform.data.remote.PlatformBackendApi
import com.nexus.platform.domain.model.GameItem

class GameRepository(context: Context) {
    private val backendApi = PlatformBackendApi(context)

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
}
