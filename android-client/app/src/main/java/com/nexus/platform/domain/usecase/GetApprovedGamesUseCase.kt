package com.nexus.platform.domain.usecase

import com.nexus.platform.data.repository.GameRepository
import com.nexus.platform.domain.model.DiscoverHomeSnapshot
import com.nexus.platform.domain.model.GameItem
import com.nexus.platform.domain.model.LibraryHomeSnapshot

class GetApprovedGamesUseCase(private val gameRepository: GameRepository) {
    suspend operator fun invoke(): List<GameItem> {
        return gameRepository.getApprovedGames()
    }

    suspend fun getLibraryHome(): LibraryHomeSnapshot? {
        return gameRepository.getLibraryHome()
    }

    suspend fun getDiscoverGames(category: String? = null): List<GameItem> {
        return gameRepository.getDiscoverGames(category)
    }

    suspend fun getDiscoverHome(): DiscoverHomeSnapshot? {
        return gameRepository.getDiscoverHome()
    }

    suspend fun markPlayed(appId: String) {
        gameRepository.markPlayed(appId)
    }

    suspend fun markShared(appId: String) {
        gameRepository.markShared(appId)
    }
}
