package com.nexus.platform.api

import android.content.Context
import android.widget.Toast
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class RequestApi(private val context: Context) : ApiHandler {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

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
                val body = when (method.uppercase()) {
                    "POST", "PUT", "PATCH" -> {
                        val requestBody = if (data != null) {
                            gson.toJson(data).toRequestBody(mediaType)
                        } else {
                            "".toRequestBody(mediaType)
                        }
                        requestBuilder.method(method, requestBody)
                    }
                    else -> requestBuilder.get()
                }

                val response = client.newRequest(requestBuilder.build()).execute()
                val responseBody = response.body?.string() ?: ""

                mapOf(
                    "statusCode" to response.code,
                    "data" to responseBody,
                    "errMsg" to "request:ok"
                )
            } catch (e: IOException) {
                e.printStackTrace()
                mapOf("errMsg" to "request:fail", "error" to e.message)
            }
        }
    }

    private val gson = com.google.gson.Gson()
}

class ToastApi(private val context: Context) : ApiHandler {
    override suspend fun handle(api: String, params: JsonObject): Any? {
        val title = params.get("title")?.asString ?: ""
        val duration = params.get("duration")?.asInt ?: 1500

        withContext(Dispatchers.Main) {
            Toast.makeText(context, title, duration).show()
        }

        return mapOf("errMsg" to "showToast:ok")
    }
}

class ModalApi(private val context: Context) : ApiHandler {
    override suspend fun handle(api: String, params: JsonObject): Any? {
        val title = params.get("title")?.asString ?: ""
        val content = params.get("content")?.asString ?: ""
        val confirmText = params.get("confirmText")?.asString ?: "确定"
        val cancelText = params.get("cancelText")?.asString ?: "取消"

        return mapOf(
            "confirm" to true,
            "cancel" to false,
            "errMsg" to "showModal:ok"
        )
    }
}

class NetworkApi(private val context: Context) : ApiHandler {
    override suspend fun handle(api: String, params: JsonObject): Any? {
        return mapOf(
            "networkType" to "wifi",
            "errMsg" to "getNetworkType:ok"
        )
    }
}
