package com.nexus.platform.api

import com.google.gson.JsonObject

/**
 * Returns a consistent stub response for bridge APIs that are declared but not
 * yet implemented on the current native runtime.
 */
class UnsupportedApi : ApiHandler {
    override suspend fun handle(api: String, params: JsonObject): Any {
        return mapOf(
            "errMsg" to "$api:fail not supported on android",
            "code" to "NOT_SUPPORTED"
        )
    }
}
