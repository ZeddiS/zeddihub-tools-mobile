package com.zeddihub.mobile.ui.helpers

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zeddihub.mobile.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.flow.collectLatest

/**
 * Rychlá poznámka s QR sdílením.
 *
 * Funguje jako jednoduchý scratchpad uložený v SharedPreferences —
 * uživatel si sem zapíše nákupní seznam / heslo k WiFi pro návštěvu /
 * libovolný krátký text a může ho:
 *   • zkopírovat do clipboardu
 *   • sdílet přes ACTION_SEND (messenger, mail, SMS)
 *   • vygenerovat QR kód — druhé zařízení si načte přes kameru
 *
 * QR má omezení ~2 953 bytes (binary) / 4 296 alfanumerických znaků.
 * Jedinou validaci děláme na `input.length > 2000` — tam už hrozí
 * že generátor neunese vysokou error correction a selže. Pod tou
 * hranicí používáme ErrorCorrectionLevel.M (15 % tolerance), nad ní
 * spadneme na L (7 %) aby se to vůbec vešlo.
 */
@Composable
fun QuickNoteScreen(padding: PaddingValues) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var text by remember { mutableStateOf(prefs.getString(KEY_NOTE, "") ?: "") }
    var lastSaved by remember { mutableStateOf(text) }
    val isDirty by remember(text, lastSaved) {
        derivedStateOf { text != lastSaved }
    }
    var showQrDialog by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    // Autosave with a real debounce. The original `LaunchedEffect(text)
    // { delay(600) }` looked like a debounce but actually wasn't — every
    // keystroke restarted the effect, and on rapid typing the coroutine
    // got cancelled before the write, sometimes leading to losing the
    // last char if the user backed out fast. snapshotFlow.debounce
    // properly coalesces edits: only the final value after 600 ms of
    // silence is written.
    LaunchedEffect(Unit) {
        snapshotFlow { text }
            .collectLatest { current ->
                if (current != lastSaved) {
                    kotlinx.coroutines.delay(600)
                    // Re-check after the wait — the value we observed
                    // might have changed in flight (collectLatest will
                    // have cancelled the previous suspension already).
                    if (current != lastSaved) {
                        prefs.edit().putString(KEY_NOTE, current).apply()
                        lastSaved = current
                    }
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(stringResource(R.string.quicknote_label)) },
            placeholder = { Text(stringResource(R.string.quicknote_placeholder)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
        )
        Text(
            text = if (isDirty)
                stringResource(R.string.quicknote_saving)
            else
                stringResource(R.string.quicknote_saved, text.length),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showQrDialog = true },
                enabled = text.isNotBlank() && text.length <= 2900,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.quicknote_qr_button))
            }
            Button(
                onClick = { clipboard.setText(AnnotatedString(text)) },
                enabled = text.isNotBlank(),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.quicknote_copy_button))
            }
            Button(
                onClick = { shareText(ctx, text) },
                enabled = text.isNotBlank(),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.quicknote_share_button))
            }
        }

        if (text.isNotBlank()) {
            TextButton(
                onClick = {
                    text = ""
                    prefs.edit().remove(KEY_NOTE).apply()
                    lastSaved = ""
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.quicknote_clear_button), color = MaterialTheme.colorScheme.error)
            }
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.quicknote_qr_limit_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp)
            )
        }
    }

    if (showQrDialog) {
        QrDialog(content = text, onDismiss = { showQrDialog = false })
    }
}

@Composable
private fun QrDialog(content: String, onDismiss: () -> Unit) {
    // Generate once per content string. Cached in remember so rotating
    // / recomposing the dialog doesn't re-encode on every frame.
    val bitmap = remember(content) { generateQrBitmap(content) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.quicknote_qr_dialog_close))
            }
        },
        title = { Text(stringResource(R.string.quicknote_qr_dialog_title)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.quicknote_qr_content_description),
                        modifier = Modifier.size(260.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.quicknote_qr_dialog_caption, content.length),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(stringResource(R.string.quicknote_qr_dialog_failed))
                }
            }
        }
    )
}

private fun generateQrBitmap(content: String): Bitmap? = runCatching {
    val sizePx = 640
    val hints = hashMapOf<EncodeHintType, Any>(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.MARGIN to 1,
        // Drop to L for long inputs — M error correction costs ~15 %
        // of the payload budget and big notes won't fit otherwise.
        EncodeHintType.ERROR_CORRECTION to
            if (content.length > 1800) ErrorCorrectionLevel.L
            else ErrorCorrectionLevel.M,
    )
    val matrix = MultiFormatWriter()
        .encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    val w = matrix.width
    val h = matrix.height
    val pixels = IntArray(w * h)
    for (y in 0 until h) {
        val off = y * w
        for (x in 0 until w) {
            pixels[off + x] = if (matrix[x, y]) Color.Black.toArgb() else Color.White.toArgb()
        }
    }
    Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, w, 0, 0, w, h)
    }
}.getOrNull()

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(),
    (red * 255).toInt(),
    (green * 255).toInt(),
    (blue * 255).toInt(),
)

private fun shareText(ctx: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooser = Intent.createChooser(intent, ctx.getString(R.string.quicknote_share_chooser))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    ctx.startActivity(chooser)
}

private const val PREFS = "zeddihub_quick_note"
private const val KEY_NOTE = "note_body"
