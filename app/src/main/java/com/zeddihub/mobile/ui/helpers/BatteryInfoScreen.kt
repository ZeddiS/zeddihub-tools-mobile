package com.zeddihub.mobile.ui.helpers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Stav baterie — realtime odběr přes `ACTION_BATTERY_CHANGED` sticky
 * broadcast (okamžitá data) + doplněk z BatteryManager (current_now
 * v µA, dostupný od API 21+).
 *
 * UI: velká stylizovaná "baterka" s procenty, pod tím grid detailů
 * (úroveň, teplota, napětí, proud, zdravotní stav, zdroj napájení,
 * technologie). Updatuje se live — broadcast listener přehazuje
 * state, Compose recomposuje jen text.
 *
 * Teplota je v desetinách stupně C (typicky 310 = 31.0°C). Napětí
 * v mV. Proud v mA (kladný = nabíjí, záporný = vybíjí — záleží na
 * výrobci, někteří to mají opačně, takže ukazujeme prostě absolutní
 * hodnotu + popisek "nabíjení"/"vybíjení" podle `status`).
 */
@Composable
fun BatteryInfoScreen(padding: PaddingValues) {
    val ctx = LocalContext.current
    var snap by remember { mutableStateOf(readBatterySnapshot(ctx)) }

    // Live: refresh on every ACTION_BATTERY_CHANGED. The system sends
    // this roughly every 30 s or on any change (level, charging state,
    // temperature spike), so we don't need a polling loop.
    DisposableEffect(ctx) {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                snap = readBatterySnapshot(ctx, intent)
            }
        }
        ctx.registerReceiver(receiver, filter)
        onDispose { runCatching { ctx.unregisterReceiver(receiver) } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero battery widget
        BatteryHero(snap)

        // Details grid
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                InfoRow("Teplota",       snap.temperatureC?.let { "%.1f °C".format(it) } ?: "—")
                InfoRow("Napětí",        snap.voltageMv?.let { "$it mV" } ?: "—")
                InfoRow("Proud",         snap.currentMa?.let { "${kotlin.math.abs(it)} mA" } ?: "—")
                InfoRow("Stav",          snap.statusLabel)
                InfoRow("Zdroj napájení", snap.pluggedLabel)
                InfoRow("Zdraví",        snap.healthLabel)
                InfoRow("Technologie",   snap.technology ?: "—")
            }
        }

        Text(
            "Data čteme v reálném čase ze systémového broadcastu — žádné oprávnění není potřeba.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BatteryHero(snap: BatterySnapshot) {
    val colors = MaterialTheme.colorScheme
    val level = snap.levelPercent.coerceIn(0, 100)
    val chargingTint = when {
        snap.isCharging              -> Color(0xFF22C55E) // green
        level >= 50                  -> colors.primary
        level >= 20                  -> Color(0xFFF59E0B) // amber
        else                         -> Color(0xFFEF4444) // red
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(width = 220.dp, height = 120.dp)) {
                    val bodyWidth = size.width - 16f   // leave space for the "nipple"
                    val bodyHeight = size.height
                    // Outer frame
                    drawRoundRect(
                        color = colors.outline,
                        topLeft = Offset(0f, 0f),
                        size = Size(bodyWidth, bodyHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f),
                    )
                    // Inner background
                    val pad = 6f
                    drawRoundRect(
                        color = colors.background,
                        topLeft = Offset(pad, pad),
                        size = Size(bodyWidth - pad * 2f, bodyHeight - pad * 2f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
                    )
                    // Fill
                    val fillWidth = (bodyWidth - pad * 2f) * (level / 100f)
                    drawRoundRect(
                        color = chargingTint,
                        topLeft = Offset(pad, pad),
                        size = Size(fillWidth, bodyHeight - pad * 2f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
                    )
                    // Nipple on the right side
                    drawRoundRect(
                        color = colors.outline,
                        topLeft = Offset(bodyWidth, bodyHeight * 0.3f),
                        size = Size(16f, bodyHeight * 0.4f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "$level %",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
            )
            Text(
                text = snap.statusLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = chargingTint,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Data reading
// ─────────────────────────────────────────────────────────────────────────────

private data class BatterySnapshot(
    val levelPercent: Int,
    val isCharging: Boolean,
    val statusLabel: String,
    val pluggedLabel: String,
    val healthLabel: String,
    val temperatureC: Float?,
    val voltageMv: Int?,
    val currentMa: Int?,
    val technology: String?,
)

private fun readBatterySnapshot(ctx: Context, intentIn: Intent? = null): BatterySnapshot {
    // Sticky broadcast — passing null returns the most recent value
    // the system has broadcast. `ACTION_BATTERY_CHANGED` is sticky.
    val intent: Intent? = intentIn
        ?: ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

    val level  = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale  = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        ?: BatteryManager.BATTERY_STATUS_UNKNOWN
    val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
    val health  = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, 0) ?: 0
    val temp    = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
    val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
    val tech    = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)

    val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else 0

    val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    val currentMicroA = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
    val currentMa = currentMicroA?.takeIf { it != Int.MIN_VALUE }?.let { it / 1000 }

    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL

    return BatterySnapshot(
        levelPercent = pct,
        isCharging = charging,
        statusLabel = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING     -> "Nabíjí se"
            BatteryManager.BATTERY_STATUS_DISCHARGING  -> "Vybíjí se"
            BatteryManager.BATTERY_STATUS_FULL         -> "Plně nabito"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Nenabíjí"
            else                                        -> "Neznámé"
        },
        pluggedLabel = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC       -> "Síťový adaptér"
            BatteryManager.BATTERY_PLUGGED_USB      -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Bezdrátově"
            0                                        -> "Odpojeno"
            else                                     -> "Neznámé ($plugged)"
        },
        healthLabel = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD             -> "Dobré"
            BatteryManager.BATTERY_HEALTH_OVERHEAT         -> "Přehřívá se"
            BatteryManager.BATTERY_HEALTH_DEAD             -> "Vadné"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE     -> "Přepětí"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Selhání"
            BatteryManager.BATTERY_HEALTH_COLD             -> "Chladno"
            else                                           -> "Neznámé"
        },
        temperatureC = if (temp >= 0) temp / 10f else null,
        voltageMv = if (voltage >= 0) voltage else null,
        currentMa = currentMa,
        technology = tech,
    )
}
