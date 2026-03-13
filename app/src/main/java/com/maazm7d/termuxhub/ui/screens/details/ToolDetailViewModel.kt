package com.maazm7d.termuxhub.ui.screens.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maazm7d.termuxhub.domain.model.ToolDetails
import com.maazm7d.termuxhub.domain.usecase.GetToolDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ToolDetailUiState {
    object Loading : ToolDetailUiState
    data class Success(val tool: ToolDetails) : ToolDetailUiState
    data class Error(val message: String) : ToolDetailUiState
}

@HiltViewModel
class ToolDetailViewModel @Inject constructor(
    private val getToolDetailsUseCase: GetToolDetailsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ToolDetailUiState>(ToolDetailUiState.Loading)
    val uiState: StateFlow<ToolDetailUiState> = _uiState.asStateFlow()

    fun load(id: String) {
        viewModelScope.launch {
            _uiState.value = ToolDetailUiState.Loading
            try {
                val tool = getToolDetailsUseCase(id)
                if (tool != null) {
                    _uiState.value = ToolDetailUiState.Success(tool)
                } else {
                    _uiState.value = ToolDetailUiState.Error("Tool not found")
                }
            } catch (e: Exception) {
                _uiState.value = ToolDetailUiState.Error(e.message ?: "Failed to load tool")
            }
        }
    }
}
