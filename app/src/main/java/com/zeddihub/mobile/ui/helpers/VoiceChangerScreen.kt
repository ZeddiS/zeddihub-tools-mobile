package com.zeddihub.mobile.ui.helpers

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.zeddihub.mobile.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

/**
 * Voice Changer — record a clip and play it back at a different
 * pitch / speed.
 *
 * Pitch shifting in 200 lines without external libraries means we
 * trade fidelity for simplicity: we change playback rate, which
 * shifts pitch and tempo together (chipmunk effect at 1.5×, bass
 * monster at 0.6×). True pitch-preserving shift would need a phase
 * vocoder or WSOLA — both worth the extra LOC if v1.0.0 expands
 * this into a "voice studio" tool, but for the first iteration the
 * pitch+speed coupling is exactly what people expect from a
 * "make me sound like a chipmunk" toy.
 *
 * The recorded buffer lives in RAM (no file I/O) — keeps the screen
 * private (no leftover wav on /sdcard if the user closes the app)
 * and dodges the WRITE_EXTERNAL_STORAGE permission entirely. A
 * 30-second cap is enforced because 30s × 44.1kHz × 16-bit ≈ 2.6 MB,
 * which is fine; longer clips would justify a streaming pipeline.
 */
@Composable
fun VoiceChangerScreen(padding: PaddingValues) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val granted = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted.value = it
    }

    var recording by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var pcm by remember { mutableStateOf<ShortArray?>(null) }
    var pitch by remember { mutableStateOf(1.0f) }
    var recordJob by remember { mutableStateOf<Job?>(null) }
    var playJob by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            recordJob?.cancel()
            playJob?.cancel()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(R.string.vc_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(stringResource(R.string.vc_body))

        if (!granted.value) {
            Button(onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) }) {
                Text(stringResource(R.string.vc_grant))
            }
            return@Column
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = !recording && !playing,
                onClick = {
                    recording = true
                    recordJob = scope.launch(Dispatchers.Default) {
                        pcm = recordPcm(maxMs = 30_000)
                        recording = false
                    }
                }
            ) { Text(stringResource(R.string.vc_record)) }
            OutlinedButton(
                enabled = recording,
                onClick = {
                    recordJob?.cancel()
                    recording = false
                }
            ) { Text(stringResource(R.string.vc_stop)) }
        }

        if (pcm != null) {
            Text(stringResource(R.string.vc_pitch) + " ${"%.2f".format(pitch)}×")
            Slider(
                value = pitch,
                onValueChange = { pitch = it },
                valueRange = 0.5f..2.0f,
                steps = 14, // 0.1× increments
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = !recording && !playing,
                    onClick = {
                        val data = pcm ?: return@Button
                        playing = true
                        playJob = scope.launch(Dispatchers.Default) {
                            playPcmAtRate(data, pitch)
                            playing = false
                        }
                    }
                ) { Text(stringResource(R.string.vc_play)) }
                OutlinedButton(
                    enabled = playing,
                    onClick = {
                        playJob?.cancel()
                        playing = false
                    }
                ) { Text(stringResource(R.string.vc_stop_play)) }
            }
            // Quick presets — chipmunk / robot / bass — these are
            // pure UX shortcuts; the slider can hit the same values.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pitch = 1.7f }) { Text("Chipmunk") }
                OutlinedButton(onClick = { pitch = 1.0f }) { Text("Original") }
                OutlinedButton(onClick = { pitch = 0.6f }) { Text("Deep") }
            }
        }
    }
}

private const val SAMPLE_RATE = 44100

private suspend fun recordPcm(maxMs: Long): ShortArray? {
    val minBuf = AudioRecord.getMinBufferSize(
        SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
    )
    val rec = try {
        AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuf * 2)
    } catch (_: SecurityException) { return null }
    if (rec.state != AudioRecord.STATE_INITIALIZED) { rec.release(); return null }
    rec.startRecording()
    val out = ByteArrayOutputStream()
    val buf = ShortArray(2048)
    val deadline = System.currentTimeMillis() + maxMs
    try {
        while (System.currentTimeMillis() < deadline &&
            kotlinx.coroutines.currentCoroutineContext()[Job]?.isActive == true) {
            val n = rec.read(buf, 0, buf.size)
            if (n <= 0) continue
            for (i in 0 until n) {
                val v = buf[i].toInt()
                out.write(v and 0xff)
                out.write((v ushr 8) and 0xff)
            }
        }
    } finally {
        rec.runCatching { stop(); release() }
    }
    val bytes = out.toByteArray()
    val shorts = ShortArray(bytes.size / 2)
    for (i in shorts.indices) {
        shorts[i] = ((bytes[i * 2].toInt() and 0xff) or
            ((bytes[i * 2 + 1].toInt() and 0xff) shl 8)).toShort()
    }
    return shorts
}

private suspend fun playPcmAtRate(data: ShortArray, rate: Float) {
    val minBuf = AudioTrack.getMinBufferSize(
        SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
    )
    val format = AudioFormat.Builder()
        .setSampleRate(SAMPLE_RATE)
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .build()
    val track = AudioTrack.Builder()
        .setAudioFormat(format)
        .setBufferSizeInBytes(minBuf * 2)
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()
    // Set playback rate — AudioTrack handles native resampling, which
    // gives us pitch+speed shift "for free". setPlaybackRate accepts
    // a value in Hz, expressed relative to the native rate.
    val target = (SAMPLE_RATE * rate).roundToInt().coerceIn(4000, SAMPLE_RATE * 2)
    runCatching { track.playbackRate = target }
    track.play()
    val chunk = 4096
    var i = 0
    try {
        while (i < data.size &&
            kotlinx.coroutines.currentCoroutineContext()[Job]?.isActive == true) {
            val end = (i + chunk).coerceAtMost(data.size)
            track.write(data, i, end - i)
            i = end
        }
    } finally {
        runCatching { track.stop(); track.release() }
    }
}
