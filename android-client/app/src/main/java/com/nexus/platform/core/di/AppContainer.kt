package com.nexus.platform.core.di

import android.content.Context
import com.nexus.platform.data.remote.BackendAuthApi
import com.nexus.platform.data.remote.PlatformBackendApi
import com.nexus.platform.data.repository.AuthRepository
import com.nexus.platform.data.repository.GameRepository
import com.nexus.platform.domain.usecase.GetApprovedGamesUseCase
import com.nexus.platform.domain.usecase.LoginUseCase
import com.nexus.platform.domain.usecase.LogoutUseCase

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val backendAuthApi: BackendAuthApi by lazy {
        BackendAuthApi()
    }

    val platformBackendApi: PlatformBackendApi by lazy {
        PlatformBackendApi(appContext)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(appContext, backendAuthApi)
    }

    val gameRepository: GameRepository by lazy {
        GameRepository(appContext, platformBackendApi)
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
