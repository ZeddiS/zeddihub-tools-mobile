package com.zeddihub.mobile.ui.tools

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.log10
import kotlin.math.sqrt

@HiltViewModel
class DecibelMeterViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    data class UiState(
        val running: Boolean = false,
        val currentDb: Double = 0.0,
        val minDb: Double? = null,
        val maxDb: Double? = null,
        val avgDb: Double? = null,
        val history: List<Double> = emptyList(),
        val calibrationOffset: Double = defaultCalibration(),
        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var job: Job? = null
    private val historyBuffer = ArrayDeque<Double>()
    private var sumDb = 0.0
    private var sampleCount = 0

    fun setCalibration(offset: Double) {
        _state.value = _state.value.copy(calibrationOffset = offset)
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (_state.value.running) return
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            _state.value = _state.value.copy(error = "no permission")
            return
        }
        historyBuffer.clear()
        sumDb = 0.0
        sampleCount = 0
        _state.value = _state.value.copy(
            running = true,
            error = null,
            minDb = null,
            maxDb = null,
            avgDb = null,
            history = emptyList()
        )

        job = viewModelScope.launch(Dispatchers.IO) {
            val sampleRate = 44_100
            val minBuf = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)
            val recorder = runCatching {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuf * 2
                )
            }.getOrNull()
            if (recorder == null || recorder.state != AudioRecord.STATE_INITIALIZED) {
                _state.value = _state.value.copy(running = false, error = "audio init failed")
                return@launch
            }
            val buf = ShortArray(minBuf)
            runCatching { recorder.startRecording() }.onFailure {
                _state.value = _state.value.copy(running = false, error = it.message)
                recorder.release()
                return@launch
            }
            try {
                while (_state.value.running) {
                    val n = recorder.read(buf, 0, buf.size)
                    if (n <= 0) continue
                    var sum = 0.0
                    for (i in 0 until n) {
                        val v = buf[i].toDouble()
                        sum += v * v
                    }
                    val rms = sqrt(sum / n)
                    val dbfs = if (rms > 0) 20.0 * log10(rms / 32768.0) else -160.0
                    val dbSpl = (dbfs + _state.value.calibrationOffset).coerceIn(0.0, 150.0)
                    historyBuffer.addLast(dbSpl)
                    while (historyBuffer.size > 120) historyBuffer.removeFirst()
                    sumDb += dbSpl
                    sampleCount++
                    val cur = _state.value
                    _state.value = cur.copy(
                        currentDb = dbSpl,
                        history = historyBuffer.toList(),
                        minDb = cur.minDb?.let { minOf(it, dbSpl) } ?: dbSpl,
                        maxDb = cur.maxDb?.let { maxOf(it, dbSpl) } ?: dbSpl,
                        avgDb = if (sampleCount > 0) sumDb / sampleCount else null
                    )
                }
            } finally {
                runCatching { recorder.stop() }
                recorder.release()
            }
        }
    }

    fun stop() {
        _state.value = _state.value.copy(running = false)
        job?.cancel()
        job = null
    }

    fun reset() {
        historyBuffer.clear()
        sumDb = 0.0
        sampleCount = 0
        _state.value = _state.value.copy(
            minDb = null,
            maxDb = null,
            avgDb = null,
            history = emptyList()
        )
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }

    companion object {
        fun defaultCalibration(): Double {
            val model = "${Build.MANUFACTURER} ${Build.MODEL}".lowercase()
            return when {
                model.contains("pixel") -> 105.0
                model.contains("samsung") -> 100.0
                model.contains("xiaomi") || model.contains("redmi") -> 98.0
                model.contains("oneplus") -> 102.0
                else -> 100.0
            }
        }
    }
}
