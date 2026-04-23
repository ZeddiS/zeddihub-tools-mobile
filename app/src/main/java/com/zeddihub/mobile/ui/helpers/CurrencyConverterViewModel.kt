package com.zeddihub.mobile.ui.helpers

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import javax.inject.Inject

/**
 * Pulls the daily ČNB central-bank exchange rate table (formerly
 * denni_kurz.txt) and caches it to [Context.filesDir] so the screen still
 * works offline. The public URL is not rate-limited for polite use.
 *
 * Format example (plain text, semicolon-separated):
 *   14.06.2026 #111
 *   země|měna|množství|kód|kurz
 *   EMU|euro|1|EUR|24,285
 *   USA|dolar|1|USD|22,614
 */
@HiltViewModel
class CurrencyConverterViewModel @Inject constructor(
    @ApplicationContext private val appCtx: Context
) : ViewModel() {

    data class Rates(
        val date: String,
        /** amount of CZK per 1 unit of the currency code (USD, EUR, JPY…).
         *  Always normalized so the "quantity" column is divided out. */
        val rates: Map<String, Double>
    )

    data class UiState(
        val rates: Rates? = null,
        val loading: Boolean = false,
        val offline: Boolean = false,
        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val cacheFile get() = java.io.File(appCtx.filesDir, "cnb_rates.txt")

    fun loadInitial() {
        if (_state.value.rates != null) return
        viewModelScope.launch {
            // Try network first; fall back to cache on error.
            val network = runCatching { withContext(Dispatchers.IO) { fetchFromCnb() } }.getOrNull()
            if (network != null) {
                runCatching { cacheFile.writeText(network) }
                _state.value = UiState(rates = parse(network), offline = false)
            } else {
                val cached = runCatching { cacheFile.readText() }.getOrNull()
                if (cached != null) {
                    _state.value = UiState(rates = parse(cached), offline = true)
                } else {
                    _state.value = UiState(rates = fallbackRates(), offline = true)
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val network = runCatching { withContext(Dispatchers.IO) { fetchFromCnb() } }
            if (network.isSuccess) {
                val txt = network.getOrThrow()
                runCatching { cacheFile.writeText(txt) }
                _state.value = UiState(rates = parse(txt), offline = false, loading = false)
            } else {
                _state.value = _state.value.copy(
                    loading = false,
                    error = network.exceptionOrNull()?.message ?: "network"
                )
            }
        }
    }

    private fun fetchFromCnb(): String {
        val url = URL("https://www.cnb.cz/cs/financni-trhy/devizovy-trh/kurzy-devizoveho-trhu/kurzy-devizoveho-trhu/denni_kurz.txt")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("User-Agent", "ZeddiHub-Mobile")
        try {
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(text: String): Rates {
        val lines = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
        if (lines.isEmpty()) return Rates(LocalDate.now().toString(), emptyMap())

        // Line 0: "14.06.2026 #111"
        val dateHeader = lines[0].substringBefore(' ')
        // Line 1: column header. Data starts at line 2.
        val rates = mutableMapOf<String, Double>()
        for (i in 2 until lines.size) {
            val parts = lines[i].split('|')
            if (parts.size < 5) continue
            val qty = parts[2].toIntOrNull() ?: continue
            val code = parts[3].trim()
            val rate = parts[4].replace(',', '.').toDoubleOrNull() ?: continue
            if (qty > 0) rates[code] = rate / qty
        }
        rates["CZK"] = 1.0
        return Rates(date = dateHeader, rates = rates)
    }

    /** Last-resort static rates so the UI at least shows something on
     *  first run when there is no cache and no connectivity. Values are
     *  intentionally round; the "(offline)" hint makes accuracy obvious. */
    private fun fallbackRates(): Rates = Rates(
        date = "fallback",
        rates = mapOf(
            "CZK" to 1.0,
            "EUR" to 24.5,
            "USD" to 22.6,
            "GBP" to 28.4,
            "PLN" to 5.6,
            "JPY" to 0.15
        )
    )
}
