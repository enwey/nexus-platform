package com.nexus.platform.bridge

import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nexus.platform.api.ApiHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NexusBridge(private val context: Context, private val webView: WebView) {
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val apiHandlers: Map<String, ApiHandler> = createApiHandlers(context)

    @JavascriptInterface
    fun postMessage(message: String) {
        try {
            val json = gson.fromJson(message, JsonObject::class.java)
            val api = json.get("api")?.asString ?: return
            val callbackId = json.get("callbackId")?.asString ?: return
            val params = json.getAsJsonObject("params") ?: JsonObject()

            scope.launch {
                try {
                    val handler = apiHandlers[api]
                    if (handler != null) {
                        val result = handler.handle(api, params)
                        sendCallback(callbackId, result, null)
                    } else {
                        sendCallback(callbackId, null, "API not implemented: $api")
                    }
                } catch (e: Exception) {
                    sendCallback(callbackId, null, e.message ?: "Unknown bridge error")
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun sendCallback(callbackId: String, data: Any?, error: String?) {
        val response = JsonObject().apply {
            addProperty("callbackId", callbackId)
            if (data != null) {
                add("data", gson.toJsonTree(data))
            }
            if (error != null) {
                add("error", JsonObject().apply {
                    addProperty("errMsg", error)
                    addProperty("code", -1)
                })
            }
        }

        val script = "window.NexusBridgeCallback(${gson.toJson(response)});"
        webView.post {
            webView.evaluateJavascript(script, null)
        }
    }

    fun createSyncBridge(): NexusSyncBridge {
        return NexusSyncBridge(gson, apiHandlers)
    }

    fun cleanup() {
        scope.cancel()
    }
}
