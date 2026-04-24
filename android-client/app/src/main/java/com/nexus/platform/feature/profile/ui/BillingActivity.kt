package com.nexus.platform.feature.profile.ui

import android.content.Intent
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.platform.R
import com.nexus.platform.core.i18n.AppLanguageManager
import com.nexus.platform.data.remote.PlatformBackendApi
import com.nexus.platform.domain.model.BillingRecord
import com.nexus.platform.ui.theme.BackgroundBase
import com.nexus.platform.ui.theme.NexusPlatformTheme
import com.nexus.platform.ui.theme.TextMuted
import com.nexus.platform.ui.components.SkeletonBlock
import com.nexus.platform.ui.components.SkeletonMotionTokens

class BillingActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NexusPlatformTheme {
                BillingScreen(
                    onBackClick = { finish() },
                    onOpenDetail = { id ->
                        startActivity(Intent(this, TransactionDetailActivity::class.java).putExtra("billing_id", id))
                    }
                )
            }
        }
    }
}

@Composable
private fun BillingScreen(
    onBackClick: () -> Unit,
    onOpenDetail: (Long) -> Unit
) {
    val context = LocalContext.current
    val backendApi = remember(context) { PlatformBackendApi(context) }
    var records by remember { mutableStateOf<List<BillingRecord>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            records = backendApi.getBillingRecords()
        } finally {
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBase)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                text = "<",
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
        Spacer(modifier = Modifier.height(18.dp))
        Crossfade(
            targetState = loading,
            animationSpec = tween(SkeletonMotionTokens.OverlayEnterMillis),
            label = "billingContent"
        ) { isLoading ->
            if (isLoading) {
                BillingSkeleton()
            } else if (records.isEmpty()) {
                Text(stringResource(R.string.billing_empty), color = TextMuted)
            } else {
                records.forEach { record ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenDetail(record.id) }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(record.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(record.subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            Text(record.createdAt, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                        Text(record.amount, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun BillingSkeleton() {
    repeat(5) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SkeletonBlock(width = 136.dp, height = 16.dp, cornerRadius = 8.dp)
                Spacer(modifier = Modifier.height(6.dp))
                SkeletonBlock(width = 188.dp, height = 12.dp, cornerRadius = 6.dp)
                Spacer(modifier = Modifier.height(6.dp))
                SkeletonBlock(width = 96.dp, height = 12.dp, cornerRadius = 6.dp)
            }
            SkeletonBlock(width = 52.dp, height = 16.dp, cornerRadius = 8.dp)
        }
    }
}
