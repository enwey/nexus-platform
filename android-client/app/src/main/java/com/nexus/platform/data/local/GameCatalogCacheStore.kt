package com.nexus.platform.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nexus.platform.domain.model.GameItem

class GameCatalogCacheStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val sessionStore = AuthSessionStore(context)
    private val gson = Gson()

    fun saveGames(games: List<GameItem>) {
        val deduped = games
            .distinctBy { it.id }
        prefs.edit()
            .putString(snapshotKey(), gson.toJson(deduped))
            .apply()
    }

    fun loadGames(): List<GameItem> {
        val raw = prefs.getString(snapshotKey(), null).orEmpty()
        if (raw.isBlank()) {
            return emptyList()
        }
        val type = object : TypeToken<List<GameItem>>() {}.type
        return runCatching { gson.fromJson<List<GameItem>>(raw, type) }
            .getOrDefault(emptyList())
    }

    private fun snapshotKey(): String {
        val token = sessionStore.accessToken().orEmpty()
        val namespace = if (token.isBlank()) {
            "guest"
        } else {
            "u_" + token.hashCode().toUInt().toString()
        }
        return "$KEY_PREFIX$namespace"
    }

    private companion object {
        private const val PREF_NAME = "game_catalog_cache"
        private const val KEY_PREFIX = "catalog_"
    }
}
