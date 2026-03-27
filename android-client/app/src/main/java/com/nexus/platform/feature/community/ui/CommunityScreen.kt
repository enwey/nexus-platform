package com.nexus.platform.feature.community.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nexus.platform.R
import com.nexus.platform.ui.theme.BackgroundBase

private val TopLevelBottomPadding = 96.dp

@Composable
fun CommunityScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBase)
            .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = TopLevelBottomPadding),
        contentAlignment = Alignment.Center
    ) {
        Text(stringResource(R.string.community_coming_soon), style = MaterialTheme.typography.bodyLarge)
    }
}
