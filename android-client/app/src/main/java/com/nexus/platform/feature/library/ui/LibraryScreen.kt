package com.nexus.platform.feature.library.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.platform.domain.model.GameItem
import com.nexus.platform.ui.theme.BackgroundSurface
import com.nexus.platform.ui.theme.BackgroundSurfaceElevated
import com.nexus.platform.ui.theme.Primary
import com.nexus.platform.ui.theme.PrimaryEnd
import com.nexus.platform.ui.theme.PrimaryStart
import com.nexus.platform.ui.theme.TextMuted

@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    onLoad: () -> Unit,
    onGameClick: (GameItem) -> Unit
) {
    LaunchedEffect(Unit) { onLoad() }

    when {
        uiState.loading -> LoadingState()
        !uiState.errorMessage.isNullOrBlank() -> ErrorState(uiState.errorMessage.orEmpty())
        uiState.games.isEmpty() -> EmptyState()
        else -> ContentState(uiState.games, onGameClick)
    }
}

@Composable
private fun ContentState(games: List<GameItem>, onGameClick: (GameItem) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }
        item { ResumeCard() }
        item { SectionHeader("My Collection", "Manage") }
        items(games.take(8)) { game -> LibraryItem(game, onGameClick) }
        item { SectionHeader("Recent", null) }
        items(games.take(4)) { game -> LibraryItem(game, onGameClick) }
        item { Spacer(modifier = Modifier.height(110.dp)) }
    }
}

@Composable
private fun ResumeCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        PrimaryStart.copy(alpha = 0.6f),
                        BackgroundSurface
                    )
                )
            )
            .padding(22.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Primary.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("Running in Background", color = Primary, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text("Cyber Blade: Origin", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text("Stage 4 complete | 2h played", color = TextMuted)
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        if (!action.isNullOrBlank()) {
            Text(action, color = Primary, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun LibraryItem(game: GameItem, onGameClick: (GameItem) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BackgroundSurface)
            .clickable { onGameClick(game) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            BackgroundSurfaceElevated,
                            PrimaryEnd.copy(alpha = 0.4f)
                        )
                    )
                )
        )
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(game.name, fontWeight = FontWeight.Bold)
            Text("v${game.version} | ${game.description}", color = TextMuted, style = MaterialTheme.typography.bodySmall)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(BackgroundSurfaceElevated)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("Play", color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(14.dp))
        Text("Loading game list")
    }
}

@Composable
private fun ErrorState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No games available")
    }
}
