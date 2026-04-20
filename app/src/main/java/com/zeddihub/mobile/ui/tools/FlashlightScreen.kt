package com.zeddihub.mobile.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zeddihub.mobile.R

@Composable
fun FlashlightScreen(padding: PaddingValues) {
    val vm: FlashlightViewModel = hiltViewModel()
    val state by vm.state.collectAsState()
    val colors = MaterialTheme.colorScheme
    var colorScreen by remember { mutableStateOf<Color?>(null) }

    if (colorScreen != null) {
        ColorScreen(color = colorScreen!!, onDismiss = { colorScreen = null })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(colors.primary.copy(alpha = 0.18f), Color.Transparent)
                    ),
                    RoundedCornerShape(18.dp)
                )
                .padding(18.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FlashlightOn, null, tint = colors.primary, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.size(10.dp))
                    Text(
                        stringResource(R.string.flashlight_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                }
                if (!state.hasTorch) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.flashlight_unsupported),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.error
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        val active = state.mode != FlashlightViewModel.Mode.OFF
        Button(
            onClick = {
                vm.setMode(
                    if (active) FlashlightViewModel.Mode.OFF
                    else FlashlightViewModel.Mode.STEADY
                )
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(72.dp),
            enabled = state.hasTorch
        ) {
            Icon(
                if (active) Icons.Default.FlashlightOff else Icons.Default.FlashlightOn,
                null, modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.size(10.dp))
            Text(
                if (active) stringResource(R.string.flashlight_off)
                else stringResource(R.string.flashlight_on),
                fontSize = 18.sp, fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(14.dp))

        Text(
            stringResource(R.string.flashlight_modes),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface
        )
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ModeChip(
                label = stringResource(R.string.flashlight_mode_strobe),
                icon = Icons.Default.Bolt,
                selected = state.mode == FlashlightViewModel.Mode.STROBE,
                onClick = { vm.setMode(FlashlightViewModel.Mode.STROBE) },
                modifier = Modifier.weight(1f),
                enabled = state.hasTorch
            )
            ModeChip(
                label = stringResource(R.string.flashlight_mode_sos),
                icon = Icons.Default.Sos,
                selected = state.mode == FlashlightViewModel.Mode.SOS,
                onClick = { vm.setMode(FlashlightViewModel.Mode.SOS) },
                modifier = Modifier.weight(1f),
                enabled = state.hasTorch
            )
        }

        if (state.mode == FlashlightViewModel.Mode.STROBE) {
            Spacer(Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        stringResource(R.string.flashlight_strobe_hz, state.strobeHz),
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface
                    )
                    Slider(
                        value = state.strobeHz,
                        onValueChange = { vm.setStrobeHz(it) },
                        valueRange = 0.5f..15f
                    )
                }
            }
        }

        if (state.supportsIntensity && state.mode == FlashlightViewModel.Mode.STEADY) {
            Spacer(Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        stringResource(R.string.flashlight_intensity, state.intensity, state.maxIntensity),
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface
                    )
                    Slider(
                        value = state.intensity.toFloat(),
                        onValueChange = { vm.setIntensity(it.toInt()) },
                        valueRange = 1f..state.maxIntensity.toFloat(),
                        steps = (state.maxIntensity - 2).coerceAtLeast(0)
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Text(
            stringResource(R.string.flashlight_color_screen),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface
        )
        Spacer(Modifier.height(8.dp))

        val palette = listOf(
            Color.White,
            Color.Red,
            Color(0xFFFFA500),
            Color.Yellow,
            Color.Green,
            Color.Cyan,
            Color.Blue,
            Color(0xFFFF00FF)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            palette.take(4).forEach { c ->
                ColorDot(c) { colorScreen = c }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            palette.drop(4).forEach { c ->
                ColorDot(c) { colorScreen = c }
            }
        }

        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = colors.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ModeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Icon(icon, null, tint = if (selected) colors.primary else colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(6.dp))
        Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun RowScopeColorDot(color: Color, onClick: () -> Unit) = ColorDot(color, onClick)

@Composable
private fun ColorDot(color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(color, CircleShape)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun ColorScreen(color: Color, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
            .clickable(onClick = onDismiss)
    ) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Close, null, tint = Color.Black.copy(alpha = 0.6f))
        }
    }
}
