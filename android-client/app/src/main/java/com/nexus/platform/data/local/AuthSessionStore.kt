package com.nexus.platform.data.local

import android.content.Context
import com.nexus.platform.domain.model.AuthSession

class AuthSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun save(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    fun save(session: AuthSession) {
        save(session.accessToken, session.refreshToken)
    }

    fun accessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun refreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun get(): AuthSession? {
        val access = accessToken()
        val refresh = refreshToken()
        return if (access.isNullOrBlank() || refresh.isNullOrBlank()) {
            null
        } else {
            AuthSession(access, refresh)
        }
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .apply()
    }

    companion object {
        private const val PREF_NAME = "auth_session"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
