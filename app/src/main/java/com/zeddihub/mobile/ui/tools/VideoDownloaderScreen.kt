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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zeddihub.mobile.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoDownloaderScreen(
    padding: PaddingValues,
    viewModel: VideoDownloaderViewModel = hiltViewModel()
) {
    val colors = MaterialTheme.colorScheme
    val state by viewModel.state.collectAsState()

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
            singleLine = true
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
