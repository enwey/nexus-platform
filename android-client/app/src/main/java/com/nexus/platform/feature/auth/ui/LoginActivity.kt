package com.nexus.platform.feature.auth.ui

import android.content.Intent
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.CreationExtras
import com.nexus.platform.NexusApplication
import com.nexus.platform.core.i18n.ApiErrorLocalizer
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
                    onEmailChange = viewModel::updateEmail,
                    onPasswordChange = viewModel::updatePassword,
                    onSignupClick = {
                        startActivity(Intent(this, RegisterActivity::class.java))
                    },
                    onForgotPasswordClick = {
                        startActivity(Intent(this, ForgotPasswordActivity::class.java))
                    },
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
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignupClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(72.dp))
        Text(
            text = stringResource(R.string.login_welcome),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.login_subtitle),
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(40.dp))

        OutlinedTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            label = { Text(stringResource(R.string.login_account_label)) },
            isError = !uiState.emailError.isNullOrBlank(),
            supportingText = {
                if (!uiState.emailError.isNullOrBlank()) {
                    val resolvedError = when (uiState.emailError) {
                        "__error_email_required__" -> stringResource(R.string.login_error_email_required)
                        else -> uiState.emailError.orEmpty()
                    }
                    Text(text = resolvedError)
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
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
        OutlinedTextField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            label = { Text(stringResource(R.string.login_password_label)) },
            isError = !uiState.passwordError.isNullOrBlank(),
            supportingText = {
                if (!uiState.passwordError.isNullOrBlank()) {
                    val resolvedError = when (uiState.passwordError) {
                        "__error_password_required__" -> stringResource(R.string.login_error_password_required)
                        else -> uiState.passwordError.orEmpty()
                    }
                    Text(text = resolvedError)
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0x0DFFFFFF),
                unfocusedContainerColor = Color(0x08FFFFFF),
                focusedIndicatorColor = Primary,
                unfocusedIndicatorColor = Color(0x14FFFFFF),
                cursorColor = Primary
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onForgotPasswordClick) {
                Text(
                    text = stringResource(R.string.login_forgot_password),
                    color = Primary
                )
            }
        }

        if (!uiState.errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            val resolvedError = when (uiState.errorMessage) {
                "__error_login_failed__" -> stringResource(R.string.login_error_failed)
                else -> ApiErrorLocalizer.localize(
                    context = context,
                    rawMessage = uiState.errorMessage,
                    fallbackRes = R.string.login_error_failed
                )
            }
            Text(
                text = resolvedError,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
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

        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.login_new_user),
                color = TextMuted
            )
            Text(
                text = stringResource(R.string.login_signup),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onSignupClick)
            )
        }
    }
}
