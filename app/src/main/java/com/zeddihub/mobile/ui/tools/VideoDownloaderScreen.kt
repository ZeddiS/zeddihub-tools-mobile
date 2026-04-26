package com.zeddihub.mobile.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zeddihub.mobile.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoDownloaderScreen(
    padding: PaddingValues,
    viewModel: VideoDownloaderViewModel = hiltViewModel(),
    /**
     * Optional URL pre-fill from outside (Share intent → MainActivity →
     * navigation arg). When present, viewModel.setUrl is called once on
     * compose entry and the URL field arrives populated.
     */
    initialUrl: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    val state by viewModel.state.collectAsState()
    val clipboard = LocalClipboardManager.current

    // Auto-paste on screen entry. We check the clipboard once: if it
    // looks like a video URL the user wasn't already going to type
    // (i.e. the field is empty), drop it in. The Share-intent path
    // takes priority — when [initialUrl] is set we trust that over the
    // clipboard so a real share doesn't get masked by a stale clip.
    val ctx = LocalContext.current
    LaunchedEffect(initialUrl) {
        if (!initialUrl.isNullOrBlank() && state.url.isBlank()) {
            viewModel.setUrl(initialUrl)
            // Came from a Share intent — auto-kick the fetch so the user
            // gets the "downloading…" toast straight away without an
            // extra tap. The existing UI shows progress + result inline.
            viewModel.fetchAndEnqueue()
            android.widget.Toast.makeText(
                ctx, ctx.getString(R.string.video_dl_starting),
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return@LaunchedEffect
        }
        if (state.url.isBlank()) {
            val clip = clipboard.getText()?.text?.trim().orEmpty()
            if (clip.startsWith("http://") || clip.startsWith("https://")) {
                viewModel.setUrl(clip)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Surface(
            color = colors.tertiaryContainer.copy(alpha = 0.45f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                stringResource(R.string.video_dl_disclaimer),
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onTertiaryContainer
            )
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = state.url,
            onValueChange = { viewModel.setUrl(it) },
            label = { Text(stringResource(R.string.video_dl_url_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                // Paste-from-clipboard. Tapping replaces (not appends)
                // the field — pasting on top of an existing URL is the
                // common case (correcting a typed URL with the share-
                // sheet copy).
                IconButton(onClick = {
                    clipboard.getText()?.text?.trim()?.takeIf { it.isNotEmpty() }?.let {
                        viewModel.setUrl(it)
                    }
                }) {
                    Icon(Icons.Default.ContentPaste, contentDescription =
                        stringResource(R.string.video_dl_paste))
                }
            }
        )

        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = state.audioOnly,
                onCheckedChange = { viewModel.setAudioOnly(it) }
            )
            Text(
                stringResource(R.string.video_dl_audio_only),
                modifier = Modifier.padding(start = 10.dp),
                color = colors.onBackground
            )
        }

        if (!state.audioOnly) {
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.video_dl_quality),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onBackground
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                VideoDownloaderViewModel.QUALITIES.forEach { q ->
                    FilterChip(
                        selected = state.quality == q,
                        onClick = { viewModel.setQuality(q) },
                        label = { Text(if (q == "max") "max" else "${q}p") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primary,
                            selectedLabelColor = colors.onPrimary
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { viewModel.fetchAndEnqueue() },
            enabled = !state.fetching && state.url.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
        ) {
            Icon(Icons.Default.Download, contentDescription = null)
            Spacer(Modifier.height(0.dp))
            Text(
                text = stringResource(R.string.video_dl_download),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Status banner
        when (state.lastStatus) {
            VideoDownloaderViewModel.Status.Fetching -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.video_dl_status_fetching),
                    color = colors.onBackground
                )
            }
            VideoDownloaderViewModel.Status.Enqueued -> {
                Text(
                    stringResource(
                        R.string.video_dl_status_done,
                        state.lastResultFile ?: "?"
                    ),
                    color = colors.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            VideoDownloaderViewModel.Status.Error -> {
                Text(
                    stringResource(
                        R.string.video_dl_status_error,
                        state.lastError ?: "unknown"
                    ),
                    color = colors.error
                )
            }
            VideoDownloaderViewModel.Status.Ready -> {
                Text(
                    stringResource(R.string.video_dl_status_ready),
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}
