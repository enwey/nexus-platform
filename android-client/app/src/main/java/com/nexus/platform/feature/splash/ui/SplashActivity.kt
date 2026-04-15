package com.nexus.platform.feature.splash.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.platform.BuildConfig
import com.nexus.platform.R
import com.nexus.platform.core.i18n.AppLanguageManager
import com.nexus.platform.feature.main.ui.MainActivity
import com.nexus.platform.feature.onboarding.data.OnboardingStore
import com.nexus.platform.feature.onboarding.ui.OnboardingActivity
import com.nexus.platform.ui.theme.BackgroundBase
import com.nexus.platform.ui.theme.NexusPlatformTheme
import com.nexus.platform.ui.theme.Primary
import com.nexus.platform.ui.theme.TextMuted
import com.nexus.platform.ui.theme.White

class SplashActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLanguageManager.ensureInitialized(this)
        setContent {
            NexusPlatformTheme {
                SplashScreen()
            }
        }
        window.decorView.postDelayed({
            val target = if (OnboardingStore.isCompleted(this)) {
                MainActivity::class.java
            } else {
                OnboardingActivity::class.java
            }
            startActivity(Intent(this, target))
            overridePendingTransition(0, 0)
            finish()
        }, 1200)
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBase)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        color = Primary,
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "N",
                    color = White,
                    style = TextStyle(
                        fontSize = 50.sp,
                        fontWeight = FontWeight.Black
                    )
                )
            }
            Spacer(modifier = Modifier.size(30.dp))
            Text(
                text = "NEXUS",
                color = White,
                style = TextStyle(
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 6.sp
                )
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.splash_runtime),
                color = TextMuted,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 4.sp
                )
            )
        }

        Text(
            text = stringResource(R.string.splash_version, BuildConfig.VERSION_NAME),
            color = Color(0xFF555555),
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .offset(y = (-18).dp)
        )
    }
}
