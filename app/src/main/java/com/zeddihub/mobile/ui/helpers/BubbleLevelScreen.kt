package com.zeddihub.mobile.ui.helpers

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zeddihub.mobile.R
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Digital bubble level that uses the accelerometer to compute pitch / roll
 * relative to the phone's frame. Drawing logic:
 *
 *   - A big circle shows the 2-axis bubble (like a carpenter's disc level)
 *   - Two horizontal bars show roll and pitch separately (single-axis)
 *
 * Calibration stores the current (x, y, z) reading as a zero offset so
 * the level can be used on tilted surfaces too. Stored only in-memory.
 */
@Composable
fun BubbleLevelScreen(padding: PaddingValues) {
    val colors = MaterialTheme.colorScheme
    val ctx = LocalContext.current

    // Raw accelerometer in m/s^2; gravity along each axis.
    var ax by remember { mutableFloatStateOf(0f) }
    var ay by remember { mutableFloatStateOf(0f) }
    var az by remember { mutableFloatStateOf(9.81f) }
    // Zero offsets applied when Calibrate is pressed.
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Track sensor availability so we can surface a friendly error
    // instead of silently rendering a frozen bubble at (0,0).
    var sensorMissing by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val sm = ctx.getSystemService(android.content.Context.SENSOR_SERVICE) as? SensorManager
        val sensor: Sensor? = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sm == null || sensor == null) {
            sensorMissing = true
            // Nothing to register / unregister — return a no-op disposer.
            return@DisposableEffect onDispose { /* noop */ }
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val v = event?.values ?: return
                // Low-pass filter smooths high-frequency jitter without
                // feeling laggy (alpha=0.2 is a good compromise).
                val alpha = 0.2f
                ax = ax * (1 - alpha) + v[0] * alpha
                ay = ay * (1 - alpha) + v[1] * alpha
                az = az * (1 - alpha) + v[2] * alpha
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(listener) }
    }

    if (sensorMissing) {
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.level_sensor_missing),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp)
            )
        }
        return
    }

    // Compute angles after offset.
    val dx = ax - offsetX
    val dy = ay - offsetY
    // pitch/roll in degrees
    val roll = (atan2(dx.toDouble(), sqrt((dy * dy + az * az).toDouble())) * 180.0 / Math.PI).toFloat()
    val pitch = (atan2(dy.toDouble(), sqrt((dx * dx + az * az).toDouble())) * 180.0 / Math.PI).toFloat()
    val tiltMag = sqrt((roll * roll + pitch * pitch).toDouble()).toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(padding)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.level_hold_flat),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        // 2-axis bubble canvas
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.size(280.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                val w = size.width
                val h = size.height
                val cx = w / 2
                val cy = h / 2
                val radius = min(w, h) / 2 - 8f

                // Outer target rings
                drawCircle(
                    color = Color(0xFF3E5266),
                    radius = radius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 3f)
                )
                drawCircle(
                    color = Color(0xFF3E5266),
                    radius = radius * 0.66f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2f)
                )
                drawCircle(
                    color = Color(0xFF3E5266),
                    radius = radius * 0.33f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2f)
                )
                // Crosshair
                drawLine(
                    color = Color(0xFF3E5266),
                    start = Offset(cx - radius, cy),
                    end = Offset(cx + radius, cy),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color(0xFF3E5266),
                    start = Offset(cx, cy - radius),
                    end = Offset(cx, cy + radius),
                    strokeWidth = 2f
                )

                // Bubble — clamp to ring edge, color shifts to green near zero.
                val maxDeg = 25f
                val nx = max(-1f, min(1f, -roll / maxDeg))
                val ny = max(-1f, min(1f, pitch / maxDeg))
                val bx = cx + nx * radius * 0.85f
                val by = cy + ny * radius * 0.85f
                val levelGood = tiltMag < 0.5f
                val bubbleColor = if (levelGood) Color(0xFF3AE06B) else Color(0xFFFFB05B)
                drawCircle(
                    color = bubbleColor.copy(alpha = 0.25f),
                    radius = 36f,
                    center = Offset(bx, by)
                )
                drawCircle(
                    color = bubbleColor,
                    radius = 22f,
                    center = Offset(bx, by)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AngleChip(
                label = stringResource(R.string.level_angle_x, roll),
                modifier = Modifier.weight(1f)
            )
            AngleChip(
                label = stringResource(R.string.level_angle_y, pitch),
                modifier = Modifier.weight(1f)
            )
            AngleChip(
                label = stringResource(R.string.level_angle_z, tiltMag),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                offsetX = ax
                offsetY = ay
            }) {
                Text(stringResource(R.string.level_calibrate))
            }
            OutlinedButton(onClick = {
                offsetX = 0f
                offsetY = 0f
            }) {
                Text(stringResource(R.string.level_reset))
            }
        }
    }
}

@Composable
private fun AngleChip(label: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.height(56.dp),
        color = colors.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )
        }
    }
}

@Suppress("unused")
private fun dummyAbs(v: Float) = abs(v)
