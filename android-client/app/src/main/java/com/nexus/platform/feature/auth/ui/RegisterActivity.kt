package com.nexus.platform.feature.auth.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.platform.R
import com.nexus.platform.core.di.appContainer
import com.nexus.platform.core.i18n.ApiErrorLocalizer
import com.nexus.platform.core.ui.showCenterToast
import com.nexus.platform.feature.common.ui.LegalWebViewActivity
import com.nexus.platform.ui.components.ActionButton
import com.nexus.platform.ui.theme.BackgroundBase
import com.nexus.platform.ui.theme.Primary
import com.nexus.platform.ui.theme.TextMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    val context = LocalContext.current
    val authRepository = remember(context) { context.appContainer.authRepository }
    val backendApi = remember(context) { context.appContainer.platformBackendApi }
    val scope = rememberCoroutineScope()

    val emptyCredentials = stringResource(R.string.login_error_empty_credentials)
    val codeLabel = stringResource(R.string.register_code_label)
    val registerSuccessText = stringResource(R.string.register_success)
    val registerTermsRequiredText = stringResource(R.string.register_terms_required)
    val registerCodeSentText = stringResource(R.string.register_code_sent)
    val registerEmailRequiredText = stringResource(R.string.login_error_email_required)
    val registerPasswordRequiredText = stringResource(R.string.login_error_password_required)

    var email by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var agreedToTerms by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var codeCountdown by remember { mutableIntStateOf(0) }

    var emailError by remember { mutableStateOf<String?>(null) }
    var codeError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var feedbackIsError by remember { mutableStateOf(false) }

    var termsUrl by remember { mutableStateOf("") }
    var privacyUrl by remember { mutableStateOf("") }
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

    LaunchedEffect(Unit) {
        delay(320)
        runCatching { backendApi.getLegalLinks() }
            .onSuccess { links ->
                termsUrl = links.termsUrl
                privacyUrl = links.privacyUrl
            }
    }

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
                verticalAlignment = Alignment.CenterVertically
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
                text = stringResource(R.string.register_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.register_subtitle), color = TextMuted)
            Spacer(modifier = Modifier.height(40.dp))

            OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                emailError = null
            },
            label = { Text(stringResource(R.string.register_account_label), style = MaterialTheme.typography.bodyLarge) },
            textStyle = inputTextStyle,
            singleLine = true,
            isError = !emailError.isNullOrBlank(),
            supportingText = {
                if (!emailError.isNullOrBlank()) {
                    Text(text = emailError.orEmpty())
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = textFieldColors
        )
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = verificationCode,
                onValueChange = {
                    verificationCode = it
                    codeError = null
                },
                label = { Text(codeLabel, style = MaterialTheme.typography.bodyLarge) },
                textStyle = inputTextStyle,
                singleLine = true,
                isError = !codeError.isNullOrBlank(),
                supportingText = {
                    if (!codeError.isNullOrBlank()) {
                        Text(text = codeError.orEmpty())
                    }
                },
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = textFieldColors
            )
            ActionButton(
                text = if (codeCountdown > 0) "${codeCountdown}s" else stringResource(R.string.register_get_code),
                onClick = {
                    val normalizedEmail = email.trim()
                    emailError = null
                    feedbackMessage = null
                    if (normalizedEmail.isBlank()) {
                        emailError = registerEmailRequiredText
                        return@ActionButton
                    }
                    if (!normalizedEmail.contains("@")) {
                        emailError = context.getString(R.string.auth_error_invalid_email)
                        return@ActionButton
                    }
                    if (codeCountdown <= 0) {
                        scope.launch {
                            val sent = backendApi.sendVerificationCodeResult(normalizedEmail, "REGISTER", "AUTH_REGISTER")
                            if (sent.success) {
                                feedbackMessage = registerCodeSentText
                                feedbackIsError = false
                                codeCountdown = 60
                                while (codeCountdown > 0) {
                                    kotlinx.coroutines.delay(1000)
                                    codeCountdown -= 1
                                }
                                showCenterToast(context, registerCodeSentText)
                            } else {
                                feedbackMessage = ApiErrorLocalizer.localize(
                                    context = context,
                                    rawMessage = sent.message,
                                    fallbackRes = R.string.register_code_failed
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
            value = password,
            onValueChange = {
                password = it
                passwordError = null
            },
            label = { Text(stringResource(R.string.register_password_label), style = MaterialTheme.typography.bodyLarge) },
            textStyle = inputTextStyle,
            singleLine = true,
            isError = !passwordError.isNullOrBlank(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
            supportingText = {
                if (!passwordError.isNullOrBlank()) {
                    Text(text = passwordError.orEmpty())
                }
            },
                trailingIcon = {
                    Text(
                        text = if (passwordVisible) {
                            stringResource(R.string.change_password_hide)
                        } else {
                            stringResource(R.string.change_password_show)
                        },
                        color = TextMuted,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.clickable { passwordVisible = !passwordVisible }
                    )
                },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = textFieldColors
        )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { agreedToTerms = !agreedToTerms }
                    .then(
                        if (agreedToTerms) Modifier.background(Primary.copy(alpha = 0.2f))
                        else Modifier.border(1.dp, TextMuted.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (agreedToTerms) {
                    Text(text = "✓", color = Primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = stringResource(R.string.register_terms_prefix), style = MaterialTheme.typography.bodySmall, color = TextMuted)
            Text(
                text = stringResource(R.string.register_terms_user_agreement),
                style = MaterialTheme.typography.bodySmall,
                color = Primary,
                modifier = Modifier.clickable {
                    LegalWebViewActivity.start(
                        context,
                        context.getString(R.string.register_terms_user_agreement),
                        termsUrl
                    )
                }
            )
            Text(text = stringResource(R.string.register_terms_connector), style = MaterialTheme.typography.bodySmall, color = TextMuted)
            Text(
                text = stringResource(R.string.register_terms_privacy_policy),
                style = MaterialTheme.typography.bodySmall,
                color = Primary,
                modifier = Modifier.clickable {
                    LegalWebViewActivity.start(
                        context,
                        context.getString(R.string.register_terms_privacy_policy),
                        privacyUrl
                    )
                }
            )
            }

            Spacer(modifier = Modifier.height(24.dp))
            ActionButton(
            text = if (isSubmitting) stringResource(R.string.common_loading) else stringResource(R.string.register_action),
            onClick = {
                if (isSubmitting) return@ActionButton
                val normalizedEmail = email.trim()
                val normalizedCode = verificationCode.trim()

                emailError = null
                codeError = null
                passwordError = null
                feedbackMessage = null

                var hasError = false
                if (normalizedEmail.isBlank()) {
                    emailError = registerEmailRequiredText
                    hasError = true
                } else if (!normalizedEmail.contains("@")) {
                    emailError = context.getString(R.string.auth_error_invalid_email)
                    hasError = true
                }
                if (password.isBlank()) {
                    passwordError = registerPasswordRequiredText
                    hasError = true
                }
                if (normalizedCode.isBlank()) {
                    codeError = codeLabel
                    hasError = true
                }
                if (normalizedEmail.isBlank() || password.isBlank()) {
                    feedbackMessage = emptyCredentials
                    feedbackIsError = true
                }
                if (!agreedToTerms) {
                    feedbackMessage = registerTermsRequiredText
                    feedbackIsError = true
                    hasError = true
                }
                if (hasError) {
                    feedbackMessage?.let { showCenterToast(context, it) }
                    return@ActionButton
                }

                isSubmitting = true
                scope.launch {
                    runCatching {
                        authRepository.register(
                            email = normalizedEmail,
                            password = password,
                            code = normalizedCode
                        )
                    }.onSuccess {
                        feedbackMessage = registerSuccessText
                        feedbackIsError = false
                        showCenterToast(context, registerSuccessText)
                        onRegisterSuccess()
                    }.onFailure {
                        feedbackMessage = ApiErrorLocalizer.localize(
                            context = context,
                            rawMessage = it.message,
                            fallbackRes = R.string.register_failed
                        )
                        feedbackIsError = true
                        showCenterToast(context, feedbackMessage.orEmpty())
                    }
                    isSubmitting = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
            )

            if (isSubmitting) {
                Spacer(modifier = Modifier.height(14.dp))
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }

            if (!feedbackMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = feedbackMessage.orEmpty(),
                    color = if (feedbackIsError) MaterialTheme.colorScheme.error else Primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(text = stringResource(R.string.register_existing_user), color = TextMuted)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.register_login),
                    color = Primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onLoginClick)
                )
            }
        }
    }
}
