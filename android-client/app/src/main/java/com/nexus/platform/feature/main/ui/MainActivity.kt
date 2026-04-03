package com.nexus.platform.feature.main.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.Scaffold
import com.nexus.platform.NexusApplication
import com.nexus.platform.core.i18n.AppLanguage
import com.nexus.platform.core.i18n.AppLanguageManager
import com.nexus.platform.data.local.GameEngagementStore
import com.nexus.platform.domain.model.GameItem
import com.nexus.platform.domain.usecase.GetApprovedGamesUseCase
import com.nexus.platform.feature.auth.ui.LoginActivity
import com.nexus.platform.feature.game.runtime.GameRuntimeActivity
import com.nexus.platform.feature.library.ui.LibraryViewModel
import com.nexus.platform.ui.navigation.MainNavGraph
import com.nexus.platform.ui.navigation.MainRoutes
import com.nexus.platform.ui.theme.BackgroundBase
import com.nexus.platform.ui.theme.NexusPlatformTheme

class MainActivity : ComponentActivity() {
    private lateinit var libraryViewModel: LibraryViewModel
    private var isLoggedIn by mutableStateOf(false)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLanguageManager.ensureInitialized(this)
        val container = (application as NexusApplication).container
        isLoggedIn = container.authRepository.currentSession() != null

        val logoutUseCase = container.logoutUseCase
        val libraryFactory = LibraryViewModelFactory(
            container.getApprovedGamesUseCase,
            GameEngagementStore(this)
        )
        libraryViewModel = ViewModelProvider(this, libraryFactory)[LibraryViewModel::class.java]

        setContent {
            NexusPlatformTheme {
                val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
                MainScreen(
                    libraryState = libraryState,
                    isLoggedIn = isLoggedIn,
                    onLoadLibrary = { libraryViewModel.load() },
                    onDiscoverCategoryChange = { category -> libraryViewModel.loadDiscoverByCategory(category) },
                    onPlayGame = { game ->
                        libraryViewModel.markPlayed(game)
                        GameRuntimeActivity.start(this, game)
                    },
                    onRequestLogin = {
                        startActivity(Intent(this, LoginActivity::class.java))
                    },
                    currentLanguage = AppLanguageManager.currentLanguage(this),
                    onChangeLanguage = { selectedLanguage ->
                        if (selectedLanguage != AppLanguageManager.currentLanguage(this)) {
                            AppLanguageManager.setLanguage(this, selectedLanguage)
                            recreate()
                        }
                    },
                    onLogout = {
                        logoutUseCase()
                        isLoggedIn = false
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val container = (application as NexusApplication).container
        isLoggedIn = container.authRepository.currentSession() != null
        if (::libraryViewModel.isInitialized) {
            libraryViewModel.refreshLocalOrder()
        }
    }
}

private class LibraryViewModelFactory(
    private val useCase: GetApprovedGamesUseCase,
    private val engagementStore: GameEngagementStore
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return LibraryViewModel(useCase, engagementStore) as T
    }
}

@Composable
private fun MainScreen(
    libraryState: com.nexus.platform.feature.library.ui.LibraryUiState,
    isLoggedIn: Boolean,
    onLoadLibrary: () -> Unit,
    onDiscoverCategoryChange: (String) -> Unit,
    onPlayGame: (GameItem) -> Unit,
    onRequestLogin: () -> Unit,
    currentLanguage: AppLanguage,
    onChangeLanguage: (AppLanguage) -> Unit,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundBase,
        bottomBar = {}
    ) { innerPadding ->
        MainNavGraph(
            navController = navController,
            libraryState = libraryState,
            onLoadLibrary = onLoadLibrary,
            onLibraryGameClick = onPlayGame,
            onLibraryMoreClick = { section ->
                navController.navigate(MainRoutes.librarySectionRoute(section.routeValue))
            },
            onPlayGame = onPlayGame,
            onDiscoverCategoryChange = onDiscoverCategoryChange,
            isLoggedIn = isLoggedIn,
            onRequestLogin = onRequestLogin,
            currentLanguage = currentLanguage,
            onChangeLanguage = onChangeLanguage,
            onLogout = onLogout,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}
