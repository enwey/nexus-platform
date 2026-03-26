package com.nexus.platform.feature.main.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.compose.material3.Scaffold
import com.nexus.platform.data.repository.AuthRepository
import com.nexus.platform.data.repository.GameRepository
import com.nexus.platform.domain.model.GameItem
import com.nexus.platform.domain.usecase.GetApprovedGamesUseCase
import com.nexus.platform.domain.usecase.LogoutUseCase
import com.nexus.platform.feature.auth.ui.LoginActivity
import com.nexus.platform.feature.game.runtime.GameRuntimeActivity
import com.nexus.platform.feature.game.ui.GameDetailScreen
import com.nexus.platform.feature.library.ui.LibraryViewModel
import com.nexus.platform.ui.components.MainBottomBar
import com.nexus.platform.ui.navigation.MainDestination
import com.nexus.platform.ui.navigation.MainNavGraph
import com.nexus.platform.ui.theme.NexusPlatformTheme
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authRepository = AuthRepository(this)
        val logoutUseCase = LogoutUseCase(authRepository)
        val gameRepository = GameRepository(this)
        val libraryFactory = LibraryViewModelFactory(GetApprovedGamesUseCase(gameRepository))
        val libraryViewModel = ViewModelProvider(this, libraryFactory)[LibraryViewModel::class.java]

        setContent {
            NexusPlatformTheme {
                val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
                MainScreen(
                    libraryState = libraryState,
                    onLoadLibrary = { libraryViewModel.load() },
                    onPlayGame = { game -> GameRuntimeActivity.start(this, game) },
                    onLogout = {
                        logoutUseCase()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

private class LibraryViewModelFactory(
    private val useCase: GetApprovedGamesUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return LibraryViewModel(useCase) as T
    }
}

@Composable
private fun MainScreen(
    libraryState: com.nexus.platform.feature.library.ui.LibraryUiState,
    onLoadLibrary: () -> Unit,
    onPlayGame: (GameItem) -> Unit,
    onLogout: () -> Unit
) {
    var selectedGame by rememberSaveable { mutableStateOf<GameItem?>(null) }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentTab = MainDestination.entries.firstOrNull { it.route == currentRoute } ?: MainDestination.Library

    if (selectedGame != null) {
        GameDetailScreen(
            game = selectedGame!!,
            onBackClick = { selectedGame = null },
            onPlayClick = { onPlayGame(selectedGame!!) }
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            MainBottomBar(
                selected = currentTab,
                onSelect = { tab ->
                    navController.navigate(tab.route) {
                        popUpTo(MainDestination.Library.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        MainNavGraph(
            navController = navController,
            libraryState = libraryState,
            onLoadLibrary = onLoadLibrary,
            onLibraryGameClick = { selectedGame = it },
            onLogout = onLogout,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
