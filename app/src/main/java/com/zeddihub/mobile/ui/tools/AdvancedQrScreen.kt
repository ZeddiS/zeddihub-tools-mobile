package com.zeddihub.mobile.ui.tools

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AColor
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.zeddihub.mobile.R
import java.io.File
import java.io.FileOutputStream

/**
 * Advanced QR tool.
 *
 *  - Generate tab:
 *      TEXT / URL / WIFI / CONTACT / GPS / SMS / MAIL types.
 *      Live-render the QR bitmap on every input change, with
 *      save-to-Pictures, share, and copy-text actions.
 *
 *  - Scan tab:
 *      Pick an image from the gallery and decode any supported
 *      1D/2D barcode via ZXing's `MultiFormatReader`.
 *      (Live-camera scanning is intentionally deferred — it
 *      requires full CameraX + analyzer plumbing.)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedQrScreen(padding: PaddingValues) {
    var tab by remember { mutableIntStateOf(0) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        PrimaryTabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }) {
                Text(stringResource(R.string.qr_tab_generate), modifier = Modifier.padding(12.dp))
            }
            Tab(selected = tab == 1, onClick = { tab = 1 }) {
                Text(stringResource(R.string.qr_tab_scan), modifier = Modifier.padding(12.dp))
            }
        }
        when (tab) {
            0 -> GenerateTab()
            1 -> ScanTab()
        }
    }
}

private enum class QrType { TEXT, URL, WIFI, CONTACT, GEO, SMS, MAIL }

@Composable
private fun GenerateTab() {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current

    var type by remember { mutableStateOf(QrType.TEXT) }

    // Text / URL
    var textValue by remember { mutableStateOf("") }

    // WiFi
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var hidden by remember { mutableStateOf(false) }
    var security by remember { mutableStateOf("WPA") }

    // Contact (MECARD)
    var cName by remember { mutableStateOf("") }
    var cPhone by remember { mutableStateOf("") }
    var cEmail by remember { mutableStateOf("") }

    // GPS
    var lat by remember { mutableStateOf("") }
    var lng by remember { mutableStateOf("") }

    // SMS
    var smsNumber by remember { mutableStateOf("") }
    var smsBody by remember { mutableStateOf("") }

    // Mail
    var mailAddr by remember { mutableStateOf("") }
    var mailSubj by remember { mutableStateOf("") }
    var mailBody by remember { mutableStateOf("") }

    val payload = remember(
        type, textValue, ssid, password, hidden, security,
        cName, cPhone, cEmail, lat, lng,
        smsNumber, smsBody, mailAddr, mailSubj, mailBody
    ) {
        buildPayload(
            type, textValue, ssid, password, hidden, security,
            cName, cPhone, cEmail, lat, lng,
            smsNumber, smsBody, mailAddr, mailSubj, mailBody
        )
    }

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(payload) {
        bitmap = if (payload.isBlank()) null else generateQr(payload)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        // Type selector chips.
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            TypeChip(type, QrType.TEXT, stringResource(R.string.qr_type_text)) { type = it }
            TypeChip(type, QrType.URL, stringResource(R.string.qr_type_url)) { type = it }
            TypeChip(type, QrType.WIFI, stringResource(R.string.qr_type_wifi)) { type = it }
            TypeChip(type, QrType.CONTACT, stringResource(R.string.qr_type_contact)) { type = it }
            TypeChip(type, QrType.GEO, stringResource(R.string.qr_type_geo)) { type = it }
            TypeChip(type, QrType.SMS, stringResource(R.string.qr_type_sms)) { type = it }
            TypeChip(type, QrType.MAIL, stringResource(R.string.qr_type_mail)) { type = it }
        }

        Spacer(Modifier.height(12.dp))

        // Inputs per type.
        when (type) {
            QrType.TEXT, QrType.URL -> {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text(if (type == QrType.URL) "URL" else "Text") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            QrType.WIFI -> {
                OutlinedTextField(ssid, { ssid = it }, label = { Text("SSID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    listOf("WPA", "WEP", "nopass").forEach { s ->
                        FilterChip(
                            selected = security == s,
                            onClick = { security = s },
                            label = { Text(s) },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = hidden, onCheckedChange = { hidden = it })
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.qr_wifi_hidden))
                }
            }
            QrType.CONTACT -> {
                OutlinedTextField(cName, { cName = it }, label = { Text(stringResource(R.string.qr_contact_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(cPhone, { cPhone = it }, label = { Text(stringResource(R.string.qr_contact_phone)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(cEmail, { cEmail = it }, label = { Text(stringResource(R.string.qr_contact_email)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            QrType.GEO -> {
                OutlinedTextField(lat, { lat = it }, label = { Text(stringResource(R.string.qr_geo_lat)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(lng, { lng = it }, label = { Text(stringResource(R.string.qr_geo_lng)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            QrType.SMS -> {
                OutlinedTextField(smsNumber, { smsNumber = it }, label = { Text("Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(smsBody, { smsBody = it }, label = { Text("Message") }, modifier = Modifier.fillMaxWidth())
            }
            QrType.MAIL -> {
                OutlinedTextField(mailAddr, { mailAddr = it }, label = { Text("To") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(mailSubj, { mailSubj = it }, label = { Text("Subject") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(mailBody, { mailBody = it }, label = { Text("Body") }, modifier = Modifier.fillMaxWidth())
            }
        }

        Spacer(Modifier.height(16.dp))

        // QR preview + actions.
        val bmp = bitmap
        if (bmp != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AColor.WHITE.toComposeColor()
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ElevatedButton(onClick = {
                    val file = saveBitmapToPictures(ctx, bmp)
                    Toast.makeText(
                        ctx,
                        if (file != null) "Saved: ${file.name}" else "Save failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }) {
                    Icon(Icons.Default.Save, null); Spacer(Modifier.width(6.dp))
                    Text("Uložit")
                }
                ElevatedButton(onClick = {
                    val file = saveBitmapToCache(ctx, bmp)
                    if (file != null) {
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            ctx, "${ctx.packageName}.fileprovider", file
                        )
                        val intent = Intent(Intent.ACTION_SEND)
                            .setType("image/png")
                            .putExtra(Intent.EXTRA_STREAM, uri)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        ctx.startActivity(Intent.createChooser(intent, null))
                    }
                }) {
                    Icon(Icons.Default.Share, null); Spacer(Modifier.width(6.dp))
                    Text("Sdílet")
                }
                ElevatedButton(
                    enabled = payload.isNotBlank(),
                    onClick = {
                        clip.setText(AnnotatedString(payload))
                        Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.common_copy))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                payload,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                stringResource(R.string.qr_error_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ScanTab() {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current

    var decoded by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val result = decodeFromImage(ctx, uri)
            decoded = result.getOrNull()
            errorMessage = result.exceptionOrNull()?.message
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(onClick = { pickLauncher.launch("image/*") }) {
            Icon(Icons.Default.Image, null); Spacer(Modifier.width(6.dp))
            Text("Vybrat obrázek")
        }

        decoded?.let { text ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.qr_scanned_text),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = {
                                clip.setText(AnnotatedString(text))
                                Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show()
                            },
                            label = { Text(stringResource(R.string.common_copy)) },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, null) }
                        )
                        if (text.startsWith("http://", true) || text.startsWith("https://", true)) {
                            AssistChip(
                                onClick = {
                                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(text)))
                                },
                                label = { Text("Otevřít") }
                            )
                        }
                    }
                }
            }
        }

        errorMessage?.let {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun TypeChip(
    current: QrType,
    value: QrType,
    label: String,
    onSelect: (QrType) -> Unit
) {
    FilterChip(
        selected = current == value,
        onClick = { onSelect(value) },
        label = { Text(label) }
    )
}

// --- Helpers ------------------------------------------------------------

private fun buildPayload(
    type: QrType,
    text: String,
    ssid: String, password: String, hidden: Boolean, security: String,
    cName: String, cPhone: String, cEmail: String,
    lat: String, lng: String,
    smsNumber: String, smsBody: String,
    mailAddr: String, mailSubj: String, mailBody: String
): String = when (type) {
    QrType.TEXT -> text
    QrType.URL -> text
    QrType.WIFI -> {
        if (ssid.isBlank()) "" else buildString {
            append("WIFI:T:$security;")
            append("S:${escapeQr(ssid)};")
            if (security != "nopass") append("P:${escapeQr(password)};")
            if (hidden) append("H:true;")
            append(";")
        }
    }
    QrType.CONTACT -> {
        if (cName.isBlank() && cPhone.isBlank() && cEmail.isBlank()) "" else buildString {
            append("MECARD:")
            if (cName.isNotBlank()) append("N:${escapeQr(cName)};")
            if (cPhone.isNotBlank()) append("TEL:${escapeQr(cPhone)};")
            if (cEmail.isNotBlank()) append("EMAIL:${escapeQr(cEmail)};")
            append(";")
        }
    }
    QrType.GEO -> {
        if (lat.isBlank() || lng.isBlank()) "" else "geo:$lat,$lng"
    }
    QrType.SMS -> {
        if (smsNumber.isBlank()) "" else "SMSTO:$smsNumber:$smsBody"
    }
    QrType.MAIL -> {
        if (mailAddr.isBlank()) "" else buildString {
            append("mailto:$mailAddr")
            val params = buildList {
                if (mailSubj.isNotBlank()) add("subject=${Uri.encode(mailSubj)}")
                if (mailBody.isNotBlank()) add("body=${Uri.encode(mailBody)}")
            }
            if (params.isNotEmpty()) {
                append("?")
                append(params.joinToString("&"))
            }
        }
    }
}

private fun escapeQr(s: String): String =
    s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace(":", "\\:")

private fun generateQr(content: String): Bitmap? = runCatching {
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 512, 512)
    val w = matrix.width
    val h = matrix.height
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    for (x in 0 until w) for (y in 0 until h) {
        bmp.setPixel(x, y, if (matrix[x, y]) AColor.BLACK else AColor.WHITE)
    }
    bmp
}.getOrNull()

private fun decodeFromImage(ctx: android.content.Context, uri: Uri): Result<String> = runCatching {
    val bmp = ctx.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) }
        ?: error("Cannot read image")
    val w = bmp.width
    val h = bmp.height
    val pixels = IntArray(w * h)
    bmp.getPixels(pixels, 0, w, 0, 0, w, h)
    val source = RGBLuminanceSource(w, h, pixels)
    val bb = BinaryBitmap(HybridBinarizer(source))
    val result = MultiFormatReader().decode(bb)
    result.text
}

private fun saveBitmapToPictures(ctx: android.content.Context, bmp: Bitmap): File? = runCatching {
    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "ZeddiHub")
    dir.mkdirs()
    val file = File(dir, "qr_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    file
}.getOrNull()

private fun saveBitmapToCache(ctx: android.content.Context, bmp: Bitmap): File? = runCatching {
    val dir = File(ctx.cacheDir, "qr_share")
    dir.mkdirs()
    val file = File(dir, "qr.png")
    FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    file
}.getOrNull()

private fun Int.toComposeColor(): androidx.compose.ui.graphics.Color =
    androidx.compose.ui.graphics.Color(this)
