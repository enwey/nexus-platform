package com.nexus.platform.feature.profile.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.nexus.platform.data.remote.PlatformBackendApi
import com.nexus.platform.domain.model.ReferralRecord
import com.nexus.platform.domain.model.ReferralSummary
import com.nexus.platform.ui.theme.BackgroundBase
import com.nexus.platform.ui.theme.NexusPlatformTheme
import com.nexus.platform.ui.theme.Primary
import com.nexus.platform.ui.theme.TextMuted
import kotlinx.coroutines.launch

class ReferralActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NexusPlatformTheme {
                ReferralScreen(onBackClick = { finish() })
            }
        }
    }
}

@Composable
private fun ReferralScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val backendApi = remember(context) { PlatformBackendApi(context) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var summary by remember { mutableStateOf<ReferralSummary?>(null) }
    var records by remember { mutableStateOf<List<ReferralRecord>>(emptyList()) }

    LaunchedEffect(Unit) {
        summary = backendApi.getReferralSummary()
        records = backendApi.getReferralRecords()
    }

    fun copyLink(link: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("referral_link", link))
        Toast.makeText(context, context.getString(R.string.referral_link_copied), Toast.LENGTH_SHORT).show()
    }

    fun share(channel: String, text: String) {
        scope.launch { backendApi.markReferralShared(channel) }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.referral_share_title)))
    }

    val link = summary?.referralLink ?: stringResource(R.string.referral_link_value)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBase)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("<", style = MaterialTheme.typography.headlineMedium, color = TextMuted, modifier = Modifier.clickable { onBackClick() })
            Text(
                text = stringResource(R.string.referral_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.referral_summary_invites, summary?.inviteCount ?: 0),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = stringResource(R.string.referral_summary_reward, summary?.totalReward ?: "0"),
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(link, modifier = Modifier.weight(1f), color = Primary)
            Text(
                stringResource(R.string.referral_link_copy),
                color = Primary,
                modifier = Modifier.clickable { copyLink(link) }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.referral_share_more), color = Primary, modifier = Modifier.clickable { share("system", link) })
            Text(stringResource(R.string.referral_share_whatsapp), color = Primary, modifier = Modifier.clickable { share("whatsapp", link) })
            Text(stringResource(R.string.referral_share_facebook), color = Primary, modifier = Modifier.clickable { share("facebook", link) })
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (records.isEmpty()) {
            Text(stringResource(R.string.referral_no_records), color = TextMuted)
        } else {
            records.forEach { record ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(record.title, fontWeight = FontWeight.SemiBold)
                        Text(record.subtitle, color = TextMuted)
                        Text(record.createdAt, color = TextMuted)
                    }
                    Text(record.reward)
                }
            }
        }
    }
}
