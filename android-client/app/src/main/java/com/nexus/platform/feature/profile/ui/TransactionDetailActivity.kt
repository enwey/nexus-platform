package com.nexus.platform.feature.profile.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.platform.R
import com.nexus.platform.core.i18n.AppLanguageManager
import com.nexus.platform.data.remote.PlatformBackendApi
import com.nexus.platform.domain.model.BillingDetail
import com.nexus.platform.ui.theme.BackgroundBase
import com.nexus.platform.ui.theme.NexusPlatformTheme
import com.nexus.platform.ui.theme.TextMuted

class TransactionDetailActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val billingId = intent.getLongExtra("billing_id", 0L)
        setContent {
            NexusPlatformTheme {
                TransactionDetailScreen(
                    billingId = billingId,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@Composable
private fun TransactionDetailScreen(
    billingId: Long,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val backendApi = remember(context) { PlatformBackendApi(context) }
    var detail by remember { mutableStateOf<BillingDetail?>(null) }

    LaunchedEffect(billingId) {
        if (billingId > 0) {
            detail = backendApi.getBillingDetail(billingId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBase)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "<",
                style = MaterialTheme.typography.headlineMedium,
                color = TextMuted,
                modifier = Modifier.clickable { onBackClick() }
            )
            Text(
                text = stringResource(R.string.transaction_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 0.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        if (detail == null) {
            Text(stringResource(R.string.transaction_empty), color = TextMuted)
        } else {
            Text(detail!!.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(detail!!.subtitle, color = TextMuted)
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.transaction_amount_format, detail!!.amount))
            Text(stringResource(R.string.transaction_type_format, detail!!.type))
            Text(stringResource(R.string.transaction_time_format, detail!!.createdAt))
            Text(stringResource(R.string.transaction_receipt_format, detail!!.receiptUrl), color = TextMuted)
        }
    }
}
