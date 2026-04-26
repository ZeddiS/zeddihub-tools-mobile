package com.zeddihub.mobile.ui.helpers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.BatteryManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.zeddihub.mobile.R
import android.content.pm.PackageManager

/**
 * USB diagnostics — what's plugged in, what mode the port is in, and
 * which USB host devices are visible.
 *
 * Android exposes USB state through ACTION_BATTERY_CHANGED extras
 * (charging plug type) and through ACTION_USB_STATE for cable +
 * configuration mode (host / device / accessory). We listen on the
 * battery intent because it works on every device; the USB intent
 * is broadcast per-device-vendor and isn't reliable across all OEMs.
 *
 * USB host enumeration uses UsbManager.deviceList — that surfaces
 * every device behind an OTG cable (flash drive, MIDI keyboard,
 * printer, etc.) along with vendor / product IDs. We don't request
 * per-device access permission here because that's a write workflow;
 * v0.9.0 stops at observation. Any host-side data transfer lands in
 * a future iteration.
 */
@Composable
fun UsbToolsScreen(padding: PaddingValues) {
    val ctx = LocalContext.current
    val um = remember { ctx.getSystemService(Context.USB_SERVICE) as? UsbManager }

    var plugged by remember { mutableStateOf(0) }
    var deviceList by remember { mutableStateOf<List<UsbInfo>>(emptyList()) }

    DisposableEffect(Unit) {
        deviceList = enumerateUsb(um)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                when (i.action) {
                    Intent.ACTION_BATTERY_CHANGED -> {
                        plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                    }
                    UsbManager.ACTION_USB_DEVICE_ATTACHED,
                    UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                        deviceList = enumerateUsb(um)
                    }
                }
            }
        }
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        ContextCompat.registerReceiver(ctx, receiver, f, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { runCatching { ctx.unregisterReceiver(receiver) } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            stringResource(R.string.usb_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(stringResource(R.string.usb_state),
                    fontWeight = FontWeight.SemiBold)
                val plugLabel = when (plugged) {
                    BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                    BatteryManager.BATTERY_PLUGGED_AC -> "AC"
                    BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                    0 -> stringResource(R.string.usb_unplugged)
                    else -> "Unknown ($plugged)"
                }
                Text("Power: $plugLabel")
                val hasOtg = ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
                val hasAcc = ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_ACCESSORY)
                Text("USB host (OTG) supported: $hasOtg")
                Text("USB accessory mode: $hasAcc")
            }
        }

        Text(
            stringResource(R.string.usb_devices_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        if (deviceList.isEmpty()) {
            Text(
                stringResource(R.string.usb_devices_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            for (d in deviceList) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(d.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            "VID: 0x%04X  PID: 0x%04X".format(d.vendorId, d.productId),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "Class: ${classLabel(d.classId)}  (${d.classId})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (d.manufacturer != null) Text("Manufacturer: ${d.manufacturer}")
                        if (d.product != null) Text("Product: ${d.product}")
                        if (d.serial != null) Text("Serial: ${d.serial}")
                    }
                }
            }
        }
    }
}

private data class UsbInfo(
    val name: String,
    val vendorId: Int, val productId: Int, val classId: Int,
    val manufacturer: String?, val product: String?, val serial: String?,
)

private fun enumerateUsb(um: UsbManager?): List<UsbInfo> {
    val list = um?.deviceList?.values ?: return emptyList()
    return list.map { d ->
        UsbInfo(
            name = d.deviceName,
            vendorId = d.vendorId, productId = d.productId, classId = d.deviceClass,
            // manufacturerName / productName / serialNumber require API 21
            // and may throw SecurityException on devices we haven't been
            // granted access to — wrap in runCatching to keep the row showing.
            manufacturer = runCatching { d.manufacturerName }.getOrNull(),
            product = runCatching { d.productName }.getOrNull(),
            serial = runCatching { d.serialNumber }.getOrNull(),
        )
    }
}

private fun classLabel(c: Int) = when (c) {
    0 -> "Per-interface"
    1 -> "Audio"
    2 -> "Comms / CDC"
    3 -> "HID"
    5 -> "Physical"
    6 -> "Image / PTP"
    7 -> "Printer"
    8 -> "Mass Storage"
    9 -> "Hub"
    10 -> "CDC Data"
    11 -> "Smart Card"
    14 -> "Video"
    220 -> "Diagnostic"
    224 -> "Wireless"
    254 -> "Application"
    255 -> "Vendor specific"
    else -> "—"
}
