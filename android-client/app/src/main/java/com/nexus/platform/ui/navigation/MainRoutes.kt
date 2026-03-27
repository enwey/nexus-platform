package com.nexus.platform.ui.navigation

object MainRoutes {
    const val HOME = "home"
    const val GAME_DETAIL = "gameDetail"
    const val GAME_DETAIL_KEY = "game_detail_key"
    const val LIBRARY_SECTION = "librarySection/{section}"
    const val LIBRARY_SECTION_ARG = "section"

    fun librarySectionRoute(section: String): String = "librarySection/$section"
}
