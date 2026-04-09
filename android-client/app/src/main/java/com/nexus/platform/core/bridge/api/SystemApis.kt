package com.nexus.platform.core.bridge.api

import android.content.Context
import com.nexus.platform.data.local.AuthSessionStore
import com.google.gson.JsonObject
import com.nexus.platform.core.bridge.RuntimeMetricsProvider
import java.security.SecureRandom
import kotlin.math.absoluteValue

/**
 * Handle wx.login.
 */
class LoginApi(private val context: Context) : ApiHandler {
    private val sessionStore = AuthSessionStore(context)
    private val random = SecureRandom()

    override suspend fun handle(api: String, params: JsonObject): Any? {
        val token = sessionStore.accessToken().orEmpty()
        val generated = if (token.isNotBlank()) {
            "nxs_${token.hashCode().absoluteValue}_${System.currentTimeMillis()}"
        } else {
            "nxs_guest_${random.nextInt(1_000_000)}_${System.currentTimeMillis()}"
        }
        return mapOf(
            "code" to generated,
            "errMsg" to "login:ok"
        )
    }
}

/**
 * Handle system info related APIs.
 */
class SystemInfoApi(
    private val context: Context,
    private val runtimeMetricsProvider: RuntimeMetricsProvider? = null
) : ApiHandler {
    override suspend fun handle(api: String, params: JsonObject): Any? {
        if (api == "wx.getMenuButtonBoundingClientRect") {
            return runtimeMetricsProvider?.getMenuButtonRect() ?: defaultMenuButtonRect()
        }

        if (runtimeMetricsProvider != null) {
            return runtimeMetricsProvider.getSystemInfo()
        }

        val metrics = context.resources.displayMetrics
        return mapOf(
            "brand" to android.os.Build.BRAND,
            "model" to android.os.Build.MODEL,
            "pixelRatio" to metrics.density,
            "screenWidth" to metrics.widthPixels,
            "screenHeight" to metrics.heightPixels,
            "windowWidth" to metrics.widthPixels,
            "windowHeight" to metrics.heightPixels,
            "language" to java.util.Locale.getDefault().language,
            "version" to "1.0.0",
            "system" to "Android ${android.os.Build.VERSION.RELEASE}",
            "platform" to "android",
            "fontSizeSetting" to 16,
            "SDKVersion" to "1.0.0",
            "benchmarkLevel" to 1,
            "albumAuthorized" to true,
            "cameraAuthorized" to true,
            "locationAuthorized" to true,
            "microphoneAuthorized" to true,
            "notificationAuthorized" to true,
            "bluetoothAuthorized" to true
        )
    }

    private fun defaultMenuButtonRect(): Map<String, Any> {
        val metrics = context.resources.displayMetrics
        val density = metrics.density
        val width = 88f
        val height = 32f
        val right = metrics.widthPixels / density - 12f
        val top = 8f + 24f
        return mapOf(
            "width" to width,
            "height" to height,
            "left" to right - width,
            "right" to right,
            "top" to top,
            "bottom" to top + height
        )
    }
}
