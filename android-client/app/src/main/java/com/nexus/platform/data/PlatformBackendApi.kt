package com.nexus.platform.data

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.nexus.platform.utils.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class PlatformBackendApi {
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun getApprovedGames(): List<Game> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/game/list")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("加载游戏列表失败: ${response.code}")
            }

            val body = response.body?.string().orEmpty()
            val payload = gson.fromJson(body, JsonObject::class.java)
            val code = payload.get("code")?.asInt ?: -1
            if (code != 0) {
                throw IOException(payload.get("message")?.asString ?: "游戏列表请求失败")
            }

            val data = payload.getAsJsonArray("data") ?: JsonArray()
            data.mapNotNull { item -> item?.asJsonObject?.toGame() }
        }
    }

    private fun JsonObject.toGame(): Game {
        val appId = get("appId")?.asString.orEmpty()
        return Game(
            id = appId,
            name = get("name")?.asString ?: "未命名游戏",
            description = get("description")?.asString ?: "暂无描述",
            iconUrl = get("iconUrl")?.asString.orEmpty(),
            downloadUrl = normalizeDownloadUrl(get("downloadUrl")?.asString.orEmpty()),
            version = get("version")?.asString ?: "1.0.0",
            md5 = get("md5")?.asString.orEmpty()
        )
    }

    private fun normalizeDownloadUrl(rawUrl: String): String {
        return rawUrl
            .replace("http://localhost", emulatorBaseHost)
            .replace("https://localhost", secureEmulatorBaseHost)
            .replace("http://127.0.0.1", emulatorBaseHost)
            .replace("https://127.0.0.1", secureEmulatorBaseHost)
    }

    companion object {
        private const val emulatorBaseHost = "http://10.0.2.2"
        private const val secureEmulatorBaseHost = "https://10.0.2.2"
        private const val baseUrl = "$emulatorBaseHost:8080/api/v1"
    }
}
