package com.nexus.platform.feature.library.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.platform.R
import com.nexus.platform.domain.model.GameItem
import com.nexus.platform.ui.components.GameLogo
import com.nexus.platform.ui.theme.AccentGreen
import com.nexus.platform.ui.theme.BackgroundBase
import com.nexus.platform.ui.theme.BackgroundSurfaceElevated
import com.nexus.platform.ui.theme.BorderLight
import com.nexus.platform.ui.theme.Primary
import com.nexus.platform.ui.theme.PrimaryEnd
import com.nexus.platform.ui.theme.PrimaryStart
import com.nexus.platform.ui.theme.TextMain
import com.nexus.platform.ui.theme.TextMuted
import kotlin.math.min

private val TopLevelBottomPadding = 96.dp

@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    onLoad: () -> Unit,
    onGameClick: (GameItem) -> Unit,
    onMoreClick: (LibrarySection) -> Unit
) {
    var hasRequested by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(uiState.games.isEmpty(), hasRequested) {
        if (uiState.games.isEmpty() && !hasRequested) {
            hasRequested = true
            onLoad()
        }
    }

    when {
        uiState.loading -> LoadingState()
        !uiState.errorMessage.isNullOrBlank() -> ErrorState(uiState.errorMessage.orEmpty())
        uiState.currentPlayingGame == null && uiState.recentGames.isEmpty() && uiState.myGames.isEmpty() ->
            ColdStartState(uiState = uiState, onGameClick = onGameClick)
        else -> ContentState(uiState, onGameClick, onMoreClick)
    }
}

@Composable
private fun ContentState(
    uiState: LibraryUiState,
    onGameClick: (GameItem) -> Unit,
    onMoreClick: (LibrarySection) -> Unit
) {
    val recentGames = remember(uiState.recentGames) { uiState.recentGames.take(8) }
    val myGameSource = remember(uiState.myGames) { uiState.myGames }
    var myLoadedCount by remember(myGameSource) {
        mutableIntStateOf(min(40, myGameSource.size))
    }
    val myRows = remember(myLoadedCount) { (0 until myLoadedCount).chunked(4) }
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            if (myLoadedCount >= myGameSource.size) return@derivedStateOf false
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleIndex >= listState.layoutInfo.totalItemsCount - 2
        }
    }
    LaunchedEffect(shouldLoadMore, myLoadedCount, myGameSource.size) {
        if (shouldLoadMore) {
            myLoadedCount = min(myLoadedCount + 40, myGameSource.size)
        }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(
            start = 24.dp,
            end = 24.dp,
            top = 24.dp,
            bottom = TopLevelBottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { Spacer(modifier = Modifier.height(10.dp)) }
        item {
            ResumeCard(
                featuredGame = uiState.currentPlayingGame,
                onGameClick = onGameClick
            )
        }
        item {
            SectionHeader(
                title = stringResource(R.string.library_section_recent),
                action = stringResource(R.string.library_more_with_arrow),
                onActionClick = { onMoreClick(LibrarySection.RECENT) }
            )
        }
        item { RecentGameGrid(recentGames, onGameClick) }
        item {
            SectionHeader(
                title = stringResource(R.string.library_section_my_games)
            )
        }
        itemsIndexed(
            items = myRows,
            key = { index, _ -> "my_row_$index" },
            contentType = { _, _ -> "home_game_row" }
        ) { _, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                row.forEach { sourceIndex ->
                    val game = myGameSource[sourceIndex]
                    GameItemCard(game, onGameClick)
                }
                repeat((4 - row.size).coerceAtLeast(0)) { EmptyGridCell() }
            }
        }
        item {
            if (myLoadedCount < myGameSource.size) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.library_load_end),
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun ResumeCard(
    featuredGame: GameItem?,
    onGameClick: (GameItem) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(32.dp))
            .border(1.dp, BorderLight)
            .then(
                if (featuredGame != null) {
                    Modifier.clickable { onGameClick(featuredGame) }
                } else {
                    Modifier
                }
            )
            .background(
                Brush.linearGradient(listOf(PrimaryStart, PrimaryEnd))
            )
    ) {
        Box(
            modifier = Modifier
                .size(130.dp)
                .align(Alignment.TopStart)
                .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(bottomEnd = 88.dp))
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.30f)
                        ),
                        startY = 0f,
                        endY = 260f
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(AccentGreen.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(stringResource(R.string.library_running), color = AccentGreen, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    featuredGame?.name ?: stringResource(R.string.library_resume_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    featuredGame?.description?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.library_resume_subtitle),
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(PrimaryStart, PrimaryEnd)))
                        .then(
                            if (featuredGame != null) {
                                Modifier.clickable { onGameClick(featuredGame) }
                            } else {
                                Modifier
                            }
                        )
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(stringResource(R.string.library_resume_action), color = TextMain, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String? = null, onActionClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        if (!action.isNullOrBlank() && onActionClick != null) {
            Text(
                action,
                color = Primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onActionClick() }
            )
        }
    }
}

@Composable
private fun RecentGameGrid(games: List<GameItem>, onGameClick: (GameItem) -> Unit) {
    if (games.isEmpty()) {
        Text(
            text = stringResource(R.string.library_empty_section),
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val firstRow = games.take(4)
            firstRow.forEach { GameItemCard(it, onGameClick) }
            repeat((4 - firstRow.size).coerceAtLeast(0)) { EmptyGridCell() }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val secondRow = games.drop(4).take(4)
            secondRow.forEach { GameItemCard(it, onGameClick) }
            repeat((4 - secondRow.size).coerceAtLeast(0)) { EmptyGridCell() }
        }
    }
}

@Composable
private fun RowScope.EmptyGridCell() {
    Spacer(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
    )
}

@Composable
private fun RowScope.GameItemCard(game: GameItem, onGameClick: (GameItem) -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable { onGameClick(game) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, BorderLight)
                .background(BackgroundSurfaceElevated)
        ) {
            GameLogo(
                iconUrl = game.iconUrl,
                seed = "${game.id}_${game.name}",
                modifier = Modifier
                    .fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = game.name,
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
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
        Text(stringResource(R.string.loading_games))
    }
}

@Composable
private fun ErrorState(message: String) {
    val resolvedMessage = if (message == "__error_load_games_failed__") {
        stringResource(R.string.load_games_failed)
    } else {
        message
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(resolvedMessage, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.no_games_available))
    }
}

@Composable
private fun ColdStartState(
    uiState: LibraryUiState,
    onGameClick: (GameItem) -> Unit
) {
    val picks = (uiState.newbieMustPlay + uiState.everyonePlaying).distinctBy { it.id }.take(4)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 24.dp,
            end = 24.dp,
            top = 24.dp,
            bottom = TopLevelBottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { Spacer(modifier = Modifier.height(10.dp)) }
        item {
            ColdStartHeroCard(
                featured = uiState.newbieMustPlay.firstOrNull() ?: uiState.everyonePlaying.firstOrNull(),
                onGameClick = onGameClick
            )
        }
        item {
            SectionHeader(
                title = stringResource(R.string.library_section_my_games)
            )
        }
        item { ColdStartEmptyCollectionCard() }
        item {
            SectionHeader(
                title = stringResource(R.string.library_coldstart_trending),
                action = stringResource(R.string.library_coldstart_refresh)
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (picks.isEmpty()) {
                    repeat(4) {
                        ColdStartPickCard(title = stringResource(R.string.discover_empty), badge = "-")
                    }
                } else {
                    picks.forEachIndexed { index, game ->
                        ColdStartPickCard(
                            title = game.name,
                            badge = "${index + 1}",
                            onClick = { onGameClick(game) }
                        )
                    }
                    repeat((4 - picks.size).coerceAtLeast(0)) {
                        ColdStartPickCard(title = "", badge = "")
                    }
                }
            }
        }
    }
}

@Composable
private fun ColdStartHeroCard(
    featured: GameItem?,
    onGameClick: (GameItem) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(32.dp))
            .border(1.dp, BorderLight)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF5C8CFF),
                        Color(0xFF9258FF)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Column {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(R.string.library_coldstart_new_player_tag),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = featured?.name ?: stringResource(R.string.library_coldstart_hero_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = featured?.description?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.library_coldstart_hero_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.82f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .then(
                        if (featured != null) {
                            Modifier.clickable { onGameClick(featured) }
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.library_coldstart_cta),
                    color = Color(0xFF1A1A1A),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ColdStartEmptyCollectionCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, BorderLight, RoundedCornerShape(24.dp))
            .background(BackgroundSurfaceElevated)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.library_coldstart_empty_icon),
                style = MaterialTheme.typography.headlineSmall
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.library_coldstart_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.library_coldstart_empty_desc),
            color = TextMuted,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, Primary, RoundedCornerShape(22.dp))
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Text(
                text = stringResource(R.string.library_coldstart_discover_action),
                color = Primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RowScope.ColdStartPickCard(
    title: String,
    badge: String,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, BorderLight, RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            PrimaryStart.copy(alpha = 0.5f),
                            PrimaryEnd.copy(alpha = 0.5f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badge,
                color = Color.White.copy(alpha = 0.65f),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
