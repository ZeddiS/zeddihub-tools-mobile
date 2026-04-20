package com.zeddihub.mobile.ui.servers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zeddihub.mobile.R
import com.zeddihub.mobile.data.remote.dto.ServerDto
import com.zeddihub.mobile.ui.common.StatusDot
import com.zeddihub.mobile.ui.dashboard.DashboardViewModel
import com.zeddihub.mobile.ui.theme.GameCs2Yellow
import com.zeddihub.mobile.ui.theme.GameCsgoGreen
import com.zeddihub.mobile.ui.theme.GameRustOrange
import com.zeddihub.mobile.ui.theme.StateDanger
import com.zeddihub.mobile.ui.theme.StateSuccess

@Composable
fun ServersScreen(
    padding: PaddingValues,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.colorScheme
    val ctx = LocalContext.current

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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.dashboard_empty), color = colors.onSurfaceVariant)
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
                    ServerCard(
                        server = server,
                        onCopyIp = {
                            val addr = server.address
                            if (addr.isNotEmpty()) {
                                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("server_ip", addr))
                                Toast.makeText(
                                    ctx,
                                    ctx.getString(R.string.server_ip_copied),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryBar(servers: List<ServerDto>) {
    val online = servers.count { it.status.equals("online", true) }
    val offline = servers.count { !it.status.equals("online", true) }
    val players = servers.sumOf { it.playersOnline }
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Chip(stringResource(R.string.dashboard_servers), servers.size.toString(), colors.primary, Modifier.weight(1f))
        Chip(stringResource(R.string.dashboard_online), online.toString(), StateSuccess, Modifier.weight(1f))
        Chip(stringResource(R.string.dashboard_offline), offline.toString(), StateDanger, Modifier.weight(1f))
        Chip(stringResource(R.string.dashboard_players), players.toString(), colors.primary, Modifier.weight(1f))
    }
}

@Composable
private fun Chip(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.height(68.dp),
        color = colors.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(value, color = accent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ServerCard(server: ServerDto, onCopyIp: () -> Unit) {
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
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(140.dp)
                    .background(accent)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
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

                if (server.address.isNotEmpty()) {
                    Surface(
                        color = colors.background,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onCopyIp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.server_address),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant
                                )
                                Text(
                                    text = server.address,
                                    color = colors.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            IconButton(onClick = onCopyIp) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = stringResource(R.string.server_copy_ip),
                                    tint = colors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Stat(Icons.Default.People, "${server.playersOnline}/${server.playersMax}")
                    Spacer(Modifier.width(14.dp))
                    Stat(
                        Icons.Default.Router,
                        server.pingMs?.let { "${it} ms" } ?: "—"
                    )
                    if (!server.map.isNullOrBlank()) {
                        Spacer(Modifier.width(14.dp))
                        Stat(Icons.Default.Map, server.map!!)
                    }
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
private fun Stat(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
    }
}
