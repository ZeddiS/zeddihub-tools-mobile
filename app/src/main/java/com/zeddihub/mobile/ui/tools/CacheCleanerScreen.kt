package com.zeddihub.mobile.ui.tools

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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

@Composable
fun CacheCleanerScreen(
    padding: PaddingValues,
    vm: CacheCleanerViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val colors = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    var pendingDeleteUri by remember { mutableStateOf<Uri?>(null) }

    val safScanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        // Persist read permission so the user doesn't have to re-grant it.
        runCatching {
            ctx.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        vm.safScanAndClean(uri, confirmDelete = false)
        pendingDeleteUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            colors.primary.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    ),
                    RoundedCornerShape(18.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CleaningServices, null, tint = colors.primary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.size(10.dp))
                    Text(
                        stringResource(R.string.cache_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.cache_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // Current sizes row
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SizeCard(
                icon = Icons.Default.Folder,
                label = stringResource(R.string.cache_app_cache),
                value = formatBytes(state.appCacheBytes),
                modifier = Modifier.weight(1f)
            )
            SizeCard(
                icon = Icons.Default.Storage,
                label = stringResource(R.string.cache_temp_files),
                value = formatBytes(state.tempBytes),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(18.dp))

        ActionCard(
            icon = Icons.Default.CleaningServices,
            title = stringResource(R.string.cache_clear_app_title),
            body = stringResource(R.string.cache_clear_app_body),
            buttonLabel = stringResource(R.string.cache_clear_now),
            onClick = {
                vm.clearAll()
                Toast.makeText(ctx, ctx.getString(R.string.cache_cleared_toast), Toast.LENGTH_SHORT).show()
            }
        )

        Spacer(Modifier.height(12.dp))

        ActionCard(
            icon = Icons.Default.Dns,
            title = stringResource(R.string.cache_dns_title),
            body = stringResource(R.string.cache_dns_body),
            buttonLabel = stringResource(R.string.cache_flush_dns),
            onClick = { vm.flushDns() }
        )

        Spacer(Modifier.height(12.dp))

        // ─── SAF scan/clean ─────────────────────────────────────────────────
        ActionCard(
            icon = Icons.Default.Search,
            title = stringResource(R.string.cache_saf_scan_title),
            body = stringResource(R.string.cache_saf_scan_body),
            buttonLabel = stringResource(R.string.cache_saf_pick_folder),
            outlined = true,
            onClick = {
                runCatching { safScanLauncher.launch(null) }
            }
        )

        if (state.safMatchCount > 0 && pendingDeleteUri != null) {
            Spacer(Modifier.height(10.dp))
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colors.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        stringResource(
                            R.string.cache_saf_results,
                            state.safMatchCount,
                            formatBytes(state.safScannedBytes)
                        ),
                        color = colors.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            pendingDeleteUri?.let { vm.safScanAndClean(it, confirmDelete = true) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(stringResource(R.string.cache_saf_delete_now))
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ─── System storage settings deeplink ──────────────────────────────
        ActionCard(
            icon = Icons.Default.FolderOpen,
            title = stringResource(R.string.cache_system_storage_title),
            body = stringResource(R.string.cache_system_storage_body),
            buttonLabel = stringResource(R.string.cache_open_system_storage),
            outlined = true,
            onClick = {
                val opened = runCatching {
                    ctx.startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
                    true
                }.getOrDefault(false)
                if (!opened) {
                    runCatching {
                        ctx.startActivity(Intent(Settings.ACTION_MEMORY_CARD_SETTINGS))
                    }
                }
            }
        )

        Spacer(Modifier.height(12.dp))

        ActionCard(
            icon = Icons.AutoMirrored.Filled.Launch,
            title = stringResource(R.string.cache_system_title),
            body = stringResource(R.string.cache_system_body),
            buttonLabel = stringResource(R.string.cache_open_system),
            outlined = true,
            onClick = {
                runCatching {
                    ctx.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.parse("package:${ctx.packageName}"))
                    )
                }
            }
        )

        state.lastAction?.let { msg ->
            Spacer(Modifier.height(14.dp))
            Text(msg, color = colors.primary, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun SizeCard(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, null, tint = colors.primary)
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.onSurface)
        }
    }
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    body: String,
    buttonLabel: String,
    outlined: Boolean = false,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = colors.primary)
                Spacer(Modifier.size(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
            }
            Spacer(Modifier.height(6.dp))
            Text(body, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            if (outlined) {
                OutlinedButton(onClick = onClick, shape = RoundedCornerShape(12.dp)) { Text(buttonLabel) }
            } else {
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) { Text(buttonLabel, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

private fun formatBytes(b: Long): String {
    if (b <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var v = b.toDouble()
    var i = 0
    while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
    return "%.1f %s".format(v, units[i])
}
