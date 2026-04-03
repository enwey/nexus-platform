package com.nexus.platform.feature.auth.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.platform.R
import com.nexus.platform.core.i18n.AppLanguageManager
import com.nexus.platform.data.remote.PlatformBackendApi
import com.nexus.platform.ui.components.ActionButton
import com.nexus.platform.ui.theme.NexusPlatformTheme
import com.nexus.platform.ui.theme.Primary
import com.nexus.platform.ui.theme.TextMuted
import kotlinx.coroutines.launch

class ForgotPasswordActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NexusPlatformTheme {
                ForgotPasswordScreen(
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
private fun ForgotPasswordScreen(
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    val context = LocalContext.current
    val backendApi = remember(context) { PlatformBackendApi(context) }
    val scope = rememberCoroutineScope()
    val accountLabel = stringResource(R.string.forgot_account_label)
    var phoneOrEmail by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
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
        Text(text = stringResource(R.string.forgot_title), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(R.string.forgot_subtitle), color = TextMuted)
        Spacer(modifier = Modifier.height(30.dp))

        OutlinedTextField(
            value = phoneOrEmail,
            onValueChange = { phoneOrEmail = it },
            label = { Text(accountLabel) },
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
                label = { Text(stringResource(R.string.forgot_code_label)) },
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
                text = if (codeCountdown > 0) "${codeCountdown}s" else stringResource(R.string.forgot_get_code),
                onClick = {
                    if (phoneOrEmail.isBlank()) {
                        Toast.makeText(context, accountLabel, Toast.LENGTH_SHORT).show()
                        return@ActionButton
                    }
                    if (codeCountdown <= 0) {
                        scope.launch {
                            val sent = backendApi.sendVerificationCode(phoneOrEmail.trim(), "RESET_PASSWORD")
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
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text(stringResource(R.string.forgot_new_password_label)) },
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
        Spacer(modifier = Modifier.height(32.dp))

        ActionButton(
            text = stringResource(R.string.forgot_action),
            onClick = {
                if (phoneOrEmail.isBlank() || verificationCode.isBlank() || newPassword.length < 8) {
                    Toast.makeText(context, "Please fill all fields and use >=8 chars password", Toast.LENGTH_SHORT).show()
                    return@ActionButton
                }
                scope.launch {
                    val ok = backendApi.resetPassword(
                        account = phoneOrEmail.trim(),
                        code = verificationCode.trim(),
                        newPassword = newPassword
                    )
                    Toast.makeText(
                        context,
                        if (ok) "Password reset success" else "Reset password failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(text = stringResource(R.string.forgot_existing_user), color = TextMuted)
            TextButton(onClick = onLoginClick) {
                Text(text = stringResource(R.string.forgot_login), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
