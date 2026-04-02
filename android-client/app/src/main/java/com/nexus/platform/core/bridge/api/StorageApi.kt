package com.nexus.platform.core.bridge.api

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.JsonObject

/**
 * Handle storage APIs backed by SharedPreferences.
 */
class StorageApi(private val context: Context) : ApiHandler {
    private val prefs: SharedPreferences = context.getSharedPreferences("wx_storage", Context.MODE_PRIVATE)
    private val gson = Gson()

    override suspend fun handle(api: String, params: JsonObject): Any? {
        return when (api) {
            "wx.setStorage" -> setStorage(params)
            "wx.getStorage" -> getStorage(params)
            "wx.removeStorage" -> removeStorage(params)
            "wx.clearStorage" -> clearStorage()
            else -> null
        }
    }

    private fun setStorage(params: JsonObject): Map<String, String> {
        val key = params.get("key")?.asString ?: return mapOf("errMsg" to "setStorage:fail")
        val data = params.get("data")
        prefs.edit().putString(key, gson.toJson(data)).apply()
        return mapOf("errMsg" to "setStorage:ok")
    }

    private fun getStorage(params: JsonObject): Map<String, Any?> {
        val key = params.get("key")?.asString ?: return mapOf("errMsg" to "getStorage:fail")
        val data = prefs.getString(key, null)
        return if (data != null) {
            mapOf(
                "data" to gson.fromJson(data, Any::class.java),
                "errMsg" to "getStorage:ok"
            )
        } else {
            mapOf("errMsg" to "getStorage:fail")
        }
    }

    private fun removeStorage(params: JsonObject): Map<String, String> {
        val key = params.get("key")?.asString ?: return mapOf("errMsg" to "removeStorage:fail")
        prefs.edit().remove(key).apply()
        return mapOf("errMsg" to "removeStorage:ok")
    }

    private fun clearStorage(): Map<String, String> {
        prefs.edit().clear().apply()
        return mapOf("errMsg" to "clearStorage:ok")
    }
}
