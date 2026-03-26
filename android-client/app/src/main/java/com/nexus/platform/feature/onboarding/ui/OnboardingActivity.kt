package com.nexus.platform.feature.onboarding.ui

import android.content.Intent
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.platform.R
import com.nexus.platform.core.i18n.AppLanguageManager
import com.nexus.platform.feature.auth.ui.LoginActivity
import com.nexus.platform.ui.components.ActionButton
import com.nexus.platform.ui.theme.BackgroundSurface
import com.nexus.platform.ui.theme.NexusPlatformTheme
import com.nexus.platform.ui.theme.Primary
import com.nexus.platform.ui.theme.PrimaryStart
import com.nexus.platform.ui.theme.TextMuted

class OnboardingActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NexusPlatformTheme {
                OnboardingScreen {
                    startActivity(Intent(this, LoginActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                }
            }
        }
    }
}

@Composable
private fun OnboardingScreen(onStartClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(32.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                PrimaryStart.copy(alpha = 0.5f),
                                BackgroundSurface
                            )
                        )
                    )
            )
            Spacer(modifier = Modifier.height(28.dp))
            Text(stringResource(R.string.onboarding_title), fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                stringResource(R.string.onboarding_subtitle),
                color = TextMuted
            )
        }
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Dot(active = true)
                Spacer(Modifier.width(8.dp))
                Dot(active = false)
                Spacer(Modifier.width(8.dp))
                Dot(active = false)
            }
            Spacer(modifier = Modifier.height(20.dp))
            ActionButton(text = stringResource(R.string.onboarding_start), onClick = onStartClick, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Dot(active: Boolean) {
    Box(
        modifier = Modifier
            .then(if (active) Modifier.width(24.dp) else Modifier.width(8.dp))
            .height(8.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
            .background(if (active) Primary else TextMuted.copy(alpha = 0.35f))
    )
}
