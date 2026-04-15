package com.nexus.platform.feature.onboarding.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale.Companion.Crop
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nexus.platform.R
import com.nexus.platform.core.i18n.AppLanguageManager
import com.nexus.platform.feature.main.ui.MainActivity
import com.nexus.platform.feature.onboarding.data.OnboardingStore
import com.nexus.platform.ui.theme.AccentGreen
import com.nexus.platform.ui.theme.BackgroundBase
import com.nexus.platform.ui.theme.BackgroundSurfaceElevated
import com.nexus.platform.ui.theme.NexusPlatformTheme
import com.nexus.platform.ui.theme.Primary
import com.nexus.platform.ui.theme.TextMuted
import com.nexus.platform.ui.theme.White

class OnboardingActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NexusPlatformTheme {
                OnboardingScreen {
                    OnboardingStore.markCompleted(this)
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
    val pages = remember { onboardingPages() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBase)
    ) {
        val heroHeight = if (maxHeight > 760.dp) 520.dp else maxHeight * 0.58f
        val bottomPadding = if (maxHeight > 760.dp) 50.dp else 28.dp

        if (currentPage < pages.lastIndex) {
            Text(
                text = stringResource(R.string.onboarding_skip),
                color = TextMuted,
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 12.dp, end = 30.dp)
                    .clickable { onStartClick() }
            )
        }

        Crossfade(
            targetState = currentPage,
            animationSpec = tween(durationMillis = 280),
            modifier = Modifier.fillMaxSize(),
            label = "onboarding_page"
        ) { pageIndex ->
            val current = pages[pageIndex]
            Column(modifier = Modifier.fillMaxSize()) {
                HeroImage(
                    imageUrl = current.imageUrl,
                    height = heroHeight,
                    alignment = current.imageAlignment
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 32.dp, end = 32.dp, bottom = bottomPadding)
                        .offset(y = (-52).dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = buildAnnotatedString {
                            append(stringResource(current.titlePrefixRes))
                            append("\n")
                            withStyle(SpanStyle(color = current.highlightColor)) {
                                append(stringResource(current.titleHighlightRes))
                            }
                        },
                        color = White,
                        style = TextStyle(
                            fontSize = 40.sp,
                            lineHeight = 48.sp,
                            fontWeight = FontWeight.Black
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(current.descriptionRes),
                        color = TextMuted,
                        style = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 26.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row {
                        pages.forEachIndexed { index, page ->
                            Dot(active = index == pageIndex, activeColor = page.highlightColor)
                            if (index < pages.lastIndex) {
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    FlatButton(
                        text = stringResource(
                            if (pageIndex == pages.lastIndex) {
                                R.string.onboarding_start
                            } else {
                                R.string.onboarding_next
                            }
                        ),
                        backgroundColor = current.buttonColor,
                        contentColor = current.buttonTextColor,
                        onClick = {
                            if (pageIndex == pages.lastIndex) {
                                onStartClick()
                            } else {
                                currentPage += 1
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroImage(
    imageUrl: String,
    height: Dp,
    alignment: Alignment
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(BackgroundBase)
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = Crop,
            alignment = alignment,
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, BackgroundBase),
                            startY = size.height * 0.4f,
                            endY = size.height
                        )
                    )
                }
        )
    }
}

@Composable
private fun FlatButton(
    text: String,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .navigationBarsPadding()
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun Dot(active: Boolean, activeColor: Color) {
    Box(
        modifier = Modifier
            .width(if (active) 32.dp else 16.dp)
            .height(4.dp)
            .background(
                color = if (active) activeColor else BackgroundSurfaceElevated,
                shape = RoundedCornerShape(2.dp)
            )
    )
}

@Immutable
private data class OnboardingPageUi(
    val imageUrl: String,
    val imageAlignment: Alignment,
    val titlePrefixRes: Int,
    val titleHighlightRes: Int,
    val descriptionRes: Int,
    val highlightColor: Color,
    val buttonColor: Color,
    val buttonTextColor: Color
)

private fun onboardingPages(): List<OnboardingPageUi> = listOf(
    OnboardingPageUi(
        imageUrl = "https://images.unsplash.com/photo-1627856013091-fed6e4e30025?auto=format&fit=crop&w=800&q=80",
        imageAlignment = Alignment.Center,
        titlePrefixRes = R.string.onboarding_title_prefix_1,
        titleHighlightRes = R.string.onboarding_title_highlight_1,
        descriptionRes = R.string.onboarding_subtitle_1,
        highlightColor = Primary,
        buttonColor = Primary,
        buttonTextColor = White
    ),
    OnboardingPageUi(
        imageUrl = "https://images.unsplash.com/photo-1638803040283-7a5ffa48bf0d?auto=format&fit=crop&w=800&q=80",
        imageAlignment = Alignment.TopCenter,
        titlePrefixRes = R.string.onboarding_title_prefix_2,
        titleHighlightRes = R.string.onboarding_title_highlight_2,
        descriptionRes = R.string.onboarding_subtitle_2,
        highlightColor = AccentGreen,
        buttonColor = AccentGreen,
        buttonTextColor = Color.Black
    ),
    OnboardingPageUi(
        imageUrl = "https://images.unsplash.com/photo-1614729939124-032f0b56c9ce?auto=format&fit=crop&w=800&q=80",
        imageAlignment = Alignment.Center,
        titlePrefixRes = R.string.onboarding_title_prefix_3,
        titleHighlightRes = R.string.onboarding_title_highlight_3,
        descriptionRes = R.string.onboarding_subtitle_3,
        highlightColor = Primary,
        buttonColor = Primary,
        buttonTextColor = White
    )
)
