package com.nexus.platform.domain.usecase

import com.nexus.platform.data.repository.GameRepository
import com.nexus.platform.domain.model.GameItem

class GetApprovedGamesUseCase(private val gameRepository: GameRepository) {
    suspend operator fun invoke(): List<GameItem> {
        return gameRepository.getApprovedGames()
    }
}
