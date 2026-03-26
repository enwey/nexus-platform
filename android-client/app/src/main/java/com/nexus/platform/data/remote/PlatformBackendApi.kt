package com.nexus.platform.data.remote

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.nexus.platform.core.network.BackendConfig
import com.nexus.platform.data.local.AuthSessionStore
import com.nexus.platform.domain.model.GameItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
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
        val requestBuilder = Request.Builder().url("${BackendConfig.apiBaseUrl}/game/list")
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
        val appId = get("appId")?.asString.orEmpty()
        return GameItem(
            id = appId,
            name = get("name")?.asString ?: "Unnamed game",
            description = get("description")?.asString ?: "No description",
            iconUrl = get("iconUrl")?.asString.orEmpty(),
            downloadUrl = normalizeDownloadUrl(get("downloadUrl")?.asString.orEmpty()),
            version = get("version")?.asString ?: "1.0.0",
            md5 = get("md5")?.asString.orEmpty()
        )
    }

    private fun normalizeDownloadUrl(rawUrl: String): String {
        return rawUrl
            .replace("http://localhost", BackendConfig.emulatorHttpHost)
            .replace("https://localhost", BackendConfig.emulatorHttpsHost)
            .replace("http://127.0.0.1", BackendConfig.emulatorHttpHost)
            .replace("https://127.0.0.1", BackendConfig.emulatorHttpsHost)
    }
}
