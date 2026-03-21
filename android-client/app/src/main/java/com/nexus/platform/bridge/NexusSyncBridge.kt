package com.nexus.platform.bridge

import android.webkit.JavascriptInterface
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nexus.platform.api.ApiHandler
import kotlinx.coroutines.runBlocking

class NexusSyncBridge(
    private val gson: Gson,
    private val apiHandlers: Map<String, ApiHandler>
) {
    @JavascriptInterface
    fun invokeSync(message: String): String {
        return try {
            val json = gson.fromJson(message, JsonObject::class.java)
            val api = json.get("api")?.asString ?: return errorResponse("", "Missing api")
            val callbackId = json.get("callbackId")?.asString ?: ""
            val params = json.getAsJsonObject("params") ?: JsonObject()
            val handler = apiHandlers[api] ?: return errorResponse(callbackId, "API not implemented: $api")
            val data = runBlocking { handler.handle(api, params) }
            gson.toJson(
                JsonObject().apply {
                    addProperty("callbackId", callbackId)
                    if (data != null) {
                        add("data", gson.toJsonTree(data))
                    }
                }
            )
        } catch (e: Exception) {
            errorResponse("", e.message ?: "Unknown sync bridge error")
        }
    }

    private fun errorResponse(callbackId: String, error: String): String {
        return gson.toJson(
            JsonObject().apply {
                addProperty("callbackId", callbackId)
                add("error", JsonObject().apply {
                    addProperty("errMsg", error)
                    addProperty("code", -1)
                })
            }
        )
    }
}
