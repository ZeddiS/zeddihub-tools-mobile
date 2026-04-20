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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zeddihub.mobile.R

@Composable
fun SpeedTestScreen(
    padding: PaddingValues,
    vm: SpeedTestViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // ── Header card with big Mbps readout ────────────────
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                colors.primary.copy(alpha = 0.20f),
                                colors.tertiary.copy(alpha = 0.10f)
                            )
                        ),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            stringResource(R.string.speedtest_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.speedtest_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )

                    Spacer(Modifier.height(24.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = state.downloadMbps?.let { "%.1f".format(it) } ?: "—",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.onBackground
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "Mbps",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = state.downloadMBs?.let { "%.2f MB/s".format(it) } ?: stringResource(R.string.speedtest_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )

                    Spacer(Modifier.height(18.dp))
                    if (state.running) {
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = colors.primary,
                            trackColor = colors.primary.copy(alpha = 0.18f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Metrics row ──────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                icon = Icons.Default.Router,
                label = stringResource(R.string.speedtest_ping),
                value = state.pingMs?.let { "$it ms" } ?: "—",
                modifier = Modifier.weight(1f),
                tint = colors.tertiary
            )
            MetricCard(
                icon = Icons.Default.CloudDownload,
                label = stringResource(R.string.speedtest_downloaded),
                value = "%.1f MB".format(state.downloadedBytes / 1e6),
                modifier = Modifier.weight(1f),
                tint = colors.primary
            )
            MetricCard(
                icon = Icons.Default.Bolt,
                label = stringResource(R.string.speedtest_elapsed),
                value = "%.1fs".format(state.elapsedSeconds),
                modifier = Modifier.weight(1f),
                tint = colors.secondary
            )
        }

        Spacer(Modifier.height(22.dp))

        Button(
            onClick = vm::start,
            enabled = !state.running,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
        ) {
            if (state.running) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = colors.onPrimary,
                    strokeWidth = 2.5.dp
                )
                Spacer(Modifier.size(12.dp))
                Text(stringResource(R.string.speedtest_running), fontWeight = FontWeight.SemiBold)
            } else {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text(
                    stringResource(R.string.speedtest_start),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        state.error?.let { err ->
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.speedtest_error, err),
                color = colors.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.speedtest_powered_by),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun MetricCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
        }
    }
}
