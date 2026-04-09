package com.nexus.platform.feature.auth.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.platform.NexusApplication
import com.nexus.platform.R
import com.nexus.platform.core.i18n.ApiErrorLocalizer
import com.nexus.platform.core.i18n.AppLanguageManager
import com.nexus.platform.data.remote.PlatformBackendApi
import com.nexus.platform.feature.common.ui.LegalWebViewActivity
import com.nexus.platform.ui.components.ActionButton
import com.nexus.platform.ui.theme.NexusPlatformTheme
import com.nexus.platform.ui.theme.Primary
import com.nexus.platform.ui.theme.TextMuted
import kotlinx.coroutines.launch

class RegisterActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NexusPlatformTheme {
                RegisterScreen(
                    onBackClick = { finish() },
                    onLoginClick = {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun RegisterScreen(
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    val context = LocalContext.current
    val authRepository = remember(context) { (context.applicationContext as NexusApplication).container.authRepository }
    val backendApi = remember(context) { PlatformBackendApi(context) }
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

    LaunchedEffect(Unit) {
        val links = backendApi.getLegalLinks()
        termsUrl = links.termsUrl
        privacyUrl = links.privacyUrl
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        TextButton(onClick = onBackClick, modifier = Modifier.padding(start = 0.dp)) {
            Text(text = "<", style = MaterialTheme.typography.headlineMedium, color = TextMuted)
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.register_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(R.string.register_subtitle), color = TextMuted)
        Spacer(modifier = Modifier.height(30.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                emailError = null
            },
            label = { Text(stringResource(R.string.register_account_label)) },
            singleLine = true,
            isError = !emailError.isNullOrBlank(),
            supportingText = {
                if (!emailError.isNullOrBlank()) {
                    Text(text = emailError.orEmpty())
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0x0DFFFFFF),
                unfocusedContainerColor = Color(0x08FFFFFF),
                focusedIndicatorColor = Primary,
                unfocusedIndicatorColor = Color(0x14FFFFFF),
                cursorColor = Primary
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = verificationCode,
                onValueChange = {
                    verificationCode = it
                    codeError = null
                },
                label = { Text(codeLabel) },
                singleLine = true,
                isError = !codeError.isNullOrBlank(),
                supportingText = {
                    if (!codeError.isNullOrBlank()) {
                        Text(text = codeError.orEmpty())
                    }
                },
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0x0DFFFFFF),
                    unfocusedContainerColor = Color(0x08FFFFFF),
                    focusedIndicatorColor = Primary,
                    unfocusedIndicatorColor = Color(0x14FFFFFF),
                    cursorColor = Primary
                )
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
                                Toast.makeText(context, registerCodeSentText, Toast.LENGTH_SHORT).show()
                            } else {
                                feedbackMessage = ApiErrorLocalizer.localize(
                                    context = context,
                                    rawMessage = sent.message,
                                    fallbackRes = R.string.register_code_failed
                                )
                                feedbackIsError = true
                                Toast.makeText(context, feedbackMessage, Toast.LENGTH_SHORT).show()
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
            label = { Text(stringResource(R.string.register_password_label)) },
            singleLine = true,
            isError = !passwordError.isNullOrBlank(),
            supportingText = {
                if (!passwordError.isNullOrBlank()) {
                    Text(text = passwordError.orEmpty())
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0x0DFFFFFF),
                unfocusedContainerColor = Color(0x08FFFFFF),
                focusedIndicatorColor = Primary,
                unfocusedIndicatorColor = Color(0x14FFFFFF),
                cursorColor = Primary
            )
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
                    feedbackMessage?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
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
                        Toast.makeText(context, registerSuccessText, Toast.LENGTH_SHORT).show()
                        context.startActivity(Intent(context, LoginActivity::class.java))
                    }.onFailure {
                        feedbackMessage = ApiErrorLocalizer.localize(
                            context = context,
                            rawMessage = it.message,
                            fallbackRes = R.string.register_failed
                        )
                        feedbackIsError = true
                        Toast.makeText(context, feedbackMessage, Toast.LENGTH_SHORT).show()
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
