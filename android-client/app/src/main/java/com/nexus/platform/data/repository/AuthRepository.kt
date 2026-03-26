package com.nexus.platform.data.repository

import android.content.Context
import com.nexus.platform.data.local.AuthSessionStore
import com.nexus.platform.data.remote.BackendAuthApi
import com.nexus.platform.domain.model.AuthSession

class AuthRepository(context: Context) {
    private val authApi = BackendAuthApi()
    private val sessionStore = AuthSessionStore(context)

    suspend fun login(username: String, password: String): AuthSession {
        val session = authApi.login(username, password)
        sessionStore.save(session)
        return session
    }

    fun currentSession(): AuthSession? = sessionStore.get()

    fun logout() {
        sessionStore.clear()
    }
}
