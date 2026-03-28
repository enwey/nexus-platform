package com.nexus.platform.core.network

import com.nexus.platform.BuildConfig
import java.net.URI

object BackendConfig {
    val apiBaseUrl: String = BuildConfig.BACKEND_BASE_URL

    private val apiUri: URI = URI(apiBaseUrl)
    val localHost: String = apiUri.host ?: "10.0.2.2"
    val localScheme: String = apiUri.scheme ?: "http"
    private val hostBase: String = buildString {
        append(localScheme)
        append("://")
        append(localHost)
        if (apiUri.port > 0) {
            append(":")
            append(apiUri.port)
        }
    }

    val localHttpHost: String = hostBase
    val localHttpsHost: String = hostBase.replaceFirst("http://", "https://")
}
