package com.zeddihub.mobile.ui.main

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zeddihub.mobile.BuildConfig
import com.zeddihub.mobile.data.local.LanguageCode
import com.zeddihub.mobile.data.repository.UpdateState

/**
 * Dashboard-top banner that nudges the user to install a new
 * published release. Opens the APK URL in the system browser when
 * tapped (the actual install flow — permission prompt, package
 * installer — is handled by Android).
 *
 * Behaviour:
 *   • Hidden when [state] is Unknown / UpToDate / dismissed.
 *   • Body shows localized release notes when available; otherwise
 *     a generic "new version is ready" message.
 *   • A small × in the corner lets the user dismiss — the banner
 *     won't reappear until a strictly newer version is published.
 */
@Composable
fun UpdateBanner(
    state: UpdateState,
    language: LanguageCode,
    onDismiss: () -> Unit,
) {
    val available = state as? UpdateState.Available ?: return
    if (available.dismissed) return
    if (available.apkUrl.isBlank()) return

    val ctx = LocalContext.current
    val colors = MaterialTheme.colorScheme

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
    val cta = when (language) {
        LanguageCode.CS -> "Stáhnout"
        LanguageCode.EN -> "Download"
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
            Spacer(Modifier.size(10.dp))
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(onClick = onDismiss) { Text(later) }
                Spacer(Modifier.size(4.dp))
                TextButton(
                    onClick = {
                        runCatching {
                            ctx.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(available.apkUrl))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    },
                    modifier = Modifier
                        .background(colors.primary, RoundedCornerShape(10.dp))
                        .clickable {},
                ) {
                    Text(cta, color = colors.onPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
