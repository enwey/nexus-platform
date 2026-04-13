package com.nexus.platform.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
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
import com.nexus.platform.domain.model.RecommendTodayItem
import com.nexus.platform.core.i18n.AppLanguage
import com.nexus.platform.domain.model.GameItem
import com.nexus.platform.feature.recommend.ui.RecommendDetailScreen
import com.nexus.platform.feature.recommend.ui.RecommendScreen
import com.nexus.platform.feature.discover.ui.DiscoverRankingScreen
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
        || route == MainRoutes.DISCOVER_RANKING || route == MainRoutes.RECOMMEND_DETAIL
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

private fun AnimatedContentTransitionScope<*>.todayDetailEnter() =
    slideInVertically(
        initialOffsetY = { fullHeight -> fullHeight / 10 },
        animationSpec = tween(320, easing = LinearOutSlowInEasing)
    ) + fadeIn(animationSpec = tween(300, easing = LinearOutSlowInEasing)) +
        scaleIn(initialScale = 0.98f, animationSpec = tween(320, easing = LinearOutSlowInEasing))

private fun AnimatedContentTransitionScope<*>.todayDetailExit() =
    slideOutVertically(
        targetOffsetY = { fullHeight -> fullHeight / 8 },
        animationSpec = tween(240, easing = FastOutLinearInEasing)
    ) + fadeOut(animationSpec = tween(220, easing = FastOutLinearInEasing)) +
        scaleOut(targetScale = 0.99f, animationSpec = tween(240, easing = FastOutLinearInEasing))

@Composable
private fun MainHomeScreen(
    libraryState: LibraryUiState,
    isLoggedIn: Boolean,
    onLoadLibrary: () -> Unit,
    onDiscoverCategoryChange: (String) -> Unit,
    onLibraryGameClick: (GameItem) -> Unit,
    onLibraryMoreClick: (LibrarySection) -> Unit,
    onToggleMyGame: (GameItem) -> Unit,
    onDiscoverRankingClick: () -> Unit,
    onDiscoverGameClick: (GameItem) -> Unit,
    onRecommendDetailClick: (RecommendTodayItem, GameItem?) -> Unit,
    onDiscoverQuickPlayClick: (GameItem) -> Unit,
    onRequestLogin: () -> Unit,
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
                    onRefresh = onLoadLibrary,
                    onGameClick = onLibraryGameClick,
                    onMoreClick = onLibraryMoreClick,
                    onToggleMyGame = onToggleMyGame,
                    onGoDiscoverClick = { selected = MainDestination.Discover }
                )

                MainDestination.Discover -> DiscoverScreen(
                    games = libraryState.discoverGames,
                    hero = libraryState.discoverHero,
                    categories = libraryState.discoverCategories,
                    onCategoryChange = onDiscoverCategoryChange,
                    onRefresh = { category ->
                        if (category == "all") onLoadLibrary() else onDiscoverCategoryChange(category)
                    },
                    onGameClick = onDiscoverGameClick,
                    onQuickPlayClick = onDiscoverQuickPlayClick,
                    onRankingClick = { onDiscoverRankingClick() }
                )
                MainDestination.Recommend -> RecommendScreen(
                    games = (libraryState.games + libraryState.discoverGames).distinctBy { it.id },
                    onGameClick = onDiscoverGameClick,
                    onCardClick = onRecommendDetailClick
                )
                MainDestination.Profile -> ProfileScreen(
                    isLoggedIn = isLoggedIn,
                    currentLanguage = currentLanguage,
                    onLanguageChange = onChangeLanguage,
                    onRequestLogin = onRequestLogin,
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
    onDiscoverCategoryChange: (String) -> Unit,
    onLibraryGameClick: (GameItem) -> Unit,
    onLibraryMoreClick: (LibrarySection) -> Unit,
    onPlayGame: (GameItem) -> Unit,
    onToggleMyGame: (GameItem) -> Unit,
    onDiscoverRankingClick: () -> Unit,
    isLoggedIn: Boolean,
    onRequestLogin: () -> Unit,
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
                isLoggedIn = isLoggedIn,
                onLoadLibrary = onLoadLibrary,
                onDiscoverCategoryChange = onDiscoverCategoryChange,
                onLibraryGameClick = onLibraryGameClick,
                onLibraryMoreClick = onLibraryMoreClick,
                onToggleMyGame = onToggleMyGame,
                onDiscoverRankingClick = onDiscoverRankingClick,
                onDiscoverGameClick = { game ->
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set(MainRoutes.GAME_DETAIL_KEY, game)
                    navController.navigate(MainRoutes.GAME_DETAIL)
                },
                onRecommendDetailClick = { item, game ->
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set(MainRoutes.RECOMMEND_DETAIL_ITEM_KEY, item)
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set(MainRoutes.RECOMMEND_DETAIL_GAME_KEY, game)
                    navController.navigate(MainRoutes.RECOMMEND_DETAIL)
                },
                onDiscoverQuickPlayClick = onPlayGame,
                onRequestLogin = onRequestLogin,
                currentLanguage = currentLanguage,
                onChangeLanguage = onChangeLanguage,
                onLogout = onLogout
            )
        }
        composable(
            route = MainRoutes.DISCOVER_RANKING,
            enterTransition = { hierarchyEnter() },
            exitTransition = { hierarchyExit() },
            popEnterTransition = { hierarchyPopEnter() },
            popExitTransition = { hierarchyPopExit() }
        ) {
            DiscoverRankingScreen(
                games = libraryState.discoverGames,
                onBackClick = { navController.popBackStack() },
                onGameClick = { game ->
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set(MainRoutes.GAME_DETAIL_KEY, game)
                    navController.navigate(MainRoutes.GAME_DETAIL)
                }
            )
        }
        composable(
            route = MainRoutes.RECOMMEND_DETAIL,
            enterTransition = { todayDetailEnter() },
            exitTransition = { todayDetailExit() },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { todayDetailExit() }
        ) { backStackEntry ->
            val item = remember(backStackEntry.id) {
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<RecommendTodayItem>(MainRoutes.RECOMMEND_DETAIL_ITEM_KEY)
            }
            val game = remember(backStackEntry.id) {
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<GameItem?>(MainRoutes.RECOMMEND_DETAIL_GAME_KEY)
            }
            if (item == null) {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
                return@composable
            }

            RecommendDetailScreen(
                item = item,
                game = game,
                onBackClick = { navController.popBackStack() },
                onPlayClick = { game?.let(onPlayGame) }
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
                LibrarySection.RECENT -> libraryState.recentGames
                LibrarySection.MY_GAMES -> libraryState.myGames
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
