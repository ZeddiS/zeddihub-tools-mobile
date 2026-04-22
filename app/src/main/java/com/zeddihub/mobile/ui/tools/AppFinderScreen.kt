package com.zeddihub.mobile.ui.tools

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.zeddihub.mobile.R

@Composable
fun AppFinderScreen(
    padding: PaddingValues,
    vm: AppFinderViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val colors = MaterialTheme.colorScheme

    val visible = remember(state) {
        val q = state.filter.trim().lowercase()
        val base = if (state.includeSystem) state.apps else state.apps.filter { !it.isSystem }
        val filtered = if (q.isEmpty()) base
        else base.filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
        when (state.sort) {
            AppFinderViewModel.SortBy.SIZE_DESC -> filtered.sortedByDescending { it.totalSize }
            AppFinderViewModel.SortBy.SIZE_ASC -> filtered.sortedBy { it.totalSize }
            AppFinderViewModel.SortBy.NAME_ASC -> filtered.sortedBy { it.label.lowercase() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(colors.primary.copy(alpha = 0.16f), Color.Transparent)
                    ),
                    RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Apps, null, tint = colors.primary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(
                        stringResource(R.string.appfinder_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = vm::reload) {
                        Icon(Icons.Default.Refresh, null, tint = colors.primary)
                    }
                }
                Text(
                    stringResource(R.string.appfinder_total, formatBytes(state.totalBytes), visible.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.filter,
            onValueChange = vm::setFilter,
            leadingIcon = { Icon(Icons.Default.Search, null) },
            placeholder = { Text(stringResource(R.string.appfinder_search_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SortChip(stringResource(R.string.appfinder_sort_size_desc), state.sort == AppFinderViewModel.SortBy.SIZE_DESC) { vm.setSort(AppFinderViewModel.SortBy.SIZE_DESC) }
            SortChip(stringResource(R.string.appfinder_sort_size_asc), state.sort == AppFinderViewModel.SortBy.SIZE_ASC) { vm.setSort(AppFinderViewModel.SortBy.SIZE_ASC) }
            SortChip(stringResource(R.string.appfinder_sort_name), state.sort == AppFinderViewModel.SortBy.NAME_ASC) { vm.setSort(AppFinderViewModel.SortBy.NAME_ASC) }
        }

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.appfinder_include_system),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = state.includeSystem, onCheckedChange = vm::setIncludeSystem)
        }

        Spacer(Modifier.height(8.dp))

        if (state.loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = colors.primary)
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
        } else {
            val maxSize = visible.maxOfOrNull { it.totalSize } ?: 1L
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visible, key = { it.packageName }) { app ->
                    AppRow(
                        app = app,
                        maxSize = maxSize,
                        onOpen = { vm.openApp(app.packageName) },
                        onSettings = { vm.openAppSettings(app.packageName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Default.FilterList, null, modifier = Modifier.size(16.dp)) }
        } else null,
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        )
    )
}

@Composable
private fun AppRow(
    app: AppFinderViewModel.AppEntry,
    maxSize: Long,
    onOpen: () -> Unit,
    onSettings: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val fraction = if (maxSize > 0) (app.totalSize.toFloat() / maxSize).coerceIn(0f, 1f) else 0f
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val bmp = remember(app.packageName) {
                    runCatching { app.icon?.toBitmap(80, 80)?.asImageBitmap() }.getOrNull()
                }
                if (bmp != null) {
                    Image(
                        bitmap = bmp,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp)
                    )
                } else {
                    Icon(Icons.Default.Apps, null, tint = colors.primary, modifier = Modifier.size(44.dp))
                }
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        app.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface
                    )
                    Text(
                        app.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        formatBytes(app.totalSize),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                    Text(
                        "APK ${formatBytes(app.apkSize)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = colors.primary,
                trackColor = colors.primary.copy(alpha = 0.14f)
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.OutlinedButton(onClick = onOpen, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Launch, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text(stringResource(R.string.appfinder_open))
                }
                androidx.compose.material3.OutlinedButton(onClick = onSettings, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Default.Settings, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text(stringResource(R.string.appfinder_manage))
                }
            }
        }
    }
}

private fun formatBytes(b: Long): String {
    if (b <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var v = b.toDouble(); var i = 0
    while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
    return "%.1f %s".format(v, units[i])
}
