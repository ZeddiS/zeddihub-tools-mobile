package com.zeddihub.mobile.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeddihub.mobile.data.remote.dto.WifiMapEntryDto
import com.zeddihub.mobile.data.remote.dto.WifiMapSubmitRequest
import com.zeddihub.mobile.data.repository.WifiMapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WifiMapViewModel @Inject constructor(
    private val repo: WifiMapRepository
) : ViewModel() {

    data class UiState(
        val entries: List<WifiMapEntryDto> = emptyList(),
        val isLoading: Boolean = false,
        val errorRes: Int? = null,
        val userLat: Double? = null,
        val userLon: Double? = null,
        val submitting: Boolean = false,
        val submitError: String? = null,
        val submitSuccess: Boolean = false
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun setLocation(lat: Double, lon: Double) {
        _state.value = _state.value.copy(userLat = lat, userLon = lon)
        if (_state.value.entries.isEmpty() && !_state.value.isLoading) {
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorRes = null)
            val s = _state.value
            val res = repo.list(lat = s.userLat, lon = s.userLon, radiusKm = null)
            _state.value = res.fold(
                onSuccess = { list -> _state.value.copy(isLoading = false, entries = list) },
                onFailure = { _state.value.copy(isLoading = false, errorRes = com.zeddihub.mobile.R.string.wifi_map_error) }
            )
        }
    }

    fun submit(
        ssid: String,
        password: String?,
        isOpen: Boolean,
        venue: String?,
        note: String?
    ) {
        val s = _state.value
        val lat = s.userLat
        val lon = s.userLon
        if (lat == null || lon == null) {
            _state.value = s.copy(submitError = "gps")
            return
        }
        viewModelScope.launch {
            _state.value = s.copy(submitting = true, submitError = null, submitSuccess = false)
            val req = WifiMapSubmitRequest(
                ssid = ssid.trim(),
                password = password?.takeIf { it.isNotBlank() && !isOpen },
                isOpen = isOpen,
                security = if (isOpen) "open" else "wpa",
                lat = lat,
                lon = lon,
                venue = venue?.takeIf { it.isNotBlank() },
                note = note?.takeIf { it.isNotBlank() }
            )
            val res = repo.submit(req)
            _state.value = res.fold(
                onSuccess = { resp ->
                    if (resp.ok) {
                        _state.value.copy(submitting = false, submitSuccess = true)
                    } else {
                        _state.value.copy(submitting = false, submitError = resp.message ?: "error")
                    }
                },
                onFailure = { _state.value.copy(submitting = false, submitError = "error") }
            )
            if (_state.value.submitSuccess) refresh()
        }
    }

    fun clearSubmitFlags() {
        _state.value = _state.value.copy(submitError = null, submitSuccess = false)
    }

    /**
     * Haversine distance in meters between user location and a target coordinate.
     * Returns null if we don't know where the user is.
     */
    fun distanceToUserMeters(targetLat: Double, targetLon: Double): Double? {
        val lat = _state.value.userLat ?: return null
        val lon = _state.value.userLon ?: return null
        return haversineMeters(lat, lon, targetLat, targetLon)
    }

    companion object {
        const val GEOFENCE_METERS = 500.0

        fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val r = 6371000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = kotlin.math.sin(dLat / 2).let { it * it } +
                kotlin.math.cos(Math.toRadians(lat1)) *
                kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2).let { it * it }
            val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
            return r * c
        }
    }
}
