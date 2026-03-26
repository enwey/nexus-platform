package com.nexus.platform.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

private data class GradientPair(val start: Color, val end: Color)

private val LogoGradients = listOf(
    GradientPair(Color(0xFF5B8CFF), Color(0xFF3B5BDB)),
    GradientPair(Color(0xFF22C55E), Color(0xFF0EA5A4)),
    GradientPair(Color(0xFFF97316), Color(0xFFEF4444)),
    GradientPair(Color(0xFF06B6D4), Color(0xFF2563EB)),
    GradientPair(Color(0xFFEC4899), Color(0xFF8B5CF6)),
    GradientPair(Color(0xFFEAB308), Color(0xFFF97316)),
    GradientPair(Color(0xFF14B8A6), Color(0xFF0EA5E9)),
    GradientPair(Color(0xFF84CC16), Color(0xFF22C55E)),
    GradientPair(Color(0xFF7C3AED), Color(0xFF4F46E5)),
    GradientPair(Color(0xFFFB7185), Color(0xFFF43F5E))
)

@Composable
fun GameLogo(
    iconUrl: String,
    seed: String,
    modifier: Modifier = Modifier
) {
    val safeSeed = if (seed.isBlank()) "nexus_logo" else seed
    val url = iconUrl.trim()
    val shouldLoadImage = url.startsWith("http://") || url.startsWith("https://")

    val context = LocalContext.current
    val imageRequest = remember(url, context, shouldLoadImage) {
        ImageRequest.Builder(context)
            .data(if (shouldLoadImage) url else null)
            .crossfade(false)
            .build()
    }
    val painter = rememberAsyncImagePainter(model = imageRequest)
    val state = painter.state

    Box(modifier = modifier) {
        GradientLogoPlaceholder(seed = safeSeed, modifier = Modifier.fillMaxSize())
        if (state is AsyncImagePainter.State.Success) {
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.12f)
                            )
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.02f))
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize(0.38f)
                .alpha(0.12f)
                .background(
                    Color.White,
                    shape = RoundedCornerShape(12.dp)
                )
        )
    }
}

@Composable
fun GradientLogoPlaceholder(
    seed: String,
    modifier: Modifier = Modifier
) {
    val safeSeed = if (seed.isBlank()) "nexus_logo" else seed
    val hash = remember(safeSeed) { safeSeed.hashCode() and Int.MAX_VALUE }
    val pair = remember(hash) { LogoGradients[hash % LogoGradients.size] }
    val direction = remember(hash) { hash % 2 }
    val start = remember(direction) {
        when (direction) {
            0 -> Offset(0f, 0f)
            else -> Offset(1200f, 0f)
        }
    }
    val end = remember(direction) {
        when (direction) {
            0 -> Offset(1200f, 1200f)
            else -> Offset(0f, 1200f)
        }
    }

    Box(
        modifier = modifier.background(
            brush = Brush.linearGradient(
                colors = listOf(pair.start, pair.end),
                start = start,
                end = end
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.White.copy(alpha = 0.04f)
                )
        )
    }
}
