package com.nexus.platform.core.bridge.api

import com.google.gson.JsonObject
import com.nexus.platform.core.bridge.RuntimeMetricsProvider

class UpdateApi(
    private val runtimeMetricsProvider: RuntimeMetricsProvider?
) : ApiHandler {
    override suspend fun handle(api: String, params: JsonObject): Any? {
        val provider = runtimeMetricsProvider
            ?: return mapOf("hasUpdate" to false, "ready" to false, "errMsg" to "$api:ok")
        return when (api) {
            "wx.update.check" -> provider.checkForUpdate()
            "wx.update.apply" -> provider.applyUpdate()
            else -> mapOf("errMsg" to "$api:ok")
        }
    }
}

