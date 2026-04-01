package com.nexus.platform.core.bridge.api

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import com.google.gson.JsonObject

/**
 * 震动API处理器
 */
class VibrateApi(private val context: Context) : ApiHandler {
    override suspend fun handle(api: String, params: JsonObject): Any? {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        return when (api) {
            "wx.vibrateShort" -> {
                vibrator.vibrate(50)
                mapOf("errMsg" to "vibrateShort:ok")
            }
            "wx.vibrateLong" -> {
                vibrator.vibrate(400)
                mapOf("errMsg" to "vibrateLong:ok")
            }
            else -> null
        }
    }
}

/**
 * 剪贴板API处理器
 */
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
 * 用户信息API处理器
 */
class UserInfoApi(private val context: Context) : ApiHandler {
    override suspend fun handle(api: String, params: JsonObject): Any? {
        return mapOf(
            "userInfo" to mapOf(
                "nickName" to "测试用户",
                "avatarUrl" to "",
                "gender" to 0,
                "language" to "zh_CN",
                "city" to "",
                "province" to "",
                "country" to "中国"
            ),
            "errMsg" to "getUserInfo:ok"
        )
    }
}

/**
 * 分享API处理器
 */
class ShareApi(private val context: Context) : ApiHandler {
    override suspend fun handle(api: String, params: JsonObject): Any? {
        return mapOf("errMsg" to "shareAppMessage:ok")
    }
}

/**
 * 图片API处理器
 */
class ImageApi(private val context: Context) : ApiHandler {
    override suspend fun handle(api: String, params: JsonObject): Any? {
        return when (api) {
            "wx.chooseImage" -> {
                mapOf(
                    "tempFilePaths" to listOf<String>(),
                    "errMsg" to "chooseImage:ok"
                )
            }
            "wx.previewImage" -> mapOf("errMsg" to "previewImage:ok")
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
            "wx.saveImageToPhotosAlbum" -> mapOf("errMsg" to "saveImageToPhotosAlbum:ok")
            else -> null
        }
    }
}

/**
 * 文件API处理器
 */
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