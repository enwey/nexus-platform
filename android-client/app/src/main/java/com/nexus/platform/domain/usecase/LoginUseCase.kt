package com.nexus.platform.domain.usecase

import com.nexus.platform.data.repository.AuthRepository
import com.nexus.platform.domain.model.AuthSession

class LoginUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(username: String, password: String): AuthSession {
        return authRepository.login(username, password)
    }
}
