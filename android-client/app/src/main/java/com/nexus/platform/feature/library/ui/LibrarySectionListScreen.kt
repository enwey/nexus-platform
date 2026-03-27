package com.nexus.platform.feature.library.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.platform.R
import com.nexus.platform.domain.model.GameItem
import com.nexus.platform.ui.components.GameLogo
import com.nexus.platform.ui.theme.BackgroundBase
import com.nexus.platform.ui.theme.BackgroundSurfaceElevated
import com.nexus.platform.ui.theme.BorderLight
import com.nexus.platform.ui.theme.Primary
import com.nexus.platform.ui.theme.TextMuted
import kotlin.math.min

private const val LOAD_MORE_SIZE = 40
private const val GRID_COLUMNS = 4

@Composable
fun LibrarySectionListScreen(
    section: LibrarySection,
    games: List<GameItem>,
    onBackClick: () -> Unit,
    onGameClick: (GameItem) -> Unit
) {
    val sourceGames = remember(section, games) {
        if (games.isEmpty()) {
            emptyList()
        } else {
            val total = maxOf(games.size, 120)
            List(total) { index ->
                val base = games[index % games.size]
                base.copy(
                    id = "${section.routeValue}_${index}_${base.id}",
                    name = "${base.name} ${index + 1}"
                )
            }
        }
    }
    var loadedCount by remember(sourceGames) {
        mutableIntStateOf(min(LOAD_MORE_SIZE, sourceGames.size))
    }
    val gridState = rememberLazyGridState()
    val shouldLoadMore by remember {
        derivedStateOf {
            if (loadedCount >= sourceGames.size) return@derivedStateOf false
            val lastVisibleIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleIndex >= gridState.layoutInfo.totalItemsCount - GRID_COLUMNS * 2
        }
    }

    LaunchedEffect(shouldLoadMore, sourceGames.size, loadedCount) {
        if (shouldLoadMore) {
            loadedCount = min(loadedCount + LOAD_MORE_SIZE, sourceGames.size)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBase)
            .padding(start = 24.dp, end = 24.dp, top = 24.dp)
    ) {
        TopBar(
            title = when (section) {
                LibrarySection.RECENT -> R.string.library_section_recent
                LibrarySection.MY_GAMES -> R.string.library_section_my_games
            },
            onBackClick = onBackClick
        )
        Spacer(modifier = Modifier.size(16.dp))

        if (sourceGames.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.library_empty_section),
                    color = TextMuted
                )
            }
            return@Column
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(GRID_COLUMNS),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(count = loadedCount, key = { index -> sourceGames[index].id }) { index ->
                val game = sourceGames[index]
                SectionGameCard(game = game, onGameClick = onGameClick)
            }
            item(span = { GridItemSpan(GRID_COLUMNS) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (loadedCount < sourceGames.size) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = androidx.compose.ui.res.stringResource(R.string.library_load_end),
                            color = TextMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(title: Int, onBackClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = androidx.compose.ui.res.stringResource(R.string.game_back),
            color = Primary,
            modifier = Modifier.clickable { onBackClick() }
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = androidx.compose.ui.res.stringResource(title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun SectionGameCard(game: GameItem, onGameClick: (GameItem) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onGameClick(game) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, BorderLight, RoundedCornerShape(14.dp))
                .background(BackgroundSurfaceElevated)
        ) {
            GameLogo(
                iconUrl = game.iconUrl,
                seed = "${game.id}_${game.name}",
                modifier = Modifier
                    .fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = game.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
