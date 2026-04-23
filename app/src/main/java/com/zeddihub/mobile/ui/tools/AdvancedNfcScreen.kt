package com.zeddihub.mobile.ui.tools

import android.app.Activity
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zeddihub.mobile.R
import java.nio.charset.Charset
import java.util.Locale

/**
 * Foreground NFC read/write screen.
 *
 * We use [NfcAdapter.enableReaderMode] rather than the legacy foreground
 * dispatch + intent-filter dance so this screen is completely
 * self-contained — no MainActivity changes, no manifest intent filters.
 * Reader mode is enabled while this composable is resumed and disabled on
 * dispose.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedNfcScreen(padding: PaddingValues) {
    val ctx = LocalContext.current
    val activity = ctx as? Activity
    val adapter = remember { NfcAdapter.getDefaultAdapter(ctx) }
    val colors = MaterialTheme.colorScheme

    var tab by remember { mutableStateOf(0) } // 0 = read, 1 = write

    // Read-side state
    var readInfo by remember { mutableStateOf<String?>(null) }
    var readPayload by remember { mutableStateOf<String?>(null) }

    // Write-side state
    var writeType by remember { mutableStateOf(WriteType.TEXT) }
    var writePayload by remember { mutableStateOf("") }
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPass by remember { mutableStateOf("") }
    var writeArmed by remember { mutableStateOf(false) }
    var writeResult by remember { mutableStateOf<String?>(null) }

    // Reader-mode callback routing. Active behavior depends on current tab /
    // armed state, so we re-key the DisposableEffect on those.
    DisposableEffect(adapter, tab, writeArmed, writeType, writePayload, wifiSsid, wifiPass) {
        if (adapter == null || activity == null || !adapter.isEnabled) {
            return@DisposableEffect onDispose { }
        }
        val flags = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V or
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        val cb = NfcAdapter.ReaderCallback { tag ->
            if (tab == 0) {
                val info = describeTag(tag)
                val msg = runCatching { readNdef(tag) }.getOrNull()
                readInfo = info
                readPayload = msg
            } else if (tab == 1 && writeArmed) {
                val record = when (writeType) {
                    WriteType.TEXT -> buildTextRecord(writePayload)
                    WriteType.URL -> NdefRecord.createUri(normalizeUrl(writePayload))
                    WriteType.WIFI -> buildWifiRecord(wifiSsid, wifiPass)
                }
                val result = runCatching { writeNdef(tag, NdefMessage(arrayOf(record))) }
                if (result.isSuccess) {
                    writeResult = "ok"
                    writeArmed = false
                } else {
                    writeResult = result.exceptionOrNull()?.message ?: "error"
                }
            }
        }
        runCatching { adapter.enableReaderMode(activity, cb, flags, null) }
        onDispose {
            runCatching { adapter.disableReaderMode(activity) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (adapter == null) {
            InfoBox(
                text = stringResource(R.string.nfc_unsupported),
                color = colors.errorContainer,
                onColor = colors.onErrorContainer
            )
            return@Column
        }
        if (!adapter.isEnabled) {
            InfoBox(
                text = stringResource(R.string.nfc_disabled),
                color = colors.errorContainer,
                onColor = colors.onErrorContainer
            )
        }

        PrimaryTabRow(selectedTabIndex = tab) {
            Tab(
                selected = tab == 0,
                onClick = { tab = 0 },
                text = { Text(stringResource(R.string.nfc_tab_read)) }
            )
            Tab(
                selected = tab == 1,
                onClick = { tab = 1 },
                text = { Text(stringResource(R.string.nfc_tab_write)) }
            )
        }

        Spacer(Modifier.height(16.dp))

        when (tab) {
            0 -> ReadPanel(readInfo, readPayload)
            else -> WritePanel(
                writeType = writeType,
                onSelectType = { writeType = it },
                payload = writePayload,
                onPayload = { writePayload = it },
                ssid = wifiSsid,
                onSsid = { wifiSsid = it },
                pass = wifiPass,
                onPass = { wifiPass = it },
                armed = writeArmed,
                onArmedToggle = {
                    writeArmed = !writeArmed
                    writeResult = null
                },
                result = writeResult
            )
        }
    }
}

@Composable
private fun ReadPanel(info: String?, payload: String?) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Nfc, contentDescription = null, tint = colors.primary)
                Spacer(Modifier.height(0.dp))
                Text(
                    text = stringResource(R.string.nfc_ready),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
            if (info != null) {
                Spacer(Modifier.height(12.dp))
                Text(info, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            if (payload != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = payload,
                    color = colors.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun WritePanel(
    writeType: WriteType,
    onSelectType: (WriteType) -> Unit,
    payload: String,
    onPayload: (String) -> Unit,
    ssid: String,
    onSsid: (String) -> Unit,
    pass: String,
    onPass: (String) -> Unit,
    armed: Boolean,
    onArmedToggle: () -> Unit,
    result: String?
) {
    val colors = MaterialTheme.colorScheme
    Text(
        stringResource(R.string.nfc_write_type),
        style = MaterialTheme.typography.labelLarge,
        color = colors.onBackground
    )
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TypeChip(
            label = stringResource(R.string.nfc_write_type_text),
            selected = writeType == WriteType.TEXT,
            onClick = { onSelectType(WriteType.TEXT) }
        )
        TypeChip(
            label = stringResource(R.string.nfc_write_type_url),
            selected = writeType == WriteType.URL,
            onClick = { onSelectType(WriteType.URL) }
        )
        TypeChip(
            label = stringResource(R.string.nfc_write_type_wifi),
            selected = writeType == WriteType.WIFI,
            onClick = { onSelectType(WriteType.WIFI) }
        )
    }

    Spacer(Modifier.height(16.dp))

    when (writeType) {
        WriteType.TEXT, WriteType.URL -> {
            OutlinedTextField(
                value = payload,
                onValueChange = onPayload,
                label = { Text(stringResource(R.string.nfc_write_payload)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        WriteType.WIFI -> {
            OutlinedTextField(
                value = ssid,
                onValueChange = onSsid,
                label = { Text(stringResource(R.string.nfc_write_ssid)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = pass,
                onValueChange = onPass,
                label = { Text(stringResource(R.string.nfc_write_password)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    Spacer(Modifier.height(20.dp))

    Button(
        onClick = onArmedToggle,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (armed) colors.error else colors.primary
        )
    ) {
        Icon(
            if (armed) Icons.Default.Close else Icons.Default.Nfc,
            contentDescription = null
        )
        Spacer(Modifier.height(0.dp))
        Text(
            text = if (armed) stringResource(R.string.nfc_write_cancel)
            else stringResource(R.string.nfc_write_arm),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 8.dp)
        )
    }

    if (armed) {
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.nfc_ready),
            color = colors.primary,
            fontWeight = FontWeight.SemiBold
        )
    }

    when (result) {
        "ok" -> {
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.nfc_write_ok),
                color = colors.primary,
                fontWeight = FontWeight.Bold
            )
        }
        null -> Unit
        else -> {
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.nfc_write_fail, result),
                color = colors.error
            )
        }
    }
}

@Composable
private fun TypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
private fun InfoBox(text: String, color: androidx.compose.ui.graphics.Color, onColor: androidx.compose.ui.graphics.Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = color
    ) {
        Text(text, modifier = Modifier.padding(12.dp), color = onColor)
    }
}

private enum class WriteType { TEXT, URL, WIFI }

// ── NDEF helpers ──────────────────────────────────────────────────────────

private fun describeTag(tag: Tag): String {
    val uid = tag.id.joinToString(":") { String.format("%02X", it) }
    val types = tag.techList.joinToString(", ") { it.substringAfterLast('.') }
    return "UID $uid\n$types"
}

private fun readNdef(tag: Tag): String {
    val ndef = Ndef.get(tag) ?: return ""
    return try {
        ndef.connect()
        val msg = ndef.cachedNdefMessage ?: ndef.ndefMessage
        if (msg == null) "" else formatNdef(msg)
    } finally {
        runCatching { ndef.close() }
    }
}

private fun formatNdef(msg: NdefMessage): String {
    val sb = StringBuilder()
    for (r in msg.records) {
        when {
            r.tnf == NdefRecord.TNF_WELL_KNOWN && r.type.contentEquals(NdefRecord.RTD_TEXT) -> {
                sb.append(parseTextPayload(r.payload)).append('\n')
            }
            r.tnf == NdefRecord.TNF_WELL_KNOWN && r.type.contentEquals(NdefRecord.RTD_URI) -> {
                sb.append(r.toUri().toString()).append('\n')
            }
            else -> {
                sb.append("[${r.type.toString(Charsets.US_ASCII)}] ${r.payload.size} B\n")
            }
        }
    }
    return sb.toString().trim()
}

private fun parseTextPayload(payload: ByteArray): String {
    if (payload.isEmpty()) return ""
    val status = payload[0].toInt()
    val isUtf16 = (status and 0x80) != 0
    val langLen = status and 0x3F
    val charset: Charset = if (isUtf16) Charsets.UTF_16 else Charsets.UTF_8
    return String(payload, 1 + langLen, payload.size - 1 - langLen, charset)
}

private fun buildTextRecord(text: String): NdefRecord {
    val lang = "cs"
    val langBytes = lang.toByteArray(Charsets.US_ASCII)
    val textBytes = text.toByteArray(Charsets.UTF_8)
    val payload = ByteArray(1 + langBytes.size + textBytes.size)
    payload[0] = langBytes.size.toByte()
    System.arraycopy(langBytes, 0, payload, 1, langBytes.size)
    System.arraycopy(textBytes, 0, payload, 1 + langBytes.size, textBytes.size)
    return NdefRecord(
        NdefRecord.TNF_WELL_KNOWN,
        NdefRecord.RTD_TEXT,
        ByteArray(0),
        payload
    )
}

private fun normalizeUrl(raw: String): android.net.Uri {
    val s = raw.trim()
    val prefixed = if (s.startsWith("http://") || s.startsWith("https://") ||
        s.startsWith("tel:") || s.startsWith("mailto:")
    ) s else "https://$s"
    return android.net.Uri.parse(prefixed)
}

/**
 * Build a minimal WPS "Credential" NDEF record — enough for Android's
 * settings to prompt a join. MAC is set to the broadcast 0xFF*6 placeholder
 * (ignored by most readers; Android accepts it).
 */
private fun buildWifiRecord(ssid: String, password: String): NdefRecord {
    val ssidBytes = ssid.toByteArray(Charsets.UTF_8)
    val passBytes = password.toByteArray(Charsets.UTF_8)

    fun tlv(type: Int, value: ByteArray): ByteArray {
        val b = ByteArray(4 + value.size)
        b[0] = ((type shr 8) and 0xff).toByte()
        b[1] = (type and 0xff).toByte()
        b[2] = ((value.size shr 8) and 0xff).toByte()
        b[3] = (value.size and 0xff).toByte()
        System.arraycopy(value, 0, b, 4, value.size)
        return b
    }

    val networkIndex = tlv(0x1026, byteArrayOf(0x01))
    val ssidTlv = tlv(0x1045, ssidBytes)
    // 0x0020 = WPA2-PSK
    val authType = tlv(0x1003, byteArrayOf(0x00, 0x20))
    // 0x0008 = AES
    val encType = tlv(0x100f, byteArrayOf(0x00, 0x08))
    val networkKey = tlv(0x1027, passBytes)
    val macAddress = tlv(0x1020, ByteArray(6) { 0xff.toByte() })

    val credentialValue = networkIndex + ssidTlv + authType + encType + networkKey + macAddress
    val credential = tlv(0x100e, credentialValue)

    val mime = "application/vnd.wfa.wsc".toByteArray(Charsets.US_ASCII)
    return NdefRecord(NdefRecord.TNF_MIME_MEDIA, mime, ByteArray(0), credential)
}

private fun writeNdef(tag: Tag, message: NdefMessage) {
    val ndef = Ndef.get(tag)
    if (ndef != null) {
        ndef.connect()
        try {
            if (!ndef.isWritable) error("tag is read-only")
            if (ndef.maxSize < message.byteArrayLength) error("tag too small")
            ndef.writeNdefMessage(message)
        } finally {
            runCatching { ndef.close() }
        }
        return
    }
    val formatable = NdefFormatable.get(tag) ?: error("tag is not NDEF-capable")
    formatable.connect()
    try {
        formatable.format(message)
    } finally {
        runCatching { formatable.close() }
    }
}

@Suppress("unused")
private val UNUSED_LOCALE = Locale.US
