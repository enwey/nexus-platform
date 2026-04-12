package com.nexus.platform.data.local

import android.content.Context

class CloudSyncStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val sessionStore = AuthSessionStore(context)

    fun isEnabled(): Boolean {
        return prefs.getBoolean(flagKey(), true)
    }

    fun setEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(flagKey(), enabled)
            .apply()
    }

    private fun flagKey(): String {
        val token = sessionStore.accessToken().orEmpty()
        val namespace = if (token.isBlank()) {
            "guest"
        } else {
            "u_" + token.hashCode().toUInt().toString()
        }
        return "$KEY_PREFIX$namespace"
    }

    private companion object {
        private const val PREF_NAME = "cloud_sync"
        private const val KEY_PREFIX = "cloud_sync_enabled_"
    }
}
