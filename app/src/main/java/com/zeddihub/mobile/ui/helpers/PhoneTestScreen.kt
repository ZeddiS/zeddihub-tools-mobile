package com.zeddihub.mobile.ui.helpers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.zeddihub.mobile.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin

/**
 * Self-test diagnostic suite — inspired by Samsung's *#0*# TestMode but
 * deliberately not a 1:1 clone (UI/iconography is original to avoid any
 * trademark concerns with Samsung's screen).
 *
 * Every test is non-destructive: nothing is written to disk, no system
 * settings change, and any hardware we light up (vibrator, torch,
 * speaker tone) shuts off the moment the user navigates away or hits
 * the stop button. Permissions are requested lazily — sensor pages
 * never prompt, while mic/torch ask only when their tab is opened.
 *
 * Page order roughly follows likely failure modes after a drop: display
 * and touch first, then vibration and sensors (orientation glass), then
 * audio, mic, torch, and buttons last.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PhoneTestScreen(padding: PaddingValues) {
    val tabs = listOf(
        R.string.pt_tab_display,
        R.string.pt_tab_touch,
        R.string.pt_tab_vibration,
        R.string.pt_tab_sensors,
        R.string.pt_tab_speakers,
        R.string.pt_tab_mic,
        R.string.pt_tab_flash,
        R.string.pt_tab_buttons,
    )
    val pager = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        ScrollableTabRow(selectedTabIndex = pager.currentPage, edgePadding = 12.dp) {
            tabs.forEachIndexed { i, res ->
                Tab(
                    selected = pager.currentPage == i,
                    onClick = { scope.launch { pager.animateScrollToPage(i) } },
                    text = { Text(stringResource(res)) }
                )
            }
        }
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            when (page) {
                0 -> DisplayTest()
                1 -> TouchTest()
                2 -> VibrationTest()
                3 -> SensorTest()
                4 -> SpeakerTest()
                5 -> MicTest()
                6 -> FlashTest()
                7 -> ButtonsTest()
            }
        }
    }
}

// ── Display test ────────────────────────────────────────────────────

/**
 * Cycles through solid colours plus a gradient to surface dead pixels,
 * stuck sub-pixels, backlight bleed, and uneven illumination. Tap the
 * screen to advance to the next colour.
 */
@Composable
private fun DisplayTest() {
    val colors = listOf(
        Color.Red, Color.Green, Color.Blue,
        Color.White, Color(0xFF202020), Color.Black,
        Color(0xFFFFFF00), Color(0xFFFF00FF), Color(0xFF00FFFF),
    )
    var idx by remember { mutableStateOf(-1) } // -1 = info panel

    if (idx < 0) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                stringResource(R.string.pt_display_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(stringResource(R.string.pt_display_body))
            Button(onClick = { idx = 0 }) {
                Text(stringResource(R.string.pt_display_start))
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors[idx])
                .pointerInput(Unit) {
                    awaitPointerEventScopeLoop {
                        val next = idx + 1
                        idx = if (next >= colors.size) -1 else next
                    }
                }
        ) {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            ) {
                Text(
                    "${idx + 1} / ${colors.size} — " + stringResource(R.string.pt_display_tap_next),
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

/**
 * Tiny helper: run [block] every time the pointer goes down. Pulls
 * the awaitPointerEvent boilerplate out of the test composables so
 * each tap-to-advance handler becomes one line.
 */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.awaitPointerEventScopeLoop(
    block: () -> Unit
) {
    awaitPointerEventScope {
        while (true) {
            val ev = awaitPointerEvent()
            if (ev.changes.any { it.changedToDown() }) block()
        }
    }
}

private fun androidx.compose.ui.input.pointer.PointerInputChange.changedToDown(): Boolean =
    this.pressed && !this.previousPressed

// ── Touch test ──────────────────────────────────────────────────────

/**
 * Multi-touch tracking. Each active pointer paints a coloured circle
 * at its position; lifting it leaves a fading trail so the user can
 * verify dead zones across the whole panel.
 */
@Composable
private fun TouchTest() {
    val points = remember { mutableStateListOf<TouchPoint>() }
    val palette = listOf(
        Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047),
        Color(0xFFFB8C00), Color(0xFF8E24AA), Color(0xFF00ACC1),
        Color(0xFFFFB300), Color(0xFF6D4C41), Color(0xFF3949AB),
        Color(0xFF7CB342),
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val ev = awaitPointerEvent()
                        // Replace the active list with this frame's pointers so
                        // a lifted finger disappears immediately.
                        points.clear()
                        ev.changes.forEachIndexed { i, change ->
                            if (change.pressed) {
                                points.add(TouchPoint(change.position, palette[i % palette.size]))
                            }
                        }
                    }
                }
            }
    ) {
        for (p in points) {
            drawCircle(color = p.color, radius = 80f, center = p.pos)
            drawCircle(color = Color.White, radius = 80f, center = p.pos, style = Stroke(width = 4f))
        }
    }
}

private data class TouchPoint(val pos: Offset, val color: Color)

private val SENSOR_TYPES = listOf(
    Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE, Sensor.TYPE_MAGNETIC_FIELD,
    Sensor.TYPE_LIGHT, Sensor.TYPE_PROXIMITY, Sensor.TYPE_PRESSURE,
    Sensor.TYPE_AMBIENT_TEMPERATURE, Sensor.TYPE_RELATIVE_HUMIDITY,
    Sensor.TYPE_GRAVITY, Sensor.TYPE_LINEAR_ACCELERATION,
    Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_STEP_COUNTER,
)

// ── Vibration ───────────────────────────────────────────────────────

@Composable
private fun VibrationTest() {
    val ctx = LocalContext.current
    val vibrator = remember { vibrator(ctx) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            stringResource(R.string.pt_vibration_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(stringResource(R.string.pt_vibration_body))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= 26) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                } else @Suppress("DEPRECATION") { vibrator?.vibrate(80) }
            }) { Text(stringResource(R.string.pt_vib_short)) }

            Button(onClick = {
                if (Build.VERSION.SDK_INT >= 26) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(600, VibrationEffect.DEFAULT_AMPLITUDE))
                } else @Suppress("DEPRECATION") { vibrator?.vibrate(600) }
            }) { Text(stringResource(R.string.pt_vib_long)) }

            Button(onClick = {
                val pattern = longArrayOf(0, 100, 80, 200, 80, 400)
                if (Build.VERSION.SDK_INT >= 26) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else @Suppress("DEPRECATION") { vibrator?.vibrate(pattern, -1) }
            }) { Text(stringResource(R.string.pt_vib_pattern)) }
        }

        if (vibrator == null) {
            Text(
                stringResource(R.string.pt_vibration_unavailable),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Suppress("DEPRECATION")
private fun vibrator(ctx: Context): Vibrator? =
    if (Build.VERSION.SDK_INT >= 31) {
        (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

// ── Sensors ─────────────────────────────────────────────────────────

@Composable
private fun SensorTest() {
    val ctx = LocalContext.current
    val sm = remember { ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager }
    val readings = remember { mutableStateMapOf<Int, String>() }

    DisposableEffect(sm) {
        if (sm == null) return@DisposableEffect onDispose { }
        // We listen on the most common sensor types. Listing them
        // explicitly (rather than getSensorList(TYPE_ALL)) keeps the
        // panel focused on the ones a user can actually interpret.
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val v = e.values
                val pretty = when (v.size) {
                    1 -> "%.2f".format(v[0])
                    2 -> "%.2f, %.2f".format(v[0], v[1])
                    3 -> "x=%.2f  y=%.2f  z=%.2f".format(v[0], v[1], v[2])
                    else -> v.joinToString { "%.2f".format(it) }
                }
                readings[e.sensor.type] = pretty
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        for (type in SENSOR_TYPES) {
            sm.getDefaultSensor(type)?.let {
                sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
        onDispose { sm.unregisterListener(listener) }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            stringResource(R.string.pt_sensors_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        if (sm == null) {
            Text(stringResource(R.string.pt_sensors_unavailable),
                color = MaterialTheme.colorScheme.error)
            return@Column
        }
        val labels = mapOf(
            Sensor.TYPE_ACCELEROMETER to "Accelerometer",
            Sensor.TYPE_GYROSCOPE to "Gyroscope",
            Sensor.TYPE_MAGNETIC_FIELD to "Magnetometer",
            Sensor.TYPE_LIGHT to "Light (lux)",
            Sensor.TYPE_PROXIMITY to "Proximity",
            Sensor.TYPE_PRESSURE to "Pressure (hPa)",
            Sensor.TYPE_AMBIENT_TEMPERATURE to "Temperature",
            Sensor.TYPE_RELATIVE_HUMIDITY to "Humidity",
            Sensor.TYPE_GRAVITY to "Gravity",
            Sensor.TYPE_LINEAR_ACCELERATION to "Linear accel",
            Sensor.TYPE_ROTATION_VECTOR to "Rotation",
            Sensor.TYPE_STEP_COUNTER to "Steps",
        )
        for ((type, label) in labels) {
            val available = sm.getDefaultSensor(type) != null
            val value = readings[type] ?: if (available) "…" else "—"
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, fontWeight = FontWeight.SemiBold)
                    Text(value, color = if (available) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ── Speakers ────────────────────────────────────────────────────────

@Composable
private fun SpeakerTest() {
    val ctx = LocalContext.current
    var playing by remember { mutableStateOf(false) }
    var freq by remember { mutableStateOf(440) }
    var channel by remember { mutableStateOf(SpeakerChannel.BOTH) }
    val scope = rememberCoroutineScope()
    var trackRef by remember { mutableStateOf<AudioTrack?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            trackRef?.runCatching { stop(); release() }
            trackRef = null
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            stringResource(R.string.pt_speakers_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(stringResource(R.string.pt_speakers_body))

        // Frequency presets
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf(100, 250, 440, 1000, 4000, 10000)) { f ->
                AssistChip(onClick = { freq = f }, label = { Text("$f Hz") })
            }
        }
        Text("Frekvence: $freq Hz")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SpeakerChannel.values().forEach { ch ->
                AssistChip(
                    onClick = { channel = ch },
                    label = { Text(ch.label) }
                )
            }
        }
        Text("Kanál: ${channel.label}")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = !playing,
                onClick = {
                    playing = true
                    scope.launch(Dispatchers.Default) {
                        val t = playSineLoop(freq, channel)
                        trackRef = t
                    }
                }
            ) { Text(stringResource(R.string.pt_speakers_play)) }
            OutlinedButton(
                enabled = playing,
                onClick = {
                    trackRef?.runCatching { stop(); release() }
                    trackRef = null
                    playing = false
                }
            ) { Text(stringResource(R.string.pt_speakers_stop)) }
        }
    }
}

private enum class SpeakerChannel(val label: String) { LEFT("L"), RIGHT("R"), BOTH("L+R") }

/**
 * Build and start a stereo AudioTrack streaming a continuous sine wave
 * at [freq] Hz. The caller owns the returned track and must stop+release
 * it. We stream rather than use MODE_STATIC with a one-period buffer so
 * frequency switching is glitch-free.
 */
private fun playSineLoop(freq: Int, ch: SpeakerChannel): AudioTrack {
    val sampleRate = 44100
    val format = AudioFormat.Builder()
        .setSampleRate(sampleRate)
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
        .build()
    val minBuf = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO,
        AudioFormat.ENCODING_PCM_16BIT)
    val track = AudioTrack.Builder()
        .setAudioFormat(format)
        .setBufferSizeInBytes(minBuf * 2)
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()
    track.play()
    Thread {
        val frame = ShortArray(1024 * 2) // 1024 stereo frames
        var phase = 0.0
        val step = 2.0 * PI * freq / sampleRate
        while (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
            for (i in 0 until 1024) {
                val v = (sin(phase) * 24000).toInt().toShort()
                frame[i * 2] = if (ch == SpeakerChannel.RIGHT) 0 else v
                frame[i * 2 + 1] = if (ch == SpeakerChannel.LEFT) 0 else v
                phase += step
                if (phase > 2.0 * PI) phase -= 2.0 * PI
            }
            try { track.write(frame, 0, frame.size) } catch (_: Throwable) { break }
        }
    }.start()
    return track
}

// ── Mic test ────────────────────────────────────────────────────────

@Composable
private fun MicTest() {
    val ctx = LocalContext.current
    val granted = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted.value = it
    }
    var rms by remember { mutableStateOf(0f) }
    var running by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    DisposableEffect(Unit) {
        onDispose { job?.cancel() }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            stringResource(R.string.pt_mic_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        if (!granted.value) {
            Button(onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) }) {
                Text(stringResource(R.string.pt_mic_grant))
            }
            return@Column
        }
        Text(stringResource(R.string.pt_mic_body))

        LinearMeter(rms = rms.coerceIn(0f, 1f))
        Text("RMS: %.3f".format(rms))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = !running,
                onClick = {
                    running = true
                    job = scope.launch(Dispatchers.Default) {
                        recordRmsLoop { newRms ->
                            withContext(Dispatchers.Main) { rms = newRms }
                        }
                    }
                }
            ) { Text(stringResource(R.string.pt_mic_start)) }
            OutlinedButton(
                enabled = running,
                onClick = {
                    job?.cancel()
                    job = null
                    running = false
                    rms = 0f
                }
            ) { Text(stringResource(R.string.pt_mic_stop)) }
        }
    }
}

@Composable
private fun LinearMeter(rms: Float) {
    val bg = MaterialTheme.colorScheme.surfaceVariant
    val fg = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .background(bg, RoundedCornerShape(6.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(rms.coerceIn(0f, 1f))
                .height(20.dp)
                .background(fg, RoundedCornerShape(6.dp))
        )
    }
}

private suspend fun recordRmsLoop(onRms: suspend (Float) -> Unit) {
    val sampleRate = 22050
    val minBuf = AudioRecord.getMinBufferSize(
        sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
    )
    val rec = try {
        AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT, minBuf * 2)
    } catch (_: SecurityException) { return }
    if (rec.state != AudioRecord.STATE_INITIALIZED) { rec.release(); return }
    rec.startRecording()
    val buf = ShortArray(1024)
    try {
        // currentCoroutineContext().isActive cleanly returns false the
        // moment the parent Job is cancelled, so the loop exits and we
        // hit the finally{} cleanup before the next read() blocks.
        while (kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]?.isActive == true) {
            val n = rec.read(buf, 0, buf.size)
            if (n <= 0) continue
            var sum = 0.0
            for (i in 0 until n) sum += (buf[i].toDouble() * buf[i])
            val rms = (kotlin.math.sqrt(sum / n) / Short.MAX_VALUE).toFloat()
            onRms(rms)
            delay(40)
        }
    } finally {
        rec.runCatching { stop(); release() }
    }
}

// ── Flash / torch ───────────────────────────────────────────────────

@Composable
private fun FlashTest() {
    val ctx = LocalContext.current
    val cm = remember { ctx.getSystemService(Context.CAMERA_SERVICE) as? CameraManager }
    val cameraId = remember {
        cm?.cameraIdList?.firstOrNull { id ->
            cm.getCameraCharacteristics(id)
                .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }
    var on by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            if (cm != null && cameraId != null) {
                runCatching { cm.setTorchMode(cameraId, false) }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            stringResource(R.string.pt_flash_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        if (cameraId == null) {
            Text(
                stringResource(R.string.pt_flash_unavailable),
                color = MaterialTheme.colorScheme.error
            )
            return@Column
        }
        Text(stringResource(R.string.pt_flash_body))
        Button(onClick = {
            val next = !on
            runCatching { cm?.setTorchMode(cameraId, next) }
                .onSuccess { on = next }
        }) {
            Text(if (on) stringResource(R.string.pt_flash_off) else stringResource(R.string.pt_flash_on))
        }
    }
}

// ── Hardware buttons ────────────────────────────────────────────────

/**
 * Volume up / down detection. We can't read Power button presses from
 * a regular app — Android intercepts those at the framework layer for
 * power and accessibility — so the test focuses on volume rockers and
 * shows a hint about back-gesture / nav buttons being verified
 * through whatever the user does to leave the screen.
 */
@Composable
private fun ButtonsTest() {
    val ctx = LocalContext.current
    val am = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var vol by remember { mutableStateOf(am.getStreamVolume(AudioManager.STREAM_MUSIC)) }
    val max = remember { am.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    var lastChange by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        // Poll volume — lighter than a content observer for a diagnostic
        // screen the user is only on for seconds at a time.
        var prev = vol
        while (true) {
            val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (cur != prev) {
                lastChange = if (cur > prev) "Volume Up" else "Volume Down"
                prev = cur
                vol = cur
            }
            delay(120)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            stringResource(R.string.pt_buttons_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(stringResource(R.string.pt_buttons_body))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Volume: $vol / $max", fontWeight = FontWeight.SemiBold)
                Text("Posledně: ${lastChange ?: "—"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        Text(
            stringResource(R.string.pt_buttons_power_note),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

