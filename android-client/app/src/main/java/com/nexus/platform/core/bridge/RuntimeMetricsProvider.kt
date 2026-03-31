package com.nexus.platform.core.bridge

interface RuntimeMetricsProvider {
    fun getSystemInfo(): Map<String, Any>
    fun getMenuButtonRect(): Map<String, Any>
    suspend fun checkForUpdate(): Map<String, Any>
    suspend fun applyUpdate(): Map<String, Any>
}
