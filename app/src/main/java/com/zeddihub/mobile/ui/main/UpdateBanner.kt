package com.zeddihub.mobile.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.zeddihub.mobile.BuildConfig
import com.zeddihub.mobile.data.local.LanguageCode
import com.zeddihub.mobile.data.repository.UpdateState
import com.zeddihub.mobile.data.update.UpdateChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Thin Hilt VM that hands the UpdateChecker singleton down to the
 * UpdateBanner Composable. Without it the banner would have to be
 * re-injected at every call site or rely on a CompositionLocal —
 * a tiny VM is the smallest change that keeps Hilt happy.
 */
@HiltViewModel
class UpdateBannerViewModel @Inject constructor(
    val updateChecker: UpdateChecker,
) : ViewModel()

/**
 * Local 2-step state for the update flow shared between the banner
 * and the startup dialog. Kept in this file (instead of a separate
 * util) because it's UI-state — the underlying data layer already has
 * its own UpdateState.
 */
sealed class UpdatePhase {
    data object Idle : UpdatePhase()
    data object Downloading : UpdatePhase()
    data class Downloaded(val file: java.io.File) : UpdatePhase()
    data object Error : UpdatePhase()
}

/**
 * Dashboard-top banner that nudges the user to install a new
 * published release.
 *
 * Behaviour change in v0.8.3 (per user report): "Stáhnout" now
 * downloads the APK in-app via [UpdateChecker] and hands it to
 * Android's package installer through a content URI — the same
 * flow Settings → Aktualizace already uses. Previously this
 * banner did `Intent(ACTION_VIEW, apkUrl)` which opened the
 * browser, defeating the whole release-gating + signed-mirror
 * pipeline.
 *
 *   • Hidden when [state] is Unknown / UpToDate / dismissed.
 *   • Body shows localized release notes when available; otherwise
 *     a generic "new version is ready" message.
 *   • Tapping Stáhnout starts a background download with progress
 *     bar; on success the system installer opens automatically.
 *   • A small × in the corner lets the user dismiss — the banner
 *     won't reappear until a strictly newer version is published.
 */
@Composable
fun UpdateBanner(
    state: UpdateState,
    language: LanguageCode,
    onDismiss: () -> Unit,
    vm: UpdateBannerViewModel = hiltViewModel(),
) {
    val available = state as? UpdateState.Available ?: return
    if (available.dismissed) return
    if (available.apkUrl.isBlank()) return

    val ctx = LocalContext.current
    val colors = MaterialTheme.colorScheme

    // Two-step UX shared with StartupUpdateDialog + Settings:
    //   Idle → Downloading → Downloaded → Install. Click "Stáhnout"
    //   only triggers the download; the resulting file is held until the
    //   user explicitly clicks "Instalovat", which fires the system
    //   package installer.
    var phase by remember { mutableStateOf<UpdatePhase>(UpdatePhase.Idle) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val downloading = phase is UpdatePhase.Downloading
    val downloaded  = phase as? UpdatePhase.Downloaded

    val notes = when (language) {
        LanguageCode.CS -> available.releaseNotesCs.ifBlank { available.releaseNotesEn }
        LanguageCode.EN -> available.releaseNotesEn.ifBlank { available.releaseNotesCs }
    }

    val headline = when (language) {
        LanguageCode.CS -> "Nová verze ${available.versionName} je k dispozici"
        LanguageCode.EN -> "New version ${available.versionName} is available"
    }
    val currentLabel = when (language) {
        LanguageCode.CS -> "Máš ${BuildConfig.VERSION_NAME}"
        LanguageCode.EN -> "You have ${BuildConfig.VERSION_NAME}"
    }
    val ctaDownload = when (language) {
        LanguageCode.CS -> "Stáhnout"
        LanguageCode.EN -> "Download"
    }
    val ctaInstall = when (language) {
        LanguageCode.CS -> "Instalovat"
        LanguageCode.EN -> "Install"
    }
    val later = when (language) {
        LanguageCode.CS -> "Později"
        LanguageCode.EN -> "Later"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = colors.surface,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            colors.primary.copy(alpha = 0.22f),
                            colors.tertiary.copy(alpha = 0.12f),
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                    )
                    Text(
                        text = currentLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = later,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            if (notes.isNotBlank()) {
                Spacer(Modifier.size(6.dp))
                Text(
                    text = notes.take(300),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            if (downloading) {
                Spacer(Modifier.size(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.size(4.dp))
                Text(
                    text = when (language) {
                        LanguageCode.CS -> "Stahuje se nová verze…"
                        LanguageCode.EN -> "Downloading the new version…"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
            }
            error?.let {
                Spacer(Modifier.size(6.dp))
                Text(it, color = colors.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.size(10.dp))
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss, enabled = !downloading) { Text(later) }
                Spacer(Modifier.size(4.dp))
                Button(
                    onClick = {
                        // Step 1: Download (2-step UX). The apk URL goes
                        // through UpdateChecker so HTTPS validation, retry,
                        // and FileProvider plumbing match the Settings →
                        // Update flow exactly.
                        // Step 2: After download, the same button becomes
                        // "Instalovat" and fires the system installer.
                        when (val ph = phase) {
                            is UpdatePhase.Idle, is UpdatePhase.Error -> {
                                phase = UpdatePhase.Downloading
                                error = null
                                scope.launch {
                                    val apk = vm.updateChecker.downloadApk(ctx, available.apkUrl)
                                    phase = if (apk != null) UpdatePhase.Downloaded(apk)
                                            else UpdatePhase.Error
                                    if (apk == null) {
                                        error = when (language) {
                                            LanguageCode.CS -> "Stahování selhalo. Zkus to znovu."
                                            LanguageCode.EN -> "Download failed. Please try again."
                                        }
                                    }
                                }
                            }
                            is UpdatePhase.Downloaded -> {
                                runCatching { vm.updateChecker.installApk(ctx, ph.file) }
                                    .onFailure { error = it.message }
                            }
                            is UpdatePhase.Downloading -> { /* button is disabled */ }
                        }
                    },
                    enabled = !downloading,
                ) {
                    when {
                        downloading -> CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = colors.onPrimary,
                        )
                        downloaded != null -> Text(ctaInstall, fontWeight = FontWeight.SemiBold)
                        else -> Text(ctaDownload, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
