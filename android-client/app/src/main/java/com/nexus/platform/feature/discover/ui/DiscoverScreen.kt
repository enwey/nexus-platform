package com.nexus.platform.feature.discover.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.platform.R
import com.nexus.platform.domain.model.DiscoverCategory
import com.nexus.platform.domain.model.DiscoverHeroCard
import com.nexus.platform.domain.model.GameItem
import com.nexus.platform.ui.components.GameLogo
import com.nexus.platform.ui.components.SectionRefreshOverlay
import com.nexus.platform.ui.components.SkeletonMotionTokens
import com.nexus.platform.ui.components.SkeletonBlock
import com.nexus.platform.ui.components.SkeletonText
import com.nexus.platform.ui.theme.BackgroundBase
import com.nexus.platform.ui.theme.BackgroundSurface
import com.nexus.platform.ui.theme.BackgroundSurfaceElevated
import com.nexus.platform.ui.theme.BorderLight
import com.nexus.platform.ui.theme.Primary
import com.nexus.platform.ui.theme.PrimaryEnd
import com.nexus.platform.ui.theme.PrimaryStart
import com.nexus.platform.ui.theme.TextMain
import com.nexus.platform.ui.theme.TextMuted
import kotlinx.coroutines.delay
import kotlin.math.min
import coil.compose.rememberAsyncImagePainter

private val TopLevelBottomPadding = 96.dp
private val QuickPlayText = TextMain
private const val DiscoverPageSize = 30

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DiscoverScreen(
    games: List<GameItem>,
    hero: DiscoverHeroCard?,
    categories: List<DiscoverCategory>,
    onCategoryChange: (String) -> Unit,
    onRefresh: (String) -> Unit,
    onGameClick: (GameItem) -> Unit,
    onQuickPlayClick: (GameItem) -> Unit,
    onRankingClick: () -> Unit
) {
    val allCategoryLabel = stringResource(R.string.discover_category_all)
    var showInitialSkeleton by rememberSaveable { mutableStateOf(games.isEmpty()) }
    val resolvedCategories = remember(categories, allCategoryLabel) {
        val allItem = DiscoverCategory(key = "all", label = allCategoryLabel)
        val fromServer = categories
            .mapNotNull { item ->
                val key = item.key.trim()
                if (key.isBlank()) null else DiscoverCategory(key = key, label = item.label.ifBlank { key })
            }
            .filter { !it.key.equals("all", ignoreCase = true) }
        listOf(allItem) + fromServer
    }
    var selectedCategoryIndex by rememberSaveable { mutableIntStateOf(0) }
    var refreshing by remember { mutableStateOf(false) }
    if (selectedCategoryIndex >= resolvedCategories.size) {
        selectedCategoryIndex = 0
    }
    val selectedCategory = resolvedCategories[selectedCategoryIndex]
    val isAllCategory = selectedCategory.key == "all"
    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = {
            refreshing = true
            onRefresh(selectedCategory.key)
        }
    )
    LaunchedEffect(refreshing, games) {
        if (refreshing) {
            delay(450)
            refreshing = false
        }
    }
    LaunchedEffect(games, hero, categories) {
        if (games.isNotEmpty() || hero != null || categories.isNotEmpty()) {
            showInitialSkeleton = false
        } else if (showInitialSkeleton) {
            delay(900)
            showInitialSkeleton = false
        }
    }

    val rankedGames = remember(games) { games.take(10) }
    val heroTarget = remember(hero, rankedGames) {
        val heroAppId = hero?.appId.orEmpty()
        rankedGames.firstOrNull { it.id == heroAppId } ?: rankedGames.firstOrNull()
    }

    val myGameSource = remember(games) { games }
    var loadedCount by remember(myGameSource) {
        mutableIntStateOf(min(DiscoverPageSize, myGameSource.size))
    }
    val loadedGames = remember(myGameSource, loadedCount) {
        myGameSource.take(loadedCount)
    }
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            if (loadedCount >= myGameSource.size) return@derivedStateOf false
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleIndex >= listState.layoutInfo.totalItemsCount - 2
        }
    }
    LaunchedEffect(shouldLoadMore, loadedCount, myGameSource.size) {
        if (shouldLoadMore) {
            loadedCount = min(loadedCount + DiscoverPageSize, myGameSource.size)
        }
    }

    Box(
        modifier = Modifier
            .background(BackgroundBase)
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        if (games.isEmpty() && !refreshing && showInitialSkeleton) {
            DiscoverSkeleton()
        } else {
            LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(modifier = Modifier.height(10.dp)) }

            item { Spacer(modifier = Modifier.height(2.dp)) }

            item {
                Box(modifier = Modifier.padding(start = 24.dp, end = 24.dp)) {
                    Banner(
                        hero = hero,
                        onClick = {
                            heroTarget?.let(onGameClick)
                        }
                    )
                    AnimatedVisibility(
                        visible = refreshing,
                        enter = fadeIn(animationSpec = tween(SkeletonMotionTokens.OverlayEnterMillis)) +
                            slideInVertically(
                                animationSpec = tween(SkeletonMotionTokens.OverlayEnterMillis),
                                initialOffsetY = { -it / 4 }
                            ),
                        exit = fadeOut(animationSpec = tween(SkeletonMotionTokens.OverlayExitMillis)) +
                            slideOutVertically(
                                animationSpec = tween(SkeletonMotionTokens.OverlayExitMillis),
                                targetOffsetY = { -it / 5 }
                            )
                    ) {
                        SectionRefreshOverlay(
                            modifier = Modifier.matchParentSize(),
                            lineWidths = listOf(108.dp, 72.dp),
                            cornerRadius = 20.dp,
                            label = stringResource(R.string.common_loading)
                        )
                    }
                }
            }

        item {
            Box(modifier = Modifier.padding(start = 24.dp, end = 24.dp)) {
                CategoryRow(
                    categories = resolvedCategories,
                    selectedIndex = selectedCategoryIndex,
                    onSelect = { index ->
                        if (index != selectedCategoryIndex) {
                            selectedCategoryIndex = index
                            loadedCount = 0
                            onCategoryChange(resolvedCategories[index].key)
                        }
                    }
                )
            }
        }

        if (isAllCategory) {
            item {
                Box(modifier = Modifier.padding(start = 24.dp, end = 24.dp)) {
                    Box {
                        SectionHeader(
                            title = stringResource(R.string.discover_section_rank),
                            action = stringResource(R.string.discover_view_more),
                            onActionClick = onRankingClick
                        )
                        AnimatedVisibility(
                            visible = refreshing,
                            enter = fadeIn(animationSpec = tween(SkeletonMotionTokens.OverlayEnterMillis)) +
                                slideInVertically(
                                    animationSpec = tween(SkeletonMotionTokens.OverlayEnterMillis),
                                    initialOffsetY = { -it / 4 }
                                ),
                            exit = fadeOut(animationSpec = tween(SkeletonMotionTokens.OverlayExitMillis)) +
                                slideOutVertically(
                                    animationSpec = tween(SkeletonMotionTokens.OverlayExitMillis),
                                    targetOffsetY = { -it / 5 }
                                )
                        ) {
                            SectionRefreshOverlay(
                                modifier = Modifier.matchParentSize(),
                                lineWidths = listOf(84.dp, 58.dp),
                                cornerRadius = 18.dp,
                                label = stringResource(R.string.common_loading)
                            )
                        }
                    }
                }
            }

            if (rankedGames.isEmpty()) {
                item {
                    Box(modifier = Modifier.padding(start = 24.dp, end = 24.dp)) {
                        Text(
                            text = stringResource(R.string.discover_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            } else {
                items(
                    items = rankedGames,
                    key = { game -> "discover_ranked_${game.id}" }
                ) { game ->
                    Box(modifier = Modifier.padding(start = 24.dp, end = 24.dp)) {
                        RankedItem(
                            game = game,
                            onGameClick = onGameClick,
                            onQuickPlayClick = onQuickPlayClick
                        )
                    }
                }
            }
        }

        if (myGameSource.isNotEmpty()) {
            item {
                Box(modifier = Modifier.padding(start = 24.dp, end = 24.dp)) {
                    Box {
                        SectionHeader(
                            title = selectedCategory.label,
                            action = null
                        )
                        AnimatedVisibility(
                            visible = refreshing,
                            enter = fadeIn(animationSpec = tween(SkeletonMotionTokens.OverlayEnterMillis)) +
                                slideInVertically(
                                    animationSpec = tween(SkeletonMotionTokens.OverlayEnterMillis),
                                    initialOffsetY = { -it / 4 }
                                ),
                            exit = fadeOut(animationSpec = tween(SkeletonMotionTokens.OverlayExitMillis)) +
                                slideOutVertically(
                                    animationSpec = tween(SkeletonMotionTokens.OverlayExitMillis),
                                    targetOffsetY = { -it / 5 }
                                )
                        ) {
                            SectionRefreshOverlay(
                                modifier = Modifier.matchParentSize(),
                                lineWidths = listOf(72.dp, 54.dp),
                                cornerRadius = 18.dp,
                                label = stringResource(R.string.common_loading)
                            )
                        }
                    }
                }
            }
        }

        if (myGameSource.isEmpty()) {
            item {
                Box(modifier = Modifier.padding(start = 24.dp, end = 24.dp)) {
                    Text(
                        text = stringResource(R.string.discover_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        } else {
            items(
                items = loadedGames,
                key = { game -> "discover_list_${game.id}" }
            ) { game ->
                Box(modifier = Modifier.padding(start = 24.dp, end = 24.dp)) {
                    RankedItem(
                        game = game,
                        onGameClick = onGameClick,
                        onQuickPlayClick = onQuickPlayClick
                    )
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, bottom = TopLevelBottomPadding),
                    contentAlignment = Alignment.Center
                ) {
                    if (loadedCount < myGameSource.size) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            repeat(2) { index ->
                                DiscoverSkeletonRow(rank = loadedGames.size + index + 1)
                            }
                        }
                    } else {
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
        }
        PullRefreshIndicator(
            refreshing = refreshing,
            state = pullRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp),
            backgroundColor = BackgroundSurface,
            contentColor = Primary
        )
    }
}

@Composable
private fun DiscoverSkeleton() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { Spacer(modifier = Modifier.height(12.dp)) }
        item {
            Box(modifier = Modifier.padding(start = 24.dp, end = 24.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF24253A), BackgroundSurface)))
                        .padding(20.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SkeletonBlock(width = 52.dp, height = 20.dp, cornerRadius = 8.dp)
                        SkeletonText(widths = listOf(184.dp, 228.dp), lineHeight = 14.dp)
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(56.dp, 72.dp, 72.dp, 72.dp).forEach { width ->
                    SkeletonBlock(width = width, height = 34.dp, cornerRadius = 17.dp)
                }
            }
        }
        item {
            Box(modifier = Modifier.padding(start = 24.dp, end = 24.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SkeletonBlock(width = 102.dp, height = 24.dp, cornerRadius = 8.dp)
                        SkeletonBlock(width = 62.dp, height = 12.dp, cornerRadius = 6.dp)
                    }
                    repeat(3) { index ->
                        DiscoverSkeletonRow(rank = index + 1)
                    }
                }
            }
        }
        item {
            Box(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = TopLevelBottomPadding)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SkeletonBlock(width = 78.dp, height = 24.dp, cornerRadius = 8.dp)
                    repeat(4) { index ->
                        DiscoverSkeletonRow(rank = index + 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverSkeletonRow(rank: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BackgroundSurface)
            .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = rank.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White.copy(alpha = 0.12f)
        )
        SkeletonBlock(width = 52.dp, height = 52.dp, cornerRadius = 12.dp)
        Column(modifier = Modifier.weight(1f)) {
            SkeletonBlock(width = 128.dp, height = 14.dp, cornerRadius = 7.dp)
            Spacer(modifier = Modifier.height(6.dp))
            SkeletonBlock(width = 166.dp, height = 12.dp, cornerRadius = 6.dp)
        }
    }
}

@Composable
private fun Banner(hero: DiscoverHeroCard?, onClick: () -> Unit) {
    val bannerCoverUrl = hero?.coverUrl?.trim().orEmpty()
    val showBannerCover = bannerCoverUrl.startsWith("http://") || bannerCoverUrl.startsWith("https://")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(PrimaryStart, PrimaryEnd)))
            .clickable { onClick() }
            .padding(20.dp),
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
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    hero?.badgeText?.ifBlank { stringResource(R.string.discover_banner_hot) }
                        ?: stringResource(R.string.discover_banner_hot),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = hero?.title?.ifBlank { stringResource(R.string.discover_banner_title) }
                    ?: stringResource(R.string.discover_banner_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            if (!hero?.subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = hero?.subtitle.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(
    categories: List<DiscoverCategory>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        categories.forEachIndexed { index, category ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected) Primary else BackgroundSurface)
                    .border(1.dp, if (selected) Primary else BorderLight, RoundedCornerShape(20.dp))
                    .clickable { onSelect(index) }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    category.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMain
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    action: String?,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        if (!action.isNullOrBlank()) {
            Row(
                modifier = Modifier.then(if (onActionClick != null) Modifier.clickable { onActionClick() } else Modifier),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Text(action, color = Primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                androidx.compose.material3.Icon(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_more),
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun RankedItem(
    game: GameItem,
    onGameClick: (GameItem) -> Unit,
    onQuickPlayClick: (GameItem) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(BackgroundSurface)
            .border(1.dp, BorderLight, RoundedCornerShape(20.dp))
            .clickable { onGameClick(game) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, BorderLight, RoundedCornerShape(14.dp))
                .background(BackgroundSurfaceElevated)
        ) {
            GameLogo(
                iconUrl = game.iconUrl,
                seed = "${game.id}_${game.name}_discover",
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = game.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = game.description.ifBlank { "v${game.version}" },
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(PrimaryStart, PrimaryEnd)))
                .clickable { onQuickPlayClick(game) }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                stringResource(R.string.discover_quick_play),
                color = QuickPlayText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black
            )
        }
    }
}
