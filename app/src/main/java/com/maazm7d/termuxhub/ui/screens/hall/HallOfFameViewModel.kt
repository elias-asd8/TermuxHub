package com.maazm7d.termuxhub.ui.screens.hall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maazm7d.termuxhub.data.repository.HallOfFameRepository
import com.maazm7d.termuxhub.domain.model.HallOfFameMember
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed interface RefreshState {
    object Idle : RefreshState
    object Loading : RefreshState
    data class Error(val message: String) : RefreshState
    object Success : RefreshState
}

@HiltViewModel
class HallOfFameViewModel @Inject constructor(
    private val repository: HallOfFameRepository
) : ViewModel() {

    // Members from database (cached)
    val members: StateFlow<List<HallOfFameMember>> = repository.observeMembers()
        .catch { exception ->
            Timber.e(exception, "Error observing members")
            emit(emptyList()) // Fallback to empty list on error
        }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _refreshState = MutableStateFlow<RefreshState>(RefreshState.Idle)
    val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()

    init {
        refresh() // Initial load
    }

    fun refresh() {
        viewModelScope.launch {
            _refreshState.value = RefreshState.Loading
            val result = repository.refresh()
            _refreshState.value = if (result.isSuccess) {
                RefreshState.Success
            } else {
                RefreshState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }
}
