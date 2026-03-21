package com.nexus.platform.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun GameDetailScreen(onBackClick: () -> Unit = {}, onPlayClick: () -> Unit = {}) {
    Column {
        Text("Game detail")
        PrimaryButton(text = "Play", onClick = onPlayClick)
        PrimaryButton(text = "Back", onClick = onBackClick)
    }
}
