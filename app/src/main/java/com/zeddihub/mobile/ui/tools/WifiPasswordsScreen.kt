package com.zeddihub.mobile.ui.tools

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.WifiPassword
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zeddihub.mobile.R
import android.graphics.Bitmap
import android.graphics.Color as AColor
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun WifiPasswordsScreen(padding: PaddingValues) {
    val colors = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    var qrDialog by remember { mutableStateOf<QrPayload?>(null) }

    val savedSsids = remember { collectSavedSsids(ctx) }

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
                    Icon(Icons.Default.WifiPassword, null, tint = colors.primary, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.size(10.dp))
                    Text(
                        stringResource(R.string.wifi_passwords_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.wifi_passwords_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.wifi_passwords_system_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.wifi_passwords_system_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { openWifiSettings(ctx) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Launch, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.wifi_passwords_open_settings))
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            stringResource(R.string.wifi_passwords_generate_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface
        )
        Spacer(Modifier.height(8.dp))
        ManualQrCard(onGenerate = { ssid, pass, sec -> qrDialog = QrPayload(ssid, pass, sec) })

        Spacer(Modifier.height(14.dp))

        Text(
            stringResource(R.string.wifi_passwords_saved_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.wifi_passwords_saved_body),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        if (savedSsids.isEmpty()) {
            Text(
                stringResource(R.string.wifi_passwords_saved_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            savedSsids.forEach { ssid ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.WifiPassword, null, tint = colors.primary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.size(10.dp))
                        Text(ssid, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    qrDialog?.let { payload ->
        AlertDialog(
            onDismissRequest = { qrDialog = null },
            confirmButton = {
                OutlinedButton(onClick = { qrDialog = null }) { Text(stringResource(R.string.common_close)) }
            },
            icon = { Icon(Icons.Default.QrCode2, null, tint = colors.primary) },
            title = { Text(payload.ssid, fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val bmp = remember(payload) { generateWifiQr(payload) }
                    bmp?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(240.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.wifi_passwords_scan_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        )
    }
}

@Composable
private fun ManualQrCard(onGenerate: (String, String, String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var ssid by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var sec by remember { mutableStateOf("WPA") }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = ssid,
                onValueChange = { ssid = it },
                label = { Text(stringResource(R.string.wifi_passwords_ssid)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text(stringResource(R.string.wifi_passwords_password)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("WPA", "WEP", "nopass").forEach { type ->
                    OutlinedButton(
                        onClick = { sec = type },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(type, fontWeight = if (sec == type) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { if (ssid.isNotBlank()) onGenerate(ssid, pass, sec) },
                enabled = ssid.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.QrCode2, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.wifi_passwords_generate))
            }
        }
    }
}

data class QrPayload(val ssid: String, val password: String, val security: String)

private fun generateWifiQr(p: QrPayload): Bitmap? = runCatching {
    val content = "WIFI:T:${p.security};S:${escapeQr(p.ssid)};P:${escapeQr(p.password)};;"
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 512, 512)
    val w = matrix.width
    val h = matrix.height
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    for (x in 0 until w) for (y in 0 until h) {
        bmp.setPixel(x, y, if (matrix[x, y]) AColor.BLACK else AColor.WHITE)
    }
    bmp
}.getOrNull()

private fun escapeQr(s: String): String =
    s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace(":", "\\:")

@Suppress("DEPRECATION")
private fun collectSavedSsids(ctx: Context): List<String> {
    return runCatching {
        val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            wm.configuredNetworks?.mapNotNull {
                it.SSID?.trim('"')?.takeIf { s -> s.isNotBlank() }
            }?.distinct()?.sorted() ?: emptyList()
        } else emptyList()
    }.getOrDefault(emptyList())
}

private fun openWifiSettings(ctx: Context) {
    runCatching {
        ctx.startActivity(
            Intent(Settings.ACTION_WIFI_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
