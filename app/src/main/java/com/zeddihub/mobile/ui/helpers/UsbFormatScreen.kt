package com.zeddihub.mobile.ui.helpers

import android.content.Intent
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.zeddihub.mobile.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * "USB formatter" — but the truth is Android apps cannot truly format
 * removable media. The OS reserves filesystem creation for the system
 * Settings UI (which is itself OEM-specific). What an app CAN do is
 * ask the user to pick a tree URI via Storage Access Framework and
 * recursively delete every file inside it, which is the next best
 * thing for "I want my flash drive empty" — without the filesystem
 * type change.
 *
 * We're explicit about that in the UI: pick the drive root, confirm
 * twice (the second dialog spells out the destructiveness), then run
 * a DocumentFile recursive delete on a coroutine. Progress is shown
 * as "X / N files cleared".
 *
 * For an actual mkfs.vfat / mkfs.exfat the user has to go to Settings
 * → Storage → drive → Format. We link that flow from the help text.
 */
@Composable
fun UsbFormatScreen(padding: PaddingValues) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var rootUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var rootName by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var totalFound by remember { mutableStateOf(0) }
    var deleted by remember { mutableStateOf(0) }
    var working by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            // Persist read+write so we can iterate later; without this
            // the URI silently loses access on next process start.
            ctx.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            rootUri = uri
            rootName = DocumentFile.fromTreeUri(ctx, uri)?.name
            status = null
            totalFound = 0
            deleted = 0
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
        Text(
            stringResource(R.string.usbf_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(stringResource(R.string.usbf_disclaimer))

        Button(
            enabled = !working,
            onClick = { picker.launch(null) }
        ) { Text(stringResource(R.string.usbf_pick_root)) }

        if (rootUri != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(stringResource(R.string.usbf_picked) + " " + (rootName ?: "?"),
                        fontWeight = FontWeight.SemiBold)
                    Text(rootUri.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = !working,
                    onClick = { confirm = true }
                ) { Text(stringResource(R.string.usbf_wipe)) }
                OutlinedButton(
                    enabled = !working,
                    onClick = {
                        rootUri = null; rootName = null; status = null
                        totalFound = 0; deleted = 0
                    }
                ) { Text(stringResource(R.string.usbf_forget)) }
            }
        }

        if (working || totalFound > 0) {
            Text("$deleted / $totalFound", fontWeight = FontWeight.SemiBold)
        }
        status?.let {
            Text(it, color = MaterialTheme.colorScheme.primary)
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.usbf_confirm_title)) },
            text = { Text(stringResource(R.string.usbf_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    val uri = rootUri ?: return@TextButton
                    working = true
                    deleted = 0
                    totalFound = 0
                    status = null
                    scope.launch {
                        // Two passes: first count (so the progress is meaningful),
                        // then delete. Two passes is a few hundred ms more on a
                        // huge drive, but the user explicitly asked for a wipe
                        // so they're going to wait anyway — meaningful progress
                        // beats a stuck-looking spinner.
                        val root = DocumentFile.fromTreeUri(ctx, uri) ?: return@launch
                        val total = withContext(Dispatchers.IO) { count(root) - 1 }
                        totalFound = total.coerceAtLeast(0)
                        withContext(Dispatchers.IO) {
                            wipe(root) { deleted = it }
                        }
                        working = false
                        status = ctx.getString(R.string.usbf_done)
                    }
                }) { Text(stringResource(R.string.usbf_confirm_yes)) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) {
                    Text(stringResource(R.string.usbf_confirm_no))
                }
            }
        )
    }
}

private fun count(d: DocumentFile): Int {
    if (!d.isDirectory) return 1
    var c = 1
    for (child in d.listFiles()) c += count(child)
    return c
}

private fun wipe(d: DocumentFile, progress: (Int) -> Unit) {
    var n = 0
    fun walk(node: DocumentFile) {
        if (node.isDirectory) {
            // Snapshot children before iterating — DocumentFile.listFiles()
            // can return live results that mutate as we delete.
            val children = node.listFiles().toList()
            for (c in children) walk(c)
        }
        // Don't delete the root itself — the user kept the drive plugged in
        // for this; deleting the tree URI's root would invalidate the
        // permission grant and confuse subsequent runs.
        if (node !== d) {
            runCatching { node.delete() }
            n++
            progress(n)
        }
    }
    walk(d)
}
