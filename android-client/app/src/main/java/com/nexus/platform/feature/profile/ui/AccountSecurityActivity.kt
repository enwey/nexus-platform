package com.nexus.platform.feature.profile.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.platform.NexusApplication
import com.nexus.platform.R
import com.nexus.platform.core.i18n.AppLanguageManager
import com.nexus.platform.core.ui.showCenterToast
import com.nexus.platform.data.remote.PlatformBackendApi
import com.nexus.platform.feature.auth.ui.LoginActivity
import com.nexus.platform.ui.theme.BackgroundBase
import com.nexus.platform.ui.theme.BackgroundSurface
import com.nexus.platform.ui.theme.BackgroundSurfaceElevated
import com.nexus.platform.ui.theme.BorderLight
import com.nexus.platform.ui.theme.NexusPlatformTheme
import com.nexus.platform.ui.theme.Primary
import com.nexus.platform.ui.theme.TextMuted
import com.nexus.platform.ui.components.ActionButton
import kotlinx.coroutines.launch

class AccountSecurityActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val authRepository = (application as NexusApplication).container.authRepository
        val logoutUseCase = (application as NexusApplication).container.logoutUseCase
        val isLoggedIn = authRepository.currentSession() != null
        setContent {
            NexusPlatformTheme {
                AccountSecurityScreen(
                    isLoggedIn = isLoggedIn,
                    onBackClick = { finish() },
                    onRequestLogin = {
                        startActivity(Intent(this, LoginActivity::class.java))
                    },
                    onLogoutCurrent = {
                        logoutUseCase()
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun AccountSecurityScreen(
    isLoggedIn: Boolean,
    onBackClick: () -> Unit,
    onRequestLogin: () -> Unit,
    onLogoutCurrent: () -> Unit
) {
    val context = LocalContext.current
    var biometricEnabled by remember { mutableStateOf(true) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBase)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = stringResource(R.string.game_back),
                tint = Color.White,
                modifier = Modifier
                    .size(48.dp)
                    .padding(10.dp)
                    .clickable { onBackClick() }
            )
            Text(
                text = stringResource(R.string.account_security_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
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
                        icon = "🧬",
                        title = stringResource(R.string.account_security_biometric),
                        subtitle = stringResource(R.string.account_security_biometric_subtitle),
                        showSwitch = true,
                        checked = biometricEnabled,
                        onCheckedChange = { biometricEnabled = it }
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
                        icon = "📋",
                        title = stringResource(R.string.account_security_device_count),
                        subtitle = stringResource(R.string.account_security_device_count_subtitle),
                        onClick = {
                            context.startActivity(Intent(context, DeviceManagementActivity::class.java))
                        },
                        showChevron = true
                    )
                    MenuItem(
                        icon = "🗑️",
                        title = stringResource(R.string.account_termination_title),
                        subtitle = stringResource(R.string.account_termination_warning_title),
                        onClick = {
                            context.startActivity(Intent(context, AccountTerminationActivity::class.java))
                        },
                        showChevron = true
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

            if (!isLoggedIn) {
                Spacer(modifier = Modifier.height(20.dp))
                ActionButton(
                    text = stringResource(R.string.profile_login_account),
                    onClick = onRequestLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFE5484D))
                        .clickable { showLogoutConfirm = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.profile_logout),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(stringResource(R.string.profile_logout)) },
            text = { Text(stringResource(R.string.profile_logout_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    onLogoutCurrent()
                    showCenterToast(context, context.getString(R.string.profile_logout))
                }) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = Color.White
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
                fontWeight = FontWeight.Medium,
                color = Color.White
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
        } else if (showArrow || showChevron) {
            Image(
                painter = painterResource(id = R.drawable.ic_more),
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
