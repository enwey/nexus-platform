﻿﻿﻿﻿﻿﻿﻿package com.nexus.platform.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DiscoverScreen(
    onGameClick: (String) -> Unit = {},
    onCategoryClick: (String) -> Unit = {}
) {
    val featuredGames = listOf(
        DiscoverEntry("watermelon", "合成大西瓜", "休闲闯关，适合快速体验"),
        DiscoverEntry("defense", "最强保卫战", "塔防策略，节奏紧凑"),
        DiscoverEntry("racing", "极速飞车", "竞速体验，适合碎片时间")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("发现游戏", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("这里先保留为静态推荐页，后续再接真实发现流。", style = MaterialTheme.typography.bodyMedium)

        featuredGames.forEach { game ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onCategoryClick("featured")
                        onGameClick(game.id)
                    }
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Column {
                        Text(game.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(game.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

private data class DiscoverEntry(
    val id: String,
    val name: String,
    val description: String
)
