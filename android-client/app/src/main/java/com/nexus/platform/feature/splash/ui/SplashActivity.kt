package com.nexus.platform.feature.splash.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.platform.feature.onboarding.ui.OnboardingActivity
import com.nexus.platform.ui.theme.NexusPlatformTheme
import com.nexus.platform.ui.theme.PrimaryEnd
import com.nexus.platform.ui.theme.PrimaryStart
import com.nexus.platform.ui.theme.TextMuted

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NexusPlatformTheme {
                SplashScreen()
            }
        }
        window.decorView.postDelayed({
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
        }, 1200)
    }
}

@Composable
private fun SplashScreen() {
    val bg = Brush.radialGradient(
        colors = listOf(
            PrimaryStart.copy(alpha = 0.25f),
            androidx.compose.ui.graphics.Color(0xFF090A0F)
        ),
        radius = 900f
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .background(
                        brush = Brush.linearGradient(listOf(PrimaryStart, PrimaryEnd)),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("N", fontWeight = FontWeight.Black)
            }
            androidx.compose.foundation.layout.Spacer(Modifier.size(24.dp))
            Text("NEXUS", fontWeight = FontWeight.Black)
            Text("NEXT-GEN RUNTIME", color = TextMuted)
        }
    }
}
