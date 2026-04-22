package com.zeddihub.mobile.ui.tools

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.log10
import kotlin.math.pow

@HiltViewModel
class WifiScannerViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    data class Network(
        val ssid: String,
        val bssid: String,
        val rssi: Int,
        val frequencyMhz: Int,
        val channel: Int,
        val band: Band,
        val security: String,
        val distanceMeters: Double,
        val history: List<Int>
    )

    enum class Band { GHZ_2_4, GHZ_5, GHZ_6, UNKNOWN }

    data class UiState(
        val scanning: Boolean = false,
        val lastScanMillis: Long = 0L,
        val networks: List<Network> = emptyList(),
        val selectedBssid: String? = null,
        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val historyMap = linkedMapOf<String, ArrayDeque<Int>>()
    private var receiver: BroadcastReceiver? = null
    private var registered = false

    @SuppressLint("MissingPermission")
    fun start() {
        if (registered) return
        val wm = appContext.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wm == null) {
            _state.value = _state.value.copy(error = "WiFi unavailable")
            return
        }
        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                ingest(wm)
            }
        }
        runCatching {
            appContext.registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
            registered = true
        }
        ingest(wm)
        viewModelScope.launch {
            while (registered) {
                _state.value = _state.value.copy(scanning = true)
                runCatching { triggerScan(wm) }
                delay(8_000)
                _state.value = _state.value.copy(scanning = false)
                delay(2_000)
            }
        }
    }

    fun stop() {
        if (!registered) return
        runCatching { appContext.unregisterReceiver(receiver) }
        receiver = null
        registered = false
    }

    fun select(bssid: String?) {
        _state.value = _state.value.copy(selectedBssid = bssid)
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }

    @SuppressLint("MissingPermission")
    private fun ingest(wm: WifiManager) {
        val results = runCatching { wm.scanResults }.getOrNull() ?: return
        val nets = results.map { sr ->
            val ssid = readSsid(sr)
            val key = sr.BSSID ?: ssid ?: ""
            val queue = historyMap.getOrPut(key) { ArrayDeque() }
            queue.addLast(sr.level)
            while (queue.size > 30) queue.removeFirst()
            val ch = frequencyToChannel(sr.frequency)
            Network(
                ssid = ssid?.takeIf { it.isNotBlank() } ?: "(hidden)",
                bssid = sr.BSSID ?: "",
                rssi = sr.level,
                frequencyMhz = sr.frequency,
                channel = ch,
                band = bandOf(sr.frequency),
                security = capabilitiesToSecurity(sr.capabilities),
                distanceMeters = estimateDistanceMeters(sr.level, sr.frequency),
                history = queue.toList()
            )
        }.sortedByDescending { it.rssi }
        _state.value = _state.value.copy(
            networks = nets,
            lastScanMillis = System.currentTimeMillis()
        )
    }

    private fun bandOf(freq: Int): Band = when {
        freq in 2400..2500 -> Band.GHZ_2_4
        freq in 4900..5900 -> Band.GHZ_5
        freq in 5925..7125 -> Band.GHZ_6
        else -> Band.UNKNOWN
    }

    private fun frequencyToChannel(freq: Int): Int = when {
        freq == 2484 -> 14
        freq in 2412..2472 -> (freq - 2412) / 5 + 1
        freq in 5160..5885 -> (freq - 5000) / 5
        freq in 5955..7115 -> (freq - 5950) / 5
        else -> 0
    }

    private fun capabilitiesToSecurity(cap: String?): String {
        val c = cap ?: return "?"
        return when {
            c.contains("WPA3") -> "WPA3"
            c.contains("WPA2") -> "WPA2"
            c.contains("WPA") -> "WPA"
            c.contains("WEP") -> "WEP"
            c.contains("OWE") -> "OWE"
            c.contains("SAE") -> "SAE"
            else -> "Open"
        }
    }

    private fun estimateDistanceMeters(rssi: Int, freqMhz: Int): Double {
        if (freqMhz <= 0) return 0.0
        val exp = (27.55 - 20.0 * log10(freqMhz.toDouble()) + kotlin.math.abs(rssi)) / 20.0
        return 10.0.pow(exp)
    }

    @Suppress("DEPRECATION")
    private fun triggerScan(wm: WifiManager) {
        // WifiManager.startScan() is deprecated since API 28 but no direct
        // replacement exists for foreground on-demand scan triggers.
        wm.startScan()
    }

    private fun readSsid(sr: ScanResult): String? {
        // ScanResult.SSID (String) is deprecated since API 33 in favour of
        // ScanResult.wifiSsid (WifiSsid). Use the modern API when available
        // and fall back to the legacy field on older devices.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val modern = runCatching { sr.wifiSsid?.toString()?.trim('"') }.getOrNull()
            if (!modern.isNullOrEmpty()) return modern
        }
        @Suppress("DEPRECATION")
        return sr.SSID
    }
}
