package com.zeddihub.mobile.ui.notifications

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.zeddihub.mobile.data.alerts.Alert
import java.text.DateFormat
import java.util.Date

@Composable
fun NotificationsScreen(
    padding: PaddingValues,
    vm: NotificationsViewModel = hiltViewModel()
) {
    val colors = MaterialTheme.colorScheme
    val alerts by vm.alerts.collectAsState()
    val unread by vm.unreadCount.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 12.dp)
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
                    Icon(
                        Icons.Default.NotificationsActive, null,
                        tint = colors.primary, modifier = Modifier.size(26.dp)
                    )
                    Spacer(Modifier.size(10.dp))
                    Text(
                        stringResource(R.string.notifications_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                    Spacer(Modifier.weight(1f))
                    if (unread > 0) {
                        IconButton(onClick = vm::markAllRead) {
                            Icon(Icons.Default.DoneAll, null, tint = colors.primary)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.notifications_summary, alerts.size, unread),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (alerts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.NotificationsNone, null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.notifications_empty),
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = vm::seedDemoAlert) {
                        Text(stringResource(R.string.notifications_seed_demo))
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(alerts, key = { it.id }) { alert ->
                    AlertCard(alert = alert, onClick = { vm.markRead(alert.id) })
                }
            }
        }
    }
}

@Composable
private fun AlertCard(alert: Alert, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tint = when (alert.severity) {
        "critical" -> colors.error
        "warn" -> colors.tertiary
        else -> colors.primary
    }
    val icon = when (alert.severity) {
        "critical" -> Icons.Default.Warning
        "warn" -> Icons.Default.Warning
        else -> Icons.Default.Info
    }
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (alert.read) colors.surface else colors.surfaceVariant.copy(alpha = 0.55f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    alert.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (alert.read) FontWeight.Normal else FontWeight.SemiBold,
                    color = colors.onSurface
                )
                if (alert.body.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        alert.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "${alert.source} · ${formatTime(alert.timestamp)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }
            if (!alert.read) {
                TextButton(onClick = onClick) {
                    Text(stringResource(R.string.notifications_mark_read))
                }
            }
        }
    }
}

private fun formatTime(ts: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(ts))
