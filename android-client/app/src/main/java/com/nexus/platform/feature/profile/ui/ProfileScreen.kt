package com.nexus.platform.feature.profile.ui

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.platform.R
import com.nexus.platform.core.i18n.AppLanguage
import com.nexus.platform.data.remote.PlatformBackendApi
import com.nexus.platform.domain.model.UserProfileDetail
import com.nexus.platform.domain.model.WalletSummary
import com.nexus.platform.ui.theme.AccentRed
import com.nexus.platform.ui.theme.BackgroundBase
import com.nexus.platform.ui.theme.BackgroundSurface
import com.nexus.platform.ui.theme.BackgroundSurfaceElevated
import com.nexus.platform.ui.theme.BorderLight
import com.nexus.platform.ui.theme.Primary
import com.nexus.platform.ui.theme.PrimaryEnd
import com.nexus.platform.ui.theme.PrimaryStart
import com.nexus.platform.ui.theme.TextMain
import com.nexus.platform.ui.theme.TextMuted

private val TopLevelBottomPadding = 96.dp

@Composable
fun ProfileScreen(
    isLoggedIn: Boolean,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onRequestLogin: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current
    val backendApi = remember(context) { PlatformBackendApi(context) }
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    var profile by remember { mutableStateOf<UserProfileDetail?>(null) }
    var wallet by remember { mutableStateOf<WalletSummary?>(null) }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            profile = runCatching { backendApi.getUserProfile() }.getOrNull()
            wallet = runCatching { backendApi.getWalletSummary() }.getOrNull()
        } else {
            profile = null
            wallet = null
        }
    }

    fun requireLogin(action: () -> Unit) {
        if (isLoggedIn) {
            action()
        } else {
            Toast.makeText(context, context.getString(R.string.profile_login_required), Toast.LENGTH_SHORT).show()
            onRequestLogin()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBase)
            .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = TopLevelBottomPadding),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        UserCard(profile = profile, isLoggedIn = isLoggedIn)
        WalletCard(
            wallet = wallet,
            onBillClick = { requireLogin { context.startActivity(Intent(context, BillingActivity::class.java)) } },
            onHowToEarnClick = { requireLogin { context.startActivity(Intent(context, ReferralActivity::class.java)) } }
        )
        ReferralCard(onClick = { requireLogin { context.startActivity(Intent(context, ReferralActivity::class.java)) } })
        MenuGroup(
            items = listOf(
                stringResource(R.string.profile_security),
                stringResource(R.string.profile_cloud_sync),
                stringResource(R.string.profile_clear_cache),
                stringResource(R.string.profile_language)
            ),
            rightTexts = listOf(
                null,
                if (isLoggedIn) stringResource(R.string.profile_sync_enabled) else stringResource(R.string.profile_sync_disabled),
                stringResource(R.string.profile_cache_size),
                stringResource(currentLanguage.labelRes)
            ),
            onItemClick = { index ->
                when (index) {
                    0 -> requireLogin { context.startActivity(Intent(context, AccountSecurityActivity::class.java)) }
                    1 -> requireLogin { context.startActivity(Intent(context, DeviceManagementActivity::class.java)) }
                    2 -> Toast.makeText(context, context.getString(R.string.profile_cache_cleared), Toast.LENGTH_SHORT).show()
                    3 -> showLanguageDialog = true
                }
            }
        )
        MenuGroup(
            items = listOf(if (isLoggedIn) stringResource(R.string.profile_logout) else stringResource(R.string.profile_login_account)),
            rightTexts = listOf(null),
            isLogout = true,
            onLogoutClick = {
                if (isLoggedIn) onLogoutClick() else onRequestLogin()
            }
        )
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.profile_system_hint),
                color = TextMuted.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.profile_language_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppLanguage.entries.forEach { language ->
                        TextButton(
                            onClick = {
                                showLanguageDialog = false
                                onLanguageChange(language)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(language.labelRes),
                                color = if (language == currentLanguage) Primary else TextMain
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun UserCard(profile: UserProfileDetail?, isLoggedIn: Boolean) {
    val displayName = if (isLoggedIn) {
        profile?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.profile_header_title)
    } else {
        stringResource(R.string.profile_guest_mode)
    }
    val accountIdText = if (isLoggedIn) {
        profile?.id?.let { "Nexus ID: $it" } ?: stringResource(R.string.profile_account_id)
    } else {
        stringResource(R.string.profile_not_logged_in)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(PrimaryStart, PrimaryEnd)))
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(BackgroundSurface)
            )
        }
        Column {
            Text(displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(BackgroundSurfaceElevated)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(accountIdText, color = TextMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun WalletCard(
    wallet: WalletSummary?,
    onBillClick: () -> Unit,
    onHowToEarnClick: () -> Unit
) {
    val balanceText = wallet?.balance?.takeIf { it.isNotBlank() }?.let { "¥ $it" }
        ?: stringResource(R.string.profile_wallet_value)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(PrimaryStart, PrimaryEnd)))
            .padding(24.dp)
            .height(140.dp)
    ) {
        Column {
            Text(
                stringResource(R.string.profile_wallet_title),
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(balanceText, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { onBillClick() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.profile_bill),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable { onHowToEarnClick() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.profile_how_to_earn),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ReferralCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(PrimaryStart.copy(alpha = 0.15f), PrimaryEnd.copy(alpha = 0.15f))))
            .border(1.dp, Primary, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🎁", style = MaterialTheme.typography.displaySmall)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.profile_referral_title),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
                Text(
                    text = stringResource(R.string.profile_referral_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Primary)
                    .clickable { onClick() }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.profile_referral_button),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
            }
        }
    }
}

@Composable
private fun MenuGroup(
    items: List<String>,
    rightTexts: List<String?>,
    isLogout: Boolean = false,
    onItemClick: ((Int) -> Unit)? = null,
    onLogoutClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(BackgroundSurface)
            .border(1.dp, BorderLight, RoundedCornerShape(20.dp))
    ) {
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isLogout && onLogoutClick != null) onLogoutClick() else onItemClick?.invoke(index)
                    }
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isLogout) AccentRed else TextMain
                )
                if (rightTexts.getOrNull(index) != null) {
                    Text(rightTexts[index].orEmpty(), color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                } else if (!isLogout) {
                    Text(">", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (index != items.lastIndex) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(BorderLight)
                )
            }
        }
    }
}
