package com.nexus.platform.feature.onboarding.ui

import android.content.Intent
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.platform.R
import com.nexus.platform.core.i18n.AppLanguageManager
import com.nexus.platform.feature.main.ui.MainActivity
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
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                }
            }
        }
    }
}

@Composable
private fun OnboardingScreen(onStartClick: () -> Unit) {
    var currentPage by remember { mutableIntStateOf(0) }
    val totalPages = 3

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Spacer(modifier = Modifier.height(20.dp))
            OnboardingPage(page = currentPage)
        }
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(totalPages) { index ->
                    Dot(active = index == currentPage)
                    if (index < totalPages - 1) {
                        Spacer(Modifier.width(8.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            if (currentPage == totalPages - 1) {
                ActionButton(
                    text = stringResource(R.string.onboarding_start),
                    onClick = onStartClick,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                ActionButton(
                    text = stringResource(R.string.onboarding_next),
                    onClick = { currentPage++ },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun OnboardingPage(page: Int) {
    when (page) {
        0 -> OnboardingPage1()
        1 -> OnboardingPage2()
        2 -> OnboardingPage3()
    }
}

@Composable
private fun OnboardingPage1() {
    Column {
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
        Text(
            text = stringResource(R.string.onboarding_title_1),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.onboarding_subtitle_1),
            color = TextMuted
        )
    }
}

@Composable
private fun OnboardingPage2() {
    Column {
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
                ),
            contentAlignment = Alignment.TopStart
        ) {
            Box(
                modifier = Modifier
                    .padding(20.dp)
                    .background(
                        androidx.compose.ui.graphics.Color(0xCC00E599),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "60 FPS ULTRA",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = androidx.compose.ui.graphics.Color.Black
                )
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.onboarding_title_2),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.onboarding_subtitle_2),
            color = TextMuted
        )
    }
}

@Composable
private fun OnboardingPage3() {
    Column {
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
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        PrimaryStart.copy(alpha = 0.4f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp)
                    )
                    .then(
                        Modifier.border(
                            width = 2.dp,
                            color = PrimaryStart,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "☁️",
                    style = MaterialTheme.typography.displayLarge
                )
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.onboarding_title_3),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.onboarding_subtitle_3),
            color = TextMuted
        )
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
