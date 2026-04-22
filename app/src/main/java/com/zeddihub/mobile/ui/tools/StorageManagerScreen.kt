package com.zeddihub.mobile.ui.tools

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zeddihub.mobile.R
import java.text.DateFormat
import java.util.Date

@Composable
fun StorageManagerScreen(
    padding: PaddingValues,
    vm: StorageManagerViewModel = hiltViewModel()
) {
    val ctx = LocalContext.current
    val state by vm.state.collectAsState()
    val colors = MaterialTheme.colorScheme
    var showDuplicates by remember { mutableStateOf(false) }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(ctx, ctx.getString(R.string.storage_deleted_toast), Toast.LENGTH_SHORT).show()
            vm.onDeleteConfirmed()
        } else {
            Toast.makeText(ctx, ctx.getString(R.string.storage_delete_cancelled), Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        HeaderCard(state = state, onRefresh = { vm.scan() })
        Spacer(Modifier.height(14.dp))

        if (state.loading && state.totals.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            CategoriesCard(state)
            Spacer(Modifier.height(14.dp))

            Text(
                stringResource(R.string.storage_largest_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )
            Spacer(Modifier.height(8.dp))
            state.largest.forEach { item ->
                FileRow(
                    item = item,
                    selected = item.id in state.selected,
                    onToggle = { vm.toggleSelect(item.id) }
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.storage_duplicates_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = { showDuplicates = !showDuplicates },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        if (showDuplicates)
                            stringResource(R.string.storage_hide)
                        else
                            stringResource(R.string.storage_show, state.duplicates.size)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            if (showDuplicates) {
                if (state.duplicates.isEmpty()) {
                    Text(
                        stringResource(R.string.storage_duplicates_none),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                } else {
                    state.duplicates.forEach { cluster ->
                        DuplicateBlock(
                            cluster = cluster,
                            selected = state.selected,
                            onToggle = { vm.toggleSelect(it) }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    // Floating action bar when at least one item is selected.
    if (state.selected.isNotEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.primaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.storage_selected_count, state.selected.size),
                        color = colors.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = { vm.clearSelection() },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(stringResource(R.string.storage_clear)) }
                    Spacer(Modifier.size(8.dp))
                    Button(
                        onClick = {
                            val uris = vm.selectedUris()
                            requestDelete(ctx, uris) { intentSender ->
                                deleteLauncher.launch(
                                    IntentSenderRequest.Builder(intentSender).build()
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(stringResource(R.string.storage_delete))
                    }
                }
            }
        }
    }
}

// ─────────────────────── Components ───────────────────────

@Composable
private fun HeaderCard(state: StorageManagerViewModel.UiState, onRefresh: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val used = (state.deviceTotalBytes - state.deviceFreeBytes).coerceAtLeast(0)
    val ratio = if (state.deviceTotalBytes > 0)
        (used.toFloat() / state.deviceTotalBytes.toFloat()).coerceIn(0f, 1f)
    else 0f

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
                Icon(Icons.Default.Storage, null, tint = colors.primary, modifier = Modifier.size(26.dp))
                Spacer(Modifier.size(10.dp))
                Text(
                    stringResource(R.string.storage_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary,
                    modifier = Modifier.weight(1f)
                )
                if (state.loading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                        color = colors.primary
                    )
                } else {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, null, tint = colors.primary)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = ratio,
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = colors.primary,
                trackColor = colors.primary.copy(alpha = 0.18f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(
                    R.string.storage_device_usage,
                    humanBytes(used),
                    humanBytes(state.deviceTotalBytes)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
            Text(
                stringResource(
                    R.string.storage_media_total,
                    humanBytes(state.totalAllBytes)
                ),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoriesCard(state: StorageManagerViewModel.UiState) {
    val colors = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                stringResource(R.string.storage_by_category),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )
            Spacer(Modifier.height(8.dp))
            state.totals.forEach { t ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(iconFor(t.category), null, tint = colors.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(10.dp))
                    Text(
                        categoryLabel(t.category),
                        color = colors.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${t.count} · ${humanBytes(t.totalBytes)}",
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun FileRow(
    item: StorageManagerViewModel.FileItem,
    selected: Boolean,
    onToggle: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) colors.primaryContainer else colors.surface
        ),
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = selected, onCheckedChange = { onToggle() })
            Spacer(Modifier.size(6.dp))
            Icon(iconFor(item.category), null, tint = colors.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.displayName,
                    color = colors.onSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    (item.relativePath ?: "") + " · " +
                        DateFormat.getDateInstance(DateFormat.SHORT)
                            .format(Date(item.dateAdded * 1000L)),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Text(
                humanBytes(item.sizeBytes),
                color = colors.primary,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun DuplicateBlock(
    cluster: StorageManagerViewModel.DuplicateCluster,
    selected: Set<Long>,
    onToggle: (Long) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "${cluster.name} · ${cluster.items.size}× · ${humanBytes(cluster.sizeBytes)}",
                color = colors.onSurface,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.height(6.dp))
            cluster.items.forEach { item ->
                FileRow(
                    item = item,
                    selected = item.id in selected,
                    onToggle = { onToggle(item.id) }
                )
            }
        }
    }
}

// ─────────────────────── Helpers ───────────────────────

private fun iconFor(c: StorageManagerViewModel.Category): ImageVector = when (c) {
    StorageManagerViewModel.Category.IMAGE -> Icons.Default.Image
    StorageManagerViewModel.Category.VIDEO -> Icons.Default.Movie
    StorageManagerViewModel.Category.AUDIO -> Icons.Default.AudioFile
    StorageManagerViewModel.Category.DOCUMENT -> Icons.Default.Description
    StorageManagerViewModel.Category.ARCHIVE -> Icons.Default.Archive
    StorageManagerViewModel.Category.OTHER -> Icons.Default.InsertDriveFile
}

@Composable
private fun categoryLabel(c: StorageManagerViewModel.Category): String = stringResource(
    when (c) {
        StorageManagerViewModel.Category.IMAGE -> R.string.storage_cat_image
        StorageManagerViewModel.Category.VIDEO -> R.string.storage_cat_video
        StorageManagerViewModel.Category.AUDIO -> R.string.storage_cat_audio
        StorageManagerViewModel.Category.DOCUMENT -> R.string.storage_cat_document
        StorageManagerViewModel.Category.ARCHIVE -> R.string.storage_cat_archive
        StorageManagerViewModel.Category.OTHER -> R.string.storage_cat_other
    }
)

private fun humanBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    var i = 0
    while (size >= 1024 && i < units.size - 1) { size /= 1024; i++ }
    return if (i == 0) "${bytes} B" else "%.2f %s".format(size, units[i])
}

private fun requestDelete(
    ctx: android.content.Context,
    uris: List<Uri>,
    onLaunch: (IntentSender) -> Unit
) {
    if (uris.isEmpty()) return
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pi: PendingIntent = MediaStore.createDeleteRequest(ctx.contentResolver, uris)
            onLaunch(pi.intentSender)
        } else {
            // Best-effort delete on older devices; this will fail silently
            // for files the app doesn't own, which is an acceptable trade-off
            // for keeping the tool scoped-storage friendly.
            var ok = 0
            uris.forEach { u ->
                runCatching {
                    ctx.contentResolver.delete(u, null, null)
                    ok++
                }
            }
            Toast.makeText(
                ctx,
                ctx.getString(R.string.storage_deleted_count, ok),
                Toast.LENGTH_SHORT
            ).show()
        }
    } catch (t: Throwable) {
        Toast.makeText(ctx, t.localizedMessage ?: "delete failed", Toast.LENGTH_SHORT).show()
    }
}
