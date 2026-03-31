package com.nexus.platform.data.local

import android.content.Context
import com.google.gson.Gson

class GameEngagementStore(context: Context) {
    private data class Snapshot(
        val lastPlayedAt: MutableMap<String, Long> = mutableMapOf(),
        val favorites: MutableMap<String, Long> = mutableMapOf(),
        var currentPlayingGameId: String? = null
    )

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val sessionStore = AuthSessionStore(context)
    private val gson = Gson()

    fun markPlayed(gameId: String, playedAt: Long = System.currentTimeMillis()) {
        val snapshot = loadSnapshot()
        snapshot.lastPlayedAt[gameId] = playedAt
        snapshot.currentPlayingGameId = gameId
        saveSnapshot(snapshot)
    }

    fun getCurrentPlayingGameId(): String? = loadSnapshot().currentPlayingGameId

    fun getLastPlayedAt(gameId: String): Long = loadSnapshot().lastPlayedAt[gameId] ?: 0L

    fun getRecentPlayedGameIdsDesc(): List<String> {
        return loadSnapshot()
            .lastPlayedAt
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
    }

    fun isFavorite(gameId: String): Boolean = loadSnapshot().favorites.containsKey(gameId)

    fun toggleFavorite(gameId: String): Boolean {
        val snapshot = loadSnapshot()
        val now = System.currentTimeMillis()
        val nowFavorite = if (snapshot.favorites.containsKey(gameId)) {
            snapshot.favorites.remove(gameId)
            false
        } else {
            snapshot.favorites[gameId] = now
            true
        }
        saveSnapshot(snapshot)
        return nowFavorite
    }

    fun getFavoriteGameIdsDesc(): List<String> {
        val snapshot = loadSnapshot()
        return snapshot.favorites.keys
            .sortedWith(
                compareByDescending<String> { snapshot.lastPlayedAt[it] ?: 0L }
                    .thenByDescending { snapshot.favorites[it] ?: 0L }
            )
    }

    private fun loadSnapshot(): Snapshot {
        val raw = prefs.getString(snapshotKey(), null).orEmpty()
        if (raw.isBlank()) {
            return Snapshot()
        }
        return runCatching { gson.fromJson(raw, Snapshot::class.java) }
            .getOrNull()
            ?: Snapshot()
    }

    private fun saveSnapshot(snapshot: Snapshot) {
        prefs.edit()
            .putString(snapshotKey(), gson.toJson(snapshot))
            .apply()
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
        private const val PREF_NAME = "game_engagement"
        private const val KEY_PREFIX = "engagement_"
    }
}
