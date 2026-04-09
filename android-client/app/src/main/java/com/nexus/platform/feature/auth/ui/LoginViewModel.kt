package com.nexus.platform.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.platform.domain.usecase.LoginUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val errorMessage: String? = null
)

private const val ERROR_LOGIN_FAILED = "__error_login_failed__"
private const val ERROR_EMAIL_REQUIRED = "__error_email_required__"
private const val ERROR_PASSWORD_REQUIRED = "__error_password_required__"

class LoginViewModel(
    private val loginUseCase: LoginUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value, emailError = null) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null) }
    }

    fun login(onSuccess: () -> Unit) {
        val snapshot = _uiState.value
        val email = snapshot.email.trim()
        val password = snapshot.password

        val emailError = if (email.isBlank()) ERROR_EMAIL_REQUIRED else null
        val passwordError = if (password.isBlank()) ERROR_PASSWORD_REQUIRED else null
        if (emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError,
                    errorMessage = null
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                emailError = null,
                passwordError = null,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            runCatching {
                loginUseCase(email, password)
            }.onSuccess {
                onSuccess()
            }.onFailure { e ->
                _uiState.update { it.copy(errorMessage = e.message ?: ERROR_LOGIN_FAILED) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
