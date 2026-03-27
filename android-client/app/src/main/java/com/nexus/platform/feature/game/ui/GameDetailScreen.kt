package com.nexus.platform.feature.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.platform.R
import com.nexus.platform.domain.model.GameItem
import com.nexus.platform.ui.components.ActionButton
import com.nexus.platform.ui.components.GameLogo
import com.nexus.platform.ui.theme.BackgroundBase
import com.nexus.platform.ui.theme.BackgroundSurface
import com.nexus.platform.ui.theme.BackgroundSurfaceElevated
import com.nexus.platform.ui.theme.BorderLight
import com.nexus.platform.ui.theme.PrimaryStart
import com.nexus.platform.ui.theme.TextMuted

@Composable
fun GameDetailScreen(
    game: GameItem,
    onBackClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBase)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            PrimaryStart.copy(alpha = 0.65f),
                            BackgroundBase
                        )
                    )
                )
                .padding(16.dp)
        ) {
            ActionButton(
                text = stringResource(R.string.game_back),
                onClick = onBackClick,
                primary = false,
                modifier = Modifier
                    .size(width = 88.dp, height = 48.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(BackgroundBase)
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .border(1.dp, BorderLight, RoundedCornerShape(22.dp))
                        .background(BackgroundSurfaceElevated)
                ) {
                    GameLogo(
                        iconUrl = game.iconUrl,
                        seed = "${game.id}_${game.name}",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.size(14.dp))
                Column {
                    Text(game.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    Text(stringResource(R.string.game_editor_pick), color = PrimaryStart, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            StatsRow(version = game.version)
            Spacer(modifier = Modifier.height(20.dp))
            Text(stringResource(R.string.game_intro_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                game.description.ifBlank { stringResource(R.string.game_intro_fallback) },
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(24.dp))
            ActionButton(
                text = stringResource(R.string.game_play_now),
                onClick = onPlayClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
            )
        }
    }
}

@Composable
private fun StatsRow(version: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BackgroundSurface)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem("4.9", stringResource(R.string.game_score_label))
        StatItem("#1", stringResource(R.string.game_rank_label))
        StatItem(version, stringResource(R.string.game_version_label))
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.ExtraBold)
        Text(label, color = TextMuted, style = MaterialTheme.typography.bodySmall)
    }
}
