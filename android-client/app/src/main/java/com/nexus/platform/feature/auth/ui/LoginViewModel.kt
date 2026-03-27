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
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

private const val ERROR_EMPTY_CREDENTIALS = "__error_empty_credentials__"
private const val ERROR_LOGIN_FAILED = "__error_login_failed__"

class LoginViewModel(
    private val loginUseCase: LoginUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateUsername(value: String) {
        _uiState.update { it.copy(username = value) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    fun login(onSuccess: () -> Unit) {
        val snapshot = _uiState.value
        val username = snapshot.username.trim()
        val password = snapshot.password

        if (username.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = ERROR_EMPTY_CREDENTIALS) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                loginUseCase(username, password)
            }.onSuccess {
                onSuccess()
            }.onFailure { e ->
                _uiState.update { it.copy(errorMessage = e.message ?: ERROR_LOGIN_FAILED) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
