package com.nexus.platform.api

import com.google.gson.JsonObject

interface ApiHandler {
    suspend fun handle(api: String, params: JsonObject): Any?
}
