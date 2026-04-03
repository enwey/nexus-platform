package com.nexus.platform.data.repository

import android.content.Context
import com.nexus.platform.BuildConfig
import com.nexus.platform.data.local.AuthSessionStore
import com.nexus.platform.data.remote.BackendAuthApi
import com.nexus.platform.domain.model.AuthSession

class AuthRepository(context: Context) {
    private val authApi = BackendAuthApi()
    private val sessionStore = AuthSessionStore(context)

    suspend fun login(username: String, password: String): AuthSession {
        val session = if (BuildConfig.USE_MOCK_DATA) {
            AuthSession(
                accessToken = "local-demo-token",
                refreshToken = "local-demo-refresh-token"
            )
        } else {
            authApi.login(username, password)
        }
        sessionStore.save(session)
        return session
    }

    suspend fun register(username: String, password: String, email: String?): AuthSession {
        val session = if (BuildConfig.USE_MOCK_DATA) {
            AuthSession(
                accessToken = "local-demo-token",
                refreshToken = "local-demo-refresh-token"
            )
        } else {
            authApi.register(username, password, email)
        }
        sessionStore.save(session)
        return session
    }

    fun currentSession(): AuthSession? = sessionStore.get()

    fun logout() {
        sessionStore.clear()
    }
}
