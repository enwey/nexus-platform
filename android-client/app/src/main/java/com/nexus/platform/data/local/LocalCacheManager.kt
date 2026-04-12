package com.nexus.platform.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.ln
import kotlin.math.pow

object LocalCacheManager {
    private val targetPrefs = listOf(
        "game_catalog_cache",
        "game_engagement",
        "cloud_sync"
    )

    suspend fun computeCacheBytes(context: Context): Long = withContext(Dispatchers.IO) {
        var total = 0L

        val gamesDir = File(context.filesDir, "games")
        total += gamesDir.dirSize()

        total += context.cacheDir
            ?.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".zip", ignoreCase = true) }
            ?.sumOf { it.length() }
            ?: 0L

        val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        total += targetPrefs.sumOf { name ->
            File(prefsDir, "$name.xml").takeIf { it.exists() }?.length() ?: 0L
        }

        total
    }

    suspend fun clearLocalCaches(context: Context): Long = withContext(Dispatchers.IO) {
        val before = computeCacheBytes(context)

        File(context.filesDir, "games").deleteRecursively()

        context.cacheDir
            ?.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".zip", ignoreCase = true) }
            ?.forEach { it.delete() }

        targetPrefs.forEach { name ->
            context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().apply()
        }

        val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        targetPrefs.forEach { name ->
            File(prefsDir, "$name.xml").delete()
        }

        val after = computeCacheBytes(context)
        (before - after).coerceAtLeast(0L)
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        if (bytes < 1024) return "$bytes B"
        val exp = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(1, 4)
        val unit = listOf("KB", "MB", "GB", "TB")[exp - 1]
        val value = bytes / 1024.0.pow(exp.toDouble())
        return String.format("%.1f %s", value, unit)
    }

    private fun File.dirSize(): Long {
        if (!exists()) return 0L
        if (isFile) return length()
        return listFiles()?.sumOf { it.dirSize() } ?: 0L
    }
}
