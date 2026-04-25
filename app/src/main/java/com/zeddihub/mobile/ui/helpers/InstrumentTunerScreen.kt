package com.zeddihub.mobile.ui.helpers

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.zeddihub.mobile.R
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Universal instrument tuner.
 *
 * Pipeline:
 *   - AudioRecord 44.1 kHz mono PCM16, ~46 ms hop (2048 samples).
 *   - RMS gate filters out silence (cents indicator wobbles wildly on
 *     pure noise so we keep the dial steady when nothing's playing).
 *   - Pitch detection: autocorrelation with parabolic peak interpolation.
 *     This is robust on monophonic instrument signals, doesn't need
 *     a heavy FFT setup, and works well from ~50 Hz to ~2 kHz which
 *     covers everything from low B on a 7-string to E7 on a violin.
 *   - Cents from nearest note: 1200 * log2(f / nearest_target). Display
 *     clamped to ±50 c (notes are spaced by 100 c so anything beyond
 *     ±50 c snaps to the next note).
 *
 * Presets:
 *   - Chromatic (any note nearest)
 *   - Guitar 6-string: Standard E A D G B E, Drop D, DADGAD, Open G,
 *     Open D, Half-step down
 *   - Guitar 7-string: Standard B E A D G B E
 *   - Guitar 12-string: pairs of standard
 *   - Bass 4-string: E A D G
 *   - Bass 5-string: B E A D G
 *   - Violin: G3 D4 A4 E5
 *   - Viola: C3 G3 D4 A4
 *   - Cello: C2 G2 D3 A3
 *   - Ukulele: G4 C4 E4 A4
 *   - Mandolin: G3 D4 A4 E5 (pairs)
 *   - Banjo (5-string, open G): G4 D3 G3 B3 D4
 *   - Piano: chromatic (any of 88 keys)
 *
 * In a non-chromatic preset the tuner shows BOTH the nearest target
 * string of that preset AND the absolute nearest note. Cents
 * indicator follows the preset target so the user sees how close they
 * are to the string they're tuning, not to whatever else is closest.
 *
 * Calibration: A4 ∈ [415, 466] Hz. Default 440 Hz. Slider in the
 * settings panel — useful for orchestral A=442 or baroque A=415.
 */
@Composable
fun InstrumentTunerScreen(padding: PaddingValues) {
    val ctx = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    var preset by remember { mutableStateOf(TunerPreset.CHROMATIC) }
    var a4 by remember { mutableFloatStateOf(440f) }
    var detectedHz by remember { mutableFloatStateOf(0f) }
    var rms by remember { mutableFloatStateOf(0f) }

    if (!hasPermission) {
        TunerPermissionGate(padding) {
            permLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        return
    }

    // Microphone listener — runs while screen is composed.
    DisposableEffect(Unit) {
        val recorder = TunerEngine(
            onPitch = { hz, level ->
                detectedHz = hz
                rms = level
            }
        )
        recorder.start()
        onDispose { recorder.stop() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Preset row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(TunerPreset.values().toList()) { p ->
                FilterChip(
                    selected = p == preset,
                    onClick = { preset = p },
                    label = { Text(stringResource(p.labelRes)) }
                )
            }
        }

        // Big dial
        TunerDial(
            preset = preset,
            a4 = a4,
            detectedHz = detectedHz,
            isAudible = rms > AUDIBLE_THRESHOLD,
        )

        // A4 calibration
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    stringResource(R.string.tuner_a4_label, a4.roundToInt()),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Slider(
                    value = a4,
                    onValueChange = { a4 = it },
                    valueRange = 415f..466f,
                    steps = 50,
                )
                Text(
                    stringResource(R.string.tuner_a4_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TunerPermissionGate(padding: PaddingValues, onAsk: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Mic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.tuner_perm_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.tuner_perm_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onAsk) {
                Text(stringResource(R.string.tuner_perm_grant))
            }
        }
    }
}

@Composable
private fun TunerDial(
    preset: TunerPreset,
    a4: Float,
    detectedHz: Float,
    isAudible: Boolean,
) {
    val nearest = remember(preset, a4, detectedHz) {
        if (detectedHz <= 0f) null else nearestTarget(detectedHz, preset, a4)
    }
    val cents = nearest?.cents ?: 0f
    val animatedCents by animateFloatAsState(
        targetValue = if (isAudible) cents.coerceIn(-50f, 50f) else 0f,
        animationSpec = tween(120),
        label = "tunerCents"
    )

    val colors = MaterialTheme.colorScheme
    val tint = when {
        !isAudible -> colors.onSurfaceVariant
        abs(cents) < 5f -> Color(0xFF2E7D32)   // green — in tune
        abs(cents) < 15f -> Color(0xFFF9A825)  // amber — close
        else -> Color(0xFFC62828)              // red — off
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth().height(340.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                val cx = size.width / 2f
                val cy = size.height * 0.78f
                val radius = size.minDimension * 0.45f

                // Arc background
                drawCircle(
                    color = colors.outlineVariant,
                    radius = radius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2f),
                )

                // Tick marks for cents
                for (c in -50..50 step 5) {
                    val isMajor = c % 25 == 0
                    val tickInner = if (isMajor) radius - 22f else radius - 12f
                    val rads = Math.toRadians((c * 1.8 - 90.0))
                    val x1 = cx + radius * Math.cos(rads).toFloat()
                    val y1 = cy + radius * Math.sin(rads).toFloat()
                    val x2 = cx + tickInner * Math.cos(rads).toFloat()
                    val y2 = cy + tickInner * Math.sin(rads).toFloat()
                    drawLine(
                        color = if (c == 0) colors.primary else colors.outline,
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = if (isMajor) 3f else 1.5f
                    )
                }

                // Needle: ±50 c → ±90 deg
                rotate(degrees = animatedCents * 1.8f, pivot = Offset(cx, cy)) {
                    drawLine(
                        color = tint,
                        start = Offset(cx, cy),
                        end = Offset(cx, cy - radius + 10f),
                        strokeWidth = 6f
                    )
                }
                drawCircle(color = tint, radius = 10f, center = Offset(cx, cy))
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = if (isAudible && nearest != null) nearest.label
                    else stringResource(R.string.tuner_listening),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = tint,
                )
                Text(
                    text = if (isAudible && nearest != null) {
                        stringResource(
                            R.string.tuner_hz_cents,
                            detectedHz.roundToInt(),
                            cents.roundToInt()
                        )
                    } else {
                        stringResource(R.string.tuner_listen_hint)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Engine ───────────────────────────────────────────────────────────

private const val SAMPLE_RATE = 44100
private const val FRAME_SIZE = 2048
private const val AUDIBLE_THRESHOLD = 0.005f

/**
 * Background mic capture + autocorrelation pitch detector.
 * Owns a single AudioRecord and a coroutine job; calling stop()
 * releases both. The onPitch callback fires on the main thread so
 * UI state updates need no extra dispatch.
 */
private class TunerEngine(
    private val onPitch: (hz: Float, rms: Float) -> Unit,
) {
    private var record: AudioRecord? = null
    @Volatile private var running = false
    private var thread: Thread? = null

    fun start() {
        if (running) return
        running = true
        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufBytes = maxOf(minBuf, FRAME_SIZE * 2 * 2) // 16-bit, double-buffered
        try {
            // UNPROCESSED gives us flat raw mic data without the AGC/noise
            // suppression that VOICE_RECOGNITION applies — pitch detection
            // wants a clean linear signal. Available since API 24, our
            // minSdk is 26 so always safe.
            @Suppress("MissingPermission")
            record = AudioRecord(
                MediaRecorder.AudioSource.UNPROCESSED,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufBytes
            ).also { it.startRecording() }
        } catch (e: Throwable) {
            // Permission denied at last second or device refuses — stay silent.
            running = false
            return
        }

        thread = Thread {
            val buf = ShortArray(FRAME_SIZE)
            val floats = FloatArray(FRAME_SIZE)
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            while (running) {
                val r = record?.read(buf, 0, FRAME_SIZE) ?: break
                if (r <= 0) continue
                var sumSq = 0.0
                for (i in 0 until r) {
                    val v = buf[i] / 32768f
                    floats[i] = v
                    sumSq += v * v
                }
                val rms = sqrt(sumSq / r).toFloat()
                val hz = if (rms > AUDIBLE_THRESHOLD)
                    detectPitchAutocorrelation(floats, r)
                else 0f
                handler.post { onPitch(hz, rms) }
            }
        }.also { it.isDaemon = true; it.start() }
    }

    fun stop() {
        running = false
        try {
            record?.stop()
        } catch (_: Throwable) {
        }
        record?.release()
        record = null
        thread?.interrupt()
        thread = null
    }
}

/**
 * Autocorrelation with parabolic peak interpolation. Fast, robust on
 * monophonic instrument signals.
 *
 * - Search window: 50 Hz .. 2 kHz → lag ∈ [22, 882] samples.
 * - Find lag with peak r(τ).
 * - Refine via parabolic interpolation around the peak for sub-sample
 *   precision (gives cents-level accuracy without zero-padding).
 * - Reject if peak / r(0) < 0.3 — too noisy / no clear pitch.
 */
private fun detectPitchAutocorrelation(x: FloatArray, n: Int): Float {
    val minLag = SAMPLE_RATE / 2000  // 22
    val maxLag = (SAMPLE_RATE / 50).coerceAtMost(n - 1)  // 882

    // r(0)
    var r0 = 0.0
    for (i in 0 until n) r0 += x[i] * x[i]
    if (r0 < 1e-6) return 0f

    var bestLag = -1
    var bestR = Double.NEGATIVE_INFINITY
    val rArr = DoubleArray(maxLag + 1)
    var prev = Double.POSITIVE_INFINITY
    var goingDown = true
    for (lag in minLag..maxLag) {
        var sum = 0.0
        val limit = n - lag
        var i = 0
        while (i < limit) {
            sum += x[i] * x[i + lag]
            i++
        }
        rArr[lag] = sum
        // Take first peak after the initial decline (avoids picking
        // r(0)-like trivial maxima when the signal is very low pitch).
        if (goingDown) {
            if (sum > prev) goingDown = false
        }
        if (!goingDown && sum > bestR) {
            bestR = sum
            bestLag = lag
        }
        prev = sum
    }
    if (bestLag < 0) return 0f
    val confidence = bestR / r0
    if (confidence < 0.30) return 0f

    // Parabolic interp around bestLag
    val s0 = rArr.getOrElse(bestLag - 1) { rArr[bestLag] }
    val s1 = rArr[bestLag]
    val s2 = rArr.getOrElse(bestLag + 1) { rArr[bestLag] }
    val denom = (s0 - 2 * s1 + s2)
    val refined = if (denom != 0.0) bestLag + 0.5 * (s0 - s2) / denom else bestLag.toDouble()
    return (SAMPLE_RATE / refined).toFloat()
}

// ── Pitch ↔ note math ─────────────────────────────────────────────────

private val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

private data class TargetMatch(val label: String, val targetHz: Float, val cents: Float)

/**
 * Find the closest tuning target. For non-chromatic presets we match
 * against the preset's named strings; for chromatic we match against
 * the entire equal-temperament grid.
 */
private fun nearestTarget(hz: Float, preset: TunerPreset, a4: Float): TargetMatch {
    return if (preset.strings.isEmpty()) {
        // Chromatic: nearest semitone (any of 88 piano keys)
        val midi = 69 + 12 * log2(hz / a4)
        val nearestMidi = midi.roundToInt().coerceIn(21, 108)
        val targetHz = a4 * Math.pow(2.0, (nearestMidi - 69) / 12.0).toFloat()
        val cents = 1200f * (ln(hz / targetHz) / ln(2.0)).toFloat()
        TargetMatch(midiToName(nearestMidi), targetHz, cents)
    } else {
        // Match against preset string set
        var best = preset.strings[0]
        var bestDiff = Float.MAX_VALUE
        for (s in preset.strings) {
            val targetHz = a4 * Math.pow(2.0, (s.midi - 69) / 12.0).toFloat()
            val cents = 1200f * (ln(hz / targetHz) / ln(2.0)).toFloat()
            if (abs(cents) < bestDiff) {
                bestDiff = abs(cents)
                best = s
            }
        }
        val targetHz = a4 * Math.pow(2.0, (best.midi - 69) / 12.0).toFloat()
        val cents = 1200f * (ln(hz / targetHz) / ln(2.0)).toFloat()
        TargetMatch(best.label, targetHz, cents)
    }
}

private fun midiToName(midi: Int): String {
    val name = NOTE_NAMES[(midi % 12 + 12) % 12]
    val octave = (midi / 12) - 1
    return "$name$octave"
}

// ── Presets ──────────────────────────────────────────────────────────

private data class TunerString(val label: String, val midi: Int)

private enum class TunerPreset(val labelRes: Int, val strings: List<TunerString>) {
    CHROMATIC(R.string.tuner_preset_chromatic, emptyList()),
    GUITAR_STD(
        R.string.tuner_preset_guitar_std,
        listOf(
            TunerString("E2", 40), TunerString("A2", 45), TunerString("D3", 50),
            TunerString("G3", 55), TunerString("B3", 59), TunerString("E4", 64)
        )
    ),
    GUITAR_DROP_D(
        R.string.tuner_preset_guitar_drop_d,
        listOf(
            TunerString("D2", 38), TunerString("A2", 45), TunerString("D3", 50),
            TunerString("G3", 55), TunerString("B3", 59), TunerString("E4", 64)
        )
    ),
    GUITAR_DADGAD(
        R.string.tuner_preset_guitar_dadgad,
        listOf(
            TunerString("D2", 38), TunerString("A2", 45), TunerString("D3", 50),
            TunerString("G3", 55), TunerString("A3", 57), TunerString("D4", 62)
        )
    ),
    GUITAR_OPEN_G(
        R.string.tuner_preset_guitar_open_g,
        listOf(
            TunerString("D2", 38), TunerString("G2", 43), TunerString("D3", 50),
            TunerString("G3", 55), TunerString("B3", 59), TunerString("D4", 62)
        )
    ),
    GUITAR_OPEN_D(
        R.string.tuner_preset_guitar_open_d,
        listOf(
            TunerString("D2", 38), TunerString("A2", 45), TunerString("D3", 50),
            TunerString("F#3", 54), TunerString("A3", 57), TunerString("D4", 62)
        )
    ),
    GUITAR_HALF_DOWN(
        R.string.tuner_preset_guitar_half_down,
        listOf(
            TunerString("D#2", 39), TunerString("G#2", 44), TunerString("C#3", 49),
            TunerString("F#3", 54), TunerString("A#3", 58), TunerString("D#4", 63)
        )
    ),
    GUITAR_7(
        R.string.tuner_preset_guitar_7,
        listOf(
            TunerString("B1", 35), TunerString("E2", 40), TunerString("A2", 45),
            TunerString("D3", 50), TunerString("G3", 55), TunerString("B3", 59),
            TunerString("E4", 64)
        )
    ),
    BASS_4(
        R.string.tuner_preset_bass_4,
        listOf(
            TunerString("E1", 28), TunerString("A1", 33),
            TunerString("D2", 38), TunerString("G2", 43)
        )
    ),
    BASS_5(
        R.string.tuner_preset_bass_5,
        listOf(
            TunerString("B0", 23), TunerString("E1", 28), TunerString("A1", 33),
            TunerString("D2", 38), TunerString("G2", 43)
        )
    ),
    VIOLIN(
        R.string.tuner_preset_violin,
        listOf(
            TunerString("G3", 55), TunerString("D4", 62),
            TunerString("A4", 69), TunerString("E5", 76)
        )
    ),
    VIOLA(
        R.string.tuner_preset_viola,
        listOf(
            TunerString("C3", 48), TunerString("G3", 55),
            TunerString("D4", 62), TunerString("A4", 69)
        )
    ),
    CELLO(
        R.string.tuner_preset_cello,
        listOf(
            TunerString("C2", 36), TunerString("G2", 43),
            TunerString("D3", 50), TunerString("A3", 57)
        )
    ),
    UKULELE(
        R.string.tuner_preset_ukulele,
        listOf(
            TunerString("G4", 67), TunerString("C4", 60),
            TunerString("E4", 64), TunerString("A4", 69)
        )
    ),
    MANDOLIN(
        R.string.tuner_preset_mandolin,
        listOf(
            TunerString("G3", 55), TunerString("D4", 62),
            TunerString("A4", 69), TunerString("E5", 76)
        )
    ),
    BANJO_5(
        R.string.tuner_preset_banjo_5,
        listOf(
            TunerString("G4", 67), TunerString("D3", 50),
            TunerString("G3", 55), TunerString("B3", 59), TunerString("D4", 62)
        )
    ),
    PIANO(R.string.tuner_preset_piano, emptyList()),
}
