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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zeddihub.mobile.R
import java.text.DateFormat
import java.util.Date
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val GAUGE_MAX_MBPS = 1000f
private const val SWEEP_ANGLE = 240f
private const val START_ANGLE = 150f

@Composable
fun SpeedTestScreen(
    padding: PaddingValues,
    vm: SpeedTestViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val colors = MaterialTheme.colorScheme

    val animated by animateFloatAsState(
        targetValue = (state.downloadMbps ?: 0.0).toFloat(),
        animationSpec = tween(durationMillis = 220),
        label = "needle"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(colors.primary.copy(alpha = 0.14f), Color.Transparent)
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
                Spacer(Modifier.height(8.dp))
                SpeedGauge(
                    mbps = animated,
                    maxMbps = GAUGE_MAX_MBPS,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.15f)
                )
                Spacer(Modifier.height(4.dp))
                if (state.running) {
                    Text(
                        stringResource(R.string.speedtest_running),
                        color = colors.primary,
                        style = MaterialTheme.typography.labelMedium
                    )
                } else if (state.downloadMBs != null) {
                    Text(
                        "%.2f MB/s".format(state.downloadMBs),
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                icon = Icons.Default.Wifi,
                label = stringResource(R.string.speedtest_ping),
                value = state.pingMs?.let { "$it ms" } ?: "—",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                icon = Icons.Default.Download,
                label = stringResource(R.string.speedtest_downloaded),
                value = formatBytes(state.downloadedBytes),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                icon = Icons.Default.Timer,
                label = stringResource(R.string.speedtest_elapsed),
                value = "%.1f s".format(state.elapsedSeconds),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(14.dp))

        Button(
            onClick = vm::start,
            shape = CircleShape,
            enabled = !state.running,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            if (state.running) {
                CircularProgressIndicator(
                    color = colors.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(10.dp))
                Text(stringResource(R.string.speedtest_running), fontWeight = FontWeight.SemiBold)
            } else {
                Icon(Icons.Default.Speed, null)
                Spacer(Modifier.size(10.dp))
                Text(
                    stringResource(R.string.speedtest_start),
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
            Spacer(Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        StatCell(label = stringResource(R.string.speedtest_min), value = state.minMbps)
                        StatCell(label = stringResource(R.string.speedtest_avg), value = state.avgMbps)
                        StatCell(label = stringResource(R.string.speedtest_max), value = state.maxMbps)
                    }
                    Spacer(Modifier.height(12.dp))
                    HistoryChart(
                        points = state.history.map { it.mbps.toFloat() }.reversed(),
                        color = colors.primary,
                        maxValue = state.maxMbps?.toFloat() ?: 100f
                    )
                    Spacer(Modifier.height(8.dp))
                    state.history.forEach { entry ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Text(
                                DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(entry.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "%.1f Mb/s".format(entry.mbps),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurface
                            )
                            entry.pingMs?.let {
                                Text(
                                    " · $it ms",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
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

@Composable
private fun SpeedGauge(mbps: Float, maxMbps: Float, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val clamped = mbps.coerceIn(0f, maxMbps)
    val fraction = clamped / maxMbps
    val progressSweep = SWEEP_ANGLE * fraction

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f + size.height * 0.05f)
            val radius = min(size.width, size.height) * 0.42f
            val stroke = 16.dp.toPx()
            val trackSize = Size(radius * 2, radius * 2)
            val topLeft = Offset(center.x - radius, center.y - radius)

            drawArc(
                color = colors.onSurface.copy(alpha = 0.08f),
                startAngle = START_ANGLE,
                sweepAngle = SWEEP_ANGLE,
                useCenter = false,
                topLeft = topLeft,
                size = trackSize,
                style = Stroke(width = stroke)
            )
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to colors.primary,
                    0.5f to colors.tertiary,
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

            val majorCount = 11
            for (i in 0 until majorCount) {
                val t = i / (majorCount - 1f)
                val angle = Math.toRadians((START_ANGLE + t * SWEEP_ANGLE).toDouble())
                val rIn = radius - stroke * 0.8f
                val rOut = radius + stroke * 0.6f
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

            rotate(degrees = START_ANGLE + progressSweep, pivot = center) {
                val needlePath = Path().apply {
                    moveTo(center.x - 4.dp.toPx(), center.y)
                    lineTo(center.x, center.y - radius * 0.95f)
                    lineTo(center.x + 4.dp.toPx(), center.y)
                    close()
                }
                drawPath(needlePath, color = colors.primary)
            }
            drawCircle(color = colors.primary, radius = 10.dp.toPx(), center = center)
            drawCircle(color = colors.surface, radius = 5.dp.toPx(), center = center)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 36.dp)
        ) {
            Text(
                "%.1f".format(clamped),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )
            Text("Mb/s", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatCell(label: String, value: Double?) {
    val colors = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant
        )
        Text(
            value?.let { "%.1f".format(it) } ?: "—",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.primary
        )
    }
}

@Composable
private fun HistoryChart(points: List<Float>, color: Color, maxValue: Float) {
    Canvas(modifier = Modifier.fillMaxWidth().height(72.dp)) {
        if (points.size < 2 || maxValue <= 0f) return@Canvas
        val stepX = size.width / (points.size - 1)
        val path = Path().apply {
            val y0 = size.height - (points[0] / maxValue) * size.height * 0.95f
            moveTo(0f, y0)
            for (i in 1 until points.size) {
                val x = i * stepX
                val y = size.height - (points[i] / maxValue) * size.height * 0.95f
                lineTo(x, y)
            }
        }
        drawPath(path, color = color, style = Stroke(width = 3.dp.toPx()))
        points.forEachIndexed { i, v ->
            val x = i * stepX
            val y = size.height - (v / maxValue) * size.height * 0.95f
            drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(x, y))
        }
    }
}

@Composable
private fun MetricCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant.copy(alpha = 0.55f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, null, tint = colors.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
        }
    }
}

private fun formatBytes(b: Long): String {
    if (b <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var v = b.toDouble()
    var i = 0
    while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
    return "%.1f %s".format(v, units[i])
}
