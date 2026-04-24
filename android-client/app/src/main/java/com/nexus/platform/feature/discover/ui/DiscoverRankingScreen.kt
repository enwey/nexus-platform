package com.nexus.platform.feature.discover.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import com.nexus.platform.R
import com.nexus.platform.domain.model.GameItem
import com.nexus.platform.ui.components.GameLogo
import com.nexus.platform.ui.components.SkeletonBlock
import com.nexus.platform.ui.components.SkeletonMotionTokens
import com.nexus.platform.ui.theme.BackgroundBase
import com.nexus.platform.ui.theme.BackgroundSurface
import com.nexus.platform.ui.theme.BackgroundSurfaceElevated
import com.nexus.platform.ui.theme.BorderLight
import com.nexus.platform.ui.theme.TextMuted

@Composable
fun DiscoverRankingScreen(
    games: List<GameItem>,
    onBackClick: () -> Unit,
    onGameClick: (GameItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBase)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = stringResource(R.string.game_back),
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier
                    .size(48.dp)
                    .padding(10.dp)
                    .clickable { onBackClick() }
            )
            Text(
                text = stringResource(R.string.discover_view_more),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Crossfade(
            targetState = games.isEmpty(),
            animationSpec = tween(SkeletonMotionTokens.OverlayEnterMillis),
            label = "rankingContent"
        ) { isEmpty ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isEmpty) {
                    items(6) { index ->
                        RankingSkeletonRow(rank = index + 1)
                    }
                } else {
                    itemsIndexed(games, key = { _, game -> "rank_${game.id}" }) { index, game ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onGameClick(game) }
                                .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
                                .background(BackgroundSurface, RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.size(10.dp))
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(BackgroundSurfaceElevated, RoundedCornerShape(12.dp))
                                    .border(1.dp, BorderLight, RoundedCornerShape(12.dp))
                            ) {
                                GameLogo(
                                    iconUrl = game.iconUrl,
                                    seed = "${game.id}_${game.name}_rank",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.size(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = game.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = game.description.ifBlank { "v${game.version}" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RankingSkeletonRow(rank: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
            .background(BackgroundSurface, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$rank",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.12f),
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.size(10.dp))
        SkeletonBlock(width = 52.dp, height = 52.dp, cornerRadius = 12.dp)
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            SkeletonBlock(width = 132.dp, height = 14.dp, cornerRadius = 7.dp)
            Spacer(modifier = Modifier.size(6.dp))
            SkeletonBlock(width = 166.dp, height = 12.dp, cornerRadius = 6.dp)
        }
    }
}
