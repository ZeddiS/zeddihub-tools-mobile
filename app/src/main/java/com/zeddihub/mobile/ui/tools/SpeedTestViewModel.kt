package com.zeddihub.mobile.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeddihub.mobile.data.telemetry.TelemetryRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlin.math.sqrt

@HiltViewModel
class SpeedTestViewModel @Inject constructor(
    private val telemetry: TelemetryRecorder
) : ViewModel() {

    enum class Phase { IDLE, META, PING, DOWNLOAD, UPLOAD, DONE }
    enum class Unit(val suffix: String) { MS("ms"), MBPS("Mbps") }

    data class HistoryEntry(
        val timestamp: Long,
        val pingMs: Double?,
        val jitterMs: Double?,
        val lossPct: Double?,
        val downloadMbps: Double,
        val uploadMbps: Double,
        val isp: String?,
        val server: String?
    )

    data class UiState(
        val phase: Phase = Phase.IDLE,
        val running: Boolean = false,
        val liveValue: Double = 0.0,
        val liveUnit: Unit = Unit.MBPS,
        val gaugeMax: Double = GAUGE_DEFAULT_MAX,

        // Final metrics
        val pingMs: Double? = null,
        val jitterMs: Double? = null,
        val lossPct: Double? = null,
        val pingAttempts: Int = 0,
        val pingSuccess: Int = 0,
        val downloadMbps: Double? = null,
        val uploadMbps: Double? = null,

        // Meta (from cloudflare /meta)
        val ip: String? = null,
        val isp: String? = null,
        val server: String? = null,
        val city: String? = null,

        // Per-phase sample streams (for sparklines)
        val downloadSamples: List<Float> = emptyList(),
        val uploadSamples: List<Float> = emptyList(),

        val history: List<HistoryEntry> = emptyList(),
        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var testJob: Job? = null

    fun start() {
        if (_state.value.running) return
        val existingHistory = _state.value.history
        _state.value = UiState(running = true, phase = Phase.META, history = existingHistory)
        testJob = viewModelScope.launch { runTest() }
    }

    fun cancel() {
        testJob?.cancel()
        _state.value = _state.value.copy(running = false, phase = Phase.IDLE)
    }

    fun clearHistory() {
        _state.value = _state.value.copy(history = emptyList())
    }

    private suspend fun runTest() {
        try {
            withContext(Dispatchers.IO) {
                // 1) Meta
                setPhase(Phase.META, Unit.MBPS, GAUGE_DEFAULT_MAX)
                fetchMeta()
                ensureActive()

                // 2) Ping
                setPhase(Phase.PING, Unit.MS, 80.0)
                runPing()
                ensureActive()

                // 3) Download
                setPhase(Phase.DOWNLOAD, Unit.MBPS, GAUGE_DEFAULT_MAX)
                val dl = measureDownload()
                _state.value = _state.value.copy(downloadMbps = dl)
                ensureActive()

                // 4) Upload — cap gauge at ~90% of DL so upload looks honest
                setPhase(Phase.UPLOAD, Unit.MBPS, maxOf(100.0, dl * 0.9))
                val ul = measureUpload()
                _state.value = _state.value.copy(uploadMbps = ul)

                // 5) Done — settle gauge on download value
                val entry = HistoryEntry(
                    timestamp = System.currentTimeMillis(),
                    pingMs = _state.value.pingMs,
                    jitterMs = _state.value.jitterMs,
                    lossPct = _state.value.lossPct,
                    downloadMbps = dl,
                    uploadMbps = ul,
                    isp = _state.value.isp,
                    server = _state.value.server
                )
                _state.value = _state.value.copy(
                    phase = Phase.DONE,
                    running = false,
                    liveValue = dl,
                    liveUnit = Unit.MBPS,
                    gaugeMax = maxOf(100.0, dl * 1.15),
                    history = (listOf(entry) + _state.value.history).take(HISTORY_SIZE)
                )
            }
            telemetry.toolRun("speedtest", true)
        } catch (ce: CancellationException) {
            _state.value = _state.value.copy(running = false, phase = Phase.IDLE)
            throw ce
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                running = false,
                phase = Phase.IDLE,
                error = e.message ?: e::class.java.simpleName
            )
            telemetry.toolRun("speedtest", false)
        }
    }

    private fun setPhase(phase: Phase, unit: Unit, gaugeMax: Double) {
        _state.value = _state.value.copy(
            phase = phase,
            liveUnit = unit,
            liveValue = 0.0,
            gaugeMax = gaugeMax,
            error = null
        )
    }

    private fun fetchMeta() {
        runCatching {
            val conn = (URL(META_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
            }
            val body = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            val json = JSONObject(body)
            val colo = json.optString("colo", "")
            _state.value = _state.value.copy(
                ip = json.optString("clientIp").takeIf { it.isNotBlank() },
                isp = json.optString("asOrganization").takeIf { it.isNotBlank() },
                server = if (colo.isNotBlank()) "Cloudflare · $colo" else "Cloudflare",
                city = json.optString("city").takeIf { it.isNotBlank() }
                    ?: json.optString("country").takeIf { it.isNotBlank() }
            )
            conn.disconnect()
        }
    }

    private suspend fun runPing() {
        val rtts = mutableListOf<Double>()
        var successes = 0
        for (i in 0 until PING_ATTEMPTS) {
            coroutineContext.ensureActive()
            val rtt = tcpPing()
            if (rtt != null) {
                rtts += rtt
                successes++
                _state.value = _state.value.copy(liveValue = rtt)
            }
            delay(80)
        }
        _state.value = _state.value.copy(
            pingMs = if (rtts.isNotEmpty()) median(rtts) else null,
            jitterMs = if (rtts.size >= 2) pstdev(rtts) else 0.0,
            lossPct = 100.0 * (PING_ATTEMPTS - successes) / PING_ATTEMPTS,
            pingAttempts = PING_ATTEMPTS,
            pingSuccess = successes
        )
    }

    private fun tcpPing(): Double? {
        for ((host, port) in PING_HOSTS) {
            val started = System.nanoTime()
            val ok = runCatching {
                Socket().use { s ->
                    s.connect(InetSocketAddress(host, port), 2000)
                }
            }.isSuccess
            if (ok) return (System.nanoTime() - started) / 1_000_000.0
        }
        return null
    }

    private suspend fun measureDownload(): Double {
        val url = URL(DL_URL.format(DL_BYTES))
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", "ZeddiHubMobile/speedtest")
        }
        val buffer = ByteArray(64 * 1024)
        val samples = mutableListOf<Float>()
        var total = 0L
        var lastBytes = 0L
        val start = System.nanoTime()
        var last = start
        try {
            conn.inputStream.use { input ->
                while (true) {
                    coroutineContext.ensureActive()
                    val n = input.read(buffer)
                    if (n < 0) break
                    total += n
                    val now = System.nanoTime()
                    val dt = (now - last) / 1e9
                    if (dt >= 0.15) {
                        val inst = ((total - lastBytes) * 8.0) / (dt * 1_000_000.0)
                        samples += inst.toFloat()
                        _state.value = _state.value.copy(
                            liveValue = inst,
                            downloadSamples = samples.toList(),
                            gaugeMax = maxOf(_state.value.gaugeMax, inst * 1.1)
                        )
                        lastBytes = total
                        last = now
                    }
                    if ((now - start) / 1e9 > CAP_SEC) break
                }
            }
        } finally {
            runCatching { conn.disconnect() }
        }
        val durSec = (System.nanoTime() - start).coerceAtLeast(1L) / 1e9
        val avg = (total * 8.0) / (durSec * 1_000_000.0)
        return summarize(avg, samples.map { it.toDouble() })
    }

    private suspend fun measureUpload(): Double {
        val url = URL(UL_URL)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            doOutput = true
            connectTimeout = 10_000
            readTimeout = 15_000
            requestMethod = "POST"
            setFixedLengthStreamingMode(UL_BYTES.toLong())
            setRequestProperty("User-Agent", "ZeddiHubMobile/speedtest")
            setRequestProperty("Content-Type", "application/octet-stream")
            useCaches = false
        }
        val chunk = ByteArray(64 * 1024)
        val samples = mutableListOf<Float>()
        var total = 0L
        var lastBytes = 0L
        val start = System.nanoTime()
        var last = start
        try {
            conn.outputStream.use { out ->
                while (total < UL_BYTES) {
                    coroutineContext.ensureActive()
                    val remaining = UL_BYTES - total
                    val toWrite = if (remaining >= chunk.size) chunk.size else remaining.toInt()
                    out.write(chunk, 0, toWrite)
                    total += toWrite
                    val now = System.nanoTime()
                    val dt = (now - last) / 1e9
                    if (dt >= 0.15) {
                        val inst = ((total - lastBytes) * 8.0) / (dt * 1_000_000.0)
                        samples += inst.toFloat()
                        _state.value = _state.value.copy(
                            liveValue = inst,
                            uploadSamples = samples.toList(),
                            gaugeMax = maxOf(_state.value.gaugeMax, inst * 1.1)
                        )
                        lastBytes = total
                        last = now
                    }
                    if ((now - start) / 1e9 > CAP_SEC) break
                }
                out.flush()
            }
            runCatching { conn.responseCode }
        } finally {
            runCatching { conn.disconnect() }
        }
        val durSec = (System.nanoTime() - start).coerceAtLeast(1L) / 1e9
        val avg = (total * 8.0) / (durSec * 1_000_000.0)
        return summarize(avg, samples.map { it.toDouble() })
    }

    private fun summarize(avg: Double, samples: List<Double>): Double {
        if (samples.isEmpty()) return avg
        if (samples.size < 10) return samples.last()
        val sorted = samples.sorted()
        val p90 = sorted[(samples.size * 0.9).toInt().coerceAtMost(sorted.lastIndex)]
        return if (avg > 0) sqrt(avg * p90) else p90
    }

    private fun median(xs: List<Double>): Double {
        val s = xs.sorted()
        val n = s.size
        return if (n % 2 == 1) s[n / 2] else (s[n / 2 - 1] + s[n / 2]) / 2.0
    }

    private fun pstdev(xs: List<Double>): Double {
        val m = xs.average()
        return sqrt(xs.sumOf { (it - m) * (it - m) } / xs.size)
    }

    companion object {
        private const val META_URL = "https://speed.cloudflare.com/meta"
        private const val DL_URL = "https://speed.cloudflare.com/__down?bytes=%d"
        private const val UL_URL = "https://speed.cloudflare.com/__up"

        // 25 MB DL + 10 MB UL, cap at 10 s — matches desktop.
        private const val DL_BYTES = 25 * 1024 * 1024
        private const val UL_BYTES = 10 * 1024 * 1024
        private const val CAP_SEC = 10.0

        private const val PING_ATTEMPTS = 12
        private val PING_HOSTS = listOf(
            "cloudflare.com" to 443,
            "google.com" to 443,
            "zeddihub.eu" to 443
        )

        private const val HISTORY_SIZE = 10
        private const val GAUGE_DEFAULT_MAX = 500.0
    }
}
