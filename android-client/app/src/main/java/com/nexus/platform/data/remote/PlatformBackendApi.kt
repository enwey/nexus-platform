package com.nexus.platform.data.remote

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.nexus.platform.core.network.BackendConfig
import com.nexus.platform.core.network.BackendHttpClientFactory
import com.nexus.platform.data.local.AuthSessionStore
import com.nexus.platform.data.local.DeviceIdentityStore
import com.nexus.platform.domain.model.DiscoverCategory
import com.nexus.platform.domain.model.DiscoverHeroCard
import com.nexus.platform.domain.model.DiscoverHomeSnapshot
import com.nexus.platform.domain.model.RecommendTodayItem
import com.nexus.platform.domain.model.BillingDetail
import com.nexus.platform.domain.model.BillingRecord
import com.nexus.platform.domain.model.DeviceSession
import com.nexus.platform.domain.model.GameItem
import com.nexus.platform.domain.model.GameRuntimeProfile
import com.nexus.platform.domain.model.LibraryHomeSnapshot
import com.nexus.platform.domain.model.ReferralRecord
import com.nexus.platform.domain.model.ReferralSummary
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PlatformBackendApi(context: Context) {
    private companion object {
        const val SUPPORTED_PACKAGE_FORMATS = "NEXUS_SECURE_ZIP_V1"
    }

    data class ApiActionResult(
        val success: Boolean,
        val message: String? = null
    )

    data class LegalLinks(
        val termsUrl: String,
        val privacyUrl: String
    )

    data class RuntimePackageKey(
        val key: String,
        val algorithm: String,
        val version: String,
        val format: String
    )

    data class RuntimePackageTicket(
        val ticket: String,
        val expiresInSeconds: Long,
        val version: String,
        val format: String
    )

    private val gson = Gson()
    private val sessionStore = AuthSessionStore(context)
    private val deviceIdentityStore = DeviceIdentityStore(context)
    private val refreshMutex = Mutex()
    private val client = BackendHttpClientFactory.create()

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
        val locales = getAsJsonObject("metadata")?.getAsJsonObject("locales")
            ?: getAsJsonObject("locales")
        return GameItem(
            id = appId,
            name = stringOrDefault("name", "Unnamed game"),
            description = stringOrDefault("description", "No description"),
            iconUrl = stringOrEmpty("iconUrl"),
            downloadUrl = normalizeDownloadUrl(stringOrEmpty("downloadUrl")),
            version = stringOrDefault("version", "1.0.0"),
            md5 = stringOrEmpty("md5"),
            category = stringOrEmpty("category"),
            requiresOnline = get("requiresOnline")?.asBoolean ?: false,
            localizedNames = locales.extractLocalizedField("name"),
            localizedDescriptions = locales.extractLocalizedField("description")
        )
    }

    private fun JsonObject?.extractLocalizedField(fieldName: String): Map<String, String> {
        if (this == null) return emptyMap()
        return entrySet().mapNotNull { (tag, value) ->
            val text = value?.asJsonObject?.get(fieldName)?.takeIf { !it.isJsonNull }?.asString.orEmpty()
            if (text.isBlank()) null else tag to text
        }.toMap()
    }

    suspend fun getDiscoverGames(
        limit: Int = 20,
        category: String? = null,
        preferHomeSnapshot: Boolean = true
    ): List<GameItem> = withContext(Dispatchers.IO) {
        val normalizedCategory = category?.trim().orEmpty().lowercase()
        val useHomeSnapshot = normalizedCategory.isBlank() || normalizedCategory == "all"
        if (useHomeSnapshot && preferHomeSnapshot) {
            val home = runCatching { getDiscoverHome(limit) }.getOrNull()
            if (home != null) {
                return@withContext (home.rankedGames + home.newbieMustPlay + home.everyonePlaying)
                    .distinctBy { it.id }
            }
        }

        val normalizedLimit = limit.coerceIn(1, 100)
        val categoryQuery = if (normalizedCategory.isBlank()) "" else "&category=$normalizedCategory"
        executeRequestWithOptionalAuthRetry { token ->
            val requestBuilder = Request.Builder()
                .url("${BackendConfig.apiBaseUrl}/discover/feed?limit=$normalizedLimit$categoryQuery")
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

    suspend fun getDiscoverHome(limit: Int = 20): DiscoverHomeSnapshot? = withContext(Dispatchers.IO) {
        val normalizedLimit = limit.coerceIn(1, 100)
        executeRequestWithOptionalAuthRetry { token ->
            val requestBuilder = Request.Builder()
                .url("${BackendConfig.apiBaseUrl}/discover/home?limit=$normalizedLimit")
            token?.let { requestBuilder.addHeader("Authorization", "Bearer $it") }
            requestBuilder.build()
        }.use { response ->
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
            val heroJson = data.getAsJsonObject("hero")
            val hero = heroJson?.let {
                DiscoverHeroCard(
                    appId = it.stringOrEmpty("appId"),
                    title = it.stringOrDefault("title", ""),
                    subtitle = it.stringOrDefault("subtitle", ""),
                    coverUrl = it.stringOrDefault("coverUrl", ""),
                    badgeText = it.stringOrDefault("badgeText", "")
                )
            }?.takeIf { it.title.isNotBlank() || it.subtitle.isNotBlank() }
            val libraryBannerJson = data.getAsJsonObject("libraryTopBanner")
            val libraryTopBanner = libraryBannerJson?.let {
                DiscoverHeroCard(
                    appId = it.stringOrEmpty("appId"),
                    title = it.stringOrDefault("title", ""),
                    subtitle = it.stringOrDefault("subtitle", ""),
                    coverUrl = it.stringOrDefault("coverUrl", ""),
                    badgeText = it.stringOrDefault("badgeText", "")
                )
            }?.takeIf { it.title.isNotBlank() || it.subtitle.isNotBlank() || it.coverUrl.isNotBlank() }
            val categories = data.getAsJsonArray("categories")
                ?.mapNotNull { it?.asString?.trim() }
                ?.filter { it.isNotBlank() }
                ?.map { name ->
                    DiscoverCategory(
                        key = name,
                        label = name
                    )
                }
                ?: emptyList()

            DiscoverHomeSnapshot(
                hero = hero,
                libraryTopBanner = libraryTopBanner,
                categories = categories,
                rankedGames = data.getAsJsonArray("rankedGames")
                    ?.mapNotNull { it?.asJsonObject?.toGame() }
                    ?: emptyList(),
                newbieMustPlay = data.getAsJsonArray("newbieMustPlay")
                    ?.mapNotNull { it?.asJsonObject?.toGame() }
                    ?: emptyList(),
                everyonePlaying = data.getAsJsonArray("everyonePlaying")
                    ?.mapNotNull { it?.asJsonObject?.toGame() }
                    ?: emptyList()
            )
        }
    }

    suspend fun getRecommendToday(limit: Int = 10): List<RecommendTodayItem> = withContext(Dispatchers.IO) {
        val normalizedLimit = limit.coerceIn(1, 20)
        executeRequestWithOptionalAuthRetry { token ->
            val requestBuilder = Request.Builder()
                .url("${BackendConfig.apiBaseUrl}/discover/community?limit=$normalizedLimit")
            token?.let { requestBuilder.addHeader("Authorization", "Bearer $it") }
            requestBuilder.build()
        }.use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to load community feed: ${response.code}")
            }
            val body = response.body?.string().orEmpty()
            val payload = gson.fromJson(body, JsonObject::class.java)
            val code = payload.get("code")?.asInt ?: -1
            if (code != 0) {
                throw IOException(payload.get("message")?.asString ?: "Community feed request failed")
            }
            val data = payload.getAsJsonArray("data") ?: JsonArray()
            data.mapNotNull { item ->
                val obj = item?.asJsonObject ?: return@mapNotNull null
                RecommendTodayItem(
                    appId = obj.stringOrEmpty("appId"),
                    gameName = obj.stringOrDefault("gameName", ""),
                    gameCategory = obj.stringOrDefault("gameCategory", ""),
                    gameIconUrl = obj.stringOrDefault("gameIconUrl", ""),
                    cardCategory = obj.stringOrDefault("cardCategory", ""),
                    cardTitle = obj.stringOrDefault("cardTitle", ""),
                    coverUrl = obj.stringOrDefault("coverUrl", ""),
                    articleTag = obj.stringOrDefault("articleTag", ""),
                    articleTitle = obj.stringOrDefault("articleTitle", ""),
                    articleBody = obj.stringOrDefault("articleBody", ""),
                    actionText = obj.stringOrDefault("actionText", "")
                )
            }
        }
    }

    suspend fun getGameRuntimeProfile(appId: String): GameRuntimeProfile? = withContext(Dispatchers.IO) {
        val normalizedAppId = appId.trim()
        if (normalizedAppId.isBlank()) return@withContext null

        executeRequestWithOptionalAuthRetry { token ->
            val requestBuilder = Request.Builder()
                .url("${BackendConfig.apiBaseUrl}/game/$normalizedAppId/runtime-profile")
            token?.let { requestBuilder.addHeader("Authorization", "Bearer $it") }
            requestBuilder.build()
        }.use { response ->
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
            return@withContext GameRuntimeProfile(
                appId = data.stringOrEmpty("appId"),
                gameName = data.stringOrDefault("gameName", ""),
                studioName = data.stringOrDefault("studioName", ""),
                playerCountText = data.stringOrDefault("playerCountText", ""),
                runtimeBannerUrl = data.stringOrDefault("runtimeBannerUrl", ""),
                runtimeLogoUrl = data.stringOrDefault("runtimeLogoUrl", ""),
                shareTitle = data.stringOrDefault("shareTitle", ""),
                shareSubtitle = data.stringOrDefault("shareSubtitle", ""),
                shareImageUrl = data.stringOrDefault("shareImageUrl", ""),
                capsuleTheme = data.stringOrDefault("capsuleTheme", "")
            )
        }
    }

    suspend fun getRuntimePackageKey(appId: String, version: String): RuntimePackageKey = withContext(Dispatchers.IO) {
        val normalizedAppId = appId.trim()
        val normalizedVersion = version.trim()
        val deviceId = deviceIdentityStore.runtimeDeviceId()
        val ticket = issueRuntimePackageTicket(normalizedAppId, normalizedVersion, deviceId)
        val response = executeRequestWithRequiredAuthRetry { token ->
            val url = "${BackendConfig.apiBaseUrl}/game/$normalizedAppId/runtime-key/redeem"
                .toHttpUrlOrNull()
                ?: throw IOException("Invalid runtime package key url")
            Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("X-Nexus-Device-Id", deviceId)
                .addHeader("X-Nexus-Supported-Package-Formats", SUPPORTED_PACKAGE_FORMATS)
                .post(
                    gson.toJson(
                        mapOf(
                            "ticket" to ticket.ticket,
                            "version" to ticket.version
                        )
                    ).toRequestBody("application/json".toMediaType())
                )
                .build()
        } ?: throw IOException("Missing valid authorization token")

        response.use { http ->
            if (!http.isSuccessful) {
                throw IOException("Failed to load runtime package key: ${http.code}")
            }
            val root = gson.fromJson(http.body?.string().orEmpty(), JsonObject::class.java)
            val code = root?.get("code")?.asInt ?: -1
            if (code != 0) {
                throw IOException(root?.get("message")?.asString ?: "Runtime package key request failed")
            }
            val data = root.getAsJsonObject("data") ?: throw IOException("Runtime package key missing data")
            RuntimePackageKey(
                key = data.get("key")?.asString.orEmpty(),
                algorithm = data.get("algorithm")?.asString.orEmpty(),
                version = data.get("version")?.asString.orEmpty(),
                format = data.get("format")?.asString.orEmpty()
            )
        }
    }

    private suspend fun issueRuntimePackageTicket(appId: String, version: String, deviceId: String): RuntimePackageTicket =
        withContext(Dispatchers.IO) {
            val response = executeRequestWithRequiredAuthRetry { token ->
                val url = "${BackendConfig.apiBaseUrl}/game/$appId/runtime-ticket"
                    .toHttpUrlOrNull()
                    ?.newBuilder()
                    ?.addQueryParameter("version", version)
                    ?.build()
                    ?: throw IOException("Invalid runtime package ticket url")
                Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("X-Nexus-Device-Id", deviceId)
                    .addHeader("X-Nexus-Supported-Package-Formats", SUPPORTED_PACKAGE_FORMATS)
                    .build()
            } ?: throw IOException("Missing valid authorization token")

            response.use { http ->
                if (!http.isSuccessful) {
                    throw IOException("Failed to issue runtime package ticket: ${http.code}")
                }
                val root = gson.fromJson(http.body?.string().orEmpty(), JsonObject::class.java)
                val code = root?.get("code")?.asInt ?: -1
                if (code != 0) {
                    throw IOException(root?.get("message")?.asString ?: "Runtime package ticket request failed")
                }
                val data = root.getAsJsonObject("data") ?: throw IOException("Runtime package ticket missing data")
                RuntimePackageTicket(
                    ticket = data.get("ticket")?.asString.orEmpty(),
                    expiresInSeconds = data.get("expiresInSeconds")?.asLong ?: 0L,
                    version = data.get("version")?.asString.orEmpty(),
                    format = data.get("format")?.asString.orEmpty()
                )
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
            val newbieMustPlay = data.getAsJsonArray("newbieMustPlay")
                ?.mapNotNull { it?.asJsonObject?.toGame() }
                ?: emptyList()
            val everyonePlaying = data.getAsJsonArray("everyonePlaying")
                ?.mapNotNull { it?.asJsonObject?.toGame() }
                ?: emptyList()
            LibraryHomeSnapshot(
                currentPlayingGame = current,
                recentGames = recent,
                myGames = myGames,
                newbieMustPlay = newbieMustPlay,
                everyonePlaying = everyonePlaying,
                favoriteCount = data.get("favoriteCount")?.asLong ?: 0L,
                shareCount = data.get("shareCount")?.asLong ?: 0L
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

    suspend fun markShared(appId: String) = withContext(Dispatchers.IO) {
        runCatching {
            executeRequestWithRequiredAuthRetry { token ->
                Request.Builder()
                    .url("${BackendConfig.apiBaseUrl}/library/$appId/share")
                    .addHeader("Authorization", "Bearer $token")
                    .post("{}".toRequestBody(null))
                    .build()
            }?.use { /* best effort */ }
        }
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

    suspend fun sendVerificationCode(email: String, purpose: String, scene: String? = null): Boolean = withContext(Dispatchers.IO) {
        return@withContext sendVerificationCodeResult(email, purpose, scene).success
    }

    suspend fun sendVerificationCodeResult(email: String, purpose: String, scene: String? = null): ApiActionResult = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            val payload = JsonObject().apply {
                addProperty("email", email)
                addProperty("purpose", purpose)
                if (!scene.isNullOrBlank()) {
                    addProperty("scene", scene)
                }
            }
            val body = gson.toJson(payload).toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("${BackendConfig.apiBaseUrl}/user/send-code")
                .addHeader("X-Client-Source", "android-client")
                .addHeader("X-Client-Scene", scene ?: "")
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                val root = gson.fromJson(raw, JsonObject::class.java)
                val message = root?.get("message")?.asString
                val success = response.isSuccessful && (root?.get("code")?.asInt ?: -1) == 0
                ApiActionResult(success = success, message = message)
            }
        }.getOrElse { e ->
            ApiActionResult(success = false, message = e.message)
        }
    }

    suspend fun resetPassword(email: String, code: String, newPassword: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext resetPasswordResult(email, code, newPassword).success
    }

    suspend fun resetPasswordResult(email: String, code: String, newPassword: String): ApiActionResult = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            val payload = JsonObject().apply {
                addProperty("email", email)
                addProperty("code", code)
                addProperty("newPassword", newPassword)
            }
            val body = gson.toJson(payload).toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("${BackendConfig.apiBaseUrl}/user/password/reset")
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                val root = gson.fromJson(raw, JsonObject::class.java)
                val message = root?.get("message")?.asString
                val success = response.isSuccessful && (root?.get("code")?.asInt ?: -1) == 0
                ApiActionResult(success = success, message = message)
            }
        }.getOrElse { e ->
            ApiActionResult(success = false, message = e.message)
        }
    }

    suspend fun getLegalLinks(): LegalLinks = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            val request = Request.Builder()
                .url("${BackendConfig.apiBaseUrl}/public/legal/config")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@use fallbackLegalLinks()
                }
                val root = gson.fromJson(response.body?.string().orEmpty(), JsonObject::class.java)
                val code = root?.get("code")?.asInt ?: -1
                if (code != 0) {
                    return@use fallbackLegalLinks()
                }
                val data = root.getAsJsonObject("data")
                val termsUrl = data?.get("termsUrl")?.asString
                val privacyUrl = data?.get("privacyUrl")?.asString
                if (termsUrl.isNullOrBlank() || privacyUrl.isNullOrBlank()) {
                    fallbackLegalLinks()
                } else {
                    LegalLinks(termsUrl = termsUrl, privacyUrl = privacyUrl)
                }
            }
        }.getOrElse { fallbackLegalLinks() }
    }

    suspend fun changePassword(email: String, code: String, newPassword: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            executeRequestWithRequiredAuthRetry { token ->
                val payload = JsonObject().apply {
                    addProperty("email", email)
                    addProperty("code", code)
                    addProperty("newPassword", newPassword)
                }
                Request.Builder()
                    .url("${BackendConfig.apiBaseUrl}/user/password/change")
                    .addHeader("Authorization", "Bearer $token")
                    .post(gson.toJson(payload).toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()
            }?.use { response ->
                if (!response.isSuccessful) return@use false
                val root = gson.fromJson(response.body?.string().orEmpty(), JsonObject::class.java)
                (root?.get("code")?.asInt ?: -1) == 0
            } ?: false
        }.getOrDefault(false)
    }

    suspend fun getDeviceSessions(): List<DeviceSession> = withContext(Dispatchers.IO) {
        executeRequestWithRequiredAuthRetry { token ->
            Request.Builder()
                .url("${BackendConfig.apiBaseUrl}/user/devices")
                .addHeader("Authorization", "Bearer $token")
                .build()
        }?.use { response ->
            if (!response.isSuccessful) return@withContext emptyList<DeviceSession>()
            val root = gson.fromJson(response.body?.string().orEmpty(), JsonObject::class.java) ?: return@withContext emptyList()
            if ((root.get("code")?.asInt ?: -1) != 0) return@withContext emptyList()
            val data = root.getAsJsonArray("data") ?: JsonArray()
            data.mapNotNull { node ->
                val obj = node?.asJsonObject ?: return@mapNotNull null
                DeviceSession(
                    deviceId = obj.get("deviceId")?.asString.orEmpty(),
                    deviceName = obj.get("deviceName")?.asString.orEmpty(),
                    model = obj.get("model")?.asString.orEmpty(),
                    ip = obj.get("ip")?.asString.orEmpty(),
                    lastActiveAt = obj.get("lastActiveAt")?.asString.orEmpty(),
                    current = obj.get("current")?.asBoolean ?: false
                )
            }
        } ?: emptyList()
    }

    suspend fun kickDevice(deviceId: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            executeRequestWithRequiredAuthRetry { token ->
                Request.Builder()
                    .url("${BackendConfig.apiBaseUrl}/user/devices/$deviceId/kick")
                    .addHeader("Authorization", "Bearer $token")
                    .post("{}".toRequestBody(null))
                    .build()
            }?.use { response ->
                if (!response.isSuccessful) return@use false
                val root = gson.fromJson(response.body?.string().orEmpty(), JsonObject::class.java)
                (root?.get("code")?.asInt ?: -1) == 0
            } ?: false
        }.getOrDefault(false)
    }

    suspend fun logoutAllDevices(): Boolean = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            executeRequestWithRequiredAuthRetry { token ->
                Request.Builder()
                    .url("${BackendConfig.apiBaseUrl}/user/logout-all")
                    .addHeader("Authorization", "Bearer $token")
                    .post("{}".toRequestBody(null))
                    .build()
            }?.use { response ->
                if (!response.isSuccessful) return@use false
                val root = gson.fromJson(response.body?.string().orEmpty(), JsonObject::class.java)
                (root?.get("code")?.asInt ?: -1) == 0
            } ?: false
        }.getOrDefault(false)
    }

    suspend fun terminateAccount(confirmText: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            executeRequestWithRequiredAuthRetry { token ->
                val payload = JsonObject().apply { addProperty("confirmText", confirmText) }
                Request.Builder()
                    .url("${BackendConfig.apiBaseUrl}/user/terminate")
                    .addHeader("Authorization", "Bearer $token")
                    .post(gson.toJson(payload).toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()
            }?.use { response ->
                if (!response.isSuccessful) return@use false
                val root = gson.fromJson(response.body?.string().orEmpty(), JsonObject::class.java)
                (root?.get("code")?.asInt ?: -1) == 0
            } ?: false
        }.getOrDefault(false)
    }

    suspend fun getBillingRecords(limit: Int = 30): List<BillingRecord> = withContext(Dispatchers.IO) {
        executeRequestWithRequiredAuthRetry { token ->
            Request.Builder()
                .url("${BackendConfig.apiBaseUrl}/wallet/billing/list?limit=${limit.coerceIn(1, 100)}")
                .addHeader("Authorization", "Bearer $token")
                .build()
        }?.use { response ->
            if (!response.isSuccessful) return@withContext emptyList<BillingRecord>()
            val root = gson.fromJson(response.body?.string().orEmpty(), JsonObject::class.java) ?: return@withContext emptyList()
            if ((root.get("code")?.asInt ?: -1) != 0) return@withContext emptyList()
            val data = root.getAsJsonArray("data") ?: JsonArray()
            data.mapNotNull { node ->
                val obj = node?.asJsonObject ?: return@mapNotNull null
                BillingRecord(
                    id = obj.get("id")?.asLong ?: 0L,
                    type = obj.get("type")?.asString.orEmpty(),
                    title = obj.get("title")?.asString.orEmpty(),
                    subtitle = obj.get("subtitle")?.asString.orEmpty(),
                    amount = obj.get("amount")?.asString.orEmpty(),
                    createdAt = obj.get("createdAt")?.asString.orEmpty()
                )
            }
        } ?: emptyList()
    }

    suspend fun getBillingDetail(id: Long): BillingDetail? = withContext(Dispatchers.IO) {
        executeRequestWithRequiredAuthRetry { token ->
            Request.Builder()
                .url("${BackendConfig.apiBaseUrl}/wallet/billing/$id")
                .addHeader("Authorization", "Bearer $token")
                .build()
        }?.use { response ->
            if (!response.isSuccessful) return@withContext null
            val root = gson.fromJson(response.body?.string().orEmpty(), JsonObject::class.java) ?: return@withContext null
            if ((root.get("code")?.asInt ?: -1) != 0) return@withContext null
            val data = root.getAsJsonObject("data") ?: return@withContext null
            BillingDetail(
                id = data.get("id")?.asLong ?: 0L,
                type = data.get("type")?.asString.orEmpty(),
                title = data.get("title")?.asString.orEmpty(),
                subtitle = data.get("subtitle")?.asString.orEmpty(),
                amount = data.get("amount")?.asString.orEmpty(),
                createdAt = data.get("createdAt")?.asString.orEmpty(),
                receiptUrl = data.get("receiptUrl")?.asString.orEmpty()
            )
        } ?: null
    }

    suspend fun getReferralSummary(): ReferralSummary? = withContext(Dispatchers.IO) {
        executeRequestWithRequiredAuthRetry { token ->
            Request.Builder()
                .url("${BackendConfig.apiBaseUrl}/referral/summary")
                .addHeader("Authorization", "Bearer $token")
                .build()
        }?.use { response ->
            if (!response.isSuccessful) return@withContext null
            val root = gson.fromJson(response.body?.string().orEmpty(), JsonObject::class.java) ?: return@withContext null
            if ((root.get("code")?.asInt ?: -1) != 0) return@withContext null
            val data = root.getAsJsonObject("data") ?: return@withContext null
            ReferralSummary(
                inviteCount = data.get("inviteCount")?.asLong ?: 0L,
                totalReward = data.get("totalReward")?.asString.orEmpty(),
                referralLink = data.get("referralLink")?.asString.orEmpty()
            )
        } ?: null
    }

    suspend fun getReferralRecords(limit: Int = 20): List<ReferralRecord> = withContext(Dispatchers.IO) {
        executeRequestWithRequiredAuthRetry { token ->
            Request.Builder()
                .url("${BackendConfig.apiBaseUrl}/referral/records?limit=${limit.coerceIn(1, 100)}")
                .addHeader("Authorization", "Bearer $token")
                .build()
        }?.use { response ->
            if (!response.isSuccessful) return@withContext emptyList<ReferralRecord>()
            val root = gson.fromJson(response.body?.string().orEmpty(), JsonObject::class.java) ?: return@withContext emptyList()
            if ((root.get("code")?.asInt ?: -1) != 0) return@withContext emptyList()
            val data = root.getAsJsonObject("data") ?: return@withContext emptyList()
            val records = data.getAsJsonArray("records") ?: JsonArray()
            records.mapNotNull { node ->
                val obj = node?.asJsonObject ?: return@mapNotNull null
                ReferralRecord(
                    id = obj.get("id")?.asLong ?: 0L,
                    title = obj.get("title")?.asString.orEmpty(),
                    subtitle = obj.get("subtitle")?.asString.orEmpty(),
                    reward = obj.get("reward")?.asString.orEmpty(),
                    createdAt = obj.get("createdAt")?.asString.orEmpty()
                )
            }
        } ?: emptyList()
    }

    suspend fun markReferralShared(channel: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            executeRequestWithRequiredAuthRetry { token ->
                val payload = JsonObject().apply { addProperty("channel", channel) }
                Request.Builder()
                    .url("${BackendConfig.apiBaseUrl}/referral/share")
                    .addHeader("Authorization", "Bearer $token")
                    .post(gson.toJson(payload).toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()
            }?.use { response ->
                if (!response.isSuccessful) return@use false
                val root = gson.fromJson(response.body?.string().orEmpty(), JsonObject::class.java)
                (root?.get("code")?.asInt ?: -1) == 0
            } ?: false
        }.getOrDefault(false)
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

    private fun fallbackLegalLinks(): LegalLinks {
        return LegalLinks(
            termsUrl = "${BackendConfig.apiBaseUrl}/public/legal/terms",
            privacyUrl = "${BackendConfig.apiBaseUrl}/public/legal/privacy"
        )
    }
}
