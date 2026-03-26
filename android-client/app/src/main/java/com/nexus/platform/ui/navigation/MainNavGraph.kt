package com.nexus.platform.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nexus.platform.feature.community.ui.CommunityScreen
import com.nexus.platform.feature.discover.ui.DiscoverScreen
import com.nexus.platform.feature.library.ui.LibraryScreen
import com.nexus.platform.feature.library.ui.LibraryUiState
import com.nexus.platform.feature.profile.ui.ProfileScreen

@Composable
fun MainNavGraph(
    navController: NavHostController,
    libraryState: LibraryUiState,
    onLoadLibrary: () -> Unit,
    onLibraryGameClick: (com.nexus.platform.domain.model.GameItem) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = MainDestination.Library.route
    ) {
        composable(MainDestination.Library.route) {
            LibraryScreen(
                uiState = libraryState,
                onLoad = onLoadLibrary,
                onGameClick = onLibraryGameClick
            )
        }
        composable(MainDestination.Discover.route) { DiscoverScreen() }
        composable(MainDestination.Community.route) { CommunityScreen() }
        composable(MainDestination.Profile.route) { ProfileScreen(onLogoutClick = onLogout) }
    }
}
