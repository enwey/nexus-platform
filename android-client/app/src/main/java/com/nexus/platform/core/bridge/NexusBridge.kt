package com.nexus.platform.core.bridge

import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nexus.platform.core.bridge.api.*
import kotlinx.coroutines.*

/**
 * JavaScript bridge for communication between WebView and native code.
 */
class NexusBridge(
    private val context: Context,
    private val webView: WebView,
    private val runtimeMetricsProvider: RuntimeMetricsProvider? = null
) {
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val apiHandlers = mutableMapOf<String, ApiHandler>()

    init {
        registerApiHandlers()
    }

    /**
     * Register API handlers.
     */
    private fun registerApiHandlers() {
        apiHandlers["wx.login"] = LoginApi(context)
        apiHandlers["wx.request"] = RequestApi(context)
        val systemInfoApi = SystemInfoApi(context, runtimeMetricsProvider)
        apiHandlers["wx.getSystemInfoSync"] = systemInfoApi
        apiHandlers["wx.getMenuButtonBoundingClientRect"] = systemInfoApi
        val updateApi = UpdateApi(runtimeMetricsProvider)
        apiHandlers["wx.update.check"] = updateApi
        apiHandlers["wx.update.apply"] = updateApi
        apiHandlers["wx.setStorage"] = StorageApi(context)
        apiHandlers["wx.getStorage"] = StorageApi(context)
        apiHandlers["wx.removeStorage"] = StorageApi(context)
        apiHandlers["wx.clearStorage"] = StorageApi(context)
        apiHandlers["wx.getUserInfo"] = UserInfoApi(context)
        apiHandlers["wx.shareAppMessage"] = ShareApi(context)
        apiHandlers["wx.showToast"] = ToastApi(context)
        apiHandlers["wx.showModal"] = ModalApi(context)
        apiHandlers["wx.downloadFile"] = FileApi(context)
        apiHandlers["wx.uploadFile"] = FileApi(context)
        apiHandlers["wx.getNetworkType"] = NetworkApi(context)
        apiHandlers["wx.chooseImage"] = ImageApi(context)
        apiHandlers["wx.setClipboardData"] = ClipboardApi(context)
        apiHandlers["wx.getClipboardData"] = ClipboardApi(context)
        apiHandlers["wx.vibrateShort"] = VibrateApi(context)
        apiHandlers["wx.vibrateLong"] = VibrateApi(context)
    }

    /**
     * Receive message from JavaScript.
     */
    @JavascriptInterface
    fun postMessage(message: String) {
        try {
            val json = gson.fromJson(message, JsonObject::class.java)
            val api = json.get("api")?.asString ?: return
            val callbackId = json.get("callbackId")?.asString ?: return
            val params = json.getAsJsonObject("params")

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
                    e.printStackTrace()
                    sendCallback(callbackId, null, e.message)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun createSyncBridge(): NexusSyncBridge {
        return NexusSyncBridge(gson, apiHandlers)
    }

    /**
     * Send callback to JavaScript.
     */
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

    /**
     * Cleanup resources.
     */
    fun cleanup() {
        scope.cancel()
    }
}