package com.nexus.platform.feature.splash.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nexus.platform.core.i18n.AppLanguage
import com.nexus.platform.core.i18n.AppLanguageManager
import com.nexus.platform.core.network.BackendConfig
import com.nexus.platform.core.network.BackendHttpClientFactory
import java.io.File
import okhttp3.Request

data class LaunchAdUiModel(
    val imageModel: Any,
    val targetUrl: String,
    val displaySeconds: Int,
    val badge: String,
    val sponsor: String,
    val title: String,
    val description: String,
    val cta: String,
    val footer: String,
    val landingTitle: String
)

private data class LaunchAdCachedPayload(
    val imageVersion: String,
    val localImagePath: String,
    val targetUrl: String,
    val displaySeconds: Int,
    val sponsorZhCn: String,
    val sponsorZhTw: String,
    val sponsorEn: String,
    val titleZhCn: String,
    val titleZhTw: String,
    val titleEn: String,
    val descriptionZhCn: String,
    val descriptionZhTw: String,
    val descriptionEn: String,
    val ctaZhCn: String,
    val ctaZhTw: String,
    val ctaEn: String,
    val footerZhCn: String,
    val footerZhTw: String,
    val footerEn: String
)

class LaunchAdRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val client = BackendHttpClientFactory.create()

    fun loadForDisplay(): LaunchAdUiModel {
        val language = AppLanguageManager.currentLanguage(context)
        val cached = loadCached()
        return if (cached != null && File(cached.localImagePath).exists()) {
            cached.toUiModel(language)
        } else {
            fallback(language)
        }
    }

    fun refreshIfNeeded() {
        val request = Request.Builder()
            .url("${BackendConfig.apiBaseUrl}/launch/ad/current")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return
            }
            val payload = gson.fromJson(response.body?.string().orEmpty(), JsonObject::class.java) ?: return
            if (payload.get("code")?.asInt != 0) {
                return
            }
            val data = payload.getAsJsonObject("data") ?: return
            val imageUrl = data.string("imageUrl")
            val targetUrl = data.string("targetUrl")
            val imageVersion = data.string("imageVersion")
            if (imageUrl.isBlank() || targetUrl.isBlank() || imageVersion.isBlank()) {
                return
            }
            val cached = loadCached()
            if (cached?.imageVersion == imageVersion) {
                return
            }

            val imageRequest = Request.Builder().url(imageUrl).build()
            client.newCall(imageRequest).execute().use { imageResponse ->
                if (!imageResponse.isSuccessful) {
                    return
                }
                val bytes = imageResponse.body?.bytes() ?: return
                if (bytes.isEmpty()) {
                    return
                }
                val directory = File(context.cacheDir, "launch-ad").apply { mkdirs() }
                val extension = imageUrl.substringAfterLast('.', "img").substringBefore('?').ifBlank { "img" }
                val targetFile = File(directory, "launch-ad-$imageVersion.$extension")
                targetFile.writeBytes(bytes)
                if (cached != null && cached.localImagePath != targetFile.absolutePath) {
                    runCatching { File(cached.localImagePath).delete() }
                }
                val next = LaunchAdCachedPayload(
                    imageVersion = imageVersion,
                    localImagePath = targetFile.absolutePath,
                    targetUrl = targetUrl,
                    displaySeconds = data.int("displaySeconds", 4).coerceAtLeast(1),
                    sponsorZhCn = data.string("sponsorZhCn"),
                    sponsorZhTw = data.string("sponsorZhTw"),
                    sponsorEn = data.string("sponsorEn"),
                    titleZhCn = data.string("titleZhCn"),
                    titleZhTw = data.string("titleZhTw"),
                    titleEn = data.string("titleEn"),
                    descriptionZhCn = data.string("descriptionZhCn"),
                    descriptionZhTw = data.string("descriptionZhTw"),
                    descriptionEn = data.string("descriptionEn"),
                    ctaZhCn = data.string("ctaZhCn"),
                    ctaZhTw = data.string("ctaZhTw"),
                    ctaEn = data.string("ctaEn"),
                    footerZhCn = data.string("footerZhCn"),
                    footerZhTw = data.string("footerZhTw"),
                    footerEn = data.string("footerEn")
                )
                prefs.edit().putString(KEY_CACHED_AD, gson.toJson(next)).apply()
            }
        }
    }

    private fun loadCached(): LaunchAdCachedPayload? {
        val raw = prefs.getString(KEY_CACHED_AD, null).orEmpty()
        if (raw.isBlank()) {
            return null
        }
        return runCatching { gson.fromJson(raw, LaunchAdCachedPayload::class.java) }.getOrNull()
    }

    private fun LaunchAdCachedPayload.toUiModel(language: AppLanguage): LaunchAdUiModel {
        val fallback = fallback(language)
        return when (language) {
            AppLanguage.SimplifiedChinese -> fallback.copy(
                imageModel = File(localImagePath),
                targetUrl = targetUrl,
                displaySeconds = displaySeconds.coerceAtLeast(1),
                sponsor = sponsorZhCn.ifBlank { fallback.sponsor },
                title = titleZhCn.ifBlank { fallback.title },
                description = descriptionZhCn.ifBlank { fallback.description },
                cta = ctaZhCn.ifBlank { fallback.cta },
                footer = footerZhCn.ifBlank { fallback.footer }
            )
            AppLanguage.TraditionalChinese -> fallback.copy(
                imageModel = File(localImagePath),
                targetUrl = targetUrl,
                displaySeconds = displaySeconds.coerceAtLeast(1),
                sponsor = sponsorZhTw.ifBlank { fallback.sponsor },
                title = titleZhTw.ifBlank { fallback.title },
                description = descriptionZhTw.ifBlank { fallback.description },
                cta = ctaZhTw.ifBlank { fallback.cta },
                footer = footerZhTw.ifBlank { fallback.footer }
            )
            AppLanguage.English -> fallback.copy(
                imageModel = File(localImagePath),
                targetUrl = targetUrl,
                displaySeconds = displaySeconds.coerceAtLeast(1),
                sponsor = sponsorEn.ifBlank { fallback.sponsor },
                title = titleEn.ifBlank { fallback.title },
                description = descriptionEn.ifBlank { fallback.description },
                cta = ctaEn.ifBlank { fallback.cta },
                footer = footerEn.ifBlank { fallback.footer }
            )
        }
    }

    private fun fallback(language: AppLanguage): LaunchAdUiModel {
        return when (language) {
            AppLanguage.SimplifiedChinese -> LaunchAdUiModel(
                imageModel = DEFAULT_IMAGE_URL,
                targetUrl = DEFAULT_TARGET_URL,
                displaySeconds = 4,
                badge = "广告",
                sponsor = "BringBox 官方活动",
                title = "新用户限时福利",
                description = "登录 BringBox，领取限时试玩权益与精选礼包，热门游戏开局更轻松。",
                cta = "立即查看",
                footer = "点击查看活动详情",
                landingTitle = "BringBox 活动"
            )
            AppLanguage.TraditionalChinese -> LaunchAdUiModel(
                imageModel = DEFAULT_IMAGE_URL,
                targetUrl = DEFAULT_TARGET_URL,
                displaySeconds = 4,
                badge = "廣告",
                sponsor = "BringBox 官方活動",
                title = "新用戶限時福利",
                description = "登入 BringBox，領取限時試玩權益與精選禮包，熱門遊戲開局更輕鬆。",
                cta = "立即查看",
                footer = "點擊查看活動詳情",
                landingTitle = "BringBox 活動"
            )
            AppLanguage.English -> LaunchAdUiModel(
                imageModel = DEFAULT_IMAGE_URL,
                targetUrl = DEFAULT_TARGET_URL,
                displaySeconds = 4,
                badge = "Ad",
                sponsor = "BringBox Official Campaign",
                title = "New Player Rewards",
                description = "Sign in to BringBox for limited-time trial perks and curated bundles for trending games.",
                cta = "View Now",
                footer = "Tap to view campaign details",
                landingTitle = "BringBox Campaign"
            )
        }
    }

    private fun JsonObject.string(key: String): String {
        return get(key)?.takeIf { !it.isJsonNull }?.asString.orEmpty()
    }

    private fun JsonObject.int(key: String, fallback: Int): Int {
        return get(key)?.takeIf { !it.isJsonNull }?.asInt ?: fallback
    }

    private companion object {
        private const val PREF_NAME = "launch_ad_cache"
        private const val KEY_CACHED_AD = "cached_ad"
        private const val DEFAULT_IMAGE_URL =
            "https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&w=1200&q=80"
        private const val DEFAULT_TARGET_URL = "https://bringbox.com/download"
    }
}
