package com.nexus.platform.data.repository

import android.content.Context
import com.nexus.platform.data.remote.PlatformBackendApi
import com.nexus.platform.domain.model.GameItem

class GameRepository(context: Context) {
    private val backendApi = PlatformBackendApi(context)

    suspend fun getApprovedGames(): List<GameItem> {
        return backendApi.getApprovedGames()
    }
}
