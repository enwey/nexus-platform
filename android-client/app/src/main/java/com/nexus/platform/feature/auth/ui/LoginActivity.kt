package com.nexus.platform.feature.auth.ui

import android.content.Intent
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.CreationExtras
import com.nexus.platform.NexusApplication
import com.nexus.platform.core.i18n.AppLanguageManager
import com.nexus.platform.domain.usecase.LoginUseCase
import com.nexus.platform.feature.main.ui.MainActivity
import com.nexus.platform.R
import com.nexus.platform.ui.components.ActionButton
import com.nexus.platform.ui.theme.NexusPlatformTheme
import com.nexus.platform.ui.theme.Primary
import com.nexus.platform.ui.theme.TextMuted

class LoginActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as NexusApplication).container

        val authRepository = container.authRepository
        if (authRepository.currentSession() != null) {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
            return
        }

        val factory = LoginViewModelFactory(container.loginUseCase)
        val viewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]

        setContent {
            NexusPlatformTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                LoginScreen(
                    uiState = state,
                    onUsernameChange = viewModel::updateUsername,
                    onPasswordChange = viewModel::updatePassword,
                    onLoginClick = {
                        viewModel.login {
                            startActivity(Intent(this, MainActivity::class.java))
                            overridePendingTransition(0, 0)
                            finish()
                        }
                    }
                )
            }
        }
    }
}

private class LoginViewModelFactory(
    private val loginUseCase: LoginUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return LoginViewModel(loginUseCase) as T
    }
}

@Composable
private fun LoginScreen(
    uiState: LoginUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(72.dp))
        Text(stringResource(R.string.login_welcome), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResource(R.string.login_subtitle), color = TextMuted)
        Spacer(modifier = Modifier.height(36.dp))

        OutlinedTextField(
            value = uiState.username,
            onValueChange = onUsernameChange,
            label = { Text(stringResource(R.string.login_account_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            label = { Text(stringResource(R.string.login_password_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = {}) { Text(stringResource(R.string.login_forgot_password), color = Primary) }
        }

        if (!uiState.errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            val resolvedError = when (uiState.errorMessage) {
                "__error_empty_credentials__" -> stringResource(R.string.login_error_empty_credentials)
                "__error_login_failed__" -> stringResource(R.string.login_error_failed)
                else -> uiState.errorMessage.orEmpty()
            }
            Text(resolvedError, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(16.dp))
        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else {
            ActionButton(
                text = stringResource(R.string.login_action),
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(stringResource(R.string.login_new_user), color = TextMuted)
            Text(stringResource(R.string.login_signup), color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(36.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(TextMuted.copy(alpha = 0.2f))
            )
            Text("  ${stringResource(R.string.login_quick)}  ", color = TextMuted)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(TextMuted.copy(alpha = 0.2f))
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            QuickLoginBox(stringResource(R.string.login_quick_google), Modifier.weight(1f))
            QuickLoginBox(stringResource(R.string.login_quick_wechat), Modifier.weight(1f))
        }
    }
}

@Composable
private fun QuickLoginBox(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x2215161D)),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontWeight = FontWeight.Black)
    }
}
