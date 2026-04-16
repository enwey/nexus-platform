package com.nexus.platform.core.network

import com.nexus.platform.BuildConfig
import java.net.URI

object BackendConfig {
    val apiBaseUrl: String = BuildConfig.BACKEND_BASE_URL
    val backendCertSha256: String = BuildConfig.BACKEND_CERT_SHA256.trim().lowercase()

    private val apiUri: URI = URI(apiBaseUrl)
    val backendHost: String = apiUri.host ?: "10.0.2.2"
    val localHost: String = apiUri.host ?: "10.0.2.2"
    val localScheme: String = apiUri.scheme ?: "http"
    val shouldPinCertificate: Boolean = apiUri.scheme.equals("https", ignoreCase = true) && backendCertSha256.isNotBlank()
    private val hostBase: String = buildString {
        append(localScheme)
        append("://")
        append(backendHost)
        if (apiUri.port > 0) {
            append(":")
            append(apiUri.port)
        }
    }

    val localHttpHost: String = hostBase
    val localHttpsHost: String = hostBase.replaceFirst("http://", "https://")
}
