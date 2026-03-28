package com.nexus.platform.feature.profile.ui

import android.os.Bundle
import android.content.Intent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

class AccountSecurityActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NexusPlatformTheme {
                AccountSecurityScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@Composable
private fun AccountSecurityScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var biometricEnabled by remember { mutableStateOf(true) }

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
                text = stringResource(R.string.account_security_title),
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
                    SectionTitle(text = stringResource(R.string.account_security_section_login))
                    Spacer(modifier = Modifier.height(12.dp))
                    MenuItem(
                        icon = "🔐",
                        title = stringResource(R.string.account_security_change_password),
                        subtitle = stringResource(R.string.account_security_change_password_subtitle),
                        onClick = {
                            context.startActivity(Intent(context, ChangePasswordActivity::class.java))
                        }
                    )
                    MenuItem(
                        icon = "📱",
                        title = stringResource(R.string.account_security_biometric),
                        subtitle = stringResource(R.string.account_security_biometric_subtitle),
                        showSwitch = true,
                        checked = biometricEnabled,
                        onCheckedChange = { biometricEnabled = it }
                    )
                    MenuItem(
                        icon = "🤖",
                        title = stringResource(R.string.account_security_ai_login),
                        subtitle = stringResource(R.string.account_security_ai_login_subtitle),
                        onClick = {
                            Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show()
                        },
                        showArrow = false
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
                    Spacer(modifier = Modifier.height(20.dp))
                    SectionTitle(text = stringResource(R.string.account_security_section_device))
                    Spacer(modifier = Modifier.height(12.dp))
                    MenuItem(
                        icon = "📱",
                        title = stringResource(R.string.account_security_current_device),
                        subtitle = stringResource(R.string.account_security_current_device_subtitle),
                        onClick = {
                            context.startActivity(Intent(context, DeviceManagementActivity::class.java))
                        }
                    )
                    MenuItem(
                        icon = "📱",
                        title = stringResource(R.string.account_security_device_count),
                        subtitle = stringResource(R.string.account_security_device_count_subtitle),
                        onClick = {
                            context.startActivity(Intent(context, DeviceManagementActivity::class.java))
                        },
                        showChevron = true
                    )
                    MenuItem(
                        icon = "❌",
                        title = stringResource(R.string.account_security_logout_all),
                        subtitle = stringResource(R.string.account_security_logout_all_subtitle),
                        onClick = {
                            Toast.makeText(context, "Logged out from other devices", Toast.LENGTH_SHORT).show()
                        }
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
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = TextMuted
    )
}

@Composable
private fun MenuItem(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {},
    showSwitch: Boolean = false,
    checked: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
    showArrow: Boolean = true,
    showChevron: Boolean = false
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
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BackgroundSurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineSmall
            )
        }
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
        if (showSwitch) {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Primary,
                    checkedTrackColor = Primary.copy(alpha = 0.3f),
                    uncheckedThumbColor = Color(0xFFD1D5DB),
                    uncheckedTrackColor = BackgroundSurfaceElevated
                )
            )
        } else if (showArrow) {
            Text(
                text = "›",
                style = MaterialTheme.typography.headlineMedium,
                color = TextMuted
            )
        } else if (showChevron) {
            Text(
                text = "›",
                style = MaterialTheme.typography.headlineMedium,
                color = TextMuted
            )
        }
    }
}
