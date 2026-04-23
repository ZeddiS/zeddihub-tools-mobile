package com.zeddihub.mobile.ui.tools

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
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
 * Continuous tone generator for hearing tests, speaker measurements and
 * the "speaker cleaner" sweep that reuses this same engine.
 *
 * Implementation: one AudioTrack in STREAM-mode, a background coroutine
 * fills the buffer with samples computed from a phase accumulator so the
 * waveform stays continuous when frequency is retuned live.
 */
@HiltViewModel
class FrequencyGeneratorViewModel @Inject constructor() : ViewModel() {

    enum class Waveform { SINE, SQUARE, TRIANGLE, SAWTOOTH }

    data class UiState(
        val frequencyHz: Float = 440f,
        val waveform: Waveform = Waveform.SINE,
        /** 0.0 – 1.0 */
        val volume: Float = 0.3f,
        val playing: Boolean = false
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var track: AudioTrack? = null
    private var job: Job? = null
    private var phase: Double = 0.0

    private val sampleRate = 44_100

    fun setFrequency(hz: Float) {
        _state.value = _state.value.copy(frequencyHz = hz.coerceIn(20f, 20_000f))
    }

    fun setWaveform(wf: Waveform) {
        _state.value = _state.value.copy(waveform = wf)
    }

    fun setVolume(v: Float) {
        val clamped = v.coerceIn(0f, 1f)
        _state.value = _state.value.copy(volume = clamped)
        track?.setVolume(clamped)
    }

    fun toggle() {
        if (_state.value.playing) stop() else start()
    }

    fun start() {
        if (_state.value.playing) return
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
        t.setVolume(_state.value.volume)
        t.play()
        track = t
        _state.value = _state.value.copy(playing = true)

        job = viewModelScope.launch(Dispatchers.Default) {
            val chunk = ShortArray(2048)
            while (_state.value.playing) {
                val freq = _state.value.frequencyHz.toDouble()
                val wf = _state.value.waveform
                val step = 2.0 * PI * freq / sampleRate
                for (i in chunk.indices) {
                    val v = when (wf) {
                        Waveform.SINE -> sin(phase)
                        Waveform.SQUARE -> if (sin(phase) >= 0.0) 1.0 else -1.0
                        Waveform.TRIANGLE -> {
                            // map phase (0..2PI) to triangle (-1..1)
                            val p = (phase % (2 * PI)) / PI // 0..2
                            if (p < 1.0) -1.0 + 2.0 * p else 3.0 - 2.0 * p
                        }
                        Waveform.SAWTOOTH -> {
                            val p = (phase % (2 * PI)) / PI // 0..2
                            p - 1.0
                        }
                    }
                    chunk[i] = (v * 0.8 * Short.MAX_VALUE).toInt().toShort()
                    phase += step
                    if (phase > 2 * PI) phase -= 2 * PI
                }
                runCatching { t.write(chunk, 0, chunk.size) }
            }
        }
    }

    fun stop() {
        _state.value = _state.value.copy(playing = false)
        job?.cancel()
        job = null
        runCatching { track?.pause(); track?.flush(); track?.stop() }
        runCatching { track?.release() }
        track = null
        phase = 0.0
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }

    companion object {
        // Presets (Hz) — useful hearing/speaker test points.
        val PRESETS: List<Pair<String, Float>> = listOf(
            "20 Hz" to 20f,
            "60 Hz" to 60f,
            "100 Hz" to 100f,
            "440 Hz" to 440f,
            "1 kHz" to 1_000f,
            "4 kHz" to 4_000f,
            "8 kHz" to 8_000f,
            "12 kHz" to 12_000f,
            "15 kHz" to 15_000f,
            "18 kHz" to 18_000f
        )

        @Suppress("unused")
        val STREAM_MUSIC = AudioManager.STREAM_MUSIC
    }
}
