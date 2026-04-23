package com.zeddihub.mobile.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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

@Composable
fun SpeakerCleanerScreen(
    padding: PaddingValues,
    viewModel: SpeakerCleanerViewModel = hiltViewModel()
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
                text = stringResource(R.string.speaker_cleaner_intro),
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onTertiaryContainer
            )
        }

        Spacer(Modifier.height(16.dp))

        // Preset picker
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            PresetChip(
                label = stringResource(R.string.speaker_cleaner_preset_water),
                selected = state.preset == SpeakerCleanerViewModel.Preset.WATER,
                onClick = { viewModel.setPreset(SpeakerCleanerViewModel.Preset.WATER) }
            )
            PresetChip(
                label = stringResource(R.string.speaker_cleaner_preset_dust),
                selected = state.preset == SpeakerCleanerViewModel.Preset.DUST,
                onClick = { viewModel.setPreset(SpeakerCleanerViewModel.Preset.DUST) }
            )
            PresetChip(
                label = stringResource(R.string.speaker_cleaner_preset_custom),
                selected = state.preset == SpeakerCleanerViewModel.Preset.CUSTOM,
                onClick = { viewModel.setPreset(SpeakerCleanerViewModel.Preset.CUSTOM) }
            )
        }

        Spacer(Modifier.height(16.dp))

        if (state.preset == SpeakerCleanerViewModel.Preset.CUSTOM) {
            OutlinedTextField(
                value = state.customHz.toString(),
                onValueChange = {
                    val v = it.filter { c -> c.isDigit() }.toIntOrNull()
                    if (v != null) viewModel.setCustomHz(v)
                },
                label = { Text("Hz") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
        }

        // Duration
        Text(
            "${stringResource(R.string.speaker_cleaner_duration)}: ${state.durationSec} s",
            style = MaterialTheme.typography.labelLarge,
            color = colors.onBackground
        )
        Slider(
            value = state.durationSec.toFloat(),
            onValueChange = { viewModel.setDuration(it.toInt()) },
            valueRange = 5f..120f,
            steps = 22 // ~5s increments
        )

        Spacer(Modifier.height(16.dp))

        // Live state
        if (state.running) {
            Surface(
                color = colors.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.speaker_cleaner_running, state.currentHz),
                        color = colors.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { state.progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Button(
            onClick = { viewModel.toggle() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.running) colors.error else colors.primary
            )
        ) {
            Icon(
                if (state.running) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null
            )
            Spacer(Modifier.height(0.dp))
            Text(
                text = if (state.running) stringResource(R.string.speaker_cleaner_stop)
                else stringResource(R.string.speaker_cleaner_start),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PresetChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 2) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}
