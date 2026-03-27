package com.nexus.platform.core.bridge.api

import android.content.Context
import android.os.Vibrator
import android.os.VibratorManager
import android.content.ClipData
import android.content.ClipboardManager
import com.google.gson.JsonObject
import android.os.Build

/**
 * 闇囧姩API澶勭悊鍣? */
class VibrateApi(private val context: Context) : ApiHandler {
    override suspend fun handle(api: String, params: JsonObject): Any? {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        when (api) {
            "wx.vibrateShort" -> {
                vibrator.vibrate(50)
                return mapOf("errMsg" to "vibrateShort:ok")
            }
            "wx.vibrateLong" -> {
                vibrator.vibrate(400)
                return mapOf("errMsg" to "vibrateLong:ok")
            }
            else -> return null
        }
    }
}

/**
 * 鍓创鏉緼PI澶勭悊鍣? */
class ClipboardApi(private val context: Context) : ApiHandler {
    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    override suspend fun handle(api: String, params: JsonObject): Any? {
        return when (api) {
            "wx.setClipboardData" -> {
                val data = params.get("data")?.asString ?: ""
                val clip = ClipData.newPlainText("text", data)
                clipboard.setPrimaryClip(clip)
                mapOf("errMsg" to "setClipboardData:ok")
            }
            "wx.getClipboardData" -> {
                val clip = clipboard.primaryClip
                val data = if (clip != null && clip.itemCount > 0) {
                    clip.getItemAt(0).text.toString()
                } else {
                    ""
                }
                mapOf("data" to data, "errMsg" to "getClipboardData:ok")
            }
            else -> null
        }
    }
}

/**
 * 鐢ㄦ埛淇℃伅API澶勭悊鍣? */
class UserInfoApi(private val context: Context) : ApiHandler {
    override suspend fun handle(api: String, params: JsonObject): Any? {
        return mapOf(
            "userInfo" to mapOf(
                "nickName" to "娴嬭瘯鐢ㄦ埛",
                "avatarUrl" to "",
                "gender" to 0,
                "language" to "zh_CN",
                "city" to "",
                "province" to "",
                "country" to "涓浗"
            ),
            "errMsg" to "getUserInfo:ok"
        )
    }
}

/**
 * 鍒嗕韩API澶勭悊鍣? */
class ShareApi(private val context: Context) : ApiHandler {
    override suspend fun handle(api: String, params: JsonObject): Any? {
        return mapOf("errMsg" to "shareAppMessage:ok")
    }
}

/**
 * 鍥剧墖API澶勭悊鍣? */
class ImageApi(private val context: Context) : ApiHandler {
    override suspend fun handle(api: String, params: JsonObject): Any? {
        return when (api) {
            "wx.chooseImage" -> {
                mapOf(
                    "tempFilePaths" to listOf<String>(),
                    "errMsg" to "chooseImage:ok"
                )
            }
            "wx.previewImage" -> {
                mapOf("errMsg" to "previewImage:ok")
            }
            "wx.getImageInfo" -> {
                mapOf(
                    "width" to 100,
                    "height" to 100,
                    "path" to "",
                    "orientation" to 0,
                    "type" to "jpg",
                    "errMsg" to "getImageInfo:ok"
                )
            }
            "wx.saveImageToPhotosAlbum" -> {
                mapOf("errMsg" to "saveImageToPhotosAlbum:ok")
            }
            else -> null
        }
    }
}

/**
 * 鏂囦欢API澶勭悊鍣? */
class FileApi(private val context: Context) : ApiHandler {
    override suspend fun handle(api: String, params: JsonObject): Any? {
        return when (api) {
            "wx.downloadFile" -> {
                mapOf(
                    "tempFilePath" to "/path/to/temp/file",
                    "statusCode" to 200,
                    "errMsg" to "downloadFile:ok"
                )
            }
            "wx.uploadFile" -> {
                mapOf(
                    "data" to "",
                    "statusCode" to 200,
                    "errMsg" to "uploadFile:ok"
                )
            }
            else -> null
        }
    }
}

