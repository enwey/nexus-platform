package com.nexus.platform

import android.app.Application
import com.nexus.platform.core.di.AppContainer
import com.nexus.platform.core.i18n.AppLanguageManager

class NexusApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        AppLanguageManager.ensureInitialized(this)
        container = AppContainer(this)
    }
}
