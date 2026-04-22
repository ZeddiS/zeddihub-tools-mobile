package com.zeddihub.mobile.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zeddihub.mobile.BuildConfig
import com.zeddihub.mobile.R
import com.zeddihub.mobile.data.local.LanguageCode
import com.zeddihub.mobile.data.local.ThemeMode

@Composable
fun SettingsScreen(
    padding: PaddingValues,
    viewModel: SettingsViewModel = hiltViewModel(),
    onLanguageChange: (LanguageCode) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onFactoryReset: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }
    var languageMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(padding)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(8.dp))

        SectionHeader(stringResource(R.string.settings_appearance))
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_language), color = colors.onSurface)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (state.language == LanguageCode.CS) "🇨🇿 Čeština" else "🇬🇧 English",
                        fontSize = 16.sp
                    )
                    IconButton(onClick = { languageMenu = true }) {
                        Text("▾", fontSize = 18.sp, color = colors.onSurface)
                    }
                    DropdownMenu(
                        expanded = languageMenu,
                        onDismissRequest = { languageMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("🇨🇿 ${stringResource(R.string.lang_cs)}") },
                            onClick = {
                                languageMenu = false
                                viewModel.setLanguage(LanguageCode.CS)
                                onLanguageChange(LanguageCode.CS)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🇬🇧 ${stringResource(R.string.lang_en)}") },
                            onClick = {
                                languageMenu = false
                                viewModel.setLanguage(LanguageCode.EN)
                                onLanguageChange(LanguageCode.EN)
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_theme), color = colors.onSurface)
                    Text(
                        text = if (state.theme == ThemeMode.DARK)
                            stringResource(R.string.theme_dark)
                        else stringResource(R.string.theme_light),
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                IconButton(onClick = {
                    val next = if (state.theme == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
                    viewModel.setTheme(next)
                    onThemeChange(next)
                }) {
                    Icon(
                        imageVector = if (state.theme == ThemeMode.DARK)
                            Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = null,
                        tint = colors.primary
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        SectionHeader(stringResource(R.string.settings_security))
        SettingsCard {
            SwitchRow(
                label = stringResource(R.string.settings_app_lock),
                sub = stringResource(R.string.settings_app_lock_desc),
                checked = state.appLockEnabled,
                onCheckedChange = viewModel::setAppLock
            )
        }

        Spacer(Modifier.height(12.dp))
        SectionHeader(stringResource(R.string.settings_notifications))
        SettingsCard {
            SwitchRow(
                label = stringResource(R.string.settings_enable_push),
                checked = state.pushEnabled,
                onCheckedChange = viewModel::setPush
            )
        }

        Spacer(Modifier.height(12.dp))
        SectionHeader(stringResource(R.string.settings_updates))
        SettingsCard {
            SwitchRow(
                label = stringResource(R.string.settings_auto_update),
                sub = stringResource(R.string.settings_auto_update_desc),
                checked = state.autoUpdate,
                onCheckedChange = viewModel::setAutoUpdate
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.checkForUpdates() }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = colors.primary)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_check_updates), color = colors.onSurface)
                }
                when (state.updateCheckState) {
                    UpdateCheckState.Checking, UpdateCheckState.Downloading ->
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.primary)
                    UpdateCheckState.UpToDate ->
                        Text("✓", color = colors.primary, fontWeight = FontWeight.Bold)
                    is UpdateCheckState.Available, is UpdateCheckState.Downloaded ->
                        Text("!", color = colors.primary, fontWeight = FontWeight.Bold)
                    is UpdateCheckState.Error ->
                        Text("!", color = colors.error, fontWeight = FontWeight.Bold)
                    else -> {}
                }
            }
            when (val u = state.updateCheckState) {
                UpdateCheckState.UpToDate -> UpdateStatusText(stringResource(R.string.update_up_to_date))
                is UpdateCheckState.Available -> UpdateAvailableRow(
                    tag = u.info.tag,
                    body = u.info.body,
                    onDownload = { viewModel.downloadUpdate(u.info) },
                    onLater = { viewModel.resetUpdateCheck() }
                )
                is UpdateCheckState.Downloaded -> UpdateInstallRow(
                    tag = u.info.tag,
                    onInstall = { viewModel.installUpdate(u.file) }
                )
                is UpdateCheckState.Error -> UpdateStatusText(
                    stringResource(R.string.update_error),
                    isError = true
                )
                else -> {}
            }
        }

        Spacer(Modifier.height(12.dp))
        SectionHeader(stringResource(R.string.settings_advanced))
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.clearCache()
                        Toast.makeText(ctx, ctx.getString(R.string.settings_cache_cleared), Toast.LENGTH_SHORT).show()
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CleaningServices, contentDescription = null, tint = colors.primary)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_clear_cache), color = colors.onSurface)
                    Text(
                        stringResource(R.string.settings_clear_cache_desc),
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showResetDialog = true }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Restore, contentDescription = null, tint = colors.error)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_factory_reset), color = colors.error)
                    Text(
                        stringResource(R.string.settings_factory_reset_desc),
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        SectionHeader(stringResource(R.string.settings_about))
        SettingsCard {
            InfoRow(stringResource(R.string.settings_version), BuildConfig.VERSION_NAME)
            InfoRow(stringResource(R.string.settings_build), BuildConfig.VERSION_CODE.toString())
            InfoRow(stringResource(R.string.settings_license), "MIT")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ZeddiS/zeddihub-tools-mobile")))
                    }
                    .padding(14.dp)
            ) {
                Text(stringResource(R.string.settings_source), color = colors.primary, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(Modifier.height(32.dp))
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.settings_factory_reset_confirm)) },
            text = { Text(stringResource(R.string.settings_factory_reset_confirm_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        viewModel.factoryReset()
                        onFactoryReset()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.error,
                        contentColor = colors.onError
                    )
                ) { Text(stringResource(R.string.settings_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = colors.onSurfaceVariant,
        modifier = Modifier.padding(start = 6.dp, top = 12.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column { content() }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    sub: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = colors.onSurface)
            sub?.let {
                Text(it, color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, color = colors.onSurface, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun UpdateStatusText(text: String, isError: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = text,
        color = if (isError) colors.error else colors.onSurfaceVariant,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
private fun UpdateAvailableRow(
    tag: String,
    body: String,
    onDownload: () -> Unit,
    onLater: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.padding(14.dp)) {
        Text(
            text = stringResource(R.string.update_available, tag),
            color = colors.primary,
            fontWeight = FontWeight.Bold
        )
        if (body.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = body.take(400),
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium
            )
        }
        Spacer(Modifier.height(10.dp))
        Row {
            Button(onClick = onDownload) {
                Text(stringResource(R.string.update_download))
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onLater) {
                Text(stringResource(R.string.update_later))
            }
        }
    }
}

@Composable
private fun UpdateInstallRow(tag: String, onInstall: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.padding(14.dp)) {
        Text(
            text = stringResource(R.string.update_available, tag),
            color = colors.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = onInstall) {
            Text(stringResource(R.string.update_install))
        }
    }
}
