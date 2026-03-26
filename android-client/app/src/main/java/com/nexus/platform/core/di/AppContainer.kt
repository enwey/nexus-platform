package com.nexus.platform.core.di

import android.content.Context
import com.nexus.platform.data.repository.AuthRepository
import com.nexus.platform.data.repository.GameRepository
import com.nexus.platform.domain.usecase.GetApprovedGamesUseCase
import com.nexus.platform.domain.usecase.LoginUseCase
import com.nexus.platform.domain.usecase.LogoutUseCase

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val authRepository: AuthRepository by lazy {
        AuthRepository(appContext)
    }

    val gameRepository: GameRepository by lazy {
        GameRepository(appContext)
    }

    val loginUseCase: LoginUseCase by lazy {
        LoginUseCase(authRepository)
    }

    val logoutUseCase: LogoutUseCase by lazy {
        LogoutUseCase(authRepository)
    }

    val getApprovedGamesUseCase: GetApprovedGamesUseCase by lazy {
        GetApprovedGamesUseCase(gameRepository)
    }
}
