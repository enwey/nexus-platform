package com.nexus.platform.ui.screens

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nexus.platform.GameActivity
import com.nexus.platform.ui.screens.LoginActivity
import com.nexus.platform.ui.theme.NexusPlatformTheme
import com.nexus.platform.utils.Game

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NexusPlatformTheme {
                MainScreen(onLogout = { navigateToLogin() })
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}

@Composable
fun MainScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var currentGame by remember { mutableStateOf<Game?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (currentGame != null) {
            GameDetailScreen(
                game = currentGame!!,
                onBackClick = { currentGame = null },
                onPlayClick = { GameActivity.start(context, currentGame!!) }
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> LibraryScreen(onGameClick = { game -> currentGame = game })
                        1 -> DiscoverScreen()
                        2 -> CommunityScreen()
                        3 -> ProfileScreen(onLogoutClick = onLogout)
                    }
                }

                BottomNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CommunityScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = "社区功能开发中...",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
