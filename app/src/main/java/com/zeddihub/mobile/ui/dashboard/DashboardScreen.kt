package com.zeddihub.mobile.ui.dashboard

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zeddihub.mobile.R
import com.zeddihub.mobile.data.remote.dto.ServerDto
import com.zeddihub.mobile.ui.common.StatusDot
import com.zeddihub.mobile.ui.theme.GameCs2Yellow
import com.zeddihub.mobile.ui.theme.GameCsgoGreen
import com.zeddihub.mobile.ui.theme.GameRustOrange
import com.zeddihub.mobile.ui.theme.StateDanger
import com.zeddihub.mobile.ui.theme.StateSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onOpenServer: (String) -> Unit,
    onLogout: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.colorScheme

    Scaffold(
        containerColor = colors.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.dashboard_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.primary,
                            fontWeight = FontWeight.Bold
                        )
                        state.username?.let {
                            Text(
                                text = stringResource(R.string.common_welcome, it),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.dashboard_refresh),
                            tint = colors.onBackground
                        )
                    }
                    IconButton(onClick = { viewModel.logout(onLogout) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = stringResource(R.string.dashboard_logout),
                            tint = colors.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.onBackground
                )
            )
        }
    ) { padding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(padding)
        ) {
            SummaryBar(state.servers)

            if (state.error != null) {
                Text(
                    text = state.error ?: "",
                    color = StateDanger,
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (state.servers.isEmpty() && state.error == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.dashboard_empty),
                        color = colors.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(state.servers, key = { it.id }) { server ->
                        ServerCard(server = server, onClick = { onOpenServer(server.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryBar(servers: List<ServerDto>) {
    val online = servers.count { it.status.equals("online", true) }
    val offline = servers.count { !it.status.equals("online", true) }
    val totalPlayers = servers.sumOf { it.playersOnline }
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SummaryChip(
            label = stringResource(R.string.dashboard_servers),
            value = servers.size.toString(),
            accent = colors.primary,
            modifier = Modifier.weight(1f)
        )
        SummaryChip(
            label = stringResource(R.string.dashboard_online),
            value = online.toString(),
            accent = StateSuccess,
            modifier = Modifier.weight(1f)
        )
        SummaryChip(
            label = stringResource(R.string.dashboard_offline),
            value = offline.toString(),
            accent = StateDanger,
            modifier = Modifier.weight(1f)
        )
        SummaryChip(
            label = stringResource(R.string.dashboard_players),
            value = totalPlayers.toString(),
            accent = colors.primary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryChip(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.height(72.dp),
        color = colors.surface,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = value,
                color = accent,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun ServerCard(
    server: ServerDto,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val accent = when (server.status.lowercase()) {
        "online" -> StateSuccess
        "crashed" -> StateDanger
        "starting" -> colors.primary
        else -> Color(0xFF6B7280)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(120.dp)
                    .background(accent)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatusDot(status = server.status)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = server.name,
                        color = colors.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    GameBadge(server.game)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Stat(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = "${server.playersOnline}/${server.playersMax}"
                    )
                    Spacer(Modifier.width(14.dp))
                    Stat(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = server.fps?.let { "FPS ${"%.0f".format(it)}" }
                            ?: server.tickRate?.let { "Tick ${"%.0f".format(it)}" }
                            ?: "—"
                    )
                    Spacer(Modifier.width(14.dp))
                    Stat(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = "CPU ${"%.0f".format(server.cpuUsage)}%"
                    )
                    Spacer(Modifier.width(14.dp))
                    Stat(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = "RAM ${server.ramUsageMb}/${server.ramTotalMb} MB"
                    )
                }

                if (!server.map.isNullOrBlank()) {
                    Text(
                        text = "Map: ${server.map}",
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun GameBadge(game: String) {
    val colors = MaterialTheme.colorScheme
    val (label, color) = when (game.uppercase()) {
        "RUST" -> "RUST" to GameRustOrange
        "CS2" -> "CS2" to GameCs2Yellow
        "CSGO" -> "CS:GO" to GameCsgoGreen
        else -> game to colors.primary
    }
    Surface(
        color = color.copy(alpha = 0.14f),
        contentColor = color,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, colors.outline)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun Stat(
    icon: @Composable () -> Unit,
    label: String
) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
