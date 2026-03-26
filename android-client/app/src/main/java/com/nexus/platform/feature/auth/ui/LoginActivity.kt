package com.nexus.platform.feature.auth.ui

import android.content.Intent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.CreationExtras
import com.nexus.platform.data.repository.AuthRepository
import com.nexus.platform.domain.usecase.LoginUseCase
import com.nexus.platform.feature.main.ui.MainActivity
import com.nexus.platform.ui.components.ActionButton
import com.nexus.platform.ui.theme.NexusPlatformTheme
import com.nexus.platform.ui.theme.Primary
import com.nexus.platform.ui.theme.TextMuted

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authRepository = AuthRepository(this)
        if (authRepository.currentSession() != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val factory = LoginViewModelFactory(LoginUseCase(authRepository))
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
        Text("欢迎回来", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(8.dp))
        Text("登录你的 Nexus 账号，同步资产与存档。", color = TextMuted)
        Spacer(modifier = Modifier.height(36.dp))

        OutlinedTextField(
            value = uiState.username,
            onValueChange = onUsernameChange,
            label = { Text("手机号 / 邮箱") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            label = { Text("输入密码") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = {}) { Text("忘记密码？", color = Primary) }
        }

        if (!uiState.errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(uiState.errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(16.dp))
        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else {
            ActionButton(
                text = "安全登录",
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("新用户？", color = TextMuted)
            Text("注册账号", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(36.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(TextMuted.copy(alpha = 0.2f))
            )
            Text("  快捷登录  ", color = TextMuted)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(TextMuted.copy(alpha = 0.2f))
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            QuickLoginBox("G", Modifier.weight(1f))
            QuickLoginBox("微", Modifier.weight(1f))
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
