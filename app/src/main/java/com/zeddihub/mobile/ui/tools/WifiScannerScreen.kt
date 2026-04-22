package com.zeddihub.mobile.ui.tools

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zeddihub.mobile.R
import com.zeddihub.mobile.ui.common.PermissionGate
import com.zeddihub.mobile.ui.common.Permissions

@Composable
fun WifiScannerScreen(padding: PaddingValues) {
    PermissionGate(
        permissions = Permissions.WIFI_SCAN,
        rationale = stringResource(R.string.wifi_scan_rationale)
    ) {
        WifiScannerContent(padding)
    }
}

@Composable
private fun WifiScannerContent(padding: PaddingValues) {
    val vm: WifiScannerViewModel = hiltViewModel()
    val state by vm.state.collectAsState()
    val colors = MaterialTheme.colorScheme

    DisposableEffect(Unit) {
        vm.start()
        onDispose { vm.stop() }
    }

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
                    Icon(Icons.Default.Wifi, null, tint = colors.primary, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.size(10.dp))
                    Text(
                        stringResource(R.string.wifi_scanner_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary,
                        modifier = Modifier.weight(1f)
                    )
                    if (state.scanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = colors.primary
                        )
                    } else {
                        IconButton(onClick = { vm.start() }) {
                            Icon(Icons.Default.Refresh, null, tint = colors.primary)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(
                        R.string.wifi_scanner_count,
                        state.networks.size
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        if (state.networks.isNotEmpty()) {
            RadarCard(
                networks = state.networks,
                selectedBssid = state.selectedBssid,
                onSelect = { vm.select(it) }
            )
            Spacer(Modifier.height(14.dp))
        }

        val twoFour = state.networks.filter { it.band == WifiScannerViewModel.Band.GHZ_2_4 }
        if (twoFour.isNotEmpty()) {
            Text(
                stringResource(R.string.wifi_scanner_channels_24),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )
            Spacer(Modifier.height(8.dp))
            ChannelOverlapCard(twoFour, minChannel = 1, maxChannel = 14)
            Spacer(Modifier.height(14.dp))
        }

        val five = state.networks.filter { it.band == WifiScannerViewModel.Band.GHZ_5 }
        if (five.isNotEmpty()) {
            Text(
                stringResource(R.string.wifi_scanner_channels_5),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )
            Spacer(Modifier.height(8.dp))
            ChannelOverlapCard(five, minChannel = 32, maxChannel = 177)
            Spacer(Modifier.height(14.dp))
        }

        Text(
            stringResource(R.string.wifi_scanner_networks),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface
        )
        Spacer(Modifier.height(8.dp))
        if (state.networks.isEmpty()) {
            Text(
                stringResource(R.string.wifi_scanner_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            state.networks.forEach { net ->
                NetworkCard(
                    net = net,
                    expanded = state.selectedBssid == net.bssid,
                    onClick = {
                        vm.select(if (state.selectedBssid == net.bssid) null else net.bssid)
                    }
                )
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun RadarCard(
    networks: List<WifiScannerViewModel.Network>,
    selectedBssid: String?,
    onSelect: (String?) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    // Stable angle per BSSID so dots don't jump frame to frame.
    val angled = networks.mapIndexed { idx, net ->
        val hash = net.bssid.hashCode()
        val angle = ((hash and 0xFFFF) / 65535f) * (2f * Math.PI.toFloat())
        Triple(net, angle, idx)
    }

    // Animated sweep line.
    val transition = rememberInfiniteTransition(label = "radar")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.wifi_scanner_radar_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )
            Spacer(Modifier.height(10.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val r = minOf(cx, cy) * 0.92f
                val ringColor = colors.primary.copy(alpha = 0.22f)
                val axisColor = colors.primary.copy(alpha = 0.12f)

                // Concentric rings (1, 5, 15, 30m-ish guides)
                for (i in 1..4) {
                    drawCircle(
                        color = ringColor,
                        radius = r * i / 4f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5f)
                    )
                }
                // Crosshair
                drawLine(axisColor, Offset(cx - r, cy), Offset(cx + r, cy), strokeWidth = 1f)
                drawLine(axisColor, Offset(cx, cy - r), Offset(cx, cy + r), strokeWidth = 1f)

                // Sweeping fan
                val sweepPath = Path().apply {
                    moveTo(cx, cy)
                    val a0 = sweep - 0.5f
                    val a1 = sweep
                    lineTo(cx + r * cos(a0), cy + r * sin(a0))
                    val steps = 16
                    for (s in 1..steps) {
                        val a = a0 + (a1 - a0) * s / steps
                        lineTo(cx + r * cos(a), cy + r * sin(a))
                    }
                    close()
                }
                drawPath(
                    path = sweepPath,
                    color = colors.primary.copy(alpha = 0.18f)
                )

                // Center "you"
                drawCircle(colors.primary, 6f, Offset(cx, cy))

                // Networks as dots — distance = stronger signal closer to center.
                angled.forEach { (net, angle, idx) ->
                    val dist = net.distanceMeters.toFloat().coerceIn(0.3f, 40f)
                    // log scale: 0.3m → near center, 40m → outer ring
                    val t = (kotlin.math.ln((dist + 0.2f).toDouble()) /
                        kotlin.math.ln(41.0)).toFloat().coerceIn(0f, 1f)
                    val pr = r * t
                    val px = cx + pr * cos(angle)
                    val py = cy + pr * sin(angle)
                    val hue = (idx * 53) % 360
                    val dotColor = Color.hsv(hue.toFloat(), 0.55f, 0.95f)
                    val selected = net.bssid == selectedBssid
                    val radius = if (selected) 14f else 9f
                    drawCircle(dotColor.copy(alpha = 0.25f), radius * 2.2f, Offset(px, py))
                    drawCircle(dotColor, radius, Offset(px, py))
                    if (selected) {
                        drawCircle(
                            color = colors.onSurface,
                            radius = radius + 3f,
                            center = Offset(px, py),
                            style = Stroke(width = 2f)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.wifi_scanner_radar_hint),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChannelOverlapCard(
    networks: List<WifiScannerViewModel.Network>,
    minChannel: Int,
    maxChannel: Int
) {
    val colors = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                Canvas(
                    modifier = Modifier
                        .height(160.dp)
                        .size(width = ((maxChannel - minChannel + 1) * 26).dp, height = 160.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val channels = (maxChannel - minChannel + 1).toFloat()
                    val dx = w / channels
                    for (i in 0..4) {
                        val y = h - (h * i / 4f)
                        drawLine(
                            color = colors.onSurface.copy(alpha = 0.08f),
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(w, y),
                            strokeWidth = 1f
                        )
                    }
                    networks.forEachIndexed { idx, net ->
                        val ch = net.channel
                        if (ch < minChannel || ch > maxChannel) return@forEachIndexed
                        val centerX = (ch - minChannel + 0.5f) * dx
                        val strength = ((net.rssi + 100).coerceIn(0, 70)) / 70f
                        val peak = h - h * strength * 0.9f
                        val halfWidth = dx * 2.2f
                        val hue = (idx * 53) % 360
                        val color = Color.hsv(hue.toFloat(), 0.65f, 0.9f, alpha = 0.35f)
                        val path = Path().apply {
                            moveTo(centerX - halfWidth, h)
                            quadraticBezierTo(centerX, peak, centerX + halfWidth, h)
                            close()
                        }
                        drawPath(path, color = color)
                        drawPath(path, color = color.copy(alpha = 0.9f), style = Stroke(width = 2f))
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkCard(
    net: WifiScannerViewModel.Network,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SignalBars(net.rssi, colors.primary)
                Spacer(Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(net.ssid, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                    Text(
                        "${net.rssi} dBm · ch ${net.channel} · ${bandLabel(net.band)} · ${net.security}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
                Text(
                    "~${"%.1f".format(net.distanceMeters)} m",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (expanded) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "BSSID: ${net.bssid} · ${net.frequencyMhz} MHz",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                SignalSparkline(net.history, colors.primary)
            }
        }
    }
}

@Composable
private fun SignalBars(rssi: Int, tint: Color) {
    val level = when {
        rssi >= -55 -> 4
        rssi >= -65 -> 3
        rssi >= -75 -> 2
        rssi >= -85 -> 1
        else -> 0
    }
    Canvas(modifier = Modifier.size(22.dp)) {
        val bars = 4
        val gap = size.width / (bars * 2f)
        val barW = (size.width - gap * (bars + 1)) / bars
        for (i in 0 until bars) {
            val h = size.height * (i + 1) / bars.toFloat()
            val x = gap + i * (barW + gap)
            val y = size.height - h
            val on = i < level
            drawRect(
                color = if (on) tint else tint.copy(alpha = 0.22f),
                topLeft = androidx.compose.ui.geometry.Offset(x, y),
                size = androidx.compose.ui.geometry.Size(barW, h)
            )
        }
    }
}

@Composable
private fun SignalSparkline(history: List<Int>, tint: Color) {
    if (history.size < 2) return
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        val min = -100f
        val max = -30f
        val range = max - min
        val dx = size.width / (history.size - 1).toFloat()
        val path = Path()
        history.forEachIndexed { i, v ->
            val y = size.height - ((v.coerceIn(min.toInt(), max.toInt()) - min) / range) * size.height
            val x = i * dx
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = tint, style = Stroke(width = 3f))
    }
}

private fun bandLabel(b: WifiScannerViewModel.Band): String = when (b) {
    WifiScannerViewModel.Band.GHZ_2_4 -> "2.4 GHz"
    WifiScannerViewModel.Band.GHZ_5 -> "5 GHz"
    WifiScannerViewModel.Band.GHZ_6 -> "6 GHz"
    WifiScannerViewModel.Band.UNKNOWN -> "?"
}
