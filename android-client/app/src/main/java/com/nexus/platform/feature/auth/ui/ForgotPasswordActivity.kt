package com.nexus.platform.feature.auth.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.platform.R
import com.nexus.platform.core.i18n.ApiErrorLocalizer
import com.nexus.platform.core.ui.showCenterToast
import com.nexus.platform.data.remote.PlatformBackendApi
import com.nexus.platform.ui.components.ActionButton
import com.nexus.platform.ui.theme.BackgroundBase
import com.nexus.platform.ui.theme.Primary
import com.nexus.platform.ui.theme.TextMuted
import kotlinx.coroutines.launch

@Composable
fun ForgotPasswordScreen(
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    val context = LocalContext.current
    val backendApi = remember(context) { PlatformBackendApi(context) }
    val scope = rememberCoroutineScope()
    val accountLabel = stringResource(R.string.forgot_account_label)
    val forgotCodeSentText = stringResource(R.string.forgot_code_sent)
    val changePasswordIncompleteText = stringResource(R.string.change_password_error_incomplete)
    val changePasswordSuccessText = stringResource(R.string.change_password_success)
    var email by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var codeCountdown by remember { mutableIntStateOf(0) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var feedbackIsError by remember { mutableStateOf(false) }
    val inputTextStyle = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = true)
    )
    val textFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        focusedIndicatorColor = Primary,
        unfocusedIndicatorColor = Color(0x33FFFFFF),
        cursorColor = Primary
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBase)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.game_back),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.forgot_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.forgot_subtitle), color = TextMuted)
            Spacer(modifier = Modifier.height(40.dp))

            OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(accountLabel, style = MaterialTheme.typography.bodyLarge) },
            textStyle = inputTextStyle,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = textFieldColors
        )
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = verificationCode,
                onValueChange = { verificationCode = it },
                label = { Text(stringResource(R.string.forgot_code_label), style = MaterialTheme.typography.bodyLarge) },
                textStyle = inputTextStyle,
                singleLine = true,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = textFieldColors
            )
            ActionButton(
                text = if (codeCountdown > 0) "${codeCountdown}s" else stringResource(R.string.forgot_get_code),
                onClick = {
                    if (email.isBlank()) {
                        showCenterToast(context, accountLabel)
                        return@ActionButton
                    }
                    if (!email.contains("@")) {
                        showCenterToast(context, context.getString(R.string.auth_error_invalid_email))
                        return@ActionButton
                    }
                    if (codeCountdown <= 0) {
                        scope.launch {
                            val result = backendApi.sendVerificationCodeResult(email.trim(), "RESET_PASSWORD", "AUTH_FORGOT_PASSWORD")
                            if (result.success) {
                                codeCountdown = 60
                                while (codeCountdown > 0) {
                                    kotlinx.coroutines.delay(1000)
                                    codeCountdown -= 1
                                }
                                feedbackMessage = forgotCodeSentText
                                feedbackIsError = false
                                showCenterToast(context, feedbackMessage.orEmpty())
                            } else {
                                feedbackMessage = ApiErrorLocalizer.localize(
                                    context = context,
                                    rawMessage = result.message,
                                    fallbackRes = R.string.forgot_code_failed
                                )
                                feedbackIsError = true
                                showCenterToast(context, feedbackMessage.orEmpty())
                            }
                        }
                    }
                },
                primary = false,
                modifier = Modifier.width(110.dp).height(56.dp)
            )
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text(stringResource(R.string.forgot_new_password_label), style = MaterialTheme.typography.bodyLarge) },
            textStyle = inputTextStyle,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = textFieldColors
        )
            Spacer(modifier = Modifier.height(32.dp))

            ActionButton(
            text = stringResource(R.string.forgot_action),
            onClick = {
                if (email.isBlank() || verificationCode.isBlank() || newPassword.length < 8) {
                    feedbackMessage = changePasswordIncompleteText
                    feedbackIsError = true
                    showCenterToast(context, feedbackMessage.orEmpty())
                    return@ActionButton
                }
                if (!email.contains("@")) {
                    showCenterToast(context, context.getString(R.string.auth_error_invalid_email))
                    return@ActionButton
                }
                scope.launch {
                    val result = backendApi.resetPasswordResult(
                        email = email.trim(),
                        code = verificationCode.trim(),
                        newPassword = newPassword
                    )
                    feedbackMessage = if (result.success) {
                        changePasswordSuccessText
                    } else {
                        ApiErrorLocalizer.localize(
                            context = context,
                            rawMessage = result.message,
                            fallbackRes = R.string.change_password_failed
                        )
                    }
                    feedbackIsError = !result.success
                    showCenterToast(context, feedbackMessage.orEmpty(), android.widget.Toast.LENGTH_LONG)
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
            )
            if (!feedbackMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = feedbackMessage.orEmpty(),
                    color = if (feedbackIsError) MaterialTheme.colorScheme.error else Primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(text = stringResource(R.string.forgot_existing_user), color = TextMuted)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.forgot_login),
                    color = Primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onLoginClick)
                )
            }
        }
    }
}
