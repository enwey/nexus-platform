﻿﻿﻿﻿﻿﻿﻿package com.nexus.platform.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class GameManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun getGameDir(gameId: String): File {
        val gamesDir = File(context.filesDir, "games")
        if (!gamesDir.exists()) {
            gamesDir.mkdirs()
        }
        return File(gamesDir, gameId)
    }

    fun isGameDownloaded(gameId: String): Boolean {
        val gameDir = getGameDir(gameId)
        return gameDir.exists() && File(gameDir, "index.html").exists()
    }

    suspend fun downloadGame(game: Game, targetDir: File) {
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(normalizeDownloadUrl(game.downloadUrl))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("游戏下载失败: ${response.code}")
                }

                val zipFile = File(context.cacheDir, "${game.id}.zip")
                response.body?.byteStream()?.use { input ->
                    FileOutputStream(zipFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val actualMd5 = calculateMD5(zipFile)
                if (game.md5.isNotEmpty() && actualMd5 != game.md5) {
                    zipFile.delete()
                    throw IOException("游戏包校验失败")
                }

                ZipUtils.unzip(zipFile, targetDir)
                zipFile.delete()
            }
        }
    }

    suspend fun readSDKContent(): String {
        return withContext(Dispatchers.IO) {
            try {
                val directFile = File(context.filesDir, "wx-mock-sdk.js")
                if (directFile.exists()) {
                    return@withContext directFile.readText()
                }

                val bundleFile = File(context.filesDir, "wx-mock-sdk.iife.js")
                if (bundleFile.exists()) {
                    return@withContext bundleFile.readText()
                }

                context.assets.open("wx-mock-sdk.iife.js").bufferedReader().use { it.readText() }
            } catch (_: Exception) {
                ""
            }
        }
    }

    private fun normalizeDownloadUrl(rawUrl: String): String {
        return rawUrl
            .replace("http://localhost", "http://10.0.2.2")
            .replace("https://localhost", "https://10.0.2.2")
            .replace("http://127.0.0.1", "http://10.0.2.2")
            .replace("https://127.0.0.1", "https://10.0.2.2")
    }

    private fun calculateMD5(file: File): String {
        val md = MessageDigest.getInstance("MD5")
        file.inputStream().use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                md.update(buffer, 0, bytesRead)
            }
        }
        val digest = md.digest()
        return digest.joinToString("") { "%02x".format(it) }
    }
}

data class Game(
    val id: String,
    val name: String,
    val description: String,
    val iconUrl: String,
    val downloadUrl: String,
    val version: String,
    val md5: String = ""
) : java.io.Serializable
