package com.zeddihub.mobile.ui.tools

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zeddihub.mobile.R
import com.zeddihub.mobile.ui.common.PermissionGate
import com.zeddihub.mobile.ui.common.Permissions
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DecibelMeterScreen(padding: PaddingValues) {
    PermissionGate(
        permissions = Permissions.MICROPHONE,
        rationale = stringResource(R.string.decibel_rationale)
    ) {
        DecibelContent(padding)
    }
}

@Composable
private fun DecibelContent(padding: PaddingValues) {
    val vm: DecibelMeterViewModel = hiltViewModel()
    val state by vm.state.collectAsState()
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(colors.primary.copy(alpha = 0.18f), Color.Transparent)
                    ),
                    RoundedCornerShape(18.dp)
                )
                .padding(18.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GraphicEq, null, tint = colors.primary, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.size(10.dp))
                    Text(
                        stringResource(R.string.decibel_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.decibel_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        DbGauge(state.currentDb.toFloat(), running = state.running)

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { if (state.running) vm.stop() else vm.start() },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    if (state.running) Icons.Default.Stop else Icons.Default.PlayArrow,
                    null, modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    if (state.running) stringResource(R.string.decibel_stop)
                    else stringResource(R.string.decibel_start)
                )
            }
            OutlinedButton(
                onClick = { vm.reset() },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(stringResource(R.string.decibel_min), state.minDb, colors.tertiary, Modifier.weight(1f))
            StatCard(stringResource(R.string.decibel_avg), state.avgDb, colors.primary, Modifier.weight(1f))
            StatCard(stringResource(R.string.decibel_max), state.maxDb, colors.error, Modifier.weight(1f))
        }

        Spacer(Modifier.height(14.dp))

        if (state.history.size >= 2) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        stringResource(R.string.decibel_history),
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    HistoryLine(state.history, colors.primary)
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    stringResource(R.string.decibel_calibration),
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
                Text(
                    stringResource(R.string.decibel_calibration_hint, state.calibrationOffset),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
                Slider(
                    value = state.calibrationOffset.toFloat(),
                    onValueChange = { vm.setCalibration(it.toDouble()) },
                    valueRange = 70f..130f
                )
            }
        }

        state.error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = colors.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun StatCard(label: String, value: Double?, tint: Color, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            Text(
                value?.let { "%.1f".format(it) } ?: "--",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = tint
            )
        }
    }
}

@Composable
private fun DbGauge(db: Float, running: Boolean) {
    val colors = MaterialTheme.colorScheme
    val animated by animateFloatAsState(
        targetValue = db,
        animationSpec = tween(150),
        label = "db"
    )
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.3f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h * 0.82f
                val radius = minOf(w, h * 1.4f) / 2f * 0.9f
                val startAngle = 150f
                val sweep = 240f
                drawArc(
                    color = colors.onSurface.copy(alpha = 0.08f),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(cx - radius, cy - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = Stroke(width = 26f)
                )
                val pct = (animated / 140f).coerceIn(0f, 1f)
                drawArc(
                    brush = Brush.sweepGradient(
                        0f to colors.primary,
                        0.5f to colors.tertiary,
                        1f to colors.error
                    ),
                    startAngle = startAngle,
                    sweepAngle = sweep * pct,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(cx - radius, cy - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = Stroke(width = 26f)
                )
                for (i in 0..14) {
                    val a = startAngle + sweep * i / 14f
                    val rad = Math.toRadians(a.toDouble())
                    val outer = radius - 6f
                    val inner = radius - 24f
                    drawLine(
                        color = colors.onSurface.copy(alpha = 0.5f),
                        start = androidx.compose.ui.geometry.Offset(
                            (cx + cos(rad) * outer).toFloat(),
                            (cy + sin(rad) * outer).toFloat()
                        ),
                        end = androidx.compose.ui.geometry.Offset(
                            (cx + cos(rad) * inner).toFloat(),
                            (cy + sin(rad) * inner).toFloat()
                        ),
                        strokeWidth = 2f
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "%.1f".format(animated),
                    fontWeight = FontWeight.Bold,
                    fontSize = 46.sp,
                    color = colors.onSurface
                )
                Text(
                    "dB SPL",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )
                if (running) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        loudnessLabel(animated),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryLine(history: List<Double>, tint: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
    ) {
        val min = 20f
        val max = 120f
        val range = max - min
        val dx = size.width / (history.size - 1).toFloat()
        val path = Path()
        history.forEachIndexed { i, v ->
            val clipped = v.toFloat().coerceIn(min, max)
            val y = size.height - ((clipped - min) / range) * size.height
            val x = i * dx
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = tint, style = Stroke(width = 3f))
    }
}

private fun loudnessLabel(db: Float): String = when {
    db < 30 -> "silent"
    db < 50 -> "quiet"
    db < 70 -> "moderate"
    db < 85 -> "loud"
    db < 100 -> "very loud"
    else -> "harmful"
}
