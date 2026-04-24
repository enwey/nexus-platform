package com.nexus.platform.data.remote

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nexus.platform.core.network.BackendConfig
import com.nexus.platform.core.network.BackendHttpClientFactory
import com.nexus.platform.domain.model.AuthSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class BackendAuthApi {
    private val gson = Gson()
    private val client = BackendHttpClientFactory.create()

    suspend fun login(email: String, password: String): AuthSession = withContext(Dispatchers.IO) {
        authWithCredential(
            path = "/user/login",
            email = email,
            password = password,
            code = null,
            errorPrefix = "Login"
        )
    }

    suspend fun register(email: String, password: String, code: String): AuthSession = withContext(Dispatchers.IO) {
        authWithCredential(
            path = "/user/register",
            email = email,
            password = password,
            code = code,
            errorPrefix = "Register"
        )
    }

    private fun authWithCredential(
        path: String,
        email: String,
        password: String,
        code: String?,
        errorPrefix: String
    ): AuthSession {
        val payload = JsonObject().apply {
            addProperty("email", email)
            addProperty("password", password)
            if (!code.isNullOrBlank()) {
                addProperty("code", code)
                addProperty("accountType", "PLAYER")
            }
        }
        val body = gson.toJson(payload).toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("${BackendConfig.apiBaseUrl}$path")
            .post(body)
            .build()

        return client.newCall(request).execute().use { response ->
            val bodyText = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("$errorPrefix failed: ${response.code}")
            }

            val root = gson.fromJson(bodyText, JsonObject::class.java)
            val code = root.get("code")?.asInt ?: -1
            if (code != 0) {
                throw IOException(root.get("message")?.asString ?: "$errorPrefix failed")
            }

            val data = root.getAsJsonObject("data") ?: throw IOException("$errorPrefix response missing data")
            val accessToken = data.get("token")?.asString ?: throw IOException("$errorPrefix response missing token")
            val refreshToken = data.get("refreshToken")?.asString ?: throw IOException("$errorPrefix response missing refreshToken")
            AuthSession(accessToken, refreshToken)
        }
    }
}
