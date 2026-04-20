package com.zeddihub.mobile.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.zeddihub.mobile.R
import com.zeddihub.mobile.data.update.ReleaseInfo
import com.zeddihub.mobile.data.update.UpdateChecker
import kotlinx.coroutines.launch

@Composable
fun StartupUpdateDialog(updateChecker: UpdateChecker) {
    val ctx = LocalContext.current
    var release by remember { mutableStateOf<ReleaseInfo?>(null) }
    var dismissed by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val info = updateChecker.fetchLatest()
        if (info != null && updateChecker.isNewer(info)) {
            release = info
        }
    }

    val info = release ?: return
    if (dismissed) return

    AlertDialog(
        onDismissRequest = { if (!downloading) dismissed = true },
        icon = { Icon(Icons.Default.SystemUpdate, null, tint = MaterialTheme.colorScheme.primary) },
        title = {
            Text(
                stringResource(R.string.update_available_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    stringResource(R.string.update_available_version, info.tag),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                if (info.body.isNotBlank()) {
                    Text(
                        info.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                }
                if (downloading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.update_downloading),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                error?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (info.apkUrl.isBlank()) {
                        dismissed = true
                        return@Button
                    }
                    downloading = true
                    error = null
                    scope.launch {
                        val apk = updateChecker.downloadApk(ctx, info.apkUrl)
                        downloading = false
                        if (apk != null) {
                            runCatching { updateChecker.installApk(ctx, apk) }
                                .onFailure { error = it.message }
                        } else {
                            error = ctx.getString(R.string.update_download_failed)
                        }
                    }
                },
                enabled = !downloading
            ) {
                if (downloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.update_install))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { dismissed = true }, enabled = !downloading) {
                Text(stringResource(R.string.update_later))
            }
        }
    )
}
