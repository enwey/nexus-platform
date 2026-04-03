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
import com.nexus.platform.core.i18n.AppLanguageManager
import com.nexus.platform.data.remote.PlatformBackendApi
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
    val termsText = stringResource(R.string.register_terms)
    val codeLabel = stringResource(R.string.register_code_label)

    var phoneOrEmail by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var agreedToTerms by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var codeCountdown by remember { mutableIntStateOf(0) }

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
        Text(text = stringResource(R.string.register_title), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(R.string.register_subtitle), color = TextMuted)
        Spacer(modifier = Modifier.height(30.dp))

        OutlinedTextField(
            value = phoneOrEmail,
            onValueChange = { phoneOrEmail = it },
            label = { Text(stringResource(R.string.register_account_label)) },
            singleLine = true,
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
                onValueChange = { verificationCode = it },
                label = { Text(codeLabel) },
                singleLine = true,
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
                    if (phoneOrEmail.isBlank()) {
                        Toast.makeText(context, emptyCredentials, Toast.LENGTH_SHORT).show()
                        return@ActionButton
                    }
                    if (codeCountdown <= 0) {
                        scope.launch {
                            val sent = backendApi.sendVerificationCode(phoneOrEmail.trim(), "REGISTER")
                            if (sent) {
                                codeCountdown = 60
                                while (codeCountdown > 0) {
                                    kotlinx.coroutines.delay(1000)
                                    codeCountdown -= 1
                                }
                                Toast.makeText(context, "Code sent", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to send code", Toast.LENGTH_SHORT).show()
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
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.register_password_label)) },
            singleLine = true,
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
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).clickable { agreedToTerms = !agreedToTerms },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(4.dp))
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
            Text(text = termsText, style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = Modifier.padding(start = 10.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))
        ActionButton(
            text = if (isSubmitting) "Submitting..." else stringResource(R.string.register_action),
            onClick = {
                if (isSubmitting) return@ActionButton
                val account = phoneOrEmail.trim()
                if (account.isBlank() || password.isBlank()) {
                    Toast.makeText(context, emptyCredentials, Toast.LENGTH_SHORT).show()
                    return@ActionButton
                }
                if (!agreedToTerms) {
                    Toast.makeText(context, termsText, Toast.LENGTH_SHORT).show()
                    return@ActionButton
                }
                if (verificationCode.isBlank()) {
                    Toast.makeText(context, codeLabel, Toast.LENGTH_SHORT).show()
                    return@ActionButton
                }
                isSubmitting = true
                scope.launch {
                    runCatching {
                        authRepository.register(
                            username = account,
                            password = password,
                            email = account.takeIf { it.contains("@") }
                        )
                    }.onSuccess {
                        Toast.makeText(context, "Register success", Toast.LENGTH_SHORT).show()
                        context.startActivity(Intent(context, LoginActivity::class.java))
                    }.onFailure {
                        Toast.makeText(context, it.message ?: "Register failed", Toast.LENGTH_SHORT).show()
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

        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(text = stringResource(R.string.register_existing_user), color = TextMuted)
            TextButton(onClick = onLoginClick) {
                Text(text = stringResource(R.string.register_login), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
