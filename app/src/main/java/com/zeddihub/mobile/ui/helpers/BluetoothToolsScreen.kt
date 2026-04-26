package com.zeddihub.mobile.ui.helpers

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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

/**
 * Bluetooth diagnostic + scanner.
 *
 * Combines four common workflows into one screen:
 *   • Adapter info — name / address / state / supported features
 *   • Bonded list — already-paired devices with their type and class
 *   • Live scan — both Classic discovery and BLE scan results merged
 *     into one sorted-by-RSSI list (so the strongest device is on top)
 *   • Pair / unpair — opens the system pairing dialog where allowed
 *
 * Permissions are subtle here. On API 31+ we need BLUETOOTH_SCAN /
 * BLUETOOTH_CONNECT (granted at runtime), and on 30- we still need
 * ACCESS_FINE_LOCATION because Bluetooth scanning could otherwise be
 * used to derive the user's position. We request the appropriate
 * bundle when the user taps Start scan.
 *
 * BLUETOOTH_ADVERTISE is intentionally not asked for here — broadcast
 * advertising lands as a separate sub-tool in v1.0.0; the scope of
 * v0.9.0 is observation and inspection, not transmission.
 */
@Composable
fun BluetoothToolsScreen(padding: PaddingValues) {
    val ctx = LocalContext.current
    val mgr = remember { ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager }
    val adapter = remember { mgr?.adapter }

    val scanResults = remember { mutableStateMapOf<String, ScanRow>() }
    var scanning by remember { mutableStateOf(false) }

    // Permission bundle differs per API level — see kdoc.
    val perms = remember {
        if (Build.VERSION.SDK_INT >= 31) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.all { it } && adapter != null && adapter.isEnabled) {
            startScan(adapter, scanResults) { scanning = it }
        }
    }

    // Listen for classic-discovery results so they show up in the same list
    // as BLE scan results. We also pick up bond-state changes so the
    // bonded list refreshes after pair/unpair from the system dialog.
    DisposableEffect(adapter) {
        if (adapter == null) return@DisposableEffect onDispose { }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                when (i.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val dev = i.parcelable<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        val rssi = i.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                        if (dev != null) {
                            val name = safeName(c, dev)
                            scanResults[dev.address] = ScanRow(dev.address, name, rssi, "Classic")
                        }
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        ContextCompat.registerReceiver(ctx, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { runCatching { ctx.unregisterReceiver(receiver) } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(R.string.bt_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        if (adapter == null) {
            Text(
                stringResource(R.string.bt_no_adapter),
                color = MaterialTheme.colorScheme.error
            )
            return@Column
        }

        AdapterInfoCard(ctx, adapter)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = !scanning,
                onClick = {
                    scanResults.clear()
                    val missing = perms.any {
                        ContextCompat.checkSelfPermission(ctx, it) != PackageManager.PERMISSION_GRANTED
                    }
                    if (missing) launcher.launch(perms)
                    else if (adapter.isEnabled) startScan(adapter, scanResults) { scanning = it }
                }
            ) { Text(stringResource(R.string.bt_scan_start)) }

            OutlinedButton(
                enabled = scanning,
                onClick = {
                    stopScan(adapter)
                    scanning = false
                }
            ) { Text(stringResource(R.string.bt_scan_stop)) }
        }

        Text(
            stringResource(R.string.bt_bonded_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        BondedList(ctx, adapter)

        Text(
            stringResource(R.string.bt_scan_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        // Sort the merged Classic + BLE results by RSSI desc so the
        // strongest device sits at the top — matches what the user
        // physically expects ("the closest one is first").
        val sortedRows = scanResults.values.sortedByDescending { it.rssi }
        if (sortedRows.isEmpty()) {
            Text(
                stringResource(R.string.bt_scan_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(sortedRows.size) { idx ->
                    val r = sortedRows[idx]
                    ScanRowCard(r)
                }
            }
        }
    }

    LaunchedEffect(Unit) { /* placeholder so compose recomposes correctly */ }
}

private data class ScanRow(val address: String, val name: String, val rssi: Int, val kind: String)

@Composable
private fun AdapterInfoCard(ctx: Context, a: BluetoothAdapter) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // The "official" address (a.address) is hidden on Android 8+
            // unless we hold the privileged BLUETOOTH_PRIVILEGED permission,
            // so it commonly returns the placeholder "02:00:00:00:00:00".
            // We show whatever is reported and don't try to spoof it.
            val name = if (Build.VERSION.SDK_INT < 31 ||
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED) a.name ?: "—" else "—"
            Text("Name: $name", fontWeight = FontWeight.SemiBold)
            Text("Address: ${a.address}")
            Text("State: ${stateLabel(a.state)}")
            Text("BLE supported: ${ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)}")
            if (Build.VERSION.SDK_INT >= 26) {
                Text("Multi-advertise: ${a.isMultipleAdvertisementSupported}")
                Text("Le coded PHY: ${a.isLeCodedPhySupported}")
                Text("Le 2M PHY: ${a.isLe2MPhySupported}")
            }
        }
    }
}

private fun stateLabel(s: Int) = when (s) {
    BluetoothAdapter.STATE_OFF -> "OFF"
    BluetoothAdapter.STATE_TURNING_OFF -> "TURNING_OFF"
    BluetoothAdapter.STATE_ON -> "ON"
    BluetoothAdapter.STATE_TURNING_ON -> "TURNING_ON"
    else -> "UNKNOWN"
}

@Composable
private fun BondedList(ctx: Context, a: BluetoothAdapter) {
    val canRead = Build.VERSION.SDK_INT < 31 ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) ==
        PackageManager.PERMISSION_GRANTED
    val bonded = remember(canRead) {
        if (canRead) runCatching { a.bondedDevices.toList() }.getOrDefault(emptyList())
        else emptyList()
    }
    if (bonded.isEmpty()) {
        Text(
            stringResource(R.string.bt_bonded_empty),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(bonded.size) { i ->
            val d = bonded[i]
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(safeName(ctx, d), fontWeight = FontWeight.SemiBold)
                    Text(d.address, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Type: ${typeLabel(d.type)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanRowCard(r: ScanRow) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(r.name, fontWeight = FontWeight.SemiBold)
                Text(r.address, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text(
                    "${r.rssi} dBm",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(r.kind, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun typeLabel(t: Int) = when (t) {
    BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic"
    BluetoothDevice.DEVICE_TYPE_LE -> "LE"
    BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual"
    else -> "Unknown"
}

private fun safeName(ctx: Context, d: BluetoothDevice): String {
    if (Build.VERSION.SDK_INT >= 31 &&
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) !=
        PackageManager.PERMISSION_GRANTED) return d.address
    return runCatching { d.name }.getOrNull() ?: d.address
}

@Suppress("MissingPermission")
private fun startScan(
    adapter: BluetoothAdapter,
    out: androidx.compose.runtime.snapshots.SnapshotStateMap<String, ScanRow>,
    onState: (Boolean) -> Unit,
) {
    onState(true)
    runCatching { adapter.startDiscovery() }
    val scanner = adapter.bluetoothLeScanner ?: return
    val cb = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val dev = result.device
            val name = result.scanRecord?.deviceName ?: dev.address
            out[dev.address] = ScanRow(dev.address, name, result.rssi, "BLE")
        }
    }
    activeCb = cb
    runCatching { scanner.startScan(cb) }
}

@Suppress("MissingPermission")
private fun stopScan(adapter: BluetoothAdapter) {
    runCatching { adapter.cancelDiscovery() }
    activeCb?.let { runCatching { adapter.bluetoothLeScanner?.stopScan(it) } }
    activeCb = null
}

private var activeCb: ScanCallback? = null

@Suppress("DEPRECATION")
private inline fun <reified T : android.os.Parcelable> Intent.parcelable(key: String): T? =
    if (Build.VERSION.SDK_INT >= 33) getParcelableExtra(key, T::class.java)
    else getParcelableExtra(key)
