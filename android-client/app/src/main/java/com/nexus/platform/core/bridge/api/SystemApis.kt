package com.nexus.platform.core.bridge.api

import android.content.Context
import com.google.gson.JsonObject
import com.nexus.platform.core.bridge.RuntimeMetricsProvider

/**
 * 鐧诲綍API澶勭悊鍣? */
class LoginApi(private val context: Context) : ApiHandler {
    override suspend fun handle(api: String, params: JsonObject): Any? {
        return mapOf(
            "code" to "mock_code_${System.currentTimeMillis()}",
            "errMsg" to "login:ok"
        )
    }
}

/**
 * 绯荤粺淇℃伅API澶勭悊鍣? */
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

