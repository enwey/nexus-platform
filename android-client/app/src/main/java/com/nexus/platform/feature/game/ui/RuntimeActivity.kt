package com.nexus.platform.feature.game.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.platform.R
import com.nexus.platform.ui.theme.AccentGreen
import com.nexus.platform.ui.theme.BackgroundBase
import com.nexus.platform.ui.theme.BackgroundSurface
import com.nexus.platform.ui.theme.BackgroundSurfaceElevated
import com.nexus.platform.ui.theme.BorderLight
import com.nexus.platform.ui.theme.NexusPlatformTheme
import com.nexus.platform.ui.theme.Primary
import com.nexus.platform.ui.theme.PrimaryEnd
import com.nexus.platform.ui.theme.PrimaryStart
import com.nexus.platform.ui.theme.TextMain
import com.nexus.platform.ui.theme.TextMuted

class RuntimeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NexusPlatformTheme {
                RuntimeScreen(
                    onDismiss = { finish() }
                )
            }
        }
    }
}

@Composable
private fun RuntimeScreen(
    onDismiss: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(BackgroundSurfaceElevated)
                    .border(1.dp, BorderLight, RoundedCornerShape(24.dp))
            )
            Spacer(modifier = Modifier.height(30.dp))
            Text(
                text = stringResource(R.string.runtime_status_preparing),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(40.dp))
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .width(150.dp)
                        .height(4.dp)
                        .background(Primary)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.runtime_status_loading_resource),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 60.dp, end = 20.dp)
        ) {
            CapsuleMenu(
                onMoreClick = { showMenu = true },
                onCloseClick = onDismiss
            )
        }

        if (showMenu) {
            RuntimeMenuOverlay(
                onDismiss = { showMenu = false },
                onCloseGame = onDismiss
            )
        }
    }
}

@Composable
private fun CapsuleMenu(
    onMoreClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .width(88.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clickable { onMoreClick() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(Color.White)
                )
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(Color.White)
                )
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(Color.White)
                )
            }
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(16.dp)
                .background(Color.White.copy(alpha = 0.2f))
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .clickable { onCloseClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "×",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RuntimeMenuOverlay(
    onDismiss: () -> Unit,
    onCloseGame: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Color(0xFF1A1B23),
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(5.dp)
                    .background(
                        Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(3.dp)
                    )
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    PrimaryStart,
                                    PrimaryEnd
                                )
                            )
                        )
                        .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
                )
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.runtime_menu_game_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.runtime_menu_game_players),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        Text(
                            text = stringResource(R.string.runtime_menu_game_studio),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(BackgroundSurfaceElevated)
                        .border(1.dp, BorderLight, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⭐",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = stringResource(R.string.runtime_menu_share_title),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShareButton(
                    icon = "💬",
                    label = stringResource(R.string.runtime_menu_share_whatsapp),
                    color = Color(0xFF25D366)
                )
                ShareButton(
                    icon = "f",
                    label = stringResource(R.string.runtime_menu_share_facebook),
                    color = Color(0xFF1877F2)
                )
                ShareButton(
                    icon = "📕",
                    label = stringResource(R.string.runtime_menu_share_xiaohongshu),
                    color = Color(0xFFFF2442)
                )
                ShareButton(
                    icon = "🔗",
                    label = stringResource(R.string.runtime_menu_share_link),
                    color = BackgroundSurfaceElevated,
                    border = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BackgroundSurfaceElevated)
                    .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
            ) {
                MenuItem(
                    icon = "➕",
                    title = stringResource(R.string.runtime_menu_add_favorite)
                )
                MenuItem(
                    icon = "🔄",
                    title = stringResource(R.string.runtime_menu_restart)
                )
                MenuItem(
                    icon = "💬",
                    title = stringResource(R.string.runtime_menu_feedback),
                    last = true
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.common_cancel),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ShareButton(
    icon: String,
    label: String,
    color: Color,
    border: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(color)
                .then(
                    if (border) {
                        Modifier.border(1.dp, BorderLight, RoundedCornerShape(15.dp))
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineSmall
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
    }
}

@Composable
private fun MenuItem(
    icon: String,
    title: String,
    last: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!last) {
                    Modifier.background(
                        BorderLight.copy(alpha = 0.08f),
                        RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 0.dp)
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
