package com.nexus.platform.feature.library.ui

enum class LibrarySection(val routeValue: String) {
    RECENT("recent"),
    MY_GAMES("my_games");

    companion object {
        fun fromRouteValue(value: String?): LibrarySection {
            return entries.firstOrNull { it.routeValue == value } ?: RECENT
        }
    }
}
