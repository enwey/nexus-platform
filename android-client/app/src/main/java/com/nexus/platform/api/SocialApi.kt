package com.nexus.platform.api

import android.content.Context
import com.google.gson.JsonObject

class SocialApi(private val context: Context) : ApiHandler {
    override suspend fun handle(api: String, params: JsonObject): Any {
        return mapOf(
            "errMsg" to "$api:fail",
            "error" to "Social API is not wired in Android client yet"
        )
    }
}
