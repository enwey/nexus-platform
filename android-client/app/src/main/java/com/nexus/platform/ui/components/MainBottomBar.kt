package com.nexus.platform.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nexus.platform.ui.navigation.MainDestination
import com.nexus.platform.ui.theme.GlassBackground
import com.nexus.platform.ui.theme.PrimaryEnd
import com.nexus.platform.ui.theme.PrimaryStart
import com.nexus.platform.ui.theme.TextMain
import com.nexus.platform.ui.theme.TextMuted

@Composable
fun MainBottomBar(
    selected: MainDestination,
    onSelect: (MainDestination) -> Unit
) {
    NavigationBar(
        containerColor = GlassBackground,
        tonalElevation = 0.dp
    ) {
        MainDestination.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TextMain,
                    selectedTextColor = TextMain,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted,
                    disabledIconColor = TextMuted.copy(alpha = 0.5f),
                    disabledTextColor = TextMuted.copy(alpha = 0.5f)
                ),
                icon = {
                    val brush = if (tab == selected) {
                        Brush.linearGradient(listOf(PrimaryStart, PrimaryEnd))
                    } else {
                        Brush.linearGradient(
                            listOf(
                                Color(0x668B8D99),
                                Color(0x668B8D99)
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(brush)
                    )
                }
            )
        }
    }
}
