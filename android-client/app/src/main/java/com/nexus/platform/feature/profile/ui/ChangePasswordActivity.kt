package com.nexus.platform.feature.profile.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.nexus.platform.R
import com.nexus.platform.ui.theme.BackgroundBase
import com.nexus.platform.ui.theme.BackgroundSurface
import com.nexus.platform.ui.theme.BackgroundSurfaceElevated
import com.nexus.platform.ui.theme.BorderLight
import com.nexus.platform.ui.theme.NexusPlatformTheme
import com.nexus.platform.ui.theme.Primary
import com.nexus.platform.ui.theme.PrimaryEnd
import com.nexus.platform.ui.theme.PrimaryStart
import com.nexus.platform.ui.theme.TextMain
import com.nexus.platform.ui.theme.TextMuted

class ChangePasswordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NexusPlatformTheme {
                ChangePasswordScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@Composable
private fun ChangePasswordScreen(
    onBackClick: () -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBase)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "‹",
                style = MaterialTheme.typography.headlineMedium,
                color = TextMuted,
                modifier = Modifier.clickable { onBackClick() }
            )
            Text(
                text = stringResource(R.string.change_password_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(BackgroundSurface)
                    .border(1.dp, BorderLight, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    InputField(
                        label = stringResource(R.string.change_password_current),
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        placeholder = stringResource(R.string.change_password_current_hint)
                    )
                    InputField(
                        label = stringResource(R.string.change_password_new),
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        placeholder = stringResource(R.string.change_password_new_hint),
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onPasswordVisibilityChange = { passwordVisible = !passwordVisible }
                    )
                    InputField(
                        label = stringResource(R.string.change_password_confirm),
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        placeholder = stringResource(R.string.change_password_confirm_hint),
                        isPassword = true,
                        showDivider = false
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.change_password_footer),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BackgroundBase)
                        .align(Alignment.TopStart)
                )
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BackgroundBase)
                        .align(Alignment.TopEnd)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                PrimaryStart,
                                PrimaryEnd
                            )
                        )
                    )
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.change_password_submit),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun InputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordVisibilityChange: () -> Unit = {},
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            if (isPassword) {
                Text(
                    text = if (passwordVisible) stringResource(R.string.change_password_hide) else stringResource(R.string.change_password_show),
                    style = MaterialTheme.typography.bodySmall,
                    color = Primary,
                    modifier = Modifier.clickable { onPasswordVisibilityChange() }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            },
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Primary
            ),
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium
        )
        if (showDivider) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        BorderLight.copy(alpha = 0.08f),
                        RoundedCornerShape(0.dp)
                    )
            )
        }
    }
}
