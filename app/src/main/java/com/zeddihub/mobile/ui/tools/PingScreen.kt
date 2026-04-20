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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.zeddihub.mobile.ui.theme.StateDanger
import com.zeddihub.mobile.ui.theme.StateSuccess

@Composable
fun PingScreen(
    padding: PaddingValues,
    viewModel: PingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(padding)
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.ping_title),
            style = MaterialTheme.typography.titleLarge,
            color = colors.onBackground,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.ping_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { viewModel.run() },
            enabled = !state.isRunning,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
        ) {
            if (state.isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.width(20.dp).height(20.dp),
                    color = colors.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.ping_running), color = colors.onPrimary)
            } else {
                Text(stringResource(R.string.ping_run), color = colors.onPrimary, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(16.dp))

        state.results.forEach { result ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                color = colors.surface,
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(result.name, color = colors.onSurface, fontWeight = FontWeight.SemiBold)
                        Text(result.address, color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    }
                    val latency = result.latencyMs
                    if (latency != null) {
                        val color = when {
                            latency < 30 -> StateSuccess
                            latency < 80 -> colors.primary
                            latency < 150 -> androidx.compose.ui.graphics.Color(0xFFF59E0B)
                            else -> StateDanger
                        }
                        Text(
                            text = stringResource(R.string.ping_ms, latency),
                            color = color,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    } else if (result.failed) {
                        Text(
                            text = stringResource(R.string.ping_failed),
                            color = StateDanger,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.width(18.dp).height(18.dp),
                            color = colors.primary,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }
    }
}
