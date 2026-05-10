package com.nexus.platform.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.platform.R
import com.nexus.platform.ui.navigation.MainDestination
import com.nexus.platform.ui.theme.BackgroundSurface
import com.nexus.platform.ui.theme.BorderLight
import com.nexus.platform.ui.theme.Primary
import com.nexus.platform.ui.theme.TextMain
import com.nexus.platform.ui.theme.TextMuted

private val BottomBarHeight = 80.dp

@Composable
fun MainBottomBar(
    selected: MainDestination,
    onSelect: (MainDestination) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(BottomBarHeight)
            .background(BackgroundSurface)
            .border(1.dp, BorderLight)
    ) {
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            containerColor = BackgroundSurface,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets(0, 0, 0, 0)
        ) {
            MainDestination.entries.forEach { tab ->
                val isSelected = tab == selected
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { if (!isSelected) onSelect(tab) },
                    icon = {
                        when (tab) {
                            MainDestination.Profile -> Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null
                            )
                            else -> Icon(
                                painter = painterResource(tabIconRes(tab)),
                                contentDescription = null
                            )
                        }
                    },
                    label = {
                        Text(
                            text = tabLabel(tab),
                            fontWeight = FontWeight.Medium
                        )
                    },
                    alwaysShowLabel = true,
                    interactionSource = remember { MutableInteractionSource() },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TextMain,
                        selectedTextColor = TextMain,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor = Primary.copy(alpha = 0.22f)
                    )
                )
            }
        }
    }
}

@Composable
private fun tabLabel(tab: MainDestination): String {
    return stringResource(tab.labelRes)
}

private fun tabIconRes(tab: MainDestination): Int {
    return when (tab) {
        MainDestination.Library -> R.drawable.ic_launcher_gamepad
        MainDestination.Discover -> R.drawable.ic_nav_explore
        MainDestination.Recommend -> R.drawable.ic_nexus_feedback
        MainDestination.Profile -> R.drawable.ic_default_avatar
    }
}
