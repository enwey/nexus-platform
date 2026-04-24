package com.nexus.platform.feature.recommend.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import coil.compose.AsyncImage
import com.nexus.platform.R
import com.nexus.platform.domain.model.RecommendTodayItem
import com.nexus.platform.domain.model.GameItem
import com.nexus.platform.ui.components.GameLogo
import com.nexus.platform.ui.components.SkeletonBlock
import com.nexus.platform.ui.components.SkeletonMotionTokens
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

private val DetailBottomPadding = 96.dp

@androidx.compose.runtime.Composable
fun RecommendDetailScreen(
    item: RecommendTodayItem,
    game: GameItem?,
    onBackClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBase)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = DetailBottomPadding),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                ) {
                    if (item.coverUrl.isBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.linearGradient(listOf(Color(0xFF24253A), BackgroundSurface)))
                                .padding(24.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                SkeletonBlock(width = 76.dp, height = 14.dp, cornerRadius = 7.dp)
                                SkeletonText(widths = listOf(188.dp, 236.dp), lineHeight = 16.dp, spacing = 10.dp, cornerRadius = 8.dp)
                            }
                        }
                    } else {
                        AsyncImage(
                            model = item.coverUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.58f))
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 52.dp, start = 20.dp)
                            .align(Alignment.TopStart)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .clickable { onBackClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = Color.White
                        )
                    }
                }
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = item.articleTag.ifBlank { stringResource(R.string.community_article_default_tag) },
                        color = Primary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.articleTitle.ifBlank { item.cardTitle.ifBlank { item.gameName } },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = item.articleBody.ifBlank { game?.description ?: "" },
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (game != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .border(1.dp, BorderLight, RoundedCornerShape(18.dp))
                            .background(BackgroundSurfaceElevated)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Crossfade(
                            targetState = game.iconUrl.isBlank(),
                            animationSpec = tween(SkeletonMotionTokens.OverlayEnterMillis),
                            label = "recommendDetailGameIcon"
                        ) { showSkeletonIcon ->
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BackgroundSurface)
                            ) {
                                if (showSkeletonIcon) {
                                    SkeletonBlock(modifier = Modifier.fillMaxSize(), height = 56.dp, cornerRadius = 12.dp)
                                } else {
                                    GameLogo(
                                        iconUrl = game.iconUrl,
                                        seed = "${game.id}_detail",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.size(12.dp))
                        Crossfade(
                            targetState = game.description.ifBlank { game.category },
                            animationSpec = tween(SkeletonMotionTokens.OverlayEnterMillis),
                            label = "recommendDetailGameMeta"
                        ) { subtitle ->
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = game.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = subtitle,
                                    color = TextMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(Brush.linearGradient(listOf(PrimaryStart, PrimaryEnd)))
                                .clickable { onPlayClick() }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = item.actionText.ifBlank { stringResource(R.string.discover_quick_play) },
                                color = TextMain,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            }
        }
    }
}
