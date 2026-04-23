package com.zeddihub.mobile.ui.main

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zeddihub.mobile.BuildConfig
import com.zeddihub.mobile.R
import com.zeddihub.mobile.data.local.CredentialStore
import com.zeddihub.mobile.ui.navigation.Destinations

@Composable
fun AccountRootScreen(
    padding: PaddingValues,
    session: CredentialStore.Session?,
    isAdmin: Boolean,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val displayName = session?.displayName ?: session?.username ?: stringResource(R.string.account_guest)
    val role = session?.role

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(padding)
            .verticalScroll(rememberScrollState())
    ) {
        // Profile header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(colors.primary.copy(alpha = 0.20f), Color.Transparent)
                    )
                )
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            Brush.linearGradient(listOf(colors.primary, colors.tertiary)),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayName.take(1).uppercase(),
                        color = colors.onPrimary,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface
                    )
                    if (!role.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        RoleBadge(role)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        SectionHeader(stringResource(R.string.account_section_me))
        AccountItem(
            icon = Icons.Default.Person,
            label = stringResource(R.string.nav_profile),
            onClick = { onNavigate(Destinations.Profile.route) }
        )
        AccountItem(
            icon = Icons.Default.Storage,
            label = stringResource(R.string.nav_servers),
            onClick = { onNavigate(Destinations.Servers.route) }
        )

        SectionHeader(stringResource(R.string.account_section_community))
        AccountItem(
            icon = Icons.Default.Forum,
            label = stringResource(R.string.nav_community),
            onClick = { onNavigate(Destinations.Community.route) }
        )
        AccountItem(
            icon = Icons.Default.Notifications,
            label = stringResource(R.string.nav_notifications),
            onClick = { onNavigate(Destinations.Notifications.route) }
        )

        SectionHeader(stringResource(R.string.account_section_app))
        AccountItem(
            icon = Icons.Default.Settings,
            label = stringResource(R.string.nav_settings),
            onClick = { onNavigate(Destinations.Settings.route) }
        )
        if (isAdmin) {
            AccountItem(
                icon = Icons.Default.AdminPanelSettings,
                label = stringResource(R.string.nav_admin),
                onClick = { onNavigate(Destinations.Admin.route) }
            )
        }

        Spacer(Modifier.height(16.dp))

        // Logout
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable(onClick = onLogout),
            color = colors.surface,
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = colors.error
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.nav_logout),
                    color = colors.error,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "ZeddiHub Mobile · v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
private fun AccountItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        color = colors.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = colors.primary)
            Spacer(Modifier.width(16.dp))
            Text(
                text = label,
                color = colors.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RoleBadge(role: String) {
    val colors = MaterialTheme.colorScheme
    val isAdmin = role.equals("admin", ignoreCase = true)
    val bg = if (isAdmin) colors.primary.copy(alpha = 0.22f) else colors.surfaceVariant
    val fg = if (isAdmin) colors.primary else colors.onSurfaceVariant
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = role.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = fg
        )
    }
}
