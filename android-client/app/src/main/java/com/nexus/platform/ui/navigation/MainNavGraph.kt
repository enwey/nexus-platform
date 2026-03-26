package com.nexus.platform.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nexus.platform.core.i18n.AppLanguage
import com.nexus.platform.domain.model.GameItem
import com.nexus.platform.feature.community.ui.CommunityScreen
import com.nexus.platform.feature.discover.ui.DiscoverScreen
import com.nexus.platform.feature.game.ui.GameDetailScreen
import com.nexus.platform.feature.library.ui.LibrarySection
import com.nexus.platform.feature.library.ui.LibrarySectionListScreen
import com.nexus.platform.feature.library.ui.LibraryScreen
import com.nexus.platform.feature.library.ui.LibraryUiState
import com.nexus.platform.feature.profile.ui.ProfileScreen
import com.nexus.platform.ui.components.MainBottomBar
import com.nexus.platform.ui.theme.BackgroundBase

private const val HIERARCHY_ENTER_DURATION = 300
private const val HIERARCHY_EXIT_DURATION = 250

private fun AnimatedContentTransitionScope<*>.targetRoute(): String? {
    return (targetState as? NavBackStackEntry)?.destination?.route
}

private fun AnimatedContentTransitionScope<*>.initialRoute(): String? {
    return (initialState as? NavBackStackEntry)?.destination?.route
}

private fun isHierarchyRoute(route: String?): Boolean {
    return route == MainRoutes.GAME_DETAIL || route == MainRoutes.LIBRARY_SECTION
}

private fun AnimatedContentTransitionScope<*>.hierarchyEnter() =
    slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(HIERARCHY_ENTER_DURATION, easing = LinearOutSlowInEasing)
    ) + fadeIn(animationSpec = tween(HIERARCHY_ENTER_DURATION, easing = LinearOutSlowInEasing))

private fun AnimatedContentTransitionScope<*>.hierarchyExit() =
    slideOutHorizontally(
        targetOffsetX = { fullWidth -> -fullWidth },
        animationSpec = tween(HIERARCHY_EXIT_DURATION, easing = FastOutLinearInEasing)
    ) + fadeOut(animationSpec = tween(HIERARCHY_EXIT_DURATION, easing = FastOutLinearInEasing))

private fun AnimatedContentTransitionScope<*>.hierarchyPopEnter() =
    slideInHorizontally(
        initialOffsetX = { fullWidth -> -fullWidth },
        animationSpec = tween(HIERARCHY_ENTER_DURATION, easing = LinearOutSlowInEasing)
    ) + fadeIn(animationSpec = tween(HIERARCHY_ENTER_DURATION, easing = LinearOutSlowInEasing))

private fun AnimatedContentTransitionScope<*>.hierarchyPopExit() =
    slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(HIERARCHY_EXIT_DURATION, easing = FastOutLinearInEasing)
    ) + fadeOut(animationSpec = tween(HIERARCHY_EXIT_DURATION, easing = FastOutLinearInEasing))

@Composable
private fun MainHomeScreen(
    libraryState: LibraryUiState,
    onLoadLibrary: () -> Unit,
    onLibraryGameClick: (GameItem) -> Unit,
    onLibraryMoreClick: (LibrarySection) -> Unit,
    onDiscoverGameClick: (GameItem) -> Unit,
    onDiscoverQuickPlayClick: (GameItem) -> Unit,
    currentLanguage: AppLanguage,
    onChangeLanguage: (AppLanguage) -> Unit,
    onLogout: () -> Unit
) {
    var selected by rememberSaveable { mutableStateOf(MainDestination.Library) }
    val stateHolder = rememberSaveableStateHolder()

    Box(modifier = Modifier.fillMaxSize()) {
        stateHolder.SaveableStateProvider(selected.route) {
            when (selected) {
                MainDestination.Library -> LibraryScreen(
                    uiState = libraryState,
                    onLoad = onLoadLibrary,
                    onGameClick = onLibraryGameClick,
                    onMoreClick = onLibraryMoreClick
                )

                MainDestination.Discover -> DiscoverScreen(
                    games = libraryState.games,
                    onGameClick = onDiscoverGameClick,
                    onQuickPlayClick = onDiscoverQuickPlayClick
                )
                MainDestination.Community -> CommunityScreen()
                MainDestination.Profile -> ProfileScreen(
                    currentLanguage = currentLanguage,
                    onLanguageChange = onChangeLanguage,
                    onLogoutClick = onLogout
                )
            }
        }
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            MainBottomBar(
                selected = selected,
                onSelect = { target ->
                    if (target != selected) {
                        selected = target
                    }
                }
            )
        }
    }
}

@Composable
fun MainNavGraph(
    navController: NavHostController,
    libraryState: LibraryUiState,
    onLoadLibrary: () -> Unit,
    onLibraryGameClick: (GameItem) -> Unit,
    onLibraryMoreClick: (LibrarySection) -> Unit,
    onPlayGame: (GameItem) -> Unit,
    currentLanguage: AppLanguage,
    onChangeLanguage: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        modifier = modifier.background(BackgroundBase),
        navController = navController,
        startDestination = MainRoutes.HOME,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable(
            route = MainRoutes.HOME,
            enterTransition = { EnterTransition.None },
            exitTransition = {
                if (isHierarchyRoute(targetRoute())) hierarchyExit()
                else ExitTransition.None
            },
            popEnterTransition = {
                if (isHierarchyRoute(initialRoute())) hierarchyPopEnter()
                else EnterTransition.None
            },
            popExitTransition = { ExitTransition.None }
        ) {
            MainHomeScreen(
                libraryState = libraryState,
                onLoadLibrary = onLoadLibrary,
                onLibraryGameClick = onLibraryGameClick,
                onLibraryMoreClick = onLibraryMoreClick,
                onDiscoverGameClick = { game ->
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set(MainRoutes.GAME_DETAIL_KEY, game)
                    navController.navigate(MainRoutes.GAME_DETAIL)
                },
                onDiscoverQuickPlayClick = onPlayGame,
                currentLanguage = currentLanguage,
                onChangeLanguage = onChangeLanguage,
                onLogout = onLogout
            )
        }
        composable(
            route = MainRoutes.GAME_DETAIL,
            enterTransition = { hierarchyEnter() },
            exitTransition = { hierarchyExit() },
            popEnterTransition = { hierarchyPopEnter() },
            popExitTransition = { hierarchyPopExit() }
        ) { backStackEntry ->
            val game = remember(backStackEntry.id) {
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<GameItem>(MainRoutes.GAME_DETAIL_KEY)
            }
            if (game == null) {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
                return@composable
            }

            GameDetailScreen(
                game = game,
                onBackClick = { navController.popBackStack() },
                onPlayClick = { onPlayGame(game) }
            )
        }
        composable(
            route = MainRoutes.LIBRARY_SECTION,
            arguments = listOf(
                navArgument(MainRoutes.LIBRARY_SECTION_ARG) { type = NavType.StringType }
            ),
            enterTransition = { hierarchyEnter() },
            exitTransition = { hierarchyExit() },
            popEnterTransition = { hierarchyPopEnter() },
            popExitTransition = { hierarchyPopExit() }
        ) { backStackEntry ->
            val section = LibrarySection.fromRouteValue(
                backStackEntry.arguments?.getString(MainRoutes.LIBRARY_SECTION_ARG)
            )
            val games = when (section) {
                LibrarySection.RECENT -> libraryState.games
                LibrarySection.MY_GAMES -> libraryState.games
            }
            LibrarySectionListScreen(
                section = section,
                games = games,
                onBackClick = { navController.popBackStack() },
                onGameClick = { game ->
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set(MainRoutes.GAME_DETAIL_KEY, game)
                    navController.navigate(MainRoutes.GAME_DETAIL)
                }
            )
        }
    }
}
