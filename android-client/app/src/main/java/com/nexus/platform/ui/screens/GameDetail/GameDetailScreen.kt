package com.nexus.platform.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.platform.utils.Game

@Composable
fun GameDetailScreen(
    game: Game,
    onBackClick: () -> Unit = {},
    onPlayClick: () -> Unit = {}
) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(game.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text("版本：${game.version}", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(game.description, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(24.dp))
        PrimaryButton(text = "开始体验", onClick = onPlayClick, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))
        PrimaryButton(text = "返回", onClick = onBackClick, modifier = Modifier.fillMaxWidth())
    }
}
