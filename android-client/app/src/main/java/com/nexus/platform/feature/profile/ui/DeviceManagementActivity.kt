package com.nexus.platform.feature.profile.ui

import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.platform.R
import com.nexus.platform.data.remote.PlatformBackendApi
import com.nexus.platform.domain.model.DeviceSession
import com.nexus.platform.ui.theme.BackgroundBase
import com.nexus.platform.ui.theme.BackgroundSurface
import com.nexus.platform.ui.theme.BackgroundSurfaceElevated
import com.nexus.platform.ui.theme.BorderLight
import com.nexus.platform.ui.theme.NexusPlatformTheme
import com.nexus.platform.ui.theme.Primary
import com.nexus.platform.ui.theme.TextMuted
import kotlinx.coroutines.launch

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
    val context = LocalContext.current
    val backendApi = remember(context) { PlatformBackendApi(context) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var devices by remember { mutableStateOf<List<DeviceSession>>(emptyList()) }

    LaunchedEffect(Unit) {
        devices = backendApi.getDeviceSessions()
    }

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
                text = "<",
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
                Column(modifier = Modifier.padding(20.dp)) {
                    if (devices.isEmpty()) {
                        Text(stringResource(R.string.device_management_empty), color = TextMuted)
                    } else {
                        devices.forEachIndexed { index, device ->
                            DeviceItem(
                                device = device,
                                onKick = {
                                    if (device.current) {
                                        Toast.makeText(context, context.getString(R.string.device_management_current_cannot_kick), Toast.LENGTH_SHORT).show()
                                        return@DeviceItem
                                    }
                                    scope.launch {
                                        val ok = backendApi.kickDevice(device.deviceId)
                                        Toast.makeText(
                                            context,
                                            if (ok) context.getString(R.string.device_management_kick_success) else context.getString(R.string.device_management_kick_failed),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        if (ok) {
                                            devices = backendApi.getDeviceSessions()
                                        }
                                    }
                                }
                            )
                            if (index != devices.lastIndex) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceItem(
    device: DeviceSession,
    onKick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onKick() }
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
            Text("💻", style = MaterialTheme.typography.headlineSmall)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(device.deviceName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text("${device.model}  ${device.ip}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            Text(device.lastActiveAt, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Text(
            text = if (device.current) stringResource(R.string.device_management_status_current) else stringResource(R.string.device_management_status_offline),
            style = MaterialTheme.typography.bodySmall,
            color = if (device.current) Primary else TextMuted
        )
    }
}
