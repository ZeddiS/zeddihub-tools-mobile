package com.zeddihub.mobile.ui.tools

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.zeddihub.mobile.R
import com.zeddihub.mobile.ui.common.PullToRefreshBox
import com.zeddihub.mobile.ui.navigation.Destinations
import java.text.DateFormat
import java.util.Date

@Composable
fun MyNetworkScreen(
    padding: PaddingValues,
    navController: NavController?,
    vm: MyNetworkViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val colors = MaterialTheme.colorScheme

    PullToRefreshBox(
        isRefreshing = state.loading,
        onRefresh = vm::refresh,
        modifier = Modifier.padding(padding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            HeaderCard(state = state, onRefresh = vm::refresh)
            Spacer(Modifier.height(14.dp))

            state.wifi?.let { wifi ->
                WifiCard(wifi)
                Spacer(Modifier.height(14.dp))
            }

            state.cellular?.let { cell ->
                CellularCard(cell)
                Spacer(Modifier.height(14.dp))
            }

            state.ip?.let { ip ->
                IpCard(ip)
                Spacer(Modifier.height(14.dp))
            }

            if (state.isConnected) {
                SpeedTestShortcut(onClick = {
                    navController?.navigate(Destinations.SpeedTest.route)
                })
                Spacer(Modifier.height(14.dp))
            }

            if (state.locationPermissionMissing) {
                PermissionWarning()
                Spacer(Modifier.height(14.dp))
            }

            state.refreshedAt.takeIf { it > 0 }?.let {
                Text(
                    stringResource(
                        R.string.mynet_updated_at,
                        DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(it))
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ──────────────────────── Cards ────────────────────────

@Composable
private fun HeaderCard(
    state: MyNetworkViewModel.UiState,
    onRefresh: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val (icon, label) = when (state.transport) {
        MyNetworkViewModel.Transport.WIFI -> Icons.Default.Wifi to stringResource(R.string.mynet_transport_wifi)
        MyNetworkViewModel.Transport.CELLULAR -> Icons.Default.SignalCellularAlt to stringResource(R.string.mynet_transport_cellular)
        MyNetworkViewModel.Transport.ETHERNET -> Icons.Default.NetworkCheck to stringResource(R.string.mynet_transport_ethernet)
        MyNetworkViewModel.Transport.VPN -> Icons.Default.NetworkCheck to "VPN"
        MyNetworkViewModel.Transport.NONE -> Icons.Default.NetworkCheck to stringResource(R.string.mynet_transport_none)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = colors.primary, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.mynet_title),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        label,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                }
                OutlinedButton(onClick = onRefresh, shape = RoundedCornerShape(12.dp)) {
                    if (state.loading) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.mynet_refresh))
                }
            }

            if (state.downstreamKbps != null || state.upstreamKbps != null) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.downstreamKbps?.let {
                        QuickStat(
                            label = stringResource(R.string.mynet_downstream),
                            value = formatKbps(it),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    state.upstreamKbps?.let {
                        QuickStat(
                            label = stringResource(R.string.mynet_upstream),
                            value = formatKbps(it),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WifiCard(wifi: MyNetworkViewModel.WifiInfo) {
    SectionCard(title = stringResource(R.string.mynet_wifi_title)) {
        wifi.ssid?.let { KeyValueRow(stringResource(R.string.mynet_wifi_ssid), it) }
        wifi.bssid?.let { KeyValueRow(stringResource(R.string.mynet_wifi_bssid), it) }
        wifi.rssi?.let {
            val bars = wifi.signalBars?.let { b -> " ($b/4)" } ?: ""
            KeyValueRow(stringResource(R.string.mynet_wifi_signal), "$it dBm$bars")
        }
        wifi.linkSpeedMbps?.let { KeyValueRow(stringResource(R.string.mynet_wifi_link_speed), "$it Mbps") }
        wifi.channel?.let { ch ->
            val band = wifi.band?.let { "$it · " } ?: ""
            KeyValueRow(stringResource(R.string.mynet_wifi_channel), "${band}ch $ch")
        } ?: wifi.frequencyMhz?.let { KeyValueRow(stringResource(R.string.mynet_wifi_frequency), "$it MHz") }
        if (wifi.hiddenSsid) {
            KeyValueRow(stringResource(R.string.mynet_wifi_hidden), stringResource(R.string.common_yes))
        }
    }
}

@Composable
private fun CellularCard(cell: MyNetworkViewModel.CellularInfo) {
    SectionCard(title = stringResource(R.string.mynet_cell_title)) {
        cell.carrier?.let { KeyValueRow(stringResource(R.string.mynet_cell_carrier), it) }
        cell.networkType?.let { KeyValueRow(stringResource(R.string.mynet_cell_network), it) }
        if (!cell.mcc.isNullOrBlank() && !cell.mnc.isNullOrBlank()) {
            KeyValueRow(stringResource(R.string.mynet_cell_mcc_mnc), "${cell.mcc} / ${cell.mnc}")
        }
        if (cell.roaming) {
            KeyValueRow(stringResource(R.string.mynet_cell_roaming), stringResource(R.string.common_yes))
        }
    }
}

@Composable
private fun IpCard(ip: MyNetworkViewModel.IpInfo) {
    SectionCard(title = stringResource(R.string.mynet_ip_title)) {
        ip.ipv4?.let { KeyValueRow(stringResource(R.string.mynet_ip_v4), it) }
        ip.ipv6?.let { KeyValueRow(stringResource(R.string.mynet_ip_v6), it) }
        ip.gateway?.let { KeyValueRow(stringResource(R.string.mynet_ip_gateway), it) }
        if (ip.dns.isNotEmpty()) {
            KeyValueRow(stringResource(R.string.mynet_ip_dns), ip.dns.joinToString("\n"))
        }
        ip.domains?.takeIf { it.isNotBlank() }?.let {
            KeyValueRow(stringResource(R.string.mynet_ip_domains), it)
        }
        KeyValueRow(
            stringResource(R.string.mynet_ip_metered),
            if (ip.isMetered) stringResource(R.string.common_yes) else stringResource(R.string.common_no)
        )
    }
}

@Composable
private fun SpeedTestShortcut(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
    ) {
        Icon(Icons.Default.NetworkCheck, null)
        Spacer(Modifier.width(10.dp))
        Text(
            stringResource(R.string.mynet_run_speedtest),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun PermissionWarning() {
    val colors = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.errorContainer)
    ) {
        Text(
            stringResource(R.string.mynet_needs_location),
            modifier = Modifier.padding(14.dp),
            color = colors.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// ──────────────────────── Primitives ────────────────────────

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.primary
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun KeyValueRow(label: String, value: String) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = colors.onSurface
        )
    }
}

@Composable
private fun QuickStat(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant.copy(alpha = 0.55f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
        }
    }
}

private fun formatKbps(kbps: Int): String = when {
    kbps >= 1_000_000 -> "%.1f Gbps".format(kbps / 1_000_000.0)
    kbps >= 1_000 -> "%.1f Mbps".format(kbps / 1_000.0)
    else -> "$kbps kbps"
}
