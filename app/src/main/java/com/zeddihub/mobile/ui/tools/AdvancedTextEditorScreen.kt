package com.zeddihub.mobile.ui.tools

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zeddihub.mobile.R
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Advanced text editor.
 *
 * Features:
 *  - Monospaced multi-line editor with live word / character counter
 *  - Open plain-text file through SAF `OpenDocument` picker
 *  - Save current text to a new file via `CreateDocument` picker
 *  - One-tap transforms: UPPER, lower, Title, reverse/sort/dedup lines,
 *    trim
 *  - Copy, share, clear
 *  - Large files (> 2 MB) open in read-only mode with a warning
 */
@Composable
fun AdvancedTextEditorScreen(padding: PaddingValues) {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current

    var text by remember { mutableStateOf("") }
    var readOnly by remember { mutableStateOf(false) }
    var currentName by remember { mutableStateOf<String?>(null) }

    val openLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val name = queryDisplayName(ctx, uri)
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    val available = input.available()
                    val big = available > 2 * 1024 * 1024
                    val reader = BufferedReader(InputStreamReader(input))
                    text = reader.readText()
                    readOnly = big
                    currentName = name
                }
            } catch (t: Throwable) {
                Toast.makeText(ctx, t.message ?: "Read failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                ctx.contentResolver.openOutputStream(uri, "w")?.use { os ->
                    os.write(text.toByteArray(Charsets.UTF_8))
                    os.flush()
                }
                currentName = queryDisplayName(ctx, uri)
                Toast.makeText(ctx, "Saved", Toast.LENGTH_SHORT).show()
            } catch (t: Throwable) {
                Toast.makeText(ctx, t.message ?: "Save failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(12.dp)
    ) {
        // Action row (horizontally scrollable so small screens still fit).
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            ElevatedButton(onClick = { openLauncher.launch(arrayOf("text/*")) }) {
                Icon(Icons.Default.FileOpen, null); Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.txt_open))
            }
            ElevatedButton(
                enabled = text.isNotEmpty() && !readOnly,
                onClick = { saveLauncher.launch(currentName ?: "document.txt") }
            ) {
                Icon(Icons.Default.Save, null); Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.txt_save_as))
            }
            ElevatedButton(
                enabled = text.isNotEmpty(),
                onClick = {
                    clip.setText(AnnotatedString(text))
                    Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.common_copy))
            }
            ElevatedButton(
                enabled = text.isNotEmpty(),
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, text)
                    ctx.startActivity(Intent.createChooser(intent, null))
                }
            ) {
                Icon(Icons.Default.Share, null); Spacer(Modifier.width(6.dp))
                Text("Sdílet")
            }
            ElevatedButton(
                enabled = text.isNotEmpty() && !readOnly,
                onClick = { text = ""; currentName = null }
            ) {
                Icon(Icons.Default.Delete, null); Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.common_clear))
            }
        }

        Spacer(Modifier.height(8.dp))

        // Transform chips.
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            AssistChip(
                onClick = { if (!readOnly) text = text.uppercase() },
                label = { Text("UPPER") }
            )
            AssistChip(
                onClick = { if (!readOnly) text = text.lowercase() },
                label = { Text("lower") }
            )
            AssistChip(
                onClick = { if (!readOnly) text = titleCase(text) },
                label = { Text("Title") }
            )
            AssistChip(
                onClick = { if (!readOnly) text = text.lines().reversed().joinToString("\n") },
                label = { Text("Reverse") },
                leadingIcon = { Icon(Icons.Default.SwapVert, null) }
            )
            AssistChip(
                onClick = { if (!readOnly) text = text.lines().sorted().joinToString("\n") },
                label = { Text("Sort") }
            )
            AssistChip(
                onClick = { if (!readOnly) text = text.trim() },
                label = { Text("Trim") }
            )
            AssistChip(
                onClick = { if (!readOnly) text = text.lines().distinct().joinToString("\n") },
                label = { Text("Dedup") }
            )
        }

        Spacer(Modifier.height(8.dp))

        if (readOnly) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    stringResource(R.string.txt_readonly),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        OutlinedTextField(
            value = text,
            onValueChange = { if (!readOnly) text = it },
            readOnly = readOnly,
            placeholder = { Text(stringResource(R.string.txt_placeholder)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val words = remember(text) { if (text.isBlank()) 0 else text.trim().split(Regex("\\s+")).size }
            val chars = text.length
            Text(
                stringResource(R.string.txt_word_count, words, chars),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            currentName?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun titleCase(s: String): String =
    s.split(" ").joinToString(" ") { w ->
        if (w.isEmpty()) w
        else w.substring(0, 1).uppercase() + w.substring(1).lowercase()
    }

private fun queryDisplayName(ctx: android.content.Context, uri: Uri): String? {
    return try {
        ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
        }
    } catch (_: Throwable) { null }
}
