package com.nexus.platform.feature.recommend.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nexus.platform.R
import com.nexus.platform.data.remote.PlatformBackendApi
import com.nexus.platform.domain.model.GameItem
import com.nexus.platform.domain.model.RecommendTodayItem
import com.nexus.platform.ui.components.GameLogo
import com.nexus.platform.ui.components.SectionRefreshOverlay
import com.nexus.platform.ui.components.SkeletonMotionTokens
import com.nexus.platform.ui.components.SkeletonBlock
import com.nexus.platform.ui.components.SkeletonText
import com.nexus.platform.ui.theme.BackgroundBase
import com.nexus.platform.ui.theme.BackgroundSurface
import com.nexus.platform.ui.theme.BorderLight
import com.nexus.platform.ui.theme.Primary
import com.nexus.platform.ui.theme.TextMuted

private val TopLevelBottomPadding = 96.dp

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun RecommendScreen(
    games: List<GameItem>,
    onGameClick: (GameItem) -> Unit,
    onCardClick: (RecommendTodayItem, GameItem?) -> Unit
) {
    val context = LocalContext.current
    val backendApi = remember(context) { PlatformBackendApi(context) }
    var loading by remember { mutableStateOf(true) }
    var items by remember { mutableStateOf<List<RecommendTodayItem>>(emptyList()) }
    var refreshTick by remember { mutableIntStateOf(0) }
    val gameMap = remember(games) { games.associateBy { it.id } }

    LaunchedEffect(refreshTick) {
        loading = true
        items = runCatching { backendApi.getRecommendToday(limit = 10) }.getOrDefault(emptyList())
        loading = false
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = loading,
        onRefresh = { refreshTick += 1 }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBase)
            .pullRefresh(pullRefreshState)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = TopLevelBottomPadding),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (loading && items.isEmpty()) {
                items(3) {
                    RecommendSkeletonCard()
                }
            } else if (items.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.community_empty),
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                itemsIndexed(items, key = { index, item -> "${item.appId}_$index" }) { _, item ->
                    RecommendTodayCard(
                        item = item,
                        isRefreshing = loading,
                        onClick = { onCardClick(item, gameMap[item.appId]) },
                        onPlayClick = {
                            gameMap[item.appId]?.let(onGameClick)
                        }
                    )
                }
            }
        }
        PullRefreshIndicator(
            refreshing = loading,
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
private fun RecommendSkeletonCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .clip(RoundedCornerShape(32.dp))
            .border(1.dp, BorderLight, RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF24253A), BackgroundSurface)))
            .padding(22.dp)
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            SkeletonBlock(width = 74.dp, height = 12.dp, cornerRadius = 6.dp)
            androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SkeletonText(widths = listOf(210.dp, 168.dp), lineHeight = 22.dp, spacing = 10.dp, cornerRadius = 8.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkeletonBlock(width = 44.dp, height = 44.dp, cornerRadius = 10.dp)
                    Spacer(modifier = Modifier.size(10.dp))
                    androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                        SkeletonBlock(width = 96.dp, height = 12.dp, cornerRadius = 6.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        SkeletonBlock(width = 54.dp, height = 10.dp, cornerRadius = 5.dp)
                    }
                    SkeletonBlock(width = 84.dp, height = 32.dp, cornerRadius = 16.dp)
                }
            }
        }
    }
}

@Composable
private fun RecommendTodayCard(
    item: RecommendTodayItem,
    isRefreshing: Boolean,
    onClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .clip(RoundedCornerShape(32.dp))
            .border(1.dp, BorderLight, RoundedCornerShape(32.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = item.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.28f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
                .padding(22.dp)
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.cardCategory.ifBlank { stringResource(R.string.community_card_default_category) },
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = item.cardTitle.ifBlank { item.gameName },
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BackgroundSurface)
                        ) {
                            GameLogo(
                                iconUrl = item.gameIconUrl,
                                seed = "${item.appId}_recommend",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.size(10.dp))
                        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.gameName,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.gameCategory.ifBlank { stringResource(R.string.discover_category_all) },
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White)
                                .clickable { onPlayClick() }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = item.actionText.ifBlank { stringResource(R.string.discover_quick_play) },
                                color = Color.Black,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = isRefreshing,
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
                lineWidths = listOf(90.dp, 60.dp),
                cornerRadius = 32.dp,
                label = stringResource(R.string.common_loading)
            )
        }
    }
}
