package com.nexus.platform.feature.splash.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nexus.platform.R
import com.nexus.platform.core.i18n.AppLanguageManager
import com.nexus.platform.feature.main.ui.MainActivity
import com.nexus.platform.feature.onboarding.data.OnboardingStore
import com.nexus.platform.feature.onboarding.ui.OnboardingActivity
import com.nexus.platform.feature.splash.data.LaunchAdRepository
import com.nexus.platform.feature.splash.data.LaunchAdUiModel
import com.nexus.platform.ui.theme.BackgroundBase
import com.nexus.platform.ui.theme.NexusPlatformTheme
import com.nexus.platform.ui.theme.Primary
import com.nexus.platform.ui.theme.White
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLanguageManager.ensureInitialized(this)
        val launchAdRepository = LaunchAdRepository(this)
        val initialAd = launchAdRepository.loadForDisplay()
        lifecycleScope.launch(Dispatchers.IO) {
            launchAdRepository.refreshIfNeeded()
        }
        setContent {
            NexusPlatformTheme {
                LaunchAdScreen(
                    ad = initialAd,
                    onContinue = ::openNextScreen
                )
            }
        }
    }

    private fun openNextScreen() {
        val target = if (OnboardingStore.isCompleted(this)) {
            MainActivity::class.java
        } else {
            OnboardingActivity::class.java
        }
        startActivity(Intent(this, target))
        overridePendingTransition(0, 0)
        finish()
    }
}

@Composable
private fun LaunchAdScreen(
    ad: LaunchAdUiModel,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        onContinue()
    }
    var remainingSeconds by remember(ad.displaySeconds) { mutableIntStateOf(ad.displaySeconds.coerceAtLeast(1)) }
    var isNavigating by remember { mutableStateOf(false) }
    var isLandingOpen by remember { mutableStateOf(false) }

    LaunchedEffect(remainingSeconds, isNavigating, isLandingOpen) {
        if (isNavigating || isLandingOpen) return@LaunchedEffect
        if (remainingSeconds <= 0) {
            isNavigating = true
            onContinue()
            return@LaunchedEffect
        }
        delay(1_000)
        if (!isNavigating && !isLandingOpen) {
            remainingSeconds -= 1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBase)
    ) {
        AsyncImage(
            model = ad.imageModel,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.18f),
                            Color.Black.copy(alpha = 0.28f),
                            BackgroundBase.copy(alpha = 0.95f),
                            BackgroundBase
                        )
                    )
                )
        )

        Text(
            text = stringResource(R.string.launch_ad_skip_countdown, remainingSeconds),
            color = Color(0xFFC7CBD8),
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 14.dp, end = 20.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.Black.copy(alpha = 0.28f))
                .clickable(enabled = !isNavigating) {
                    if (!isNavigating) {
                        isNavigating = true
                        onContinue()
                    }
                }
                .padding(horizontal = 14.dp, vertical = 9.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 40.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.Black.copy(alpha = 0.34f))
                .clickable(enabled = !isNavigating) {
                    if (!isNavigating) {
                        isLandingOpen = true
                        launcher.launch(
                            LaunchAdWebViewActivity.intent(
                                context = context,
                                title = ad.landingTitle,
                                url = ad.targetUrl
                            )
                        )
                    }
                }
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = ad.badge,
                    color = White,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Primary)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = ad.sponsor,
                    color = White.copy(alpha = 0.78f),
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF2D69FF), Color(0xFF7D37FF))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "B",
                        color = White,
                        style = TextStyle(
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black
                        )
                    )
                }

                Spacer(modifier = Modifier.size(16.dp))

                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = ad.title,
                        color = White,
                        style = TextStyle(
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = ad.description,
                        color = Color(0xFFC7CBD8),
                        style = TextStyle(
                            fontSize = 15.sp,
                            lineHeight = 21.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = ad.cta,
                    color = White,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF5C67FF), Color(0xFF7E59FF))
                            )
                        )
                        .padding(horizontal = 22.dp, vertical = 14.dp)
                )
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = ad.footer,
                    color = Color(0xFFA8ADBF),
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
