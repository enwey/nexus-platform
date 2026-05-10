package com.nexus.platform.core.bridge.api

import android.app.AlertDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nexus.platform.R
import com.nexus.platform.core.network.BackendHttpClientFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import kotlin.coroutines.resume

/**
 * Handle wx.request.
 */
class RequestApi(private val context: Context) : ApiHandler {
    private companion object {
        val client = BackendHttpClientFactory.create(
            connectTimeoutSeconds = 30,
            readTimeoutSeconds = 30,
            writeTimeoutSeconds = 30
        )
        val gson = Gson()
    }

    override suspend fun handle(api: String, params: JsonObject): Any? {
        val url = params.get("url")?.asString ?: return mapOf("errMsg" to "request:fail")
        val method = params.get("method")?.asString ?: "GET"
        val data = params.get("data")
        val header = params.getAsJsonObject("header")

        return withContext(Dispatchers.IO) {
            try {
                val requestBuilder = Request.Builder().url(url)
                header?.keySet()?.forEach { key ->
                    header.get(key)?.asString?.let { value ->
                        requestBuilder.addHeader(key, value)
                    }
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                when (method.uppercase()) {
                    "POST", "PUT", "PATCH", "DELETE" -> {
                        val requestBody = if (data != null) gson.toJson(data).toRequestBody(mediaType) else "".toRequestBody(mediaType)
                        requestBuilder.method(method.uppercase(), requestBody)
                    }
                    else -> requestBuilder.get()
                }

                val response = client.newCall(requestBuilder.build()).execute()
                val responseBody = response.body?.string() ?: ""
                mapOf(
                    "statusCode" to response.code,
                    "data" to responseBody,
                    "header" to response.headers.toMultimap(),
                    "errMsg" to "request:ok"
                )
            } catch (e: IOException) {
                mapOf("errMsg" to "request:fail", "error" to e.message)
            }
        }
    }
}

/**
 * Handle wx.showToast.
 */
class ToastApi(private val context: Context) : ApiHandler {
    override suspend fun handle(api: String, params: JsonObject): Any? {
        val title = params.get("title")?.asString ?: ""
        val duration = params.get("duration")?.asInt ?: 1500
        withContext(Dispatchers.Main) {
            val toastDuration = if (duration >= 2500) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            Toast.makeText(context, title, toastDuration).show()
        }
        return mapOf("errMsg" to "showToast:ok")
    }
}

/**
 * Handle wx.showModal.
 */
class ModalApi(private val context: Context) : ApiHandler {
    override suspend fun handle(api: String, params: JsonObject): Any? {
        val title = params.get("title")?.asString ?: ""
        val content = params.get("content")?.asString ?: ""
        val confirmText = params.get("confirmText")?.asString ?: context.getString(R.string.common_confirm)
        val cancelText = params.get("cancelText")?.asString ?: context.getString(R.string.common_cancel)

        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val dialog = AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(content)
                    .setCancelable(true)
                    .setPositiveButton(confirmText) { d, _ ->
                        d.dismiss()
                        if (continuation.isActive) {
                            continuation.resume(mapOf("confirm" to true, "cancel" to false, "errMsg" to "showModal:ok"))
                        }
                    }
                    .setNegativeButton(cancelText) { d, _ ->
                        d.dismiss()
                        if (continuation.isActive) {
                            continuation.resume(mapOf("confirm" to false, "cancel" to true, "errMsg" to "showModal:ok"))
                        }
                    }
                    .setOnCancelListener {
                        if (continuation.isActive) {
                            continuation.resume(mapOf("confirm" to false, "cancel" to true, "errMsg" to "showModal:ok"))
                        }
                    }
                    .create()

                dialog.show()
                continuation.invokeOnCancellation { dialog.dismiss() }
            }
        }
    }
}

/**
 * Handle wx.getNetworkType.
 */
class NetworkApi(private val context: Context) : ApiHandler {
    override suspend fun handle(api: String, params: JsonObject): Any? {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = manager.activeNetwork
        val capabilities = activeNetwork?.let { manager.getNetworkCapabilities(it) }
        val networkType = when {
            capabilities == null -> "none"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "4g"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "bluetooth"
            else -> "unknown"
        }
        return mapOf(
            "networkType" to networkType,
            "errMsg" to "getNetworkType:ok"
        )
    }
}
