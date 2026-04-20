package com.zeddihub.mobile.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeddihub.mobile.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import kotlin.system.measureTimeMillis

data class PingResult(
    val name: String,
    val address: String,
    val latencyMs: Int? = null,
    val failed: Boolean = false
)

data class PingUiState(
    val results: List<PingResult> = emptyList(),
    val isRunning: Boolean = false
)

@HiltViewModel
class PingViewModel @Inject constructor(
    private val serverRepository: ServerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PingUiState())
    val state: StateFlow<PingUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            serverRepository.getAll().onSuccess { list ->
                _state.value = PingUiState(
                    results = list.mapNotNull { s ->
                        if (s.address.isEmpty()) null
                        else PingResult(name = s.name, address = s.address)
                    }
                )
            }
        }
    }

    fun run() {
        if (_state.value.isRunning) return
        _state.value = _state.value.copy(isRunning = true, results = _state.value.results.map {
            it.copy(latencyMs = null, failed = false)
        })
        viewModelScope.launch {
            val initial = _state.value.results
            val measured = withContext(Dispatchers.IO) {
                initial.map { r ->
                    async { r to measure(r.address) }
                }.awaitAll()
            }
            _state.value = PingUiState(
                results = measured.map { (r, ms) ->
                    if (ms != null) r.copy(latencyMs = ms) else r.copy(failed = true)
                },
                isRunning = false
            )
        }
    }

    private fun measure(address: String): Int? {
        val parts = address.split(":")
        if (parts.size != 2) return null
        val host = parts[0]
        val port = parts[1].toIntOrNull() ?: return null
        return runCatching {
            var result: Int
            Socket().use { socket ->
                val elapsed = measureTimeMillis {
                    socket.connect(InetSocketAddress(host, port), 2000)
                }
                result = elapsed.toInt()
            }
            result
        }.getOrNull()
    }
}
