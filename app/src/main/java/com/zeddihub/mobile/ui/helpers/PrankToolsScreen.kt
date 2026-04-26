package com.zeddihub.mobile.ui.helpers

import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.zeddihub.mobile.R
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Hub + 20 prank effects. Each prank is fullscreen and exits on
 * volume-down (handled at the host activity level via the back
 * navigation chain) or by tapping a small "X" in the corner.
 *
 * We keep all 20 in this single file because each prank is small
 * (~30-40 LOC of Compose), and bundling them together avoids 20
 * tiny file imports through the navigation graph. The hub is a
 * 2-column grid of cards.
 */
@Composable
fun PrankToolsScreen(padding: PaddingValues) {
    var active by remember { mutableStateOf<Prank?>(null) }
    if (active != null) {
        PrankFullscreen(active!!) { active = null }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            stringResource(R.string.prank_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            stringResource(R.string.prank_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(Prank.values().toList()) { p ->
                PrankCard(p) { active = p }
            }
        }
    }
}

@Composable
private fun stringResource(id: Int) =
    androidx.compose.ui.res.stringResource(id)

@Composable
private fun PrankCard(p: Prank, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(p.emoji, fontSize = 32.sp)
            Text(
                stringResource(p.titleRes),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private enum class Prank(val emoji: String, val titleRes: Int) {
    DISPLAY_CRACK("💥", R.string.prank_display_crack),
    PIXEL_BROKEN("🩻", R.string.prank_pixel_broken),
    FAKE_VIRUS("☠️", R.string.prank_fake_virus),
    FAKE_POLICE("🚨", R.string.prank_fake_police),
    FAKE_UPDATE("🔄", R.string.prank_fake_update),
    FAKE_CALL("📞", R.string.prank_fake_call),
    FAKE_BATTERY("🔋", R.string.prank_fake_battery),
    GLITCH_RGB("📺", R.string.prank_glitch),
    STROBE("⚡", R.string.prank_strobe),
    AIR_HORN("📯", R.string.prank_air_horn),
    EARTHQUAKE("🌋", R.string.prank_earthquake),
    SPIDER("🕷️", R.string.prank_spider),
    BURNING("🔥", R.string.prank_burning),
    XRAY("🦴", R.string.prank_xray),
    MIRROR_FREEZE("🧊", R.string.prank_mirror_freeze),
    FAKE_WIFI_LOST("📶", R.string.prank_fake_wifi),
    LOADING_FOREVER("⏳", R.string.prank_loading_forever),
    FAKE_DISK_FULL("💾", R.string.prank_disk_full),
    FAKE_BANK_OTP("🏦", R.string.prank_bank_otp),
    MAGIC_BUTTON("🎲", R.string.prank_magic_button),
}

@Composable
private fun PrankFullscreen(p: Prank, onExit: () -> Unit) {
    // KEEP_SCREEN_ON during pranks so the display doesn't time out
    // mid-effect. Cleanup runs when this composable leaves.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(onClick = onExit)
    ) {
        when (p) {
            Prank.DISPLAY_CRACK -> DisplayCrackPrank()
            Prank.PIXEL_BROKEN -> PixelBrokenPrank()
            Prank.FAKE_VIRUS -> FakeVirusPrank()
            Prank.FAKE_POLICE -> FakePolicePrank()
            Prank.FAKE_UPDATE -> FakeUpdatePrank()
            Prank.FAKE_CALL -> FakeCallPrank()
            Prank.FAKE_BATTERY -> FakeBatteryPrank()
            Prank.GLITCH_RGB -> GlitchPrank()
            Prank.STROBE -> StrobePrank()
            Prank.AIR_HORN -> AirHornPrank()
            Prank.EARTHQUAKE -> EarthquakePrank()
            Prank.SPIDER -> SpiderPrank()
            Prank.BURNING -> BurningPrank()
            Prank.XRAY -> XRayPrank()
            Prank.MIRROR_FREEZE -> MirrorFreezePrank()
            Prank.FAKE_WIFI_LOST -> FakeWifiLostPrank()
            Prank.LOADING_FOREVER -> LoadingForeverPrank()
            Prank.FAKE_DISK_FULL -> FakeDiskFullPrank()
            Prank.FAKE_BANK_OTP -> FakeBankOtpPrank()
            Prank.MAGIC_BUTTON -> MagicButtonPrank()
        }
        // Tiny exit chip so users can leave even when the prank is
        // a single-tap-spam type (e.g. Display Crack).
        Surface(
            shape = RoundedCornerShape(50),
            color = Color.White.copy(alpha = 0.15f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .clickable(onClick = onExit)
        ) {
            Text("✕", color = Color.White,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
        }
    }
}

// ── 1. Display Crack ─────────────────────────────────────────────────
@Composable
private fun DisplayCrackPrank() {
    val cracks = remember { mutableStateListOf<Offset>() }
    Canvas(
        modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val ev = awaitPointerEvent()
                    ev.changes.forEach { c ->
                        if (c.pressed && c.previousPressed.not()) {
                            cracks.add(c.position)
                        }
                    }
                }
            }
        }
    ) {
        for (origin in cracks) {
            drawCrackAt(origin)
        }
        if (cracks.isEmpty()) {
            // Pre-seed one crack so the screen looks pre-broken on entry.
            drawCrackAt(Offset(size.width / 2, size.height / 2))
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCrackAt(origin: Offset) {
    val rng = Random(origin.x.toInt() * 31 + origin.y.toInt())
    repeat(8) {
        val angle = rng.nextDouble(0.0, 2 * PI)
        val length = rng.nextFloat() * 600f + 200f
        val end = Offset(
            origin.x + (length * kotlin.math.cos(angle)).toFloat(),
            origin.y + (length * kotlin.math.sin(angle)).toFloat()
        )
        // Jagged path with a few midpoint deviations so cracks zig-zag.
        val path = Path().apply {
            moveTo(origin.x, origin.y)
            var cur = origin
            for (s in 1..5) {
                val t = s / 5f
                val mid = Offset(
                    origin.x + (end.x - origin.x) * t + rng.nextFloat() * 30 - 15,
                    origin.y + (end.y - origin.y) * t + rng.nextFloat() * 30 - 15,
                )
                lineTo(mid.x, mid.y)
                cur = mid
            }
            lineTo(end.x, end.y)
        }
        drawPath(path, color = Color.White.copy(alpha = 0.85f),
            style = Stroke(width = rng.nextFloat() * 3 + 1))
    }
}

// ── 2. Pixel broken (slowly spreading) ──────────────────────────────
@Composable
private fun PixelBrokenPrank() {
    val pixels = remember { mutableStateListOf<Offset>() }
    LaunchedEffect(Unit) {
        while (true) {
            delay(80)
            val rng = Random.Default
            pixels.add(Offset(
                rng.nextFloat() * 1200f,
                rng.nextFloat() * 2400f,
            ))
        }
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        for (p in pixels) {
            drawCircle(
                color = if (Random.nextBoolean()) Color.Black else Color.Red,
                radius = Random.nextFloat() * 8f + 2f,
                center = p
            )
        }
    }
}

// ── 3. Fake virus ────────────────────────────────────────────────────
@Composable
private fun FakeVirusPrank() {
    val flash by rememberInfiniteTransition(label = "v").animateFloat(
        0f, 1f, infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "vf"
    )
    Box(
        modifier = Modifier.fillMaxSize().background(
            Color(0xFF8B0000).copy(alpha = 0.5f + 0.5f * flash)
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⚠", color = Color.Yellow, fontSize = 100.sp)
            Text("KRITICKÁ INFEKCE", color = Color.White,
                fontWeight = FontWeight.Bold, fontSize = 28.sp)
            Text("Detekováno 47 hrozeb", color = Color.White, fontSize = 18.sp)
            Text("Nevypínejte zařízení!", color = Color.Yellow, fontSize = 16.sp)
        }
    }
}

// ── 4. Fake police ───────────────────────────────────────────────────
@Composable
private fun FakePolicePrank() {
    val phase by rememberInfiniteTransition(label = "p").animateFloat(
        0f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "pp"
    )
    Box(
        modifier = Modifier.fillMaxSize().background(
            if (phase < 0.5f) Color(0xFFB71C1C) else Color(0xFF0D47A1)
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🚨", fontSize = 80.sp)
            Text("FBI / POLICIE", color = Color.White,
                fontWeight = FontWeight.Bold, fontSize = 32.sp)
            Text("ZAŘÍZENÍ ZABLOKOVÁNO", color = Color.White, fontSize = 20.sp)
            Text("Nelegální aktivita detekována", color = Color.Yellow,
                style = TextStyle(fontSize = 14.sp))
        }
    }
}

// ── 5. Fake update ───────────────────────────────────────────────────
@Composable
private fun FakeUpdatePrank() {
    var pct by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (pct < 99) {
            delay(800)
            pct += 1
            // Stick at certain "milestones" to add realism
            if (pct == 23 || pct == 67 || pct == 91) delay(3000)
        }
    }
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🤖", fontSize = 64.sp)
            Text("Instaluje se aktualizace systému…",
                color = Color.White, fontSize = 18.sp)
            Text("$pct %", color = Color.White,
                fontWeight = FontWeight.Bold, fontSize = 48.sp)
            Text("Nevypínejte zařízení", color = Color.Gray, fontSize = 14.sp)
        }
    }
}

// ── 6. Fake call ─────────────────────────────────────────────────────
@Composable
private fun FakeCallPrank() {
    val ctx = LocalContext.current
    val vibrator = remember { vibratorFor(ctx) }
    LaunchedEffect(Unit) {
        repeat(20) {
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator?.vibrate(VibrationEffect.createOneShot(800,
                    VibrationEffect.DEFAULT_AMPLITUDE))
            }
            delay(1500)
        }
    }
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1B5E20)),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📞", fontSize = 100.sp)
            Text("Boss", color = Color.White,
                fontWeight = FontWeight.Bold, fontSize = 36.sp)
            Text("+420 777 666 555", color = Color.White, fontSize = 16.sp)
            Text("příchozí hovor…", color = Color.LightGray, fontSize = 14.sp)
        }
    }
}

// ── 7. Fake battery 1% ───────────────────────────────────────────────
@Composable
private fun FakeBatteryPrank() {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔋", fontSize = 80.sp)
            Text("1 %", color = Color.Red,
                fontWeight = FontWeight.Bold, fontSize = 80.sp)
            Text("Připojte nabíječku", color = Color.White, fontSize = 18.sp)
        }
    }
}

// ── 8. Glitch / RGB shift ────────────────────────────────────────────
@Composable
private fun GlitchPrank() {
    val r by rememberInfiniteTransition(label = "g").animateFloat(
        0f, 1f, infiniteRepeatable(tween(80, easing = LinearEasing)), label = "gr"
    )
    Box(modifier = Modifier.fillMaxSize()) {
        val shift = (r * 60 - 30).dp
        Box(modifier = Modifier.fillMaxSize().background(Color.Red.copy(alpha = 0.4f))
            .padding(start = shift))
        Box(modifier = Modifier.fillMaxSize().background(Color.Green.copy(alpha = 0.4f)))
        Box(modifier = Modifier.fillMaxSize().background(Color.Blue.copy(alpha = 0.4f))
            .padding(end = shift))
    }
}

// ── 9. Strobe / disco ────────────────────────────────────────────────
@Composable
private fun StrobePrank() {
    val phase by rememberInfiniteTransition(label = "s").animateFloat(
        0f, 1f, infiniteRepeatable(tween(80), RepeatMode.Reverse), label = "sp"
    )
    Box(modifier = Modifier.fillMaxSize().background(
        if (phase < 0.5f) Color.White else Color.Black
    ))
}

// ── 10. Air horn ─────────────────────────────────────────────────────
@Composable
private fun AirHornPrank() {
    LaunchedEffect(Unit) {
        playSineForDuration(800f, 1500)
    }
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFFC107)),
        contentAlignment = Alignment.Center) {
        Text("📯", fontSize = 200.sp)
    }
}

// ── 11. Earthquake (vibration + shake text) ──────────────────────────
@Composable
private fun EarthquakePrank() {
    val ctx = LocalContext.current
    val vibrator = remember { vibratorFor(ctx) }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 26) {
            val pattern = LongArray(40) { if (it % 2 == 0) 50 else 30 }
            val amplitudes = IntArray(40) { 255 }
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
        }
    }
    val rng = remember { Random.Default }
    val shake by rememberInfiniteTransition(label = "e").animateFloat(
        0f, 1f, infiniteRepeatable(tween(50, easing = LinearEasing)), label = "es"
    )
    val dx = ((shake - 0.5f) * 40).dp
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF263238)),
        contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.padding(start = dx),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🌋", fontSize = 100.sp)
            Text("ZEMĚTŘESENÍ!", color = Color.White,
                fontWeight = FontWeight.Bold, fontSize = 32.sp)
        }
    }
}

// ── 12. Spider walking ───────────────────────────────────────────────
@Composable
private fun SpiderPrank() {
    val t by rememberInfiniteTransition(label = "sp").animateFloat(
        0f, 1f, infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "spt"
    )
    val x = (t * 1000).dp
    val y = (sin(t * 4 * PI).toFloat() * 200 + 400).dp
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Text("🕷️", fontSize = 80.sp,
            modifier = Modifier.padding(start = x, top = y))
    }
}

// ── 13. Burning screen ───────────────────────────────────────────────
@Composable
private fun BurningPrank() {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFF5722)),
        contentAlignment = Alignment.Center) {
        Text("🔥", fontSize = 200.sp)
    }
}

// ── 14. X-Ray scanner (fake) ─────────────────────────────────────────
@Composable
private fun XRayPrank() {
    val scan by rememberInfiniteTransition(label = "x").animateFloat(
        0f, 1f, infiniteRepeatable(tween(2500), RepeatMode.Reverse), label = "xs"
    )
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1B1B1B)),
        contentAlignment = Alignment.Center) {
        Text("🦴", fontSize = (120 + scan * 20).sp)
    }
}

// ── 15. Mirror freeze ────────────────────────────────────────────────
@Composable
private fun MirrorFreezePrank() {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFB3E5FC)),
        contentAlignment = Alignment.Center) {
        Text("🧊", fontSize = 200.sp)
    }
}

// ── 16. Fake WiFi lost ───────────────────────────────────────────────
@Composable
private fun FakeWifiLostPrank() {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF263238)),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📶", fontSize = 100.sp)
            Text("WiFi odpojeno", color = Color.White,
                fontWeight = FontWeight.Bold, fontSize = 28.sp)
            Text("Heslo bylo změněno", color = Color.Gray, fontSize = 16.sp)
        }
    }
}

// ── 17. Loading forever ──────────────────────────────────────────────
@Composable
private fun LoadingForeverPrank() {
    val rot by rememberInfiniteTransition(label = "l").animateFloat(
        0f, 360f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "lr"
    )
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D47A1)),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Rotation drives the spinning emoji
            Text("⏳", fontSize = 100.sp,
                modifier = Modifier.padding(16.dp))
            Text("Načítá se…", color = Color.White, fontSize = 24.sp)
            Text("Toto může trvat několik hodin",
                color = Color.LightGray, fontSize = 14.sp)
        }
    }
}

// ── 18. Fake disk full ───────────────────────────────────────────────
@Composable
private fun FakeDiskFullPrank() {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFB71C1C)),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("💾", fontSize = 100.sp)
            Text("ÚLOŽIŠTĚ PLNÉ", color = Color.White,
                fontWeight = FontWeight.Bold, fontSize = 28.sp)
            Text("0 B volných ze 256 GB", color = Color.Yellow, fontSize = 18.sp)
            Text("Nelze pokračovat v práci", color = Color.LightGray, fontSize = 14.sp)
        }
    }
}

// ── 19. Fake bank OTP ────────────────────────────────────────────────
@Composable
private fun FakeBankOtpPrank() {
    val code = remember { (100000..999999).random().toString() }
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF004D40)),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🏦", fontSize = 80.sp)
            Text("Banka XYZ", color = Color.White, fontWeight = FontWeight.Bold,
                fontSize = 24.sp)
            Text("Ověřovací kód:", color = Color.LightGray, fontSize = 16.sp)
            Text(code, color = Color.Yellow, fontWeight = FontWeight.Bold,
                fontSize = 56.sp)
            Text("Nesdílejte tento kód", color = Color.Red, fontSize = 14.sp)
        }
    }
}

// ── 20. Magic button (random outcome) ───────────────────────────────
@Composable
private fun MagicButtonPrank() {
    var msg by remember { mutableStateOf("Stiskni!") }
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF4A148C))
            .clickable {
                msg = listOf(
                    "Vyhrál jsi 1000000 Kč",
                    "Tvůj telefon explodoval",
                    "Telefon byl prodán",
                    "Účet zablokován",
                    "Nový majitel!",
                    "Boss volá zpět",
                    "Mama tě hledá",
                    "Pizza za 5 minut",
                ).random()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🎲", fontSize = 120.sp)
            Text(msg, color = Color.White,
                fontWeight = FontWeight.Bold, fontSize = 24.sp)
        }
    }
}

// ── helpers ──────────────────────────────────────────────────────────

@Suppress("DEPRECATION")
private fun vibratorFor(ctx: android.content.Context): Vibrator? =
    if (Build.VERSION.SDK_INT >= 31) {
        (ctx.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE)
            as? android.os.VibratorManager)?.defaultVibrator
    } else {
        ctx.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
    }

private fun playSineForDuration(freq: Float, ms: Int) {
    val sampleRate = 44100
    val format = AudioFormat.Builder()
        .setSampleRate(sampleRate)
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .build()
    val frames = sampleRate * ms / 1000
    val buf = ShortArray(frames)
    var phase = 0.0
    val step = 2.0 * PI * freq / sampleRate
    for (i in buf.indices) {
        buf[i] = (sin(phase) * 24000).toInt().toShort()
        phase += step
    }
    val track = AudioTrack.Builder()
        .setAudioFormat(format)
        .setBufferSizeInBytes(buf.size * 2)
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()
    track.write(buf, 0, buf.size)
    track.play()
}
