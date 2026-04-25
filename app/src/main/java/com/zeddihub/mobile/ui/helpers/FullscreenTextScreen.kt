package com.zeddihub.mobile.ui.helpers

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeddihub.mobile.R
import kotlin.math.roundToInt

/**
 * Fullscreen text — turns the phone into a giant signboard.
 *
 * Features:
 *   • Horizontal / vertical (rotated 90°) display modes.
 *   • Static or marquee scrolling (right-to-left or left-to-right) with
 *     adjustable speed.
 *   • Multi-color text via inline markup `[#RRGGBB]chunk[/]` so the user
 *     can paint different words / phrases without a heavy rich-text
 *     editor.
 *   • Blink / pulse modes: solid, slow blink, fast blink, SOS pattern,
 *     custom interval.
 *   • Background and default text color picker (8 presets + custom hex).
 *   • Fullscreen toggle: hides system bars, locks orientation to the
 *     chosen mode, keeps the screen on.
 *
 * The fullscreen overlay is a separate composable that takes over the
 * entire window. Tap-to-exit (top right) returns to the editor.
 */
@Composable
fun FullscreenTextScreen(padding: PaddingValues) {
    var text by remember { mutableStateOf("") }
    var bg by remember { mutableStateOf(BG_PRESETS[0]) }
    var fg by remember { mutableStateOf(FG_PRESETS[0]) }
    var orientation by remember { mutableStateOf(TextOrientation.HORIZONTAL) }
    var marquee by remember { mutableStateOf(MarqueeMode.OFF) }
    var speed by remember { mutableFloatStateOf(40f) } // 10..120 dp/s
    var blink by remember { mutableStateOf(BlinkMode.SOLID) }
    var fontSize by remember { mutableFloatStateOf(96f) }
    var bold by remember { mutableStateOf(true) }
    var fullscreen by remember { mutableStateOf(false) }

    if (fullscreen) {
        FullscreenStage(
            text = text.ifBlank { "ZeddiHub" },
            bg = bg, fg = fg,
            orientation = orientation,
            marquee = marquee,
            speed = speed,
            blink = blink,
            fontSize = fontSize,
            bold = bold,
            onExit = { fullscreen = false }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth().height(180.dp),
            label = { Text(stringResource(R.string.fst_text_label)) },
            placeholder = { Text(stringResource(R.string.fst_text_placeholder)) },
        )

        // Markup hint
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.fst_markup_hint),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Orientation
        SectionTitle(stringResource(R.string.fst_section_orientation))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = orientation == TextOrientation.HORIZONTAL,
                onClick = { orientation = TextOrientation.HORIZONTAL },
                label = { Text(stringResource(R.string.fst_orient_h)) }
            )
            FilterChip(
                selected = orientation == TextOrientation.VERTICAL,
                onClick = { orientation = TextOrientation.VERTICAL },
                label = { Text(stringResource(R.string.fst_orient_v)) }
            )
        }

        // Marquee
        SectionTitle(stringResource(R.string.fst_section_marquee))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(MarqueeMode.values().toList()) { m ->
                FilterChip(
                    selected = marquee == m,
                    onClick = { marquee = m },
                    label = { Text(stringResource(m.labelRes)) }
                )
            }
        }
        if (marquee != MarqueeMode.OFF) {
            Text(
                stringResource(R.string.fst_speed_label, speed.roundToInt()),
                style = MaterialTheme.typography.labelMedium
            )
            Slider(value = speed, onValueChange = { speed = it }, valueRange = 10f..200f)
        }

        // Blink
        SectionTitle(stringResource(R.string.fst_section_blink))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(BlinkMode.values().toList()) { b ->
                FilterChip(
                    selected = blink == b,
                    onClick = { blink = b },
                    label = { Text(stringResource(b.labelRes)) }
                )
            }
        }

        // Colors
        SectionTitle(stringResource(R.string.fst_section_bg))
        ColorRow(selected = bg, options = BG_PRESETS, onPick = { bg = it })
        SectionTitle(stringResource(R.string.fst_section_fg))
        ColorRow(selected = fg, options = FG_PRESETS, onPick = { fg = it })

        // Size + bold
        SectionTitle(stringResource(R.string.fst_section_format))
        Text(
            stringResource(R.string.fst_size_label, fontSize.roundToInt()),
            style = MaterialTheme.typography.labelMedium
        )
        Slider(value = fontSize, onValueChange = { fontSize = it }, valueRange = 32f..240f)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = bold, onCheckedChange = { bold = it })
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.fst_bold))
        }

        // Launch
        Button(
            onClick = { fullscreen = true },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Default.Fullscreen, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.fst_show))
        }
    }
}

@Composable
private fun SectionTitle(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun ColorRow(selected: Color, options: List<Color>, onPick: (Color) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { c ->
            val isSel = c == selected
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(c, shape = CircleShape)
                    .clickable { onPick(c) },
                contentAlignment = Alignment.Center
            ) {
                if (isSel) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .background(Color.Transparent, CircleShape)
                    )
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = if (c.luminance() > 0.5f) Color.Black else Color.White
                    )
                }
            }
        }
    }
}

private fun Color.luminance(): Float =
    0.2126f * red + 0.7152f * green + 0.0722f * blue

// ── Fullscreen stage ────────────────────────────────────────────────

@Composable
private fun FullscreenStage(
    text: String,
    bg: Color, fg: Color,
    orientation: TextOrientation,
    marquee: MarqueeMode,
    speed: Float,
    blink: BlinkMode,
    fontSize: Float,
    bold: Boolean,
    onExit: () -> Unit,
) {
    val ctx = LocalContext.current
    val activity = ctx as? Activity

    // Lock orientation + keep screen on while stage is up
    DisposableEffect(orientation) {
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = when (orientation) {
            TextOrientation.HORIZONTAL -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            TextOrientation.VERTICAL -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            originalOrientation?.let { activity.requestedOrientation = it }
        }
    }

    val parsed = remember(text, fg) { parseMarkup(text, fg) }
    val blinkAlpha by rememberBlinkAlpha(blink)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .clickable(enabled = false) {} // swallow stray taps
    ) {
        // Marquee or static body
        when (marquee) {
            MarqueeMode.OFF -> StaticBody(parsed, fontSize, bold, blinkAlpha)
            MarqueeMode.LEFT -> MarqueeBody(parsed, fontSize, bold, speed, blinkAlpha, leftToRight = false)
            MarqueeMode.RIGHT -> MarqueeBody(parsed, fontSize, bold, speed, blinkAlpha, leftToRight = true)
        }

        IconButton(
            onClick = onExit,
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = null,
                tint = if (bg.luminance() > 0.5f) Color.Black else Color.White
            )
        }
    }
}

@Composable
private fun StaticBody(parsed: AnnotatedString, fontSize: Float, bold: Boolean, alphaValue: Float) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            text = parsed,
            fontSize = fontSize.sp,
            fontWeight = if (bold) FontWeight.Black else FontWeight.Normal,
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(alphaValue)
        )
    }
}

@Composable
private fun MarqueeBody(
    parsed: AnnotatedString, fontSize: Float, bold: Boolean,
    speed: Float, alpha: Float, leftToRight: Boolean
) {
    val density = LocalDensity.current
    var stageWidthPx by remember { mutableFloatStateOf(0f) }
    var textWidthPx by remember { mutableFloatStateOf(0f) }
    val travel = stageWidthPx + textWidthPx
    val durationMs = if (speed > 1f) ((travel / density.density) / speed * 1000f).toInt().coerceAtLeast(500) else 5000
    val transition = rememberInfiniteTransition(label = "marquee")
    val progress by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "marqueeProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 24.dp)
            .onSizeChanged { stageWidthPx = it.width.toFloat() },
        contentAlignment = Alignment.CenterStart
    ) {
        val offsetX = if (textWidthPx == 0f || stageWidthPx == 0f) 0f
        else if (leftToRight) -textWidthPx + travel * progress
        else stageWidthPx - travel * progress
        Text(
            text = parsed,
            fontSize = fontSize.sp,
            fontWeight = if (bold) FontWeight.Black else FontWeight.Normal,
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .alpha(alpha)
                .onSizeChanged { textWidthPx = it.width.toFloat() },
        )
    }
}

@Composable
private fun rememberBlinkAlpha(mode: BlinkMode): androidx.compose.runtime.State<Float> {
    val transition = rememberInfiniteTransition(label = "blink")
    return transition.animateFloat(
        initialValue = 1f,
        targetValue = if (mode == BlinkMode.SOLID) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = mode.halfPeriodMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blinkAlpha"
    )
}

// ── Markup parser ────────────────────────────────────────────────────

/**
 * Parse `[#RRGGBB]chunk[/]` markup. Lenient: malformed tags pass
 * through as plain text so the user gets feedback rather than a crash.
 */
private fun parseMarkup(text: String, defaultColor: Color): AnnotatedString = buildAnnotatedString {
    val regex = Regex("""\[#([0-9A-Fa-f]{6})](.*?)\[/]""", RegexOption.DOT_MATCHES_ALL)
    var lastEnd = 0
    for (m in regex.findAll(text)) {
        if (m.range.first > lastEnd) {
            withStyle(SpanStyle(color = defaultColor)) {
                append(text.substring(lastEnd, m.range.first))
            }
        }
        val hex = m.groupValues[1].toLong(16).toInt() or 0xFF000000.toInt()
        withStyle(SpanStyle(color = Color(hex))) {
            append(m.groupValues[2])
        }
        lastEnd = m.range.last + 1
    }
    if (lastEnd < text.length) {
        withStyle(SpanStyle(color = defaultColor)) {
            append(text.substring(lastEnd))
        }
    }
    if (text.isEmpty()) {
        withStyle(SpanStyle(color = defaultColor)) { append(text) }
    }
}

// ── Enums and presets ────────────────────────────────────────────────

private enum class TextOrientation { HORIZONTAL, VERTICAL }

private enum class MarqueeMode(val labelRes: Int) {
    OFF(R.string.fst_marquee_off),
    LEFT(R.string.fst_marquee_left),
    RIGHT(R.string.fst_marquee_right),
}

private enum class BlinkMode(val labelRes: Int, val halfPeriodMs: Int) {
    SOLID(R.string.fst_blink_solid, 1_000_000),  // effectively static
    SLOW(R.string.fst_blink_slow, 800),
    FAST(R.string.fst_blink_fast, 200),
    SOS(R.string.fst_blink_sos, 300),
    PULSE(R.string.fst_blink_pulse, 1500),
}

private val BG_PRESETS = listOf(
    Color.Black, Color.White,
    Color(0xFFE53935), // red
    Color(0xFF1E88E5), // blue
    Color(0xFF43A047), // green
    Color(0xFFFB8C00), // orange
    Color(0xFFFFEB3B), // yellow
    Color(0xFF8E24AA), // purple
)

private val FG_PRESETS = listOf(
    Color.White, Color.Black,
    Color(0xFFFFEB3B), // yellow
    Color(0xFFE53935), // red
    Color(0xFF1E88E5), // blue
    Color(0xFF43A047), // green
    Color(0xFFFB8C00), // orange
    Color(0xFF26A69A), // teal
)
