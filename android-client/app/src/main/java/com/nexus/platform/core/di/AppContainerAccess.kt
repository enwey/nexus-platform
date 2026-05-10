package com.nexus.platform.core.di

import android.content.Context
import com.nexus.platform.NexusApplication

val Context.appContainer: AppContainer
    get() = (applicationContext as NexusApplication).container
