package com.nexus.platform.core.bridge.api

import com.google.gson.JsonObject

/**
 * Bridge API handler interface.
 */
interface ApiHandler {
    /**
     * Handle a bridge API call.
     */
    suspend fun handle(api: String, params: JsonObject): Any?
}
