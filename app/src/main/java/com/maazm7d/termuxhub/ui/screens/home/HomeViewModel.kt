package com.maazm7d.termuxhub.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maazm7d.termuxhub.domain.usecase.GetStarsUseCase
import com.maazm7d.termuxhub.domain.usecase.GetToolsUseCase
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

data class HomeUiState(
    val tools: List<com.maazm7d.termuxhub.domain.model.Tool> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getToolsUseCase: GetToolsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val refreshToolsUseCase: RefreshToolsUseCase,
    private val getStarsUseCase: GetStarsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _starsMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val starsMap: StateFlow<Map<String, Int>> = _starsMap.asStateFlow()

    init {
        loadTools()
        fetchStarsOnce()
    }

    private fun loadTools() {
        viewModelScope.launch {
            getToolsUseCase()
                .onStart { _uiState.value = _uiState.value.copy(isLoading = true) }
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load tools"
                    )
                }
                .collect { tools ->
                    _uiState.value = HomeUiState(
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
                // After refresh, the flow will automatically emit updated tools
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = e.message ?: "Refresh failed"
                )
            }
        }
    }

    fun toggleFavorite(toolId: String) {
        viewModelScope.launch {
            toggleFavoriteUseCase(toolId)
        }
    }

    private fun fetchStarsOnce() {
        viewModelScope.launch {
            try {
                val stars = getStarsUseCase()
                _starsMap.value = stars
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
