package com.zeddihub.mobile.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zeddihub.mobile.R
import kotlin.math.ln
import kotlin.math.exp

/**
 * Tone generator UI. Frequency slider is *logarithmic* (20 Hz – 20 kHz)
 * because that's how humans actually perceive pitch — a linear slider
 * spends 99% of its travel between 200 Hz and 20 kHz.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FrequencyGeneratorScreen(
    padding: PaddingValues,
    viewModel: FrequencyGeneratorViewModel = hiltViewModel()
) {
    val colors = MaterialTheme.colorScheme
    val state by viewModel.state.collectAsState()

    // Log scale helpers (20 Hz -> slider 0.0, 20 kHz -> slider 1.0).
    val minLn = ln(20.0)
    val maxLn = ln(20_000.0)
    val sliderValue = ((ln(state.frequencyHz.toDouble()) - minLn) / (maxLn - minLn)).toFloat()

    var freqText by remember(state.frequencyHz) { mutableStateOf(state.frequencyHz.toInt().toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Surface(
            color = colors.errorContainer.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.freq_warning),
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onErrorContainer
            )
        }

        Spacer(Modifier.height(20.dp))

        // Big frequency readout
        Text(
            text = formatHz(state.frequencyHz),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = colors.primary
        )
        Text(
            text = stringResource(R.string.freq_hz_label),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))

        // Log slider
        Slider(
            value = sliderValue.coerceIn(0f, 1f),
            onValueChange = { v ->
                val hz = exp(minLn + v * (maxLn - minLn)).toFloat()
                viewModel.setFrequency(hz)
            },
            colors = SliderDefaults.colors(
                thumbColor = colors.primary,
                activeTrackColor = colors.primary
            )
        )

        Spacer(Modifier.height(4.dp))

        // Numeric entry
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = freqText,
                onValueChange = {
                    freqText = it.filter { c -> c.isDigit() || c == '.' }
                    freqText.toFloatOrNull()?.let { v -> viewModel.setFrequency(v) }
                },
                label = { Text(stringResource(R.string.freq_hz_label)) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Waveform picker
        Text(
            stringResource(R.string.freq_waveform),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            WaveformChip(
                label = stringResource(R.string.freq_waveform_sine),
                selected = state.waveform == FrequencyGeneratorViewModel.Waveform.SINE,
                onClick = { viewModel.setWaveform(FrequencyGeneratorViewModel.Waveform.SINE) }
            )
            WaveformChip(
                label = stringResource(R.string.freq_waveform_square),
                selected = state.waveform == FrequencyGeneratorViewModel.Waveform.SQUARE,
                onClick = { viewModel.setWaveform(FrequencyGeneratorViewModel.Waveform.SQUARE) }
            )
            WaveformChip(
                label = stringResource(R.string.freq_waveform_triangle),
                selected = state.waveform == FrequencyGeneratorViewModel.Waveform.TRIANGLE,
                onClick = { viewModel.setWaveform(FrequencyGeneratorViewModel.Waveform.TRIANGLE) }
            )
            WaveformChip(
                label = stringResource(R.string.freq_waveform_sawtooth),
                selected = state.waveform == FrequencyGeneratorViewModel.Waveform.SAWTOOTH,
                onClick = { viewModel.setWaveform(FrequencyGeneratorViewModel.Waveform.SAWTOOTH) }
            )
        }

        Spacer(Modifier.height(16.dp))

        // Volume
        Text(
            stringResource(R.string.freq_volume) + "  ${(state.volume * 100).toInt()} %",
            style = MaterialTheme.typography.labelLarge,
            color = colors.onBackground
        )
        Slider(
            value = state.volume,
            onValueChange = { viewModel.setVolume(it) }
        )

        Spacer(Modifier.height(16.dp))

        // Presets
        Text(
            stringResource(R.string.freq_preset),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onBackground
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FrequencyGeneratorViewModel.PRESETS.forEach { (label, hz) ->
                AssistChip(
                    onClick = { viewModel.setFrequency(hz) },
                    label = { Text(label) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = colors.surfaceVariant
                    )
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { viewModel.toggle() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.playing) colors.error else colors.primary
            )
        ) {
            Icon(
                if (state.playing) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null
            )
            Spacer(Modifier.height(0.dp))
            Text(
                text = if (state.playing) stringResource(R.string.freq_stop)
                else stringResource(R.string.freq_start),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WaveformChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

private fun formatHz(hz: Float): String {
    return if (hz >= 1000f) {
        "%.2f kHz".format(hz / 1000f)
    } else {
        "%.0f Hz".format(hz)
    }
}
