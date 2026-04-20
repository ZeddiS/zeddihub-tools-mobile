package com.zeddihub.mobile.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject

data class IpLookupResult(
    val ip: String,
    val country: String?,
    val countryCode: String?,
    val region: String?,
    val city: String?,
    val isp: String?,
    val org: String?,
    val timezone: String?
)

data class IpLookupUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val result: IpLookupResult? = null,
    val error: String? = null
)

@HiltViewModel
class IpLookupViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(IpLookupUiState())
    val state: StateFlow<IpLookupUiState> = _state.asStateFlow()

    fun onQueryChange(value: String) {
        _state.value = _state.value.copy(query = value)
    }

    fun lookup() {
        val q = _state.value.query.trim()
        if (q.isEmpty()) return
        _state.value = _state.value.copy(isLoading = true, error = null, result = null)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val encoded = URLEncoder.encode(q, "UTF-8")
                    val url = URL("https://ip-api.com/json/$encoded?fields=status,message,country,countryCode,regionName,city,isp,org,timezone,query")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 4000
                    conn.readTimeout = 4000
                    conn.requestMethod = "GET"
                    conn.inputStream.bufferedReader().use { it.readText() }
                }
            }
            result.onSuccess { body ->
                val json = runCatching { JSONObject(body) }.getOrNull()
                if (json != null && json.optString("status") == "success") {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        result = IpLookupResult(
                            ip = json.optString("query"),
                            country = json.optString("country").takeIf { it.isNotEmpty() },
                            countryCode = json.optString("countryCode").takeIf { it.isNotEmpty() },
                            region = json.optString("regionName").takeIf { it.isNotEmpty() },
                            city = json.optString("city").takeIf { it.isNotEmpty() },
                            isp = json.optString("isp").takeIf { it.isNotEmpty() },
                            org = json.optString("org").takeIf { it.isNotEmpty() },
                            timezone = json.optString("timezone").takeIf { it.isNotEmpty() }
                        )
                    )
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = "fail")
                }
            }.onFailure {
                _state.value = _state.value.copy(isLoading = false, error = it.message)
            }
        }
    }
}
