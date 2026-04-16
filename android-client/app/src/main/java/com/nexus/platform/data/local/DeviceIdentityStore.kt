package com.nexus.platform.data.local

import android.content.Context
import java.util.UUID

class DeviceIdentityStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun runtimeDeviceId(): String {
        val existing = prefs.getString(KEY_RUNTIME_DEVICE_ID, null)?.trim().orEmpty()
        if (existing.isNotEmpty()) {
            return existing
        }
        val generated = "rtdev_" + UUID.randomUUID().toString().replace("-", "")
        prefs.edit().putString(KEY_RUNTIME_DEVICE_ID, generated).apply()
        return generated
    }

    companion object {
        private const val PREF_NAME = "device_identity"
        private const val KEY_RUNTIME_DEVICE_ID = "runtime_device_id"
    }
}
