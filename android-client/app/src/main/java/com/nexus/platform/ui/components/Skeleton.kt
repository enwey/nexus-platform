package com.nexus.platform.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nexus.platform.ui.theme.BackgroundSurfaceElevated

object SkeletonMotionTokens {
    const val ShimmerDurationMillis = 1450
    const val OverlayEnterMillis = 260
    const val OverlayExitMillis = 220
    const val SkeletonBaseAlpha = 0.06f
    const val SkeletonGlowAlpha = 0.12f
    const val SectionOverlayAlpha = 0.16f
    const val PillBackgroundAlpha = 0.72f
}

@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    height: Dp,
    width: Dp? = null,
    cornerRadius: Dp = 16.dp
) {
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier)
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .skeletonShimmer()
    )
}

@Composable
fun SkeletonText(
    widths: List<Dp>,
    modifier: Modifier = Modifier,
    lineHeight: Dp = 12.dp,
    spacing: Dp = 8.dp,
    cornerRadius: Dp = 6.dp
) {
    Column(modifier = modifier) {
        widths.forEachIndexed { index, width ->
            SkeletonBlock(
                width = width,
                height = lineHeight,
                cornerRadius = cornerRadius
            )
            if (index != widths.lastIndex) {
                Box(modifier = Modifier.height(spacing))
            }
        }
    }
}

fun Modifier.skeletonShimmer(
    baseColor: Color = Color.White.copy(alpha = SkeletonMotionTokens.SkeletonBaseAlpha),
    glowColor: Color = Color.White.copy(alpha = SkeletonMotionTokens.SkeletonGlowAlpha)
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val translate = transition.animateFloat(
        initialValue = -400f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SkeletonMotionTokens.ShimmerDurationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "skeletonTranslate"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(baseColor, glowColor, baseColor),
            start = Offset(translate.value - 260f, 0f),
            end = Offset(translate.value, 260f)
        ),
        shape = RoundedCornerShape(16.dp)
    )
        .background(BackgroundSurfaceElevated, RoundedCornerShape(16.dp))
}

@Composable
fun SectionRefreshOverlay(
    modifier: Modifier = Modifier,
    lineWidths: List<Dp> = listOf(92.dp, 64.dp),
    cornerRadius: Dp = 20.dp,
    label: String = "Refreshing"
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.Black.copy(alpha = SkeletonMotionTokens.SectionOverlayAlpha))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(cornerRadius))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.End
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = SkeletonMotionTokens.PillBackgroundAlpha))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                androidx.compose.material3.Text(
                    text = label,
                    color = Color.White
                )
            }
            Box(modifier = Modifier.height(8.dp))
            lineWidths.forEachIndexed { index, width ->
                SkeletonBlock(width = width, height = 10.dp, cornerRadius = 5.dp)
                if (index != lineWidths.lastIndex) {
                    Box(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
