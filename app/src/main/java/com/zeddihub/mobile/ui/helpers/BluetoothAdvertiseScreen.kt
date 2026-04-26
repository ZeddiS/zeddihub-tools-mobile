package com.zeddihub.mobile.ui.helpers

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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

/**
 * BLE manufacturer-data advertiser.
 *
 * Pairs nicely with [BluetoothToolsScreen] (the scanner): start an
 * advertisement here and it'll show up immediately on a second device
 * running the scanner. The intent is hobbyist-grade — we don't expose
 * the full AdvertiseData surface (services, txPower, includeName) because
 * the editing UI for that becomes a ten-row form for very little payoff
 * over "type two hex strings and broadcast".
 *
 * Manufacturer ID is the 16-bit Bluetooth SIG company ID. Common values:
 *   • 0x004C — Apple (used by AirTag / iBeacon)
 *   • 0x0006 — Microsoft
 *   • 0xFFFF — testing / unassigned
 *
 * Payload is the raw bytes that follow the company ID. iBeacon format
 * starts with 02 15 (subtype + length), but we stay format-agnostic and
 * let the user paste whatever hex they want, capped at 24 bytes (the
 * comfortable BLE 31-byte limit minus the header overhead).
 */
@Composable
fun BluetoothAdvertiseScreen(padding: PaddingValues) {
    val ctx = LocalContext.current
    val mgr = remember { ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager }
    val adapter = remember { mgr?.adapter }
    val advertiser = remember { adapter?.bluetoothLeAdvertiser }

    val granted = remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 31 ||
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_ADVERTISE) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted.value = it
    }

    var companyId by remember { mutableStateOf("004C") }
    var payload by remember { mutableStateOf("0215AA112233445566778899AABBCCDDEEFF0001") }
    var running by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var callback by remember { mutableStateOf<AdvertiseCallback?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            callback?.let { runCatching { advertiser?.stopAdvertising(it) } }
            callback = null
        }
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
            stringResource(R.string.btad_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(stringResource(R.string.btad_body))

        if (advertiser == null || adapter?.isMultipleAdvertisementSupported != true) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.btad_no_advertise),
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            return@Column
        }

        if (!granted.value) {
            Button(onClick = { launcher.launch(Manifest.permission.BLUETOOTH_ADVERTISE) }) {
                Text(stringResource(R.string.btad_grant))
            }
            return@Column
        }

        OutlinedTextField(
            value = companyId, onValueChange = { companyId = it.uppercase() },
            label = { Text(stringResource(R.string.btad_company_id)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = payload, onValueChange = { payload = it.uppercase() },
            label = { Text(stringResource(R.string.btad_payload)) },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = !running,
                onClick = {
                    val cid = companyId.toIntOrNull(16)
                    val data = parseHex(payload)
                    if (cid == null || data == null) {
                        error = "Hex parse failed"
                        return@Button
                    }
                    error = null
                    val cb = startAdvertising(advertiser, cid, data) { msg ->
                        error = msg
                        running = false
                    }
                    callback = cb
                    running = cb != null
                }
            ) { Text(if (running) stringResource(R.string.btad_running)
                else stringResource(R.string.btad_start)) }
            OutlinedButton(
                enabled = running,
                onClick = {
                    callback?.let { runCatching { advertiser.stopAdvertising(it) } }
                    callback = null
                    running = false
                }
            ) { Text(stringResource(R.string.btad_stop)) }
        }

        error?.let {
            Text(stringResource(R.string.btad_error, it),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun parseHex(s: String): ByteArray? {
    val clean = s.replace(" ", "").replace(":", "")
    if (clean.length % 2 != 0) return null
    return runCatching {
        ByteArray(clean.length / 2) { i ->
            clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }.getOrNull()
}

@Suppress("MissingPermission")
private fun startAdvertising(
    advertiser: BluetoothLeAdvertiser,
    companyId: Int,
    payload: ByteArray,
    onError: (String) -> Unit,
): AdvertiseCallback? {
    val settings = AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
        .setConnectable(false)
        .setTimeout(0) // 0 = advertise indefinitely until stopped
        .build()
    val data = AdvertiseData.Builder()
        .addManufacturerData(companyId, payload)
        .build()
    val cb = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            onError("code $errorCode " + when (errorCode) {
                ADVERTISE_FAILED_DATA_TOO_LARGE -> "(payload too large)"
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "(too many)"
                ADVERTISE_FAILED_ALREADY_STARTED -> "(already running)"
                ADVERTISE_FAILED_INTERNAL_ERROR -> "(internal)"
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "(unsupported)"
                else -> ""
            })
        }
    }
    return runCatching { advertiser.startAdvertising(settings, data, cb); cb }
        .onFailure { onError(it.message ?: "start failed") }
        .getOrNull()
}
