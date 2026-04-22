package com.zeddihub.mobile.ui.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeddihub.mobile.data.remote.dto.ServerDto
import com.zeddihub.mobile.data.repository.AuthRepository
import com.zeddihub.mobile.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val authRepository: AuthRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    data class UiState(
        val servers: List<ServerDto> = emptyList(),
        val isRefreshing: Boolean = false,
        val error: String? = null,
        val username: String? = null
    )

    private val _state = MutableStateFlow(
        UiState(username = authRepository.session.value?.displayName ?: authRepository.session.value?.username)
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var pollJob: Job? = null

    init {
        savedStateHandle.get<String>(KEY_LAST_ERROR)?.let { cached ->
            _state.value = _state.value.copy(error = cached)
        }
        refresh()
        startPolling()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRefreshing = true, error = null)
            val result = serverRepository.getAll()
            _state.value = if (result.isSuccess) {
                _state.value.copy(
                    isRefreshing = false,
                    servers = result.getOrNull() ?: emptyList()
                ).also { savedStateHandle[KEY_LAST_ERROR] = null }
            } else {
                val msg = result.exceptionOrNull()?.localizedMessage
                savedStateHandle[KEY_LAST_ERROR] = msg
                _state.value.copy(isRefreshing = false, error = msg)
            }
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                delay(10_000)
                val result = serverRepository.getAll()
                if (result.isSuccess) {
                    _state.value = _state.value.copy(
                        servers = result.getOrNull() ?: _state.value.servers
                    )
                }
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }

    companion object {
        private const val KEY_LAST_ERROR = "dashboard_last_error"
    }
}
