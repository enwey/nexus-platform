package com.nexus.platform.core.bridge.api

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.JsonObject

/**
 * 瀛樺偍API澶勭悊鍣紝鎻愪緵鏈湴鏁版嵁瀛樺偍鍔熻兘
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

    /**
     * 璁剧疆瀛樺偍鏁版嵁
     * @param params 鍖呭惈key鍜宒ata鐨勫弬鏁?     * @return 鎿嶄綔缁撴灉
     */
    private fun setStorage(params: JsonObject): Map<String, String> {
        val key = params.get("key")?.asString ?: return mapOf("errMsg" to "setStorage:fail")
        val data = params.get("data")
        
        prefs.edit().putString(key, gson.toJson(data)).apply()
        return mapOf("errMsg" to "setStorage:ok")
    }

    /**
     * 鑾峰彇瀛樺偍鏁版嵁
     * @param params 鍖呭惈key鐨勫弬鏁?     * @return 瀛樺偍鐨勬暟鎹?     */
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
     * 鍒犻櫎瀛樺偍鏁版嵁
     * @param params 鍖呭惈key鐨勫弬鏁?     * @return 鎿嶄綔缁撴灉
     */
    private fun removeStorage(params: JsonObject): Map<String, String> {
        val key = params.get("key")?.asString ?: return mapOf("errMsg" to "removeStorage:fail")
        prefs.edit().remove(key).apply()
        return mapOf("errMsg" to "removeStorage:ok")
    }

    /**
     * 娓呯┖鎵€鏈夊瓨鍌ㄦ暟鎹?     * @return 鎿嶄綔缁撴灉
     */
    private fun clearStorage(): Map<String, String> {
        prefs.edit().clear().apply()
        return mapOf("errMsg" to "clearStorage:ok")
    }
}

