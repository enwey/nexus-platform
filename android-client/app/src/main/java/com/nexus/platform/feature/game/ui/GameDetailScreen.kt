package com.nexus.platform.feature.game.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.platform.domain.model.GameItem
import com.nexus.platform.ui.components.ActionButton
import com.nexus.platform.ui.theme.BackgroundBase
import com.nexus.platform.ui.theme.BackgroundSurface
import com.nexus.platform.ui.theme.BackgroundSurfaceElevated
import com.nexus.platform.ui.theme.PrimaryEnd
import com.nexus.platform.ui.theme.PrimaryStart
import com.nexus.platform.ui.theme.TextMuted

@Composable
fun GameDetailScreen(
    game: GameItem,
    onBackClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
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
                text = "返回",
                onClick = onBackClick,
                primary = false,
                modifier = Modifier
                    .size(width = 72.dp, height = 40.dp)
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
                        .background(
                            Brush.linearGradient(listOf(BackgroundSurfaceElevated, PrimaryEnd.copy(alpha = 0.4f)))
                        )
                )
                Spacer(modifier = Modifier.size(14.dp))
                Column {
                    Text(game.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    Text("Nexus 官方优选作品", color = PrimaryStart, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            StatsRow(version = game.version)
            Spacer(modifier = Modifier.height(20.dp))
            Text("游戏简介", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                game.description.ifBlank { "极速启动、稳定帧率、云原生资源分发，为你提供高品质轻游戏体验。" },
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(24.dp))
            ActionButton(
                text = "立即秒开",
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
        StatItem("4.9", "评分")
        StatItem("#1", "动作榜")
        StatItem(version, "版本")
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.ExtraBold)
        Text(label, color = TextMuted, style = MaterialTheme.typography.bodySmall)
    }
}
