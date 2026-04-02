package com.nexus.platform.data.remote

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.nexus.platform.core.network.BackendConfig
import com.nexus.platform.data.local.AuthSessionStore
import com.nexus.platform.domain.model.GameItem
import com.nexus.platform.domain.model.LibraryHomeSnapshot
import com.nexus.platform.domain.model.UserProfileDetail
import com.nexus.platform.domain.model.WalletSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PlatformBackendApi(context: Context) {
    private val gson = Gson()
    private val sessionStore = AuthSessionStore(context)
    private val refreshMutex = Mutex()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun getApprovedGames(): List<GameItem> = withContext(Dispatchers.IO) {
        executeRequestWithOptionalAuthRetry { token ->
            val requestBuilder = Request.Builder()
                .url("${BackendConfig.apiBaseUrl}/game/public/list")
            token?.let { requestBuilder.addHeader("Authorization", "Bearer $it") }
            requestBuilder.build()
        }.use { response ->
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

    suspend fun getDiscoverGames(limit: Int = 20): List<GameItem> = withContext(Dispatchers.IO) {
        val normalizedLimit = limit.coerceIn(1, 100)
        executeRequestWithOptionalAuthRetry { token ->
            val requestBuilder = Request.Builder()
                .url("${BackendConfig.apiBaseUrl}/discover/feed?limit=$normalizedLimit")
            token?.let { requestBuilder.addHeader("Authorization", "Bearer $it") }
            requestBuilder.build()
        }.use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to load discover feed: ${response.code}")
            }
            val body = response.body?.string().orEmpty()
            val payload = gson.fromJson(body, JsonObject::class.java)
            val code = payload.get("code")?.asInt ?: -1
            if (code != 0) {
                throw IOException(payload.get("message")?.asString ?: "Discover feed request failed")
            }
            val data = payload.getAsJsonArray("data") ?: JsonArray()
            data.mapNotNull { item -> item?.asJsonObject?.toGame() }
        }
    }

    suspend fun getLibraryHome(): LibraryHomeSnapshot? = withContext(Dispatchers.IO) {
        executeRequestWithRequiredAuthRetry { token ->
            Request.Builder()
                .url("${BackendConfig.apiBaseUrl}/library/home")
                .addHeader("Authorization", "Bearer $token")
                .build()
        }?.use { response ->
            if (!response.isSuccessful) {
                return@withContext null
            }

            val body = response.body?.string().orEmpty()
            val payload = gson.fromJson(body, JsonObject::class.java) ?: return@withContext null
            val code = payload.get("code")?.asInt ?: -1
            if (code != 0) {
                return@withContext null
            }
            val data = payload.getAsJsonObject("data") ?: return@withContext null
            val current = data.getAsJsonObject("currentPlayingGame")?.toGame()
            val recent = data.getAsJsonArray("recentGames")
                ?.mapNotNull { it?.asJsonObject?.toGame() }
                ?: emptyList()
            val myGames = data.getAsJsonArray("myGames")
                ?.mapNotNull { it?.asJsonObject?.toGame() }
                ?: emptyList()
            LibraryHomeSnapshot(
                currentPlayingGame = current,
                recentGames = recent,
                myGames = myGames
            )
        } ?: return@withContext null
    }

    suspend fun markPlayed(appId: String) = withContext(Dispatchers.IO) {
        runCatching {
            executeRequestWithRequiredAuthRetry { token ->
                Request.Builder()
                    .url("${BackendConfig.apiBaseUrl}/library/$appId/play")
                    .addHeader("Authorization", "Bearer $token")
                    .post("{}".toRequestBody(null))
                    .build()
            }?.use { /* best effort */ }
        }
    }

    suspend fun setFavorite(appId: String, favorite: Boolean): Boolean = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            executeRequestWithRequiredAuthRetry { token ->
                val requestBuilder = Request.Builder()
                    .url("${BackendConfig.apiBaseUrl}/library/$appId/favorite")
                    .addHeader("Authorization", "Bearer $token")
                if (favorite) {
                    requestBuilder.post("{}".toRequestBody(null)).build()
                } else {
                    requestBuilder.delete().build()
                }
            }?.use { response ->
                response.isSuccessful
            } ?: false
        }.getOrDefault(false)
    }

    suspend fun getUserProfile(): UserProfileDetail? = withContext(Dispatchers.IO) {
        executeRequestWithRequiredAuthRetry { token ->
            Request.Builder()
                .url("${BackendConfig.apiBaseUrl}/user/profile")
                .addHeader("Authorization", "Bearer $token")
                .build()
        }?.use { response ->
            if (!response.isSuccessful) {
                return@withContext null
            }
            val body = response.body?.string().orEmpty()
            val payload = gson.fromJson(body, JsonObject::class.java) ?: return@withContext null
            val code = payload.get("code")?.asInt ?: -1
            if (code != 0) {
                return@withContext null
            }
            val data = payload.getAsJsonObject("data") ?: return@withContext null
            UserProfileDetail(
                id = data.get("id")?.asLong ?: 0L,
                username = data.get("username")?.asString.orEmpty(),
                email = data.get("email")?.asString.orEmpty(),
                phone = data.get("phone")?.asString.orEmpty(),
                role = data.get("role")?.asString.orEmpty(),
                displayName = data.get("displayName")?.asString.orEmpty(),
                avatarUrl = data.get("avatarUrl")?.asString.orEmpty(),
                languageTag = data.get("languageTag")?.asString.orEmpty()
            )
        } ?: return@withContext null
    }

    suspend fun getWalletSummary(): WalletSummary? = withContext(Dispatchers.IO) {
        executeRequestWithRequiredAuthRetry { token ->
            Request.Builder()
                .url("${BackendConfig.apiBaseUrl}/wallet/summary")
                .addHeader("Authorization", "Bearer $token")
                .build()
        }?.use { response ->
            if (!response.isSuccessful) {
                return@withContext null
            }
            val body = response.body?.string().orEmpty()
            val payload = gson.fromJson(body, JsonObject::class.java) ?: return@withContext null
            val code = payload.get("code")?.asInt ?: -1
            if (code != 0) {
                return@withContext null
            }
            val data = payload.getAsJsonObject("data") ?: return@withContext null
            WalletSummary(
                balance = data.get("balance")?.asString.orEmpty(),
                frozenBalance = data.get("frozenBalance")?.asString.orEmpty(),
                availableBalance = data.get("availableBalance")?.asString.orEmpty(),
                todayIncome = data.get("todayIncome")?.asString.orEmpty(),
                totalIncome = data.get("totalIncome")?.asString.orEmpty()
            )
        } ?: return@withContext null
    }

    private suspend fun executeRequestWithRequiredAuthRetry(
        buildRequest: (accessToken: String) -> Request
    ): okhttp3.Response? {
        val accessToken = sessionStore.accessToken() ?: return null
        val first = client.newCall(buildRequest(accessToken)).execute()
        if (first.code != 401) {
            return first
        }
        first.close()
        val refreshedAccessToken = refreshAccessToken(accessToken) ?: return null
        return client.newCall(buildRequest(refreshedAccessToken)).execute()
    }

    private suspend fun executeRequestWithOptionalAuthRetry(
        buildRequest: (accessToken: String?) -> Request
    ): okhttp3.Response {
        val accessToken = sessionStore.accessToken()
        val first = client.newCall(buildRequest(accessToken)).execute()
        if (first.code != 401 || accessToken.isNullOrBlank()) {
            return first
        }
        first.close()
        val refreshedAccessToken = refreshAccessToken(accessToken)
        return if (refreshedAccessToken.isNullOrBlank()) {
            client.newCall(buildRequest(null)).execute()
        } else {
            client.newCall(buildRequest(refreshedAccessToken)).execute()
        }
    }

    private suspend fun refreshAccessToken(staleAccessToken: String): String? {
        return refreshMutex.withLock {
            val latestAccessToken = sessionStore.accessToken()
            if (!latestAccessToken.isNullOrBlank() && latestAccessToken != staleAccessToken) {
                return@withLock latestAccessToken
            }

            val refreshToken = sessionStore.refreshToken() ?: return@withLock null
            val payload = JsonObject().apply { addProperty("refreshToken", refreshToken) }
            val request = Request.Builder()
                .url("${BackendConfig.apiBaseUrl}/user/refresh")
                .post(
                    gson.toJson(payload).toRequestBody("application/json; charset=utf-8".toMediaType())
                )
                .build()

            return@withLock runCatching {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        if (response.code == 401 || response.code == 403) {
                            sessionStore.clear()
                        }
                        return@use null
                    }
                    val body = response.body?.string().orEmpty()
                    val root = gson.fromJson(body, JsonObject::class.java) ?: return@use null
                    val code = root.get("code")?.asInt ?: -1
                    if (code != 0) {
                        return@use null
                    }
                    val data = root.getAsJsonObject("data") ?: return@use null
                    val newAccessToken = data.get("token")?.asString ?: return@use null
                    val newRefreshToken = data.get("refreshToken")?.asString ?: return@use null
                    sessionStore.save(newAccessToken, newRefreshToken)
                    newAccessToken
                }
            }.getOrNull()
        }
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
