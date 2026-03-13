package com.maazm7d.termuxhub.ui.screens.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maazm7d.termuxhub.domain.usecase.GetSavedToolsUseCase
import com.maazm7d.termuxhub.domain.usecase.RefreshToolsUseCase
import com.maazm7d.termuxhub.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavedUiState(
    val tools: List<com.maazm7d.termuxhub.domain.model.Tool> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val getSavedToolsUseCase: GetSavedToolsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val refreshToolsUseCase: RefreshToolsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavedUiState(isLoading = true))
    val uiState: StateFlow<SavedUiState> = _uiState.asStateFlow()

    init {
        loadSavedTools()
    }

    private fun loadSavedTools() {
        viewModelScope.launch {
            getSavedToolsUseCase()
                .onStart { _uiState.value = _uiState.value.copy(isLoading = true) }
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load saved tools"
                    )
                }
                .collect { tools ->
                    _uiState.value = SavedUiState(
                        tools = tools,
                        isLoading = false,
                        isRefreshing = false,
                        error = null
                    )
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            try {
                refreshToolsUseCase()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = e.message ?: "Refresh failed"
                )
            }
        }
    }

    fun removeTool(tool: com.maazm7d.termuxhub.domain.model.Tool) {
        viewModelScope.launch {
            toggleFavoriteUseCase(tool.id)
        }
    }
}
