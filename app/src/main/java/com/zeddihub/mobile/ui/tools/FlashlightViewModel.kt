package com.zeddihub.mobile.ui.tools

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class FlashlightViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    enum class Mode { OFF, STEADY, STROBE, SOS }

    data class UiState(
        val mode: Mode = Mode.OFF,
        val strobeHz: Float = 4f,
        val intensity: Int = 1,
        val maxIntensity: Int = 1,
        val supportsIntensity: Boolean = false,
        val hasTorch: Boolean = true,
        val error: String? = null
    )

    private val _state = MutableStateFlow(detectCapabilities())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val cm: CameraManager? =
        appContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    private var cameraId: String? = null
    private var job: Job? = null

    init {
        cameraId = runCatching {
            cm?.cameraIdList?.firstOrNull { id ->
                cm.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
    }

    fun setMode(mode: Mode) {
        job?.cancel()
        job = null
        _state.value = _state.value.copy(mode = mode, error = null)
        when (mode) {
            Mode.OFF -> setTorch(false)
            Mode.STEADY -> setTorch(true)
            Mode.STROBE -> startStrobe()
            Mode.SOS -> startSos()
        }
    }

    fun setStrobeHz(hz: Float) {
        _state.value = _state.value.copy(strobeHz = hz.coerceIn(0.5f, 15f))
        if (_state.value.mode == Mode.STROBE) {
            job?.cancel()
            startStrobe()
        }
    }

    fun setIntensity(level: Int) {
        _state.value = _state.value.copy(intensity = level.coerceAtLeast(1))
        if (_state.value.mode == Mode.STEADY) setTorch(true)
    }

    private fun setTorch(on: Boolean) {
        val id = cameraId ?: return
        val m = cm ?: return
        runCatching {
            if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && _state.value.supportsIntensity
            ) {
                m.turnOnTorchWithStrengthLevel(id, _state.value.intensity)
            } else {
                m.setTorchMode(id, on)
            }
        }.onFailure { e ->
            _state.value = _state.value.copy(error = e.message)
        }
    }

    private fun startStrobe() {
        job = viewModelScope.launch(Dispatchers.IO) {
            val id = cameraId ?: return@launch
            val m = cm ?: return@launch
            var on = false
            while (_state.value.mode == Mode.STROBE) {
                on = !on
                runCatching { m.setTorchMode(id, on) }
                val periodMs = (1000f / (_state.value.strobeHz * 2f)).toLong().coerceAtLeast(30L)
                delay(periodMs)
            }
            runCatching { m.setTorchMode(id, false) }
        }
    }

    private fun startSos() {
        val dot = 200L
        val dash = 600L
        val gap = 200L
        val letterGap = 600L
        val pattern = listOf(dot, dot, dot, dash, dash, dash, dot, dot, dot)
        val m = cm ?: return
        val id = cameraId ?: return
        job = viewModelScope.launch(Dispatchers.IO) {
            while (_state.value.mode == Mode.SOS) {
                for ((i, d) in pattern.withIndex()) {
                    if (_state.value.mode != Mode.SOS) break
                    runCatching { m.setTorchMode(id, true) }
                    delay(d)
                    runCatching { m.setTorchMode(id, false) }
                    delay(if (i == 2 || i == 5) letterGap else gap)
                }
                delay(1200)
            }
            runCatching { m.setTorchMode(id, false) }
        }
    }

    override fun onCleared() {
        job?.cancel()
        val id = cameraId
        val m = cm
        if (id != null && m != null) runCatching { m.setTorchMode(id, false) }
        super.onCleared()
    }

    private fun detectCapabilities(): UiState {
        val m = appContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        val id = runCatching {
            m?.cameraIdList?.firstOrNull { cid ->
                m.getCameraCharacteristics(cid)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
        if (id == null || m == null) return UiState(hasTorch = false)
        var maxLevel = 1
        var supports = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                val ch = m.getCameraCharacteristics(id)
                val max = ch.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL)
                if (max != null && max > 1) {
                    maxLevel = max
                    supports = true
                }
            }
        }
        return UiState(maxIntensity = maxLevel, supportsIntensity = supports, intensity = maxLevel)
    }
}
