package com.nexus.platform.feature.profile.ui

import android.content.Intent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.platform.R
import com.nexus.platform.core.i18n.AppLanguage
import com.nexus.platform.core.ui.showCenterToast
import com.nexus.platform.data.local.CloudSyncStore
import com.nexus.platform.data.local.GameCatalogCacheStore
import com.nexus.platform.data.local.GameEngagementStore
import com.nexus.platform.data.local.LocalCacheManager
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
import com.nexus.platform.ui.components.SkeletonBlock
import com.nexus.platform.ui.components.SkeletonMotionTokens
import kotlinx.coroutines.launch

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
    val cloudSyncStore = remember(context) { CloudSyncStore(context) }
    val engagementStore = remember(context) { GameEngagementStore(context) }
    val catalogCacheStore = remember(context) { GameCatalogCacheStore(context) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    var profile by remember { mutableStateOf<UserProfileDetail?>(null) }
    var wallet by remember { mutableStateOf<WalletSummary?>(null) }
    var cloudSyncEnabled by remember { mutableStateOf(false) }
    var cacheSizeText by remember { mutableStateOf("0 B") }

    LaunchedEffect(isLoggedIn) {
        cacheSizeText = LocalCacheManager.formatBytes(LocalCacheManager.computeCacheBytes(context))
        if (isLoggedIn) {
            profile = runCatching { backendApi.getUserProfile() }.getOrNull()
            wallet = runCatching { backendApi.getWalletSummary() }.getOrNull()
            cloudSyncEnabled = cloudSyncStore.isEnabled()
            if (cloudSyncEnabled) {
                runCatching { backendApi.getLibraryHome() }.getOrNull()?.let { home ->
                    val mergedGames = (home.recentGames + home.myGames + listOfNotNull(home.currentPlayingGame))
                        .distinctBy { it.id }
                    catalogCacheStore.saveGames(mergedGames)
                    engagementStore.applyCloudState(
                        currentPlayingGameId = home.currentPlayingGame?.id,
                        recentGameIds = home.recentGames.map { it.id },
                        favoriteGameIds = home.myGames.map { it.id }
                    )
                }
            }
        } else {
            profile = null
            wallet = null
            cloudSyncEnabled = false
        }
    }

    fun requireLogin(action: () -> Unit) {
        if (isLoggedIn) {
            action()
        } else {
            showCenterToast(context, context.getString(R.string.profile_login_required))
            onRequestLogin()
        }
    }

    val showSkeleton = isLoggedIn && profile == null && wallet == null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBase)
            .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = TopLevelBottomPadding),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Crossfade(
            targetState = showSkeleton,
            animationSpec = tween(SkeletonMotionTokens.OverlayEnterMillis),
            label = "profileContent"
        ) { showingSkeleton ->
            if (showingSkeleton) {
                ProfileSkeleton()
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    UserCard(
                        profile = profile,
                        isLoggedIn = isLoggedIn,
                        onLoginClick = onRequestLogin
                    )
                    WalletCard(
                        wallet = wallet,
                        onBillClick = { requireLogin { context.startActivity(Intent(context, BillingActivity::class.java)) } },
                        onHowToEarnClick = { context.startActivity(Intent(context, ReferralActivity::class.java)) }
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
                            if (isLoggedIn && cloudSyncEnabled) stringResource(R.string.profile_sync_enabled) else stringResource(R.string.profile_sync_disabled),
                            cacheSizeText,
                            stringResource(currentLanguage.labelRes)
                        ),
                        onItemClick = { index ->
                            when (index) {
                                0 -> requireLogin { context.startActivity(Intent(context, AccountSecurityActivity::class.java)) }
                                1 -> requireLogin {
                                    val targetEnabled = !cloudSyncEnabled
                                    cloudSyncStore.setEnabled(targetEnabled)
                                    cloudSyncEnabled = targetEnabled
                                    if (targetEnabled) {
                                        scope.launch {
                                            val synced = runCatching { backendApi.getLibraryHome() }.getOrNull()
                                            if (synced != null) {
                                                val mergedGames = (synced.recentGames + synced.myGames + listOfNotNull(synced.currentPlayingGame))
                                                    .distinctBy { it.id }
                                                catalogCacheStore.saveGames(mergedGames)
                                                engagementStore.applyCloudState(
                                                    currentPlayingGameId = synced.currentPlayingGame?.id,
                                                    recentGameIds = synced.recentGames.map { it.id },
                                                    favoriteGameIds = synced.myGames.map { it.id }
                                                )
                                                showCenterToast(context, context.getString(R.string.profile_sync_enabled))
                                            } else {
                                                showCenterToast(context, context.getString(R.string.common_error_network))
                                            }
                                        }
                                    } else {
                                        showCenterToast(context, context.getString(R.string.profile_sync_disabled))
                                    }
                                }
                                2 -> {
                                    scope.launch {
                                        val cleared = LocalCacheManager.clearLocalCaches(context)
                                        cacheSizeText = LocalCacheManager.formatBytes(LocalCacheManager.computeCacheBytes(context))
                                        cloudSyncEnabled = if (isLoggedIn) cloudSyncStore.isEnabled() else false
                                        val msg = "${context.getString(R.string.profile_cache_cleared)} (${LocalCacheManager.formatBytes(cleared)})"
                                        showCenterToast(context, msg)
                                    }
                                }
                                3 -> showLanguageDialog = true
                            }
                        }
                    )
                }
            }
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
                                color = if (language == currentLanguage) Primary else TextMain,
                                style = MaterialTheme.typography.bodyLarge
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
private fun ProfileSkeleton() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SkeletonBlock(width = 80.dp, height = 80.dp, cornerRadius = 40.dp)
        Column(modifier = Modifier.weight(1f)) {
            SkeletonBlock(width = 136.dp, height = 26.dp, cornerRadius = 10.dp)
            Spacer(modifier = Modifier.height(10.dp))
            SkeletonBlock(width = 88.dp, height = 18.dp, cornerRadius = 9.dp)
        }
        SkeletonBlock(width = 82.dp, height = 34.dp, cornerRadius = 17.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(164.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF24253A), BackgroundSurface)))
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                SkeletonBlock(width = 68.dp, height = 12.dp, cornerRadius = 6.dp)
                Spacer(modifier = Modifier.height(10.dp))
                SkeletonBlock(width = 142.dp, height = 34.dp, cornerRadius = 10.dp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SkeletonBlock(modifier = Modifier.weight(1f), height = 38.dp, cornerRadius = 12.dp)
                SkeletonBlock(modifier = Modifier.weight(1f), height = 38.dp, cornerRadius = 12.dp)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(PrimaryStart.copy(alpha = 0.12f), PrimaryEnd.copy(alpha = 0.12f))))
            .border(1.dp, Primary, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            SkeletonBlock(width = 42.dp, height = 42.dp, cornerRadius = 21.dp)
            Column(modifier = Modifier.weight(1f)) {
                SkeletonBlock(width = 108.dp, height = 14.dp, cornerRadius = 7.dp)
                Spacer(modifier = Modifier.height(10.dp))
                SkeletonBlock(width = 186.dp, height = 12.dp, cornerRadius = 6.dp)
            }
            SkeletonBlock(width = 64.dp, height = 28.dp, cornerRadius = 14.dp)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(BackgroundSurface)
            .border(1.dp, BorderLight, RoundedCornerShape(20.dp))
    ) {
        repeat(4) { index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBlock(width = 96.dp, height = 14.dp, cornerRadius = 7.dp)
                SkeletonBlock(width = if (index == 0) 14.dp else 62.dp, height = 12.dp, cornerRadius = 6.dp)
            }
            if (index != 3) {
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

@Composable
private fun UserCard(
    profile: UserProfileDetail?,
    isLoggedIn: Boolean,
    onLoginClick: () -> Unit
) {
    val displayName = if (isLoggedIn) {
        profile?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.profile_header_title)
    } else {
        stringResource(R.string.profile_guest_mode)
    }
    val accountIdText = if (isLoggedIn) {
        profile?.id?.let { stringResource(R.string.profile_account_id_format, it) } ?: stringResource(R.string.profile_account_id)
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
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_default_avatar),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(BackgroundSurfaceElevated)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(accountIdText, color = TextMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
        if (!isLoggedIn) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Primary)
                    .clickable { onLoginClick() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.profile_login_account),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
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
    val balanceText = wallet?.balance?.takeIf { it.isNotBlank() }?.trim()
        ?.removePrefix("¥")
        ?.removePrefix("￥")
        ?.trim()
        ?: stringResource(R.string.profile_wallet_value)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(PrimaryStart, PrimaryEnd)))
            .height(164.dp)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    stringResource(R.string.profile_wallet_title),
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall.merge(
                        TextStyle(
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    ),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    balanceText,
                    style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Monospace),
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            }
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.profile_referral_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(12.dp))
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
                    Image(
                        painter = painterResource(id = R.drawable.ic_more),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
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
