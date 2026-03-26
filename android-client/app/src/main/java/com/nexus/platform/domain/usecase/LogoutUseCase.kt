package com.nexus.platform.domain.usecase

import com.nexus.platform.data.repository.AuthRepository

class LogoutUseCase(private val authRepository: AuthRepository) {
    operator fun invoke() {
        authRepository.logout()
    }
}
