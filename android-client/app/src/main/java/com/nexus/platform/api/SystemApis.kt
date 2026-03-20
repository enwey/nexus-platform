package com.nexus.platform.api

import android.content.Context
import com.google.gson.JsonObject

class LoginApi(private val context: Context) : ApiHandler {
    override suspend fun handle(api: String, params: JsonObject): Any? {
        return mapOf(
            "code" to "mock_code_${System.currentTimeMillis()}",
            "errMsg" to "login:ok"
        )
    }
}

class SystemInfoApi(private val context: Context) : ApiHandler {
    override suspend fun handle(api: String, params: JsonObject): Any? {
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
}
