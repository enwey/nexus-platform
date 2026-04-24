package com.nexus.platform.feature.library.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import com.nexus.platform.R
import com.nexus.platform.core.i18n.ApiErrorLocalizer
import com.nexus.platform.domain.model.DiscoverHeroCard
import com.nexus.platform.domain.model.GameItem
import com.nexus.platform.ui.components.GameLogo
import com.nexus.platform.ui.components.SkeletonBlock
import com.nexus.platform.ui.components.SkeletonText
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
import coil.compose.rememberAsyncImagePainter

private val TopLevelBottomPadding = 96.dp
private val LibraryContentInset = 24.dp

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    onLoad: () -> Unit,
    onRefresh: () -> Unit,
    onGameClick: (GameItem) -> Unit,
    onMoreClick: (LibrarySection) -> Unit,
    onToggleMyGame: (GameItem) -> Unit,
    onGoDiscoverClick: () -> Unit
) {
    var hasRequested by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(uiState.games.isEmpty(), hasRequested) {
        if (uiState.games.isEmpty() && !hasRequested) {
            hasRequested = true
            onLoad()
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.loading,
        onRefresh = onRefresh
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        val hasLibraryContent =
            uiState.currentPlayingGame != null || uiState.recentGames.isNotEmpty() || uiState.myGames.isNotEmpty()
        when {
            uiState.loading && !hasLibraryContent && uiState.games.isEmpty() -> LibrarySkeletonState()
            !uiState.errorMessage.isNullOrBlank() -> ErrorState(uiState.errorMessage.orEmpty())
            uiState.currentPlayingGame == null && uiState.recentGames.isEmpty() && uiState.myGames.isEmpty() ->
                ColdStartState(
                    uiState = uiState,
                    onGameClick = onGameClick,
                    onGoDiscoverClick = onGoDiscoverClick
                )
            else -> ContentState(uiState, onGameClick, onMoreClick, onToggleMyGame)
        }
        PullRefreshIndicator(
            refreshing = uiState.loading,
            state = pullRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp),
            backgroundColor = BackgroundSurfaceElevated,
            contentColor = Primary
        )
    }
}

@Composable
private fun LibrarySkeletonState() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = LibraryContentInset,
            end = LibraryContentInset,
            top = LibraryContentInset,
            bottom = TopLevelBottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF24253A), BackgroundSurfaceElevated)))
                    .border(1.dp, BorderLight, RoundedCornerShape(32.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SkeletonBlock(width = 78.dp, height = 22.dp, cornerRadius = 12.dp)
                    SkeletonText(widths = listOf(186.dp, 232.dp), lineHeight = 14.dp)
                    SkeletonBlock(width = 116.dp, height = 42.dp, cornerRadius = 21.dp)
                }
            }
        }
        item { LibrarySkeletonSection(titleWidth = 118.dp) }
        item { LibrarySkeletonSection(titleWidth = 132.dp) }
    }
}

@Composable
private fun LibrarySkeletonSection(titleWidth: androidx.compose.ui.unit.Dp) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SkeletonBlock(width = titleWidth, height = 26.dp, cornerRadius = 8.dp)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    repeat(4) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SkeletonBlock(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f),
                                height = 76.dp,
                                cornerRadius = 18.dp
                            )
                            SkeletonBlock(width = 56.dp, height = 10.dp, cornerRadius = 5.dp)
                            SkeletonBlock(width = 42.dp, height = 24.dp, cornerRadius = 12.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContentState(
    uiState: LibraryUiState,
    onGameClick: (GameItem) -> Unit,
    onMoreClick: (LibrarySection) -> Unit,
    onToggleMyGame: (GameItem) -> Unit
) {
    val recentGames = remember(uiState.recentGames) { uiState.recentGames.take(8) }
    val myGameSource = remember(uiState.myGames) { uiState.myGames }
    val myGameIds = remember(myGameSource) { myGameSource.map { it.id }.toSet() }
    var pendingToggleGame by remember { mutableStateOf<GameItem?>(null) }
    var draggingGame by remember { mutableStateOf<GameItem?>(null) }
    var draggingPoint by remember { mutableStateOf(Offset.Zero) }
    var myGamesDropZone by remember { mutableStateOf<Rect?>(null) }
    val density = LocalDensity.current
    var myLoadedCount by remember(myGameSource) {
        mutableIntStateOf(min(40, myGameSource.size))
    }
    val myRows = remember(myLoadedCount) { (0 until myLoadedCount).chunked(4) }
    val everyonePlayingGames = remember(uiState.everyonePlaying, uiState.discoverGames) {
        resolveEveryonePlayingGames(uiState)
    }
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
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = LibraryContentInset,
                end = LibraryContentInset,
                top = LibraryContentInset,
                bottom = TopLevelBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
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
            item {
                RecentGameGrid(
                    games = recentGames,
                    onGameClick = onGameClick,
                    onLongPress = null,
                    onDragStart = { game, point ->
                        if (game.id !in myGameIds) {
                            draggingGame = game
                            draggingPoint = point
                        }
                    },
                    onDragMove = { _, point -> draggingPoint = point },
                    onDragEnd = { game ->
                        val targetZone = myGamesDropZone
                        if (targetZone != null && targetZone.contains(draggingPoint) && game.id !in myGameIds) {
                            onToggleMyGame(game)
                        }
                        draggingGame = null
                    }
                )
            }
            if (myGameSource.isEmpty()) {
                item {
                    SectionHeader(
                        title = stringResource(R.string.library_coldstart_trending)
                    )
                }
                item {
                    RecentGameGrid(
                        games = everyonePlayingGames,
                        onGameClick = onGameClick,
                        onLongPress = { game -> pendingToggleGame = game }
                    )
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                myGamesDropZone = coordinates.boundsInRoot()
                            }
                    ) {
                        SectionHeader(
                            title = stringResource(R.string.library_section_my_games)
                        )
                    }
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
                            GameItemCard(
                                game = game,
                                onGameClick = onGameClick,
                                onLongPress = { pendingToggleGame = game }
                            )
                        }
                        repeat((4 - row.size).coerceAtLeast(0)) { EmptyGridCell() }
                    }
                }
                item {
                    if (myLoadedCount < myGameSource.size) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            repeat(2) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    repeat(4) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            SkeletonBlock(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(1f),
                                                height = 76.dp,
                                                cornerRadius = 18.dp
                                            )
                                            SkeletonBlock(width = 56.dp, height = 10.dp, cornerRadius = 5.dp)
                                        }
                                    }
                                }
                            }
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
        val dragGame = draggingGame
        if (dragGame != null) {
            val halfSizePx = with(density) { 38.dp.toPx() }
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (draggingPoint.x - halfSizePx).toInt(),
                            (draggingPoint.y - halfSizePx).toInt()
                        )
                    }
                    .size(76.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, BorderLight, RoundedCornerShape(18.dp))
                    .background(BackgroundSurfaceElevated.copy(alpha = 0.96f))
            ) {
                GameLogo(
                    iconUrl = dragGame.iconUrl,
                    seed = "${dragGame.id}_${dragGame.name}",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    val targetGame = pendingToggleGame
    if (targetGame != null) {
        val inMyGames = targetGame.id in myGameIds
        AlertDialog(
            onDismissRequest = { pendingToggleGame = null },
            title = {
                Text(
                    text = if (inMyGames) {
                        stringResource(R.string.runtime_menu_remove_favorite)
                    } else {
                        stringResource(R.string.runtime_menu_add_favorite)
                    }
                )
            },
            text = {
                Text(
                    text = if (inMyGames) {
                        stringResource(R.string.library_remove_my_game_confirm)
                    } else {
                        stringResource(R.string.library_add_my_game_confirm)
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onToggleMyGame(targetGame)
                        pendingToggleGame = null
                    }
                ) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingToggleGame = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
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
            Row(
                modifier = Modifier.clickable { onActionClick() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Text(
                    action,
                    color = Primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    painter = painterResource(R.drawable.ic_more),
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun RecentGameGrid(
    games: List<GameItem>,
    onGameClick: (GameItem) -> Unit,
    onLongPress: ((GameItem) -> Unit)? = null,
    onDragStart: ((GameItem, Offset) -> Unit)? = null,
    onDragMove: ((GameItem, Offset) -> Unit)? = null,
    onDragEnd: ((GameItem) -> Unit)? = null
) {
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
            firstRow.forEach { game ->
                GameItemCard(
                    game = game,
                    onGameClick = onGameClick,
                    onLongPress = onLongPress,
                    onDragStart = onDragStart,
                    onDragMove = onDragMove,
                    onDragEnd = onDragEnd
                )
            }
            repeat((4 - firstRow.size).coerceAtLeast(0)) { EmptyGridCell() }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val secondRow = games.drop(4).take(4)
            secondRow.forEach { game ->
                GameItemCard(
                    game = game,
                    onGameClick = onGameClick,
                    onLongPress = onLongPress,
                    onDragStart = onDragStart,
                    onDragMove = onDragMove,
                    onDragEnd = onDragEnd
                )
            }
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
@OptIn(ExperimentalFoundationApi::class)
private fun RowScope.GameItemCard(
    game: GameItem,
    onGameClick: (GameItem) -> Unit,
    onLongPress: ((GameItem) -> Unit)? = null,
    onDragStart: ((GameItem, Offset) -> Unit)? = null,
    onDragMove: ((GameItem, Offset) -> Unit)? = null,
    onDragEnd: ((GameItem) -> Unit)? = null
) {
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    Column(
        modifier = Modifier
            .weight(1f)
            .onGloballyPositioned { coordinates = it }
            .then(
                if (onLongPress == null) {
                    Modifier.clickable { onGameClick(game) }
                } else {
                    Modifier.combinedClickable(
                        onClick = { onGameClick(game) },
                        onLongClick = { onLongPress(game) }
                    )
                }
            )
            .then(
                if (onDragStart == null || onDragMove == null || onDragEnd == null) {
                    Modifier
                } else {
                    Modifier.pointerInput(game.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { startOffset ->
                                val rootPoint = coordinates?.localToRoot(startOffset) ?: return@detectDragGesturesAfterLongPress
                                onDragStart(game, rootPoint)
                            },
                            onDrag = { change, _ ->
                                val rootPoint = coordinates?.localToRoot(change.position) ?: return@detectDragGesturesAfterLongPress
                                change.consume()
                                onDragMove(game, rootPoint)
                            },
                            onDragEnd = { onDragEnd(game) },
                            onDragCancel = { onDragEnd(game) }
                        )
                    }
                }
            ),
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
    val context = LocalContext.current
    val resolvedMessage = if (message == "__error_load_games_failed__") {
        stringResource(R.string.load_games_failed)
    } else {
        ApiErrorLocalizer.localize(
            context = context,
            rawMessage = message,
            fallbackRes = R.string.load_games_failed
        )
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
    onGameClick: (GameItem) -> Unit,
    onGoDiscoverClick: () -> Unit
) {
    val everyonePlayingGames = remember(uiState.everyonePlaying, uiState.discoverGames) {
        resolveEveryonePlayingGames(uiState)
    }
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
                banner = uiState.libraryTopBanner,
                featured = uiState.newbieMustPlay.firstOrNull() ?: uiState.everyonePlaying.firstOrNull(),
                onGameClick = onGameClick
            )
        }
        item {
            SectionHeader(
                title = stringResource(R.string.library_section_my_games)
            )
        }
        item { ColdStartEmptyCollectionCard(onDiscoverClick = onGoDiscoverClick) }
        item {
            SectionHeader(
                title = stringResource(R.string.library_coldstart_trending)
            )
        }
        item {
            RecentGameGrid(
                games = everyonePlayingGames,
                onGameClick = onGameClick
            )
        }
    }
}

private fun resolveEveryonePlayingGames(uiState: LibraryUiState): List<GameItem> {
    return uiState.everyonePlaying
        .ifEmpty { uiState.discoverGames }
        .take(8)
}

@Composable
private fun ColdStartHeroCard(
    banner: DiscoverHeroCard?,
    featured: GameItem?,
    onGameClick: (GameItem) -> Unit
) {
    val bannerCoverUrl = banner?.coverUrl?.trim().orEmpty()
    val showBannerCover = bannerCoverUrl.startsWith("http://") || bannerCoverUrl.startsWith("https://")
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
        if (showBannerCover) {
            Image(
                painter = rememberAsyncImagePainter(model = bannerCoverUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f))
            )
        }
        Column {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = banner?.badgeText?.takeIf { it.isNotBlank() } ?: stringResource(R.string.library_coldstart_new_player_tag),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = banner?.title?.takeIf { it.isNotBlank() }
                    ?: featured?.name
                    ?: stringResource(R.string.library_coldstart_hero_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = banner?.subtitle?.takeIf { it.isNotBlank() }
                    ?: featured?.description?.takeIf { it.isNotBlank() }
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
private fun ColdStartEmptyCollectionCard(onDiscoverClick: (() -> Unit)? = null) {
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
                .then(if (onDiscoverClick != null) Modifier.clickable { onDiscoverClick() } else Modifier)
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
