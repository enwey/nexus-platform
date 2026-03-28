package com.nexus.platform.feature.profile.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.platform.R
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

class DeviceManagementActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NexusPlatformTheme {
                DeviceManagementScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@Composable
private fun DeviceManagementScreen(
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBase)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "‹",
                style = MaterialTheme.typography.headlineMedium,
                color = TextMuted,
                modifier = Modifier.clickable { onBackClick() }
            )
            Text(
                text = stringResource(R.string.device_management_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(BackgroundSurface)
                    .border(1.dp, BorderLight, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    DeviceItem(
                        isCurrent = true,
                        deviceName = stringResource(R.string.device_management_current_name),
                        deviceModel = stringResource(R.string.device_management_current_model),
                        lastActive = stringResource(R.string.device_management_current_last_active),
                        ipAddress = stringResource(R.string.device_management_current_ip),
                        onClick = { }
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                BorderLight.copy(alpha = 0.08f),
                                RoundedCornerShape(0.dp)
                            )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.device_management_other_title),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DeviceItem(
                        isCurrent = false,
                        deviceName = stringResource(R.string.device_management_other_1_name),
                        deviceModel = stringResource(R.string.device_management_other_1_model),
                        lastActive = stringResource(R.string.device_management_other_1_last_active),
                        ipAddress = stringResource(R.string.device_management_other_1_ip),
                        onClick = { }
                    )
                    DeviceItem(
                        isCurrent = false,
                        deviceName = stringResource(R.string.device_management_other_2_name),
                        deviceModel = stringResource(R.string.device_management_other_2_model),
                        lastActive = stringResource(R.string.device_management_other_2_last_active),
                        ipAddress = stringResource(R.string.device_management_other_2_ip),
                        onClick = { }
                    )
                }

                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BackgroundBase)
                        .align(Alignment.TopStart)
                )
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BackgroundBase)
                        .align(Alignment.TopEnd)
                )
            }
        }
    }
}

@Composable
private fun DeviceItem(
    isCurrent: Boolean,
    deviceName: String,
    deviceModel: String,
    lastActive: String,
    ipAddress: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(BackgroundSurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "📱",
                style = MaterialTheme.typography.headlineSmall
            )
        }
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = deviceName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = deviceModel,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Primary.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.device_management_current_tag),
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = lastActive,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Text(
                    text = ipAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
        Text(
            text = "›",
            style = MaterialTheme.typography.headlineMedium,
            color = TextMuted
        )
    }
}
