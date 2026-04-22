package com.zeddihub.mobile.ui.tools

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zeddihub.mobile.R
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// Gauge geometry
private const val SWEEP_ANGLE = 240f
private const val START_ANGLE = 150f

@Composable
fun SpeedTestScreen(
    padding: PaddingValues,
    vm: SpeedTestViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current

    // Scale-in on first composition
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.92f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "cardScale"
    )
    val fadeIn by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(420, easing = LinearEasing),
        label = "cardFade"
    )

    // Ripple trigger counter (re-triggered each time running goes false → true)
    var rippleTick by remember { mutableStateOf(0) }
    LaunchedEffect(state.running) {
        if (state.running) rippleTick++
    }

    // Ripple animation (0..1) driven by rippleTick
    val rippleProgress = remember { Animatable(0f) }
    LaunchedEffect(rippleTick) {
        if (rippleTick > 0) {
            rippleProgress.snapTo(0f)
            rippleProgress.animateTo(1f, animationSpec = tween(900))
        }
    }

    // Smoothly animate the live gauge value
    val animatedValue by animateFloatAsState(
        targetValue = state.liveValue.toFloat(),
        animationSpec = tween(durationMillis = 240),
        label = "needle"
    )

    val phaseColor = phaseColor(state.phase, colors.primary)
    val animatedPhaseColor by animateColorAsState(
        targetValue = phaseColor,
        animationSpec = tween(350),
        label = "phaseColor"
    )

    val shareTitle = stringResource(R.string.speedtest_share_chooser_title)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .scale(scale)
    ) {
        // ───── Gauge card ─────
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(animatedPhaseColor.copy(alpha = 0.14f * fadeIn), Color.Transparent)
                        )
                    )
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.speedtest_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    stringResource(R.string.speedtest_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))

                // ── Gauge (pure, no overlay) ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.25f)
                ) {
                    SpeedGauge(
                        value = animatedValue,
                        maxValue = state.gaugeMax.toFloat().coerceAtLeast(1f),
                        color = animatedPhaseColor,
                        rippleProgress = rippleProgress.value,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ── Big value below gauge, fades only on phase change ──
                // The inner Text reads state.liveValue directly so the number
                // updates smoothly during a phase without re-running the fade.
                AnimatedContent(
                    targetState = state.phase,
                    transitionSpec = {
                        fadeIn(tween(320)) togetherWith fadeOut(tween(220))
                    },
                    label = "valuePhase"
                ) { _ ->
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            formatValue(state.liveValue, state.liveUnit),
                            style = MaterialTheme.typography.displayMedium.copy(fontSize = 56.sp),
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            state.liveUnit.suffix,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // ── Phase label at the bottom ──
                AnimatedContent(
                    targetState = state.phase,
                    transitionSpec = {
                        fadeIn(tween(260)) togetherWith fadeOut(tween(180))
                    },
                    label = "phaseLabel"
                ) { phase ->
                    Text(
                        phaseLabel(phase),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = animatedPhaseColor
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ───── 4 metric cards ─────
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                icon = Icons.Default.Wifi,
                label = stringResource(R.string.speedtest_ping),
                value = state.pingMs?.let { "%.0f".format(Locale.US, it) } ?: "—",
                unit = "ms",
                accent = phaseColor(SpeedTestViewModel.Phase.PING, colors.primary),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                icon = Icons.Default.GraphicEq,
                label = stringResource(R.string.speedtest_jitter),
                value = state.jitterMs?.let { "%.1f".format(Locale.US, it) } ?: "—",
                unit = "ms",
                accent = Color(0xFF3B82F6),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                icon = Icons.Default.Download,
                label = stringResource(R.string.speedtest_download),
                value = state.downloadMbps?.let { "%.1f".format(Locale.US, it) } ?: "—",
                unit = "Mbps",
                accent = phaseColor(SpeedTestViewModel.Phase.DOWNLOAD, colors.primary),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                icon = Icons.Default.Upload,
                label = stringResource(R.string.speedtest_upload),
                value = state.uploadMbps?.let { "%.1f".format(Locale.US, it) } ?: "—",
                unit = "Mbps",
                accent = Color(0xFFFFB91F),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(14.dp))

        // ───── Start / Cancel button ─────
        Button(
            onClick = { if (state.running) vm.cancel() else vm.start() },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (state.running) {
                CircularProgressIndicator(
                    color = colors.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(10.dp))
                Text(stringResource(R.string.speedtest_cancel), fontWeight = FontWeight.SemiBold)
            } else {
                Icon(Icons.Default.Speed, null)
                Spacer(Modifier.size(10.dp))
                Text(
                    stringResource(
                        if (state.phase == SpeedTestViewModel.Phase.DONE)
                            R.string.speedtest_run_again
                        else R.string.speedtest_start
                    ),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        state.error?.let { err ->
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.speedtest_error, err),
                color = colors.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // ───── Extended results ─────
        AnimatedVisibility(
            visible = state.phase == SpeedTestViewModel.Phase.DONE,
            enter = fadeIn(tween(450)) + expandVertically(tween(450)),
            exit = fadeOut(tween(180)) + shrinkVertically(tween(200))
        ) {
            Column {
                Spacer(Modifier.height(18.dp))
                ExtendedResults(
                    state = state,
                    primary = colors.primary,
                    onShareImage = {
                        SpeedTestShare.shareResultAsImage(context, state, shareTitle)
                    },
                    onShareText = {
                        SpeedTestShare.shareResultAsText(context, state, shareTitle)
                    },
                    onSharePdf = {
                        SpeedTestShare.shareResultAsPdf(context, state, shareTitle)
                    }
                )
            }
        }

        // ───── History ─────
        if (state.history.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.speedtest_history),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = vm::clearHistory) {
                    Text(stringResource(R.string.speedtest_clear_history))
                }
            }
            Spacer(Modifier.height(6.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    state.history.forEach { entry ->
                        HistoryRow(entry, colors)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.speedtest_powered_by),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant
        )
    }
}

// ──────────────── Extended results ────────────────

@Composable
private fun ExtendedResults(
    state: SpeedTestViewModel.UiState,
    primary: Color,
    onShareImage: () -> Unit,
    onShareText: () -> Unit,
    onSharePdf: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.speedtest_extended_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { expanded = !expanded }) {
                    Text(
                        stringResource(
                            if (expanded) R.string.speedtest_collapse
                            else R.string.speedtest_expand
                        )
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            // ── Ping breakdown (always shown in DONE state) ──
            if (state.pingAttempts > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PingStatCell(
                        label = stringResource(R.string.speedtest_ping_min),
                        value = state.pingMin,
                        color = Color(0xFF22C55E),
                        modifier = Modifier.weight(1f)
                    )
                    PingStatCell(
                        label = stringResource(R.string.speedtest_ping_avg),
                        value = state.pingAvg ?: state.pingMs,
                        color = primary,
                        modifier = Modifier.weight(1f)
                    )
                    PingStatCell(
                        label = stringResource(R.string.speedtest_ping_max),
                        value = state.pingMax,
                        color = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    )
                    PingStatCell(
                        label = stringResource(R.string.speedtest_jitter),
                        value = state.jitterMs,
                        color = Color(0xFF3B82F6),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))

                Text(
                    stringResource(
                        R.string.speedtest_loss_summary,
                        state.pingSuccess, state.pingAttempts, state.lossPct ?: 0.0
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
            }

            // ── Sparklines ──
            if (state.downloadSamples.isNotEmpty()) {
                SparklineRow(
                    label = stringResource(R.string.speedtest_download_samples),
                    samples = state.downloadSamples,
                    color = primary
                )
                Spacer(Modifier.height(8.dp))
            }
            if (state.uploadSamples.isNotEmpty()) {
                SparklineRow(
                    label = stringResource(R.string.speedtest_upload_samples),
                    samples = state.uploadSamples,
                    color = Color(0xFFFFB91F)
                )
                Spacer(Modifier.height(12.dp))
            }

            // ── Collapsible meta block ──
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(220)) + expandVertically(tween(260)),
                exit = fadeOut(tween(160)) + shrinkVertically(tween(200))
            ) {
                Column {
                    if (listOfNotNull(state.ip, state.isp, state.server, state.city).isNotEmpty()) {
                        MetaRow(Icons.Default.Public, stringResource(R.string.speedtest_meta_ip), state.ip ?: "—")
                        MetaRow(Icons.Default.NetworkCheck, stringResource(R.string.speedtest_meta_isp), state.isp ?: "—")
                        MetaRow(Icons.Default.CloudQueue, stringResource(R.string.speedtest_meta_server), state.server ?: "—")
                        state.city?.let { MetaRow(Icons.Default.Public, stringResource(R.string.speedtest_meta_city), it) }
                    }
                    state.connectionType?.let {
                        val detail = buildString {
                            append(it)
                            if (!state.ssid.isNullOrBlank()) append("  ·  ${state.ssid}")
                            state.rssi?.let { r -> append("  ·  $r dBm") }
                        }
                        MetaRow(Icons.Default.Wifi, stringResource(R.string.speedtest_meta_connection), detail)
                    }
                    state.finishedAt?.let {
                        MetaRow(
                            Icons.Default.Speed,
                            stringResource(R.string.speedtest_meta_time),
                            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                                .format(Date(it))
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            // ── Share actions ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onShareImage,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.speedtest_share_image))
                }
                OutlinedButton(
                    onClick = onSharePdf,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.speedtest_share_pdf))
                }
            }
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onShareText, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.speedtest_share_text))
            }
        }
    }
}

@Composable
private fun PingStatCell(
    label: String,
    value: Double?,
    color: Color,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                value?.let { "%.1f".format(Locale.US, it) } ?: "—",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                "ms",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetaRow(icon: ImageVector, label: String, value: String) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Icon(icon, null, tint = colors.primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.width(72.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface
        )
    }
}

@Composable
private fun SparklineRow(label: String, samples: List<Float>, color: Color) {
    val colors = MaterialTheme.colorScheme
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            val last = samples.lastOrNull() ?: 0f
            Text(
                "%.1f Mbps".format(Locale.US, last),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
        Spacer(Modifier.height(4.dp))
        Sparkline(samples = samples, color = color)
    }
}

@Composable
private fun Sparkline(samples: List<Float>, color: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        if (samples.size < 2) return@Canvas
        val maxV = samples.max().coerceAtLeast(0.001f)
        val stepX = size.width / (samples.size - 1)
        val path = Path().apply {
            val y0 = size.height - (samples[0] / maxV) * size.height * 0.95f
            moveTo(0f, y0)
            for (i in 1 until samples.size) {
                val x = i * stepX
                val y = size.height - (samples[i] / maxV) * size.height * 0.95f
                lineTo(x, y)
            }
        }
        // Fill under curve for visual polish
        val fill = Path().apply {
            addPath(path)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            fill,
            brush = Brush.verticalGradient(
                listOf(color.copy(alpha = 0.35f), Color.Transparent)
            )
        )
        drawPath(path, color = color, style = Stroke(width = 3.dp.toPx()))
    }
}

@Composable
private fun HistoryRow(entry: SpeedTestViewModel.HistoryEntry, colors: androidx.compose.material3.ColorScheme) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(Date(entry.timestamp)),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.weight(1.3f)
        )
        Text(
            entry.pingMs?.let { "%.0f ms".format(Locale.US, it) } ?: "—",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF22C55E),
            modifier = Modifier.weight(0.7f)
        )
        Text(
            "%.1f ↓".format(Locale.US, entry.downloadMbps),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = colors.primary,
            modifier = Modifier.weight(0.9f)
        )
        Text(
            "%.1f ↑".format(Locale.US, entry.uploadMbps),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFFFB91F),
            modifier = Modifier.weight(0.9f)
        )
    }
}

// ──────────────── Gauge ────────────────

@Composable
private fun SpeedGauge(
    value: Float,
    maxValue: Float,
    color: Color,
    rippleProgress: Float,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val clamped = value.coerceIn(0f, maxValue)
    val fraction = if (maxValue > 0f) clamped / maxValue else 0f
    val progressSweep = SWEEP_ANGLE * fraction

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = min(size.width, size.height) * 0.42f
            val stroke = 16.dp.toPx()
            val trackSize = Size(radius * 2, radius * 2)
            val topLeft = Offset(center.x - radius, center.y - radius)

            // Track
            drawArc(
                color = colors.onSurface.copy(alpha = 0.08f),
                startAngle = START_ANGLE,
                sweepAngle = SWEEP_ANGLE,
                useCenter = false,
                topLeft = topLeft,
                size = trackSize,
                style = Stroke(width = stroke)
            )
            // Progress
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to color.copy(alpha = 0.9f),
                    0.5f to color,
                    1.0f to Color(0xFFE53935),
                    center = center
                ),
                startAngle = START_ANGLE,
                sweepAngle = progressSweep,
                useCenter = false,
                topLeft = topLeft,
                size = trackSize,
                style = Stroke(width = stroke)
            )

            // Tick marks
            val majorCount = 11
            for (i in 0 until majorCount) {
                val t = i / (majorCount - 1f)
                val angle = Math.toRadians((START_ANGLE + t * SWEEP_ANGLE).toDouble())
                val rIn = radius - stroke * 1.2f
                val rOut = radius - stroke * 0.3f
                drawLine(
                    color = colors.onSurface.copy(alpha = 0.45f),
                    start = Offset(
                        center.x + cos(angle).toFloat() * rIn,
                        center.y + sin(angle).toFloat() * rIn
                    ),
                    end = Offset(
                        center.x + cos(angle).toFloat() * rOut,
                        center.y + sin(angle).toFloat() * rOut
                    ),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Needle — draw pointing RIGHT then rotate by START+progress
            rotate(degrees = START_ANGLE + progressSweep, pivot = center) {
                val tipLen = radius - stroke * 0.6f
                val baseHalf = 4.dp.toPx()
                val needle = Path().apply {
                    moveTo(center.x, center.y - baseHalf)
                    lineTo(center.x + tipLen, center.y)
                    lineTo(center.x, center.y + baseHalf)
                    close()
                }
                drawPath(needle, color = color)
            }

            // Hub
            drawCircle(color = color, radius = 10.dp.toPx(), center = center)
            drawCircle(color = colors.surface, radius = 5.dp.toPx(), center = center)

            // Ripple — expanding ring around the gauge from center
            if (rippleProgress > 0f && rippleProgress < 1f) {
                val rippleR = radius * (1.0f + rippleProgress * 0.4f)
                val alpha = (1f - rippleProgress) * 0.6f
                drawCircle(
                    color = color.copy(alpha = alpha),
                    radius = rippleR,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }
    }
}

// ──────────────── Metric card ────────────────

@Composable
private fun MetricCard(
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant.copy(alpha = 0.55f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            AnimatedContent(
                targetState = value,
                transitionSpec = { slideUpTransition() },
                label = "metric-$label"
            ) { v ->
                Text(
                    v,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
            }
            Text(
                unit,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

// ──────────────── Helpers ────────────────

private fun slideUpTransition(): ContentTransform =
    (slideInVertically(animationSpec = tween(320)) { it } +
        fadeIn(animationSpec = tween(240))) togetherWith
        (slideOutVertically(animationSpec = tween(260)) { -it } +
            fadeOut(animationSpec = tween(180)))

@Composable
private fun phaseLabel(phase: SpeedTestViewModel.Phase): String = when (phase) {
    SpeedTestViewModel.Phase.IDLE -> stringResource(R.string.speedtest_phase_idle)
    SpeedTestViewModel.Phase.META -> stringResource(R.string.speedtest_phase_meta)
    SpeedTestViewModel.Phase.PING -> stringResource(R.string.speedtest_phase_ping)
    SpeedTestViewModel.Phase.DOWNLOAD -> stringResource(R.string.speedtest_phase_download)
    SpeedTestViewModel.Phase.UPLOAD -> stringResource(R.string.speedtest_phase_upload)
    SpeedTestViewModel.Phase.DONE -> stringResource(R.string.speedtest_phase_done)
}

private fun phaseColor(phase: SpeedTestViewModel.Phase, primary: Color): Color = when (phase) {
    SpeedTestViewModel.Phase.PING -> Color(0xFF22C55E)
    SpeedTestViewModel.Phase.DOWNLOAD -> primary
    SpeedTestViewModel.Phase.UPLOAD -> Color(0xFFFFB91F)
    SpeedTestViewModel.Phase.DONE -> primary
    else -> primary
}

private fun formatValue(v: Double, unit: SpeedTestViewModel.Unit): String = when (unit) {
    SpeedTestViewModel.Unit.MS -> "%.0f".format(Locale.US, v)
    SpeedTestViewModel.Unit.MBPS -> if (v < 100) "%.2f".format(Locale.US, v)
    else "%.1f".format(Locale.US, v)
}
