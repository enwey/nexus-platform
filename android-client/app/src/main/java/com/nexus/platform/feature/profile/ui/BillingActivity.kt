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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.platform.R
import com.nexus.platform.ui.theme.AccentGreen
import com.nexus.platform.ui.theme.BackgroundBase
import com.nexus.platform.ui.theme.BackgroundSurface
import com.nexus.platform.ui.theme.BorderLight
import com.nexus.platform.ui.theme.NexusPlatformTheme
import com.nexus.platform.ui.theme.Primary
import com.nexus.platform.ui.theme.TextMain
import com.nexus.platform.ui.theme.TextMuted

class BillingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NexusPlatformTheme {
                BillingScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@Composable
private fun BillingScreen(
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
                text = stringResource(R.string.billing_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = stringResource(R.string.billing_income_title),
                    value = stringResource(R.string.billing_income_value),
                    color = AccentGreen
                )
                StatCard(
                    title = stringResource(R.string.billing_expense_title),
                    value = stringResource(R.string.billing_expense_value),
                    color = TextMain
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.billing_recent_title),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BillingItem(
                    icon = "🤝",
                    title = stringResource(R.string.billing_item_invite_title),
                    subtitle = stringResource(R.string.billing_item_invite_subtitle),
                    amount = stringResource(R.string.billing_item_invite_amount),
                    time = stringResource(R.string.billing_item_invite_time),
                    isPositive = true
                )
                BillingItem(
                    icon = "🎮",
                    title = stringResource(R.string.billing_item_purchase_title),
                    subtitle = stringResource(R.string.billing_item_purchase_subtitle),
                    amount = stringResource(R.string.billing_item_purchase_amount),
                    time = stringResource(R.string.billing_item_purchase_time),
                    isPositive = false
                )
                BillingItem(
                    icon = "🕹️",
                    title = stringResource(R.string.billing_item_play_title),
                    subtitle = stringResource(R.string.billing_item_play_subtitle),
                    amount = stringResource(R.string.billing_item_play_amount),
                    time = stringResource(R.string.billing_item_play_time),
                    isPositive = true
                )
                BillingItem(
                    icon = "💎",
                    title = stringResource(R.string.billing_item_item_title),
                    subtitle = stringResource(R.string.billing_item_item_subtitle),
                    amount = stringResource(R.string.billing_item_item_amount),
                    time = stringResource(R.string.billing_item_item_time),
                    isPositive = false
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.billing_recent_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF444444)
                )
            }
        }
    }
}

@Composable
private fun RowScope.StatCard(
    title: String,
    value: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(BackgroundSurface)
            .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun BillingItem(
    icon: String,
    title: String,
    subtitle: String,
    amount: String,
    time: String,
    isPositive: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isPositive) {
                        AccentGreen.copy(alpha = 0.1f)
                    } else {
                        Color(0xFFFF4D4F).copy(alpha = 0.1f)
                    }
                )
                .border(
                    1.dp,
                    if (isPositive) {
                        AccentGreen.copy(alpha = 0.2f)
                    } else {
                        Color(0xFFFF4D4F).copy(alpha = 0.2f)
                    },
                    RoundedCornerShape(12.dp)
                ),
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
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = amount,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isPositive) AccentGreen else TextMain
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = time,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }
}
