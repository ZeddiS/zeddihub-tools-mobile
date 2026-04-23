package com.zeddihub.mobile.ui.tools

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.sin

/**
 * Plays a low-frequency cleaning program through the loudspeaker.
 *
 *  - [Preset.WATER]  165 Hz pulsated tone (Apple Watch inspired).
 *  - [Preset.DUST]   sweep 40 – 200 Hz, rising.
 *  - [Preset.CUSTOM] constant tone at [UiState.customHz].
 *
 * Caller picks a [UiState.durationSec] between 10 – 120 s.
 */
@HiltViewModel
class SpeakerCleanerViewModel @Inject constructor() : ViewModel() {

    enum class Preset { WATER, DUST, CUSTOM }

    data class UiState(
        val preset: Preset = Preset.WATER,
        val customHz: Int = 165,
        val durationSec: Int = 30,
        val running: Boolean = false,
        /** Hz currently being played (for UI readout). */
        val currentHz: Int = 0,
        /** 0f..1f progress. */
        val progress: Float = 0f
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var job: Job? = null
    private var track: AudioTrack? = null
    private val sampleRate = 44_100

    fun setPreset(p: Preset) { _state.value = _state.value.copy(preset = p) }
    fun setCustomHz(hz: Int) { _state.value = _state.value.copy(customHz = hz.coerceIn(20, 500)) }
    fun setDuration(sec: Int) { _state.value = _state.value.copy(durationSec = sec.coerceIn(5, 180)) }

    fun toggle() { if (_state.value.running) stop() else start() }

    fun start() {
        if (_state.value.running) return
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        val t = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(minBuf * 2)
            .build()
        t.setVolume(1.0f)
        t.play()
        track = t
        _state.value = _state.value.copy(running = true, progress = 0f)

        job = viewModelScope.launch(Dispatchers.Default) {
            val preset = _state.value.preset
            val durationMs = _state.value.durationSec * 1000L
            val chunk = ShortArray(2048)
            var phase = 0.0
            val startMs = System.currentTimeMillis()

            while (_state.value.running) {
                val elapsed = System.currentTimeMillis() - startMs
                if (elapsed >= durationMs) break
                val progress = (elapsed.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

                val (freq, amplitude) = when (preset) {
                    Preset.WATER -> {
                        // Pulsate on/off every 0.5s for that Apple-style hum.
                        val phaseMs = elapsed % 1000L
                        val amp = if (phaseMs < 500L) 1.0 else 0.0
                        165.0 to amp
                    }
                    Preset.DUST -> {
                        // Linear sweep 40Hz → 200Hz across the whole duration.
                        val f = 40.0 + (200.0 - 40.0) * progress
                        f to 1.0
                    }
                    Preset.CUSTOM -> _state.value.customHz.toDouble() to 1.0
                }

                val step = 2.0 * PI * freq / sampleRate
                for (i in chunk.indices) {
                    val v = sin(phase) * 0.85 * amplitude
                    chunk[i] = (v * Short.MAX_VALUE).toInt().toShort()
                    phase += step
                    if (phase > 2 * PI) phase -= 2 * PI
                }
                runCatching { t.write(chunk, 0, chunk.size) }

                _state.value = _state.value.copy(
                    currentHz = freq.toInt(),
                    progress = progress
                )
            }
            stop()
        }
    }

    fun stop() {
        _state.value = _state.value.copy(running = false, currentHz = 0, progress = 0f)
        job?.cancel()
        job = null
        runCatching { track?.pause(); track?.flush(); track?.stop() }
        runCatching { track?.release() }
        track = null
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}
