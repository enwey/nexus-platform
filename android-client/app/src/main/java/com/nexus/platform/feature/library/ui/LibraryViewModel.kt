package com.nexus.platform.feature.library.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.platform.domain.model.GameItem
import com.nexus.platform.domain.usecase.GetApprovedGamesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val games: List<GameItem> = emptyList()
)

private const val ERROR_LOAD_GAMES_FAILED = "__error_load_games_failed__"

class LibraryViewModel(
    private val getApprovedGamesUseCase: GetApprovedGamesUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    fun load() {
        _uiState.update { it.copy(loading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { getApprovedGamesUseCase() }
                .onSuccess { games ->
                    _uiState.update { it.copy(loading = false, games = games) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            errorMessage = e.message ?: ERROR_LOAD_GAMES_FAILED
                        )
                    }
                }
        }
    }
}
