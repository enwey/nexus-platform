package com.nexus.platform.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun ProfileScreen(onLogoutClick: () -> Unit) {
    Column {
        Text("Profile screen")
        PrimaryButton(text = "Logout", onClick = onLogoutClick)
    }
}
