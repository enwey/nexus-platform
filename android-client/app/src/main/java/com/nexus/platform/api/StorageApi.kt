package com.nexus.platform.api

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.JsonObject

/**
 * 存储API处理器，提供本地数据存储功能
 */
class StorageApi(private val context: Context) : ApiHandler {
    private val prefs: SharedPreferences = context.getSharedPreferences("wx_storage", Context.MODE_PRIVATE)
    private val gson = Gson()

    override suspend fun handle(api: String, params: JsonObject): Any? {
        return when (api) {
            "wx.setStorage", "wx.setStorageSync" -> setStorage(params)
            "wx.getStorage", "wx.getStorageSync" -> getStorage(params)
            "wx.removeStorage", "wx.removeStorageSync" -> removeStorage(params)
            "wx.clearStorage", "wx.clearStorageSync" -> clearStorage()
            else -> null
        }
    }

    /**
     * 设置存储数据
     * @param params 包含key和data的参数
     * @return 操作结果
     */
    private fun setStorage(params: JsonObject): Map<String, String> {
        val key = params.get("key")?.asString ?: return mapOf("errMsg" to "setStorage:fail")
        val data = params.get("data")
        
        prefs.edit().putString(key, gson.toJson(data)).apply()
        return mapOf("errMsg" to "setStorage:ok")
    }

    /**
     * 获取存储数据
     * @param params 包含key的参数
     * @return 存储的数据
     */
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

    /**
     * 删除存储数据
     * @param params 包含key的参数
     * @return 操作结果
     */
    private fun removeStorage(params: JsonObject): Map<String, String> {
        val key = params.get("key")?.asString ?: return mapOf("errMsg" to "removeStorage:fail")
        prefs.edit().remove(key).apply()
        return mapOf("errMsg" to "removeStorage:ok")
    }

    /**
     * 清空所有存储数据
     * @return 操作结果
     */
    private fun clearStorage(): Map<String, String> {
        prefs.edit().clear().apply()
        return mapOf("errMsg" to "clearStorage:ok")
    }
}
