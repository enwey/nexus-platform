package com.nexus.platform.core.bridge.api

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import androidx.core.net.toUri
import com.google.gson.JsonObject
import com.nexus.platform.data.remote.PlatformBackendApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URLConnection
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Handle vibration APIs.
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
 * Handle clipboard APIs.
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
                    clip.getItemAt(0).coerceToText(context).toString()
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
 * Handle user info APIs.
 */
class UserInfoApi(private val context: Context) : ApiHandler {
    private val backendApi = PlatformBackendApi(context)

    override suspend fun handle(api: String, params: JsonObject): Any? {
        val profile = runCatching { backendApi.getUserProfile() }.getOrNull()
        val nick = profile?.username?.ifBlank { profile.email } ?: "Guest"
        val locale = java.util.Locale.getDefault()
        return mapOf(
            "userInfo" to mapOf(
                "nickName" to nick,
                "avatarUrl" to (profile?.avatarUrl ?: ""),
                "gender" to 0,
                "language" to locale.toLanguageTag(),
                "city" to "",
                "province" to "",
                "country" to locale.displayCountry
            ),
            "errMsg" to "getUserInfo:ok"
        )
    }
}

/**
 * Handle share APIs.
 */
class ShareApi(private val context: Context) : ApiHandler {
    override suspend fun handle(api: String, params: JsonObject): Any? {
        return mapOf("errMsg" to "shareAppMessage:ok")
    }
}

/**
 * Handle image APIs.
 */
class ImageApi(private val context: Context) : ApiHandler {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun handle(api: String, params: JsonObject): Any? {
        return when (api) {
            "wx.chooseImage" -> chooseImage(params)
            "wx.previewImage" -> previewImage(params)
            "wx.getImageInfo" -> getImageInfo(params)
            "wx.saveImageToPhotosAlbum" -> saveImageToPhotosAlbum(params)
            else -> null
        }
    }

    private suspend fun chooseImage(params: JsonObject): Map<String, Any> = withContext(Dispatchers.IO) {
        val explicit = params.get("filePath")?.asString?.takeIf { it.isNotBlank() }
        if (explicit != null) {
            return@withContext mapOf(
                "tempFilePaths" to listOf(explicit),
                "errMsg" to "chooseImage:ok"
            )
        }

        val count = params.get("count")?.asInt?.coerceIn(1, 9) ?: 1
        val items = mutableListOf<String>()
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val sort = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sort
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext() && items.size < count) {
                val id = cursor.getLong(idCol)
                val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                items.add(uri.toString())
            }
        }

        if (items.isEmpty()) {
            mapOf("errMsg" to "chooseImage:fail no image found")
        } else {
            mapOf("tempFilePaths" to items, "errMsg" to "chooseImage:ok")
        }
    }

    private suspend fun previewImage(params: JsonObject): Map<String, Any> = withContext(Dispatchers.Main) {
        val current = params.get("current")?.asString
            ?: params.getAsJsonArray("urls")?.firstOrNull()?.asString
            ?: return@withContext mapOf("errMsg" to "previewImage:fail missing url")
        val uri = parseUri(current)
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "image/*")
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return@withContext runCatching {
            context.startActivity(intent)
            mapOf("errMsg" to "previewImage:ok")
        }.getOrElse {
            mapOf("errMsg" to "previewImage:fail ${it.message}")
        }
    }

    private suspend fun getImageInfo(params: JsonObject): Map<String, Any> = withContext(Dispatchers.IO) {
        val src = params.get("src")?.asString ?: return@withContext mapOf("errMsg" to "getImageInfo:fail missing src")
        return@withContext runCatching {
            val (width, height) = decodeSize(src)
            val type = inferImageType(src)
            mapOf(
                "width" to width,
                "height" to height,
                "path" to src,
                "orientation" to "up",
                "type" to type,
                "errMsg" to "getImageInfo:ok"
            )
        }.getOrElse {
            mapOf("errMsg" to "getImageInfo:fail ${it.message}")
        }
    }

    private suspend fun saveImageToPhotosAlbum(params: JsonObject): Map<String, Any> = withContext(Dispatchers.IO) {
        val path = params.get("filePath")?.asString ?: return@withContext mapOf("errMsg" to "saveImageToPhotosAlbum:fail missing filePath")
        return@withContext runCatching {
            val input = openInputStream(path) ?: throw IllegalStateException("Unable to read image")
            val fileName = "nexus_${System.currentTimeMillis()}.${inferImageType(path)}"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, URLConnection.guessContentTypeFromName(fileName) ?: "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/BringBox")
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("Failed to create media item")
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    input.use { it.copyTo(output) }
                } ?: throw IllegalStateException("Failed to open output stream")
                mapOf("savedFilePath" to uri.toString(), "errMsg" to "saveImageToPhotosAlbum:ok")
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "BringBox")
                if (!dir.exists()) dir.mkdirs()
                val target = File(dir, fileName)
                input.use { src -> FileOutputStream(target).use { dst -> src.copyTo(dst) } }
                MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), null, null)
                mapOf("savedFilePath" to target.absolutePath, "errMsg" to "saveImageToPhotosAlbum:ok")
            }
        }.getOrElse {
            mapOf("errMsg" to "saveImageToPhotosAlbum:fail ${it.message}")
        }
    }

    private fun decodeSize(src: String): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val uri = parseUri(src)
        when (uri.scheme?.lowercase()) {
            "http", "https" -> {
                val req = Request.Builder().url(src).build()
                client.newCall(req).execute().use { rsp ->
                    if (!rsp.isSuccessful) throw IllegalStateException("HTTP ${rsp.code}")
                    val bytes = rsp.body?.bytes() ?: ByteArray(0)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                }
            }
            else -> {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, options)
                } ?: throw IllegalStateException("Unable to open input stream")
            }
        }
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            throw IllegalStateException("Unable to decode image size")
        }
        return options.outWidth to options.outHeight
    }

    private fun inferImageType(path: String): String {
        val lower = path.lowercase()
        return when {
            lower.endsWith(".png") -> "png"
            lower.endsWith(".webp") -> "webp"
            else -> "jpg"
        }
    }

    private fun parseUri(value: String): Uri {
        val parsed = value.toUri()
        return if (parsed.scheme.isNullOrBlank()) {
            File(value).toUri()
        } else {
            parsed
        }
    }

    private fun openInputStream(path: String) = when (parseUri(path).scheme?.lowercase()) {
        "http", "https" -> {
            client.newCall(Request.Builder().url(path).build()).execute().use { rsp ->
                if (!rsp.isSuccessful) {
                    null
                } else {
                    val bytes = rsp.body?.bytes() ?: return@use null
                    ByteArrayInputStream(bytes)
                }
            }
        }
        else -> context.contentResolver.openInputStream(parseUri(path))
    }
}

/**
 * Handle file APIs.
 */
class FileApi(private val context: Context) : ApiHandler {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun handle(api: String, params: JsonObject): Any? {
        return when (api) {
            "wx.downloadFile" -> downloadFile(params)
            "wx.uploadFile" -> uploadFile(params)
            else -> null
        }
    }

    private suspend fun downloadFile(params: JsonObject): Map<String, Any> = withContext(Dispatchers.IO) {
        val url = params.get("url")?.asString ?: return@withContext mapOf("errMsg" to "downloadFile:fail missing url")
        return@withContext runCatching {
            val fileName = params.get("fileName")?.asString?.takeIf { it.isNotBlank() }
                ?: "download_${System.currentTimeMillis()}.bin"
            val target = File(context.cacheDir, fileName)
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) {
                    return@use mapOf("errMsg" to "downloadFile:fail HTTP ${response.code}", "statusCode" to response.code)
                }
                response.body?.byteStream()?.use { input ->
                    FileOutputStream(target).use { output -> input.copyTo(output) }
                } ?: return@use mapOf("errMsg" to "downloadFile:fail empty body", "statusCode" to response.code)
                mapOf(
                    "tempFilePath" to target.absolutePath,
                    "statusCode" to response.code,
                    "fileSize" to target.length(),
                    "errMsg" to "downloadFile:ok"
                )
            }
        }.getOrElse {
            mapOf("errMsg" to "downloadFile:fail ${it.message}", "statusCode" to -1)
        }
    }

    private suspend fun uploadFile(params: JsonObject): Map<String, Any> = withContext(Dispatchers.IO) {
        val url = params.get("url")?.asString ?: return@withContext mapOf("errMsg" to "uploadFile:fail missing url")
        val filePath = params.get("filePath")?.asString ?: return@withContext mapOf("errMsg" to "uploadFile:fail missing filePath")
        val fileNameKey = params.get("name")?.asString?.ifBlank { "file" } ?: "file"
        val localFile = resolveLocalFile(filePath) ?: return@withContext mapOf("errMsg" to "uploadFile:fail file not found")
        val formData = params.getAsJsonObject("formData")

        return@withContext runCatching {
            val mime = URLConnection.guessContentTypeFromName(localFile.name) ?: "application/octet-stream"
            val bodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
            bodyBuilder.addFormDataPart(fileNameKey, localFile.name, localFile.asRequestBody(mime.toMediaTypeOrNull()))
            formData?.entrySet()?.forEach { entry ->
                bodyBuilder.addFormDataPart(entry.key, entry.value.asString)
            }
            val request = Request.Builder()
                .url(url)
                .post(bodyBuilder.build())
                .build()
            client.newCall(request).execute().use { response ->
                val content = response.body?.string().orEmpty()
                mapOf(
                    "data" to content,
                    "statusCode" to response.code,
                    "errMsg" to if (response.isSuccessful) "uploadFile:ok" else "uploadFile:fail HTTP ${response.code}"
                )
            }
        }.getOrElse {
            mapOf("errMsg" to "uploadFile:fail ${it.message}", "statusCode" to -1)
        }
    }

    private fun resolveLocalFile(path: String): File? {
        val uri = path.toUri()
        if (uri.scheme.isNullOrBlank()) {
            val file = File(path)
            return file.takeIf { it.exists() && it.isFile }
        }
        return when (uri.scheme?.lowercase()) {
            "file" -> File(uri.path ?: "").takeIf { it.exists() && it.isFile }
            "content" -> {
                val target = File(context.cacheDir, "upload_${System.currentTimeMillis()}.tmp")
                return runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(target).use { output -> input.copyTo(output) }
                    }
                    target.takeIf { it.exists() && it.isFile }
                }.getOrNull()
            }
            else -> null
        }
    }
}
