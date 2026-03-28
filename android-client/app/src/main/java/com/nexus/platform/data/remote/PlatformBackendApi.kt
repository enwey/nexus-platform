package com.nexus.platform.data.remote

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.nexus.platform.core.network.BackendConfig
import com.nexus.platform.data.local.AuthSessionStore
import com.nexus.platform.domain.model.GameItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException
import java.util.concurrent.TimeUnit

class PlatformBackendApi(context: Context) {
    private val gson = Gson()
    private val sessionStore = AuthSessionStore(context)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun getApprovedGames(): List<GameItem> = withContext(Dispatchers.IO) {
        val requestBuilder = Request.Builder().url("${BackendConfig.apiBaseUrl}/game/public/list")
        sessionStore.accessToken()?.let { token ->
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        val request = requestBuilder.build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to load game list: ${response.code}")
            }

            val body = response.body?.string().orEmpty()
            val payload = gson.fromJson(body, JsonObject::class.java)
            val code = payload.get("code")?.asInt ?: -1
            if (code != 0) {
                throw IOException(payload.get("message")?.asString ?: "Game list request failed")
            }

            val data = payload.getAsJsonArray("data") ?: JsonArray()
            data.mapNotNull { item -> item?.asJsonObject?.toGame() }
        }
    }

    private fun JsonObject.toGame(): GameItem {
        val appId = stringOrEmpty("appId")
        return GameItem(
            id = appId,
            name = stringOrDefault("name", "Unnamed game"),
            description = stringOrDefault("description", "No description"),
            iconUrl = stringOrEmpty("iconUrl"),
            downloadUrl = normalizeDownloadUrl(stringOrEmpty("downloadUrl")),
            version = stringOrDefault("version", "1.0.0"),
            md5 = stringOrEmpty("md5")
        )
    }

    private fun JsonObject.stringOrEmpty(key: String): String {
        return (get(key) as? JsonPrimitive)?.asString?.takeIf { it.isNotBlank() }.orEmpty()
    }

    private fun JsonObject.stringOrDefault(key: String, fallback: String): String {
        return stringOrEmpty(key).ifBlank { fallback }
    }

    private fun normalizeDownloadUrl(rawUrl: String): String {
        if (rawUrl.startsWith("/")) {
            return BackendConfig.localHttpHost + rawUrl
        }
        return runCatching {
            val url = rawUrl.toHttpUrlOrNull() ?: return@runCatching rawUrl
            val host = url.host.lowercase()
            if (host == "localhost" || host == "127.0.0.1" || host == "::1") {
                url.newBuilder()
                    .host(BackendConfig.localHost)
                    .build()
                    .toString()
            } else {
                rawUrl
            }
        }.getOrElse {
            rawUrl
                .replace("http://localhost", "http://${BackendConfig.localHost}")
                .replace("https://localhost", "https://${BackendConfig.localHost}")
                .replace("http://127.0.0.1", "http://${BackendConfig.localHost}")
                .replace("https://127.0.0.1", "https://${BackendConfig.localHost}")
        }
    }
}
