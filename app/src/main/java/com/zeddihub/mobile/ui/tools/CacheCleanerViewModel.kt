package com.zeddihub.mobile.ui.tools

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeddihub.mobile.data.telemetry.TelemetryRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CacheCleanerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient,
    private val telemetry: TelemetryRecorder
) : ViewModel() {

    data class UiState(
        val appCacheBytes: Long = 0,
        val tempBytes: Long = 0,
        val lastAction: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch(Dispatchers.IO) {
        val cache = dirSize(context.cacheDir)
        val temp = dirSize(context.externalCacheDir) + dirSize(File(context.filesDir, "tmp"))
        _state.value = _state.value.copy(appCacheBytes = cache, tempBytes = temp)
    }

    fun clearAll() = viewModelScope.launch(Dispatchers.IO) {
        val before = _state.value.appCacheBytes + _state.value.tempBytes
        wipeDir(context.cacheDir)
        context.externalCacheDir?.let { wipeDir(it) }
        wipeDir(File(context.filesDir, "tmp"))
        httpClient.cache?.evictAll()
        refresh()
        _state.value = _state.value.copy(lastAction = "Cleared ${before / 1024} KB")
        telemetry.toolRun("cache_clear_all", true)
    }

    fun flushDns() = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            // OkHttp has no public DNS cache invalidation, but evicting connections
            // drops cached addresses held by active pooled connections.
            httpClient.connectionPool.evictAll()
        }
        _state.value = _state.value.copy(lastAction = "DNS/connection pool flushed")
        telemetry.toolRun("cache_flush_dns", true)
    }

    private fun dirSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private fun wipeDir(dir: File?) {
        if (dir == null || !dir.exists()) return
        dir.listFiles()?.forEach { it.deleteRecursively() }
    }
}
