package com.zeddihub.mobile.ui.tools

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zeddihub.mobile.R

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
        Text(
            text = stringResource(R.string.device_info_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground
        )

        Spacer(Modifier.height(16.dp))

        Section(stringResource(R.string.device_info_device)) {
            Row2(stringResource(R.string.device_info_model), info.model)
            Row2(stringResource(R.string.device_info_manufacturer), info.manufacturer)
            Row2(stringResource(R.string.device_info_android), info.androidVersion)
            Row2(stringResource(R.string.device_info_sdk), info.sdk.toString())
        }

        Spacer(Modifier.height(10.dp))

        Section(stringResource(R.string.device_info_memory)) {
            Row2(stringResource(R.string.device_info_ram_available), "${info.ramAvailableMb} MB")
            Row2(stringResource(R.string.device_info_ram_total), "${info.ramTotalMb} MB")
        }

        Spacer(Modifier.height(10.dp))

        Section(stringResource(R.string.device_info_battery)) {
            Row2(stringResource(R.string.device_info_battery_level), "${info.batteryLevel}%")
            Row2(stringResource(R.string.device_info_battery_charging), if (info.charging) "Yes" else "No")
        }

        Spacer(Modifier.height(10.dp))

        Section(stringResource(R.string.device_info_network)) {
            Row2(stringResource(R.string.device_info_connection), info.connection)
            Row2(stringResource(R.string.device_info_ip), info.ip)
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = colors.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 6.dp)
    )
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
            fontWeight = FontWeight.Medium
        )
    }
}

data class DeviceInfo(
    val model: String,
    val manufacturer: String,
    val androidVersion: String,
    val sdk: Int,
    val ramAvailableMb: Long,
    val ramTotalMb: Long,
    val batteryLevel: Int,
    val charging: Boolean,
    val connection: String,
    val ip: String
)

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
        }?.firstOrNull { !it.isLoopbackAddress && it is java.net.Inet4Address }?.hostAddress ?: "—"
    }.getOrDefault("—")

    val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val batteryPct = if (level >= 0 && scale > 0) level * 100 / scale else 0
    val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING
        || status == BatteryManager.BATTERY_STATUS_FULL

    return DeviceInfo(
        model = Build.MODEL ?: "—",
        manufacturer = Build.MANUFACTURER ?: "—",
        androidVersion = Build.VERSION.RELEASE ?: "—",
        sdk = Build.VERSION.SDK_INT,
        ramAvailableMb = mem.availMem / 1_048_576,
        ramTotalMb = mem.totalMem / 1_048_576,
        batteryLevel = batteryPct,
        charging = charging,
        connection = connection,
        ip = ip ?: "—"
    )
}
