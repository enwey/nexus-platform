package com.nexus.platform.feature.auth.ui

import android.content.Intent
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nexus.platform.NexusApplication
import com.nexus.platform.core.i18n.ApiErrorLocalizer
import com.nexus.platform.core.i18n.AppLanguageManager
import com.nexus.platform.core.ui.showCenterToast
import com.nexus.platform.domain.usecase.LoginUseCase
import com.nexus.platform.feature.main.ui.MainActivity
import com.nexus.platform.R
import com.nexus.platform.ui.components.ActionButton
import com.nexus.platform.ui.theme.NexusPlatformTheme
import com.nexus.platform.ui.theme.Primary
import com.nexus.platform.ui.theme.TextMuted
import com.nexus.platform.ui.theme.BackgroundBase

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
            finish()
            return
        }

        val factory = LoginViewModelFactory(container.loginUseCase)
        val viewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]

        setContent {
            NexusPlatformTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = AuthRoutes.LOGIN,
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None }
                ) {
                    composable(
                        route = AuthRoutes.LOGIN,
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { -it },
                                animationSpec = tween(AUTH_EXIT_MS, easing = FastOutLinearInEasing)
                            )
                        },
                        popEnterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { -it },
                                animationSpec = tween(AUTH_ENTER_MS, easing = LinearOutSlowInEasing)
                            )
                        }
                    ) {
                        LoginScreen(
                            uiState = state,
                            onEmailChange = viewModel::updateEmail,
                            onPasswordChange = viewModel::updatePassword,
                            onCloseClick = { finish() },
                            onSignupClick = {
                                navController.navigate(AuthRoutes.REGISTER) {
                                    launchSingleTop = true
                                }
                            },
                            onForgotPasswordClick = {
                                navController.navigate(AuthRoutes.FORGOT) {
                                    launchSingleTop = true
                                }
                            },
                            onLoginClick = {
                                viewModel.login {
                                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                    finish()
                                }
                            }
                        )
                    }
                    composable(
                        route = AuthRoutes.REGISTER,
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(AUTH_ENTER_MS, easing = LinearOutSlowInEasing)
                            )
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { -it },
                                animationSpec = tween(AUTH_EXIT_MS, easing = FastOutLinearInEasing)
                            )
                        },
                        popEnterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { -it },
                                animationSpec = tween(AUTH_ENTER_MS, easing = LinearOutSlowInEasing)
                            )
                        },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(AUTH_EXIT_MS, easing = FastOutLinearInEasing)
                            )
                        }
                    ) {
                        RegisterScreen(
                            onBackClick = { navController.popBackStack() },
                            onLoginClick = { navController.popBackStack(AuthRoutes.LOGIN, false) },
                            onRegisterSuccess = { navController.popBackStack(AuthRoutes.LOGIN, false) }
                        )
                    }
                    composable(
                        route = AuthRoutes.FORGOT,
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(AUTH_ENTER_MS, easing = LinearOutSlowInEasing)
                            )
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { -it },
                                animationSpec = tween(AUTH_EXIT_MS, easing = FastOutLinearInEasing)
                            )
                        },
                        popEnterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { -it },
                                animationSpec = tween(AUTH_ENTER_MS, easing = LinearOutSlowInEasing)
                            )
                        },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(AUTH_EXIT_MS, easing = FastOutLinearInEasing)
                            )
                        }
                    ) {
                        ForgotPasswordScreen(
                            onBackClick = { navController.popBackStack() },
                            onLoginClick = { navController.popBackStack(AuthRoutes.LOGIN, false) }
                        )
                    }
                }
            }
        }
    }
}

private object AuthRoutes {
    const val LOGIN = "auth_login"
    const val REGISTER = "auth_register"
    const val FORGOT = "auth_forgot"
}

private const val AUTH_ENTER_MS = 280
private const val AUTH_EXIT_MS = 240

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
    onCloseClick: () -> Unit,
    onSignupClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    val context = LocalContext.current
    var passwordVisible by remember { mutableStateOf(false) }
    val inputTextStyle = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )
    val textFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        focusedIndicatorColor = Primary,
        unfocusedIndicatorColor = Color(0x33FFFFFF),
        cursorColor = Primary
    )

    LaunchedEffect(uiState.errorMessage) {
        if (!uiState.errorMessage.isNullOrBlank()) {
            val resolvedError = when (uiState.errorMessage) {
                "__error_login_failed__" -> context.getString(R.string.login_error_failed)
                else -> ApiErrorLocalizer.localize(
                    context = context,
                    rawMessage = uiState.errorMessage,
                    fallbackRes = R.string.login_error_failed
                )
            }
            showCenterToast(context, resolvedError)
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
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onCloseClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.common_close),
                        tint = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
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
            label = { Text(stringResource(R.string.login_account_label), style = MaterialTheme.typography.bodyLarge) },
            textStyle = inputTextStyle,
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
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) {
                            stringResource(R.string.change_password_hide)
                        } else {
                            stringResource(R.string.change_password_show)
                        },
                        tint = TextMuted
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = textFieldColors
        )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            label = { Text(stringResource(R.string.login_password_label), style = MaterialTheme.typography.bodyLarge) },
            textStyle = inputTextStyle,
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
                .heightIn(min = 56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = textFieldColors
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

            Spacer(modifier = Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else {
                    ActionButton(
                        text = stringResource(R.string.login_action),
                        onClick = onLoginClick,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    )
                }
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
                    color = Primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onSignupClick)
                )
            }
        }
    }
}
