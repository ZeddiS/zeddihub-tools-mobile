package com.zeddihub.mobile.ui.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiPassword
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.zeddihub.mobile.R
import kotlin.random.Random
import android.graphics.Color as AColor

/**
 * WiFi Nástroje — converted from the original "WiFi passwords" screen into a
 * toolbox with three cards: QR generator, strong password generator, and a
 * locally-persisted "My WiFi" list (SSID + password) with QR export.
 */
@Composable
fun WifiToolsScreen(padding: PaddingValues) {
    val colors = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    var qrDialog by remember { mutableStateOf<QrPayload?>(null) }
    val store = remember { MyWifiStore(ctx) }
    var entries by remember { mutableStateOf(store.load()) }
    val systemSaved = remember { collectSavedSsids(ctx) }

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
                        stringResource(R.string.wifi_tools_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.wifi_tools_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // QR generator — can also save to My WiFi.
        SectionHeader(stringResource(R.string.wifi_tools_generate_title))
        ManualQrCard(
            onGenerate = { ssid, pass, sec -> qrDialog = QrPayload(ssid, pass, sec) },
            onSave = { ssid, pass, sec ->
                val updated = store.upsert(MyWifi(ssid, pass, sec))
                entries = updated
                Toast.makeText(ctx, ctx.getString(R.string.wifi_tools_saved_toast), Toast.LENGTH_SHORT).show()
            }
        )

        Spacer(Modifier.height(14.dp))

        // Strong password generator.
        SectionHeader(stringResource(R.string.wifi_tools_pwdgen_title))
        PasswordGeneratorCard()

        Spacer(Modifier.height(14.dp))

        // My WiFi saved list.
        SectionHeader(stringResource(R.string.wifi_tools_mywifi_title))
        if (entries.isEmpty()) {
            Text(
                stringResource(R.string.wifi_tools_mywifi_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            entries.forEach { entry ->
                MyWifiCard(
                    entry = entry,
                    onShowQr = { qrDialog = QrPayload(entry.ssid, entry.password, entry.security) },
                    onDelete = {
                        entries = store.delete(entry.ssid)
                    }
                )
                Spacer(Modifier.height(6.dp))
            }
        }

        if (systemSaved.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            SectionHeader(stringResource(R.string.wifi_tools_system_title))
            Text(
                stringResource(R.string.wifi_tools_system_body),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            systemSaved.forEach { ssid ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Wifi, null, tint = colors.primary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.size(10.dp))
                        Text(ssid, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface, modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { openWifiSettings(ctx) }, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.AutoMirrored.Filled.Launch, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.wifi_tools_open_settings))
            }
        }

        Spacer(Modifier.height(24.dp))
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
                        stringResource(R.string.wifi_tools_scan_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    val colors = MaterialTheme.colorScheme
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = colors.onSurface,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun ManualQrCard(
    onGenerate: (String, String, String) -> Unit,
    onSave: (String, String, String) -> Unit
) {
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
                label = { Text(stringResource(R.string.wifi_tools_ssid)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text(stringResource(R.string.wifi_tools_password)) },
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { if (ssid.isNotBlank()) onGenerate(ssid, pass, sec) },
                    enabled = ssid.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.QrCode2, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.wifi_tools_generate))
                }
                OutlinedButton(
                    onClick = { if (ssid.isNotBlank()) onSave(ssid, pass, sec) },
                    enabled = ssid.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.wifi_tools_save))
                }
            }
        }
    }
}

@Composable
private fun PasswordGeneratorCard() {
    val colors = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    var length by remember { mutableStateOf(16) }
    var useSymbols by remember { mutableStateOf(true) }
    var useDigits by remember { mutableStateOf(true) }
    var useUpper by remember { mutableStateOf(true) }
    var generated by remember { mutableStateOf(generatePassword(length, useUpper, useDigits, useSymbols)) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    generated,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.wifi_tools_pwdgen_length, length),
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = { if (length > 6) length -= 1 },
                    shape = RoundedCornerShape(10.dp)
                ) { Text("−") }
                Spacer(Modifier.size(6.dp))
                OutlinedButton(
                    onClick = { if (length < 48) length += 1 },
                    shape = RoundedCornerShape(10.dp)
                ) { Text("+") }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip(
                    selected = useUpper,
                    label = "A-Z",
                    onToggle = { useUpper = !useUpper },
                    modifier = Modifier.weight(1f)
                )
                Chip(
                    selected = useDigits,
                    label = "0-9",
                    onToggle = { useDigits = !useDigits },
                    modifier = Modifier.weight(1f)
                )
                Chip(
                    selected = useSymbols,
                    label = "#@!",
                    onToggle = { useSymbols = !useSymbols },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        generated = generatePassword(length, useUpper, useDigits, useSymbols)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Autorenew, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.wifi_tools_pwdgen_regen))
                }
                OutlinedButton(
                    onClick = {
                        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("password", generated))
                        Toast.makeText(
                            ctx,
                            ctx.getString(R.string.wifi_tools_pwdgen_copied),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.wifi_tools_pwdgen_copy))
                }
            }
        }
    }
}

@Composable
private fun Chip(
    selected: Boolean,
    label: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val bg = if (selected) colors.primary.copy(alpha = 0.20f) else colors.background
    val fg = if (selected) colors.primary else colors.onSurfaceVariant
    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(10.dp))
            .clickable { onToggle() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = fg, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MyWifiCard(
    entry: MyWifi,
    onShowQr: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Wifi, null, tint = colors.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.ssid, color = colors.onSurface, fontWeight = FontWeight.SemiBold)
                Text(
                    entry.security +
                        if (entry.password.isNotBlank()) " · ${maskPassword(entry.password)}" else "",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            IconButton(onClick = onShowQr) {
                Icon(Icons.Default.QrCode2, null, tint = colors.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = colors.error)
            }
        }
    }
}

private fun maskPassword(p: String): String =
    if (p.length <= 2) "••" else p.take(1) + "•".repeat(p.length - 2) + p.takeLast(1)

// ───────── Data / utilities ─────────

data class QrPayload(val ssid: String, val password: String, val security: String)

data class MyWifi(val ssid: String, val password: String, val security: String)

private class MyWifiStore(ctx: Context) {
    private val prefs = ctx.applicationContext.getSharedPreferences("wifi_tools", Context.MODE_PRIVATE)

    fun load(): List<MyWifi> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return raw.split('\u0001').mapNotNull { line ->
            val parts = line.split('\u0002')
            if (parts.size == 3) MyWifi(parts[0], parts[1], parts[2]) else null
        }
    }

    fun upsert(entry: MyWifi): List<MyWifi> {
        val cur = load().filter { !it.ssid.equals(entry.ssid, ignoreCase = true) } + entry
        save(cur)
        return cur
    }

    fun delete(ssid: String): List<MyWifi> {
        val cur = load().filter { !it.ssid.equals(ssid, ignoreCase = true) }
        save(cur)
        return cur
    }

    private fun save(list: List<MyWifi>) {
        val raw = list.joinToString("\u0001") {
            "${it.ssid}\u0002${it.password}\u0002${it.security}"
        }
        prefs.edit().putString(KEY, raw).apply()
    }

    companion object {
        private const val KEY = "my_wifi_list_v1"
    }
}

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

private fun generatePassword(
    length: Int,
    upper: Boolean,
    digits: Boolean,
    symbols: Boolean
): String {
    val lowers = "abcdefghijkmnopqrstuvwxyz"
    val uppers = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    val nums = "23456789"
    val syms = "!#\$%&*+-=?@^_~"
    val alphabet = buildString {
        append(lowers)
        if (upper) append(uppers)
        if (digits) append(nums)
        if (symbols) append(syms)
    }
    if (alphabet.isEmpty()) return ""
    val random = Random.Default
    return (1..length.coerceAtLeast(4))
        .map { alphabet[random.nextInt(alphabet.length)] }
        .joinToString("")
}
