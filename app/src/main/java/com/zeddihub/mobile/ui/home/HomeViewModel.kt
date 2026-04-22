package com.zeddihub.mobile.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeddihub.mobile.data.remote.dto.ServerDto
import com.zeddihub.mobile.data.repository.AuthRepository
import com.zeddihub.mobile.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    data class UiState(
        val servers: List<ServerDto> = emptyList(),
        val displayName: String? = null,
        val isRefreshing: Boolean = false
    )

    private val _state = MutableStateFlow(
        UiState(
            displayName = authRepository.session.value?.displayName
                ?: authRepository.session.value?.username
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRefreshing = true)
            serverRepository.getAll().onSuccess { list ->
                _state.value = _state.value.copy(servers = list)
            }
            _state.value = _state.value.copy(isRefreshing = false)
        }
    }
}
