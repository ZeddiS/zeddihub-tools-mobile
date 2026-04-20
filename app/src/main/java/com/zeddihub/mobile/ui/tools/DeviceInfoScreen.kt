package com.zeddihub.mobile.ui.tools

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.view.Display
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zeddihub.mobile.R
import java.io.File
import java.io.RandomAccessFile

@Composable
fun DeviceInfoScreen(padding: PaddingValues) {
    val colors = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val info = remember { collectInfo(ctx) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(padding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Transparent,
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(colors.primary.copy(alpha = 0.16f), Color.Transparent)
                        ),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Devices, null, tint = colors.primary, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.device_info_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${info.manufacturer} ${info.model} · Android ${info.androidVersion} (API ${info.sdk})",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Section(icon = Icons.Default.Devices, title = stringResource(R.string.device_info_device)) {
            Row2(stringResource(R.string.device_info_model), info.model)
            Row2(stringResource(R.string.device_info_manufacturer), info.manufacturer)
            Row2(stringResource(R.string.device_info_android), "${info.androidVersion} (API ${info.sdk})")
            Row2(stringResource(R.string.device_info_build), info.buildId)
            Row2(stringResource(R.string.device_info_bootloader), info.bootloader)
            Row2(stringResource(R.string.device_info_fingerprint), info.fingerprint)
        }

        Spacer(Modifier.height(10.dp))

        Section(icon = Icons.Default.Memory, title = stringResource(R.string.device_info_cpu)) {
            Row2(stringResource(R.string.device_info_cpu_soc), info.socModel)
            Row2(stringResource(R.string.device_info_cpu_cores), "${info.cpuCores}")
            Row2(stringResource(R.string.device_info_cpu_abi), info.cpuAbi)
            Row2(stringResource(R.string.device_info_cpu_freq), info.cpuFreqRange)
            Row2(stringResource(R.string.device_info_cpu_governor), info.cpuGovernor)
        }

        Spacer(Modifier.height(10.dp))

        Section(icon = Icons.Default.Memory, title = stringResource(R.string.device_info_memory)) {
            RamBar(
                availableMb = info.ramAvailableMb,
                totalMb = info.ramTotalMb
            )
            Spacer(Modifier.height(6.dp))
            Row2(stringResource(R.string.device_info_ram_available), "${info.ramAvailableMb} MB")
            Row2(stringResource(R.string.device_info_ram_total), "${info.ramTotalMb} MB")
            Row2(stringResource(R.string.device_info_low_memory), if (info.lowMemory) "Yes" else "No")
        }

        Spacer(Modifier.height(10.dp))

        Section(icon = Icons.Default.Storage, title = stringResource(R.string.device_info_storage)) {
            info.storageVolumes.forEach { vol ->
                StorageBar(
                    label = vol.label,
                    freeBytes = vol.freeBytes,
                    totalBytes = vol.totalBytes
                )
                Spacer(Modifier.height(4.dp))
            }
        }

        Spacer(Modifier.height(10.dp))

        Section(icon = Icons.Default.Monitor, title = stringResource(R.string.device_info_display)) {
            Row2(stringResource(R.string.device_info_resolution), "${info.displayWidth} × ${info.displayHeight} px")
            Row2(stringResource(R.string.device_info_dpi), "${info.displayDpi} dpi")
            Row2(stringResource(R.string.device_info_refresh), "%.1f Hz".format(info.refreshRate))
            Row2(stringResource(R.string.device_info_hdr), if (info.hdrSupport) "Yes" else "No")
        }

        Spacer(Modifier.height(10.dp))

        Section(icon = Icons.Default.Battery5Bar, title = stringResource(R.string.device_info_battery)) {
            Row2(stringResource(R.string.device_info_battery_level), "${info.batteryLevel}%")
            Row2(stringResource(R.string.device_info_battery_charging), if (info.charging) "Yes" else "No")
            Row2(stringResource(R.string.device_info_battery_health), info.batteryHealth)
            Row2(stringResource(R.string.device_info_battery_temp), "${info.batteryTempC} °C")
            Row2(stringResource(R.string.device_info_battery_tech), info.batteryTech)
        }

        Spacer(Modifier.height(10.dp))

        Section(icon = Icons.Default.Wifi, title = stringResource(R.string.device_info_network)) {
            Row2(stringResource(R.string.device_info_connection), info.connection)
            Row2(stringResource(R.string.device_info_ip), info.ip)
        }

        Spacer(Modifier.height(10.dp))

        Section(icon = Icons.Default.Sensors, title = stringResource(R.string.device_info_sensors)) {
            if (info.sensors.isEmpty()) {
                Text("—", color = colors.onSurfaceVariant)
            } else {
                info.sensors.forEach { s ->
                    Row2(s.name, "${s.vendor}")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Section(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
        Icon(icon, null, tint = colors.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = colors.primary, fontWeight = FontWeight.SemiBold)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) { content() }
    }
}

@Composable
private fun Row2(label: String, value: String) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.width(140.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RamBar(availableMb: Long, totalMb: Long) {
    if (totalMb <= 0) return
    val used = (totalMb - availableMb).coerceAtLeast(0)
    val fraction = used.toFloat() / totalMb.toFloat()
    val colors = MaterialTheme.colorScheme
    Column {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("${used} / ${totalMb} MB", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            Text("%.0f%%".format(fraction * 100), style = MaterialTheme.typography.labelSmall, color = colors.primary, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = colors.primary,
            trackColor = colors.primary.copy(alpha = 0.15f)
        )
    }
}

@Composable
private fun StorageBar(label: String, freeBytes: Long, totalBytes: Long) {
    val colors = MaterialTheme.colorScheme
    val used = (totalBytes - freeBytes).coerceAtLeast(0)
    val fraction = if (totalBytes > 0) used.toFloat() / totalBytes.toFloat() else 0f
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface, fontWeight = FontWeight.Medium)
            Text(
                "${formatBytesReadable(used)} / ${formatBytesReadable(totalBytes)}",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = colors.primary,
            trackColor = colors.primary.copy(alpha = 0.15f)
        )
    }
}

private fun formatBytesReadable(b: Long): String {
    if (b <= 0) return "0 B"
    val u = arrayOf("B", "KB", "MB", "GB", "TB")
    var v = b.toDouble()
    var i = 0
    while (v >= 1024 && i < u.lastIndex) { v /= 1024; i++ }
    return "%.1f %s".format(v, u[i])
}

data class SensorEntry(val name: String, val vendor: String)

data class StorageVolume(val label: String, val freeBytes: Long, val totalBytes: Long)

data class DeviceInfo(
    val model: String,
    val manufacturer: String,
    val androidVersion: String,
    val sdk: Int,
    val buildId: String,
    val bootloader: String,
    val fingerprint: String,
    val socModel: String,
    val cpuCores: Int,
    val cpuAbi: String,
    val cpuFreqRange: String,
    val cpuGovernor: String,
    val ramAvailableMb: Long,
    val ramTotalMb: Long,
    val lowMemory: Boolean,
    val displayWidth: Int,
    val displayHeight: Int,
    val displayDpi: Int,
    val refreshRate: Float,
    val hdrSupport: Boolean,
    val batteryLevel: Int,
    val charging: Boolean,
    val batteryHealth: String,
    val batteryTempC: Float,
    val batteryTech: String,
    val connection: String,
    val ip: String,
    val sensors: List<SensorEntry>,
    val storageVolumes: List<StorageVolume>
)

@Suppress("DEPRECATION")
private fun collectInfo(ctx: Context): DeviceInfo {
    val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val mem = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }

    val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = cm.activeNetwork
    val caps = activeNetwork?.let { cm.getNetworkCapabilities(it) }
    val connection = when {
        caps == null -> "Offline"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
        else -> "Other"
    }

    val ip = runCatching {
        java.net.NetworkInterface.getNetworkInterfaces()?.toList()?.flatMap { nif ->
            nif.inetAddresses.toList()
        }?.firstOrNull { !it.isLoopbackAddress && it is java.net.Inet4Address }?.hostAddress
    }.getOrNull() ?: "—"

    val battery = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val batteryPct = if (level >= 0 && scale > 0) level * 100 / scale else 0
    val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING
        || status == BatteryManager.BATTERY_STATUS_FULL
    val healthInt = battery?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
    val health = when (healthInt) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
        BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
        BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
        else -> "Unknown"
    }
    val tempTenths = battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
    val tempC = tempTenths / 10f
    val tech = battery?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "—"

    // Display
    val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val display: Display = wm.defaultDisplay
    val dm = ctx.resources.displayMetrics
    val refresh = display.refreshRate
    val hdr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        display.hdrCapabilities?.supportedHdrTypes?.isNotEmpty() ?: false
    } else false

    // Sensors
    val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val sensors = sm.getSensorList(Sensor.TYPE_ALL)
        .map { SensorEntry(it.name, it.vendor ?: "—") }
        .distinctBy { it.name }
        .sortedBy { it.name }

    // CPU
    val cores = Runtime.getRuntime().availableProcessors()
    val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "—"
    val socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        "${Build.SOC_MANUFACTURER} ${Build.SOC_MODEL}" else Build.HARDWARE ?: "—"
    val (minFreq, maxFreq) = cpuFreqRange(cores)
    val cpuFreqStr = if (maxFreq > 0)
        "%.0f MHz – %.0f MHz".format(minFreq / 1000.0, maxFreq / 1000.0)
    else "—"
    val governor = runCatching {
        File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor").readText().trim()
    }.getOrNull() ?: "—"

    // Storage
    val sMgr = ctx.getSystemService(Context.STORAGE_SERVICE) as StorageManager
    val volumes = mutableListOf<StorageVolume>()
    val internal = Environment.getDataDirectory()
    runCatching {
        val stat = StatFs(internal.absolutePath)
        volumes += StorageVolume(
            label = "Interní",
            freeBytes = stat.availableBytes,
            totalBytes = stat.totalBytes
        )
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        sMgr.storageVolumes.forEach { sv ->
            if (sv.isPrimary) return@forEach
            runCatching {
                val dir = sv.directory ?: return@forEach
                val stat = StatFs(dir.absolutePath)
                volumes += StorageVolume(
                    label = sv.getDescription(ctx) ?: (if (sv.isRemovable) "SD/USB" else "External"),
                    freeBytes = stat.availableBytes,
                    totalBytes = stat.totalBytes
                )
            }
        }
    }

    return DeviceInfo(
        model = Build.MODEL ?: "—",
        manufacturer = Build.MANUFACTURER ?: "—",
        androidVersion = Build.VERSION.RELEASE ?: "—",
        sdk = Build.VERSION.SDK_INT,
        buildId = Build.DISPLAY ?: Build.ID ?: "—",
        bootloader = Build.BOOTLOADER ?: "—",
        fingerprint = Build.FINGERPRINT ?: "—",
        socModel = socModel,
        cpuCores = cores,
        cpuAbi = abi,
        cpuFreqRange = cpuFreqStr,
        cpuGovernor = governor,
        ramAvailableMb = mem.availMem / 1_048_576,
        ramTotalMb = mem.totalMem / 1_048_576,
        lowMemory = mem.lowMemory,
        displayWidth = dm.widthPixels,
        displayHeight = dm.heightPixels,
        displayDpi = dm.densityDpi,
        refreshRate = refresh,
        hdrSupport = hdr,
        batteryLevel = batteryPct,
        charging = charging,
        batteryHealth = health,
        batteryTempC = tempC,
        batteryTech = tech,
        connection = connection,
        ip = ip,
        sensors = sensors,
        storageVolumes = volumes
    )
}

private fun cpuFreqRange(cores: Int): Pair<Long, Long> {
    var min = Long.MAX_VALUE
    var max = 0L
    for (i in 0 until cores) {
        runCatching {
            val minPath = "/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_min_freq"
            val maxPath = "/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq"
            val mi = RandomAccessFile(minPath, "r").use { it.readLine()?.trim()?.toLong() ?: 0 }
            val ma = RandomAccessFile(maxPath, "r").use { it.readLine()?.trim()?.toLong() ?: 0 }
            if (mi > 0) min = kotlin.math.min(min, mi)
            if (ma > 0) max = kotlin.math.max(max, ma)
        }
    }
    return (if (min == Long.MAX_VALUE) 0 else min) to max
}
