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
import androidx.compose.foundation.layout.RowScope
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

class ReferralActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NexusPlatformTheme {
                ReferralScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@Composable
private fun ReferralScreen(
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBase)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            PrimaryStart,
                            PrimaryEnd
                        )
                    )
                )
                .padding(horizontal = 24.dp, vertical = 40.dp)
        ) {
            Text(
                text = "‹",
                style = MaterialTheme.typography.headlineMedium,
                color = TextMain,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .clickable { onBackClick() }
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎁",
                    style = MaterialTheme.typography.displayLarge
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.referral_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.referral_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMain.copy(alpha = 0.8f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(BackgroundSurface)
                    .border(1.dp, BorderLight, RoundedCornerShape(20.dp))
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    title = stringResource(R.string.referral_stat_invite_title),
                    value = stringResource(R.string.referral_stat_invite_value),
                    isLast = false
                )
                StatCard(
                    title = stringResource(R.string.referral_stat_reward_title),
                    value = stringResource(R.string.referral_stat_reward_value),
                    isLast = true
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = stringResource(R.string.referral_link_title),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BackgroundSurfaceElevated)
                    .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.referral_link_value),
                    style = MaterialTheme.typography.bodySmall,
                    color = Primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Primary)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.referral_link_copy),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = TextMain
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = stringResource(R.string.referral_share_title),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShareButton(
                    icon = "💬",
                    label = stringResource(R.string.referral_share_whatsapp),
                    color = Color(0xFF25D366)
                )
                ShareButton(
                    icon = "f",
                    label = stringResource(R.string.referral_share_facebook),
                    color = Color(0xFF1877F2)
                )
                ShareButton(
                    icon = "📕",
                    label = stringResource(R.string.referral_share_xiaohongshu),
                    color = Color(0xFFFF2442)
                )
                ShareButton(
                    icon = "🔗",
                    label = stringResource(R.string.referral_share_more),
                    color = BackgroundSurfaceElevated,
                    border = true
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(BackgroundSurface)
                    .border(1.dp, BorderLight, RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📜",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = stringResource(R.string.referral_rules_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        RuleItem(
                            number = 1,
                            title = stringResource(R.string.referral_rule_1_title),
                            description = stringResource(R.string.referral_rule_1_desc)
                        )
                        RuleItem(
                            number = 2,
                            title = stringResource(R.string.referral_rule_2_title),
                            description = stringResource(R.string.referral_rule_2_desc)
                        )
                        RuleItem(
                            number = 3,
                            title = stringResource(R.string.referral_rule_3_title),
                            description = stringResource(R.string.referral_rule_3_desc)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.StatCard(
    title: String,
    value: String,
    isLast: Boolean
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .then(
                if (!isLast) {
                    Modifier.background(
                        Color.Transparent,
                        RoundedCornerShape(0.dp)
                    )
                } else {
                    Modifier
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
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
private fun RuleItem(
    number: Int,
    title: String,
    description: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(BackgroundSurfaceElevated)
                .border(1.dp, Primary, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$number",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            lineHeight = 20.sp
        )
    }
}
