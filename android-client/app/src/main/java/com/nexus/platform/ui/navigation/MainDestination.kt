package com.nexus.platform.ui.navigation

import androidx.annotation.StringRes
import com.nexus.platform.R

enum class MainDestination(val route: String, @StringRes val labelRes: Int) {
    Library("library", R.string.tab_library),
    Discover("discover", R.string.tab_discover),
    Community("community", R.string.tab_community),
    Profile("profile", R.string.tab_profile)
}
