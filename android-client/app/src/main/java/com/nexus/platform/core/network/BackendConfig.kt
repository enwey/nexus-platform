package com.nexus.platform.core.network

object BackendConfig {
    private const val emulatorBaseHost = "http://10.0.2.2"
    const val apiBaseUrl: String = "$emulatorBaseHost:8080/api/v1"
    const val emulatorHttpHost: String = "http://10.0.2.2"
    const val emulatorHttpsHost: String = "https://10.0.2.2"
}
