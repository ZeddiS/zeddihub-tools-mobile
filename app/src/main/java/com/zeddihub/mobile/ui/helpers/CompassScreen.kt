package com.zeddihub.mobile.ui.helpers

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zeddihub.mobile.R
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Kompas — používá TYPE_ROTATION_VECTOR pro stabilnější a přesnější
 * azimut než kombinace accelerometeru + magnetometeru. RotationVector
 * dělá Android sensor fusion, takže dostáváme už vyhlazený kvaternion.
 *
 * UI: velká kompasová růžice (N/E/S/W + stupně po 30°), pod ní
 * aktuální azimut ve stupních + světová strana. Rotace je animovaná
 * krátkým tween spring-like efektem (220 ms) aby růžice nepoblikávala
 * při šumu ze senzoru.
 *
 * Poznámky:
 *   • Hlášku "bez senzoru" ukážeme na zařízeních bez ROTATION_VECTOR
 *     (staré tablety / některé emulátory).
 *   • Azimut se kalibruje podle směru, kterým je zařízení otočeno
 *     horní stranou (portrait). Pro jiná natočení by šlo použít
 *     `remapCoordinateSystem`, ale 99 % uživatelů drží telefon
 *     svisle, takže to nekomplikujeme.
 */
@Composable
fun CompassScreen(padding: PaddingValues) {
    val ctx = LocalContext.current
    val sensorManager = remember { ctx.getSystemService(android.content.Context.SENSOR_SERVICE) as? SensorManager }
    val rotationSensor = remember { sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }

    var azimuth by remember { mutableFloatStateOf(0f) }

    DisposableEffect(rotationSensor) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor?.type != Sensor.TYPE_ROTATION_VECTOR) return
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)
                // azimuth in radians → degrees, normalised to [0, 360)
                val deg = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
                azimuth = deg
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (sensorManager != null && rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sensorManager?.unregisterListener(listener) }
    }

    // Smooth the dial with a short tween so it tracks the azimuth
    // without jittering around noisy sensor values. The trick is to
    // animate "shortest path" around the 0/360 seam — we feed the
    // animator the delta in [-180, 180] and accumulate manually.
    val smoothAngle = rememberSmoothedAngle(azimuth)

    if (rotationSensor == null) {
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(R.string.compass_no_sensor),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp)
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
            contentAlignment = Alignment.Center
        ) {
            CompassRose(angleDeg = smoothAngle)
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "${azimuth.roundToInt()}°  ${cardinal(azimuth)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.compass_caption),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Text(
            stringResource(R.string.compass_calibration_tip),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Accumulator that converts the raw [0, 360) sensor azimuth into a
 * monotone-ish angle so `animateFloatAsState` can tween along the
 * shortest arc around the 0/360 boundary. Without this the dial would
 * spin the long way round every time the user crosses north.
 */
@Composable
private fun rememberSmoothedAngle(rawDeg: Float): Float {
    var smoothed by remember { mutableFloatStateOf(rawDeg) }
    // Compute the shortest signed delta to the current smoothed value.
    val delta = ((rawDeg - smoothed + 540f) % 360f) - 180f
    smoothed += delta
    val animated by animateFloatAsState(
        targetValue = smoothed,
        animationSpec = tween(durationMillis = 180),
        label = "compassAngle"
    )
    return animated
}

@Composable
private fun CompassRose(angleDeg: Float) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val onSurface = MaterialTheme.colorScheme.onSurface
    val tertiary = MaterialTheme.colorScheme.tertiary

    // Hoist the Paint and pre-resolved ARGB ints out of the draw scope.
    // The original code allocated a new Paint and called Color.toArgb()
    // (which itself constructs an Int from four float multiplications)
    // on every frame. With the sensor pumping at SENSOR_DELAY_UI plus
    // the smoothing animation, that's ~60 frames/sec of throwaway
    // garbage. `remember` keeps a single shared instance.
    val cardinalPaint = remember { android.graphics.Paint().apply {
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
        textSize = 38f
        isFakeBoldText = true
    } }
    val primaryArgb = remember(primary) { primary.toArgb() }
    val onSurfaceArgb = remember(onSurface) { onSurface.toArgb() }
    val letters = remember { listOf(0 to "N", 90 to "E", 180 to "S", 270 to "W") }

    Canvas(modifier = Modifier.size(300.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.minDimension / 2f - 12f

        // Ring
        drawCircle(
            color = outline,
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 2f)
        )

        rotate(degrees = -angleDeg, pivot = Offset(cx, cy)) {
            // Tick marks every 30°
            for (deg in 0 until 360 step 10) {
                val isMajor = (deg % 30 == 0)
                val isCardinal = (deg % 90 == 0)
                val tickInner = if (isCardinal) radius - 26f
                else if (isMajor) radius - 18f
                else radius - 10f
                val radians = Math.toRadians(deg.toDouble() - 90.0)
                val x1 = cx + (radius - 2f) * Math.cos(radians).toFloat()
                val y1 = cy + (radius - 2f) * Math.sin(radians).toFloat()
                val x2 = cx + tickInner * Math.cos(radians).toFloat()
                val y2 = cy + tickInner * Math.sin(radians).toFloat()
                drawLine(
                    color = if (isCardinal) primary else outline,
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = if (isCardinal) 3.5f else if (isMajor) 2.2f else 1.2f
                )
            }

            // Cardinal letters (N/E/S/W). Drawn via nativeCanvas because
            // Compose doesn't yet expose a first-class text in Canvas API
            // that rotates cleanly with the shape.
            for ((deg, label) in letters) {
                cardinalPaint.color = if (label == "N") primaryArgb else onSurfaceArgb
                val radians = Math.toRadians(deg.toDouble() - 90.0)
                val r = radius - 52f
                val x = cx + r * Math.cos(radians).toFloat()
                val y = cy + r * Math.sin(radians).toFloat() +
                    (cardinalPaint.textSize / 3f) // baseline adjust
                drawContext.canvas.nativeCanvas.drawText(label, x, y, cardinalPaint)
            }
        }

        // Fixed red needle pointing north (inside the rotating rose it
        // would follow the dial — we want it static in world space).
        val needleLen = radius - 40f
        drawLine(
            color = tertiary,
            start = Offset(cx, cy),
            end = Offset(cx, cy - needleLen),
            strokeWidth = 5f
        )
        drawCircle(
            color = primary,
            radius = 8f,
            center = Offset(cx, cy),
        )
    }
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(),
    (red * 255).toInt(),
    (green * 255).toInt(),
    (blue * 255).toInt(),
)

/**
 * Cardinal direction (8-point rose) for a given azimuth.
 * Czech abbreviations since the rest of the app is CS-first.
 */
private fun cardinal(azimuth: Float): String {
    val dirs = arrayOf("S", "SV", "V", "JV", "J", "JZ", "Z", "SZ")
    val idx = (((azimuth + 22.5f) % 360f) / 45f).toInt().coerceIn(0, 7)
    // Hide "null" N indicator when value is wildly off (e.g. sensor
    // hasn't reported anything yet) — normalised azimuth keeps it at 0.
    return if (abs(azimuth) < 0.001f) "S" else dirs[idx]
}
