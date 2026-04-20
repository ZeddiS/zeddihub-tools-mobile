package com.zeddihub.mobile.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeddihub.mobile.data.telemetry.TelemetryRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import kotlin.system.measureNanoTime

@HiltViewModel
class SpeedTestViewModel @Inject constructor(
    private val telemetry: TelemetryRecorder
) : ViewModel() {

    data class UiState(
        val running: Boolean = false,
        val progress: Float = 0f,
        val downloadMbps: Double? = null,
        val downloadMBs: Double? = null,
        val downloadedBytes: Long = 0,
        val elapsedSeconds: Double = 0.0,
        val pingMs: Long? = null,
        val error: String? = null,
        val history: List<Entry> = emptyList(),
        val minMbps: Double? = null,
        val maxMbps: Double? = null,
        val avgMbps: Double? = null
    ) {
        data class Entry(val mbps: Double, val pingMs: Long?, val timestamp: Long)
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun start() {
        if (_state.value.running) return
        _state.value = _state.value.copy(
            running = true,
            progress = 0f,
            downloadMbps = null,
            downloadMBs = null,
            downloadedBytes = 0,
            elapsedSeconds = 0.0,
            pingMs = null,
            error = null
        )
        viewModelScope.launch { runTest() }
    }

    private suspend fun runTest() {
        // ── 1) Ping to CF endpoint ───────────────────────────
        val ping = withContext(Dispatchers.IO) {
            runCatching {
                val ns = measureNanoTime {
                    val c = (URL(PING_URL).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 5_000
                        readTimeout = 5_000
                        requestMethod = "HEAD"
                    }
                    c.responseCode
                    c.disconnect()
                }
                ns / 1_000_000L
            }.getOrNull()
        }
        _state.value = _state.value.copy(pingMs = ping)

        // ── 2) Download test ─────────────────────────────────
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val conn = (URL(DOWNLOAD_URL).openConnection() as HttpURLConnection).apply {
                    setRequestProperty("User-Agent", "ZeddiHubMobile/speedtest")
                    connectTimeout = 10_000
                    readTimeout = 30_000
                }
                conn.inputStream.use { input ->
                    val start = System.nanoTime()
                    val buf = ByteArray(CHUNK)
                    var downloaded = 0L
                    val total = conn.contentLengthLong.takeIf { it > 0 } ?: TEST_BYTES
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        downloaded += n
                        val pct = (downloaded.toFloat() / total).coerceIn(0f, 1f)
                        val elapsed = (System.nanoTime() - start) / 1e9
                        val mbps = if (elapsed > 0.05) (downloaded * 8.0) / (elapsed * 1_000_000.0) else 0.0
                        val mBs = if (elapsed > 0.05) downloaded / (elapsed * 1_000_000.0) else 0.0
                        _state.value = _state.value.copy(
                            progress = pct,
                            downloadedBytes = downloaded,
                            downloadMbps = mbps,
                            downloadMBs = mBs,
                            elapsedSeconds = elapsed
                        )
                    }
                    val elapsed = (System.nanoTime() - start) / 1e9
                    val mbps = (downloaded * 8.0) / (elapsed * 1_000_000.0)
                    val mBs = downloaded / (elapsed * 1_000_000.0)
                    val entry = UiState.Entry(mbps, _state.value.pingMs, System.currentTimeMillis())
                    val newHistory = (listOf(entry) + _state.value.history).take(5)
                    val speeds = newHistory.map { it.mbps }
                    _state.value = _state.value.copy(
                        running = false,
                        progress = 1f,
                        downloadedBytes = downloaded,
                        downloadMbps = mbps,
                        downloadMBs = mBs,
                        elapsedSeconds = elapsed,
                        history = newHistory,
                        minMbps = speeds.minOrNull(),
                        maxMbps = speeds.maxOrNull(),
                        avgMbps = speeds.average()
                    )
                    conn.disconnect()
                }
            }
        }
        result.onFailure { e ->
            _state.value = _state.value.copy(running = false, error = e.message ?: "error")
        }
        telemetry.toolRun("speedtest", result.isSuccess)
    }

    fun clearHistory() {
        _state.value = _state.value.copy(
            history = emptyList(),
            minMbps = null, maxMbps = null, avgMbps = null
        )
    }

    companion object {
        private const val TEST_BYTES = 10_000_000L
        private const val DOWNLOAD_URL = "https://speed.cloudflare.com/__down?bytes=$TEST_BYTES"
        private const val PING_URL = "https://speed.cloudflare.com/__up"
        private const val CHUNK = 65536
    }
}
