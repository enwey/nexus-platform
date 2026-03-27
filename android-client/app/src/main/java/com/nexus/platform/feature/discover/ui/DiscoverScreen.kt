package com.nexus.platform.feature.discover.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.platform.R
import com.nexus.platform.domain.model.GameItem
import com.nexus.platform.ui.components.GameLogo
import com.nexus.platform.ui.theme.BackgroundSurface
import com.nexus.platform.ui.theme.BackgroundSurfaceElevated
import com.nexus.platform.ui.theme.BackgroundBase
import com.nexus.platform.ui.theme.BorderLight
import com.nexus.platform.ui.theme.Primary
import com.nexus.platform.ui.theme.PrimaryEnd
import com.nexus.platform.ui.theme.PrimaryStart
import com.nexus.platform.ui.theme.TextMain
import com.nexus.platform.ui.theme.TextMuted

private val TopLevelBottomPadding = 96.dp
private val QuickPlayText = TextMain

@Composable
fun DiscoverScreen(
    games: List<GameItem>,
    onGameClick: (GameItem) -> Unit,
    onQuickPlayClick: (GameItem) -> Unit
) {
    val fallbackGames = listOf(
        GameItem(
            id = "discover_demo_1",
            name = stringResource(R.string.discover_rank_1_title),
            description = stringResource(R.string.discover_rank_1_subtitle),
            iconUrl = "",
            downloadUrl = "",
            version = "1.0.0"
        ),
        GameItem(
            id = "discover_demo_2",
            name = stringResource(R.string.discover_rank_2_title),
            description = stringResource(R.string.discover_rank_2_subtitle),
            iconUrl = "",
            downloadUrl = "",
            version = "1.0.0"
        )
    )
    val rankedGames = if (games.isEmpty()) fallbackGames else games.take(8)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBase)
            .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = TopLevelBottomPadding),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(stringResource(R.string.discover_title), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
        Banner()
        CategoryRow()
        SectionHeader(stringResource(R.string.discover_section_rank), stringResource(R.string.discover_view_more))
        rankedGames.take(2).forEach { game ->
            RankedItem(
                game = game,
                onGameClick = onGameClick,
                onQuickPlayClick = onQuickPlayClick
            )
        }
    }
}

@Composable
private fun Banner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(PrimaryStart, PrimaryEnd)))
            .padding(20.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Column {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(stringResource(R.string.discover_banner_hot), style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.discover_banner_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun CategoryRow() {
    val categories = listOf(
        stringResource(R.string.discover_category_all),
        stringResource(R.string.discover_category_action),
        stringResource(R.string.discover_category_casual),
        stringResource(R.string.discover_category_rpg)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        categories.forEachIndexed { index, label ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (index == 0) Primary else BackgroundSurface)
                    .border(1.dp, if (index == 0) Primary else BorderLight, RoundedCornerShape(20.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMain
                )
            }
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
            Text(action, color = Primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
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
