package com.nexus.platform.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NexusColors = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    background = Background,
    surface = Surface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceAlt,
    onSurfaceVariant = TextSecondary
)

@Composable
fun NexusPlatformTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NexusColors,
        typography = Typography,
        content = content
    )
}
