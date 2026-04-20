package com.zeddihub.mobile.ui.common

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zeddihub.mobile.BuildConfig
import com.zeddihub.mobile.R
import com.zeddihub.mobile.ui.navigation.Destinations
import kotlinx.coroutines.launch

data class SidebarItem(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    val section: Int = 0
)

private val baseSidebarItems = listOf(
    SidebarItem(Destinations.Home.route, R.string.nav_home, Icons.Default.Home, 0),
    SidebarItem(Destinations.Servers.route, R.string.nav_servers, Icons.Default.Storage, 0),
    SidebarItem(Destinations.Ping.route, R.string.nav_ping, Icons.Default.Speed, 1),
    SidebarItem(Destinations.IpLookup.route, R.string.nav_ip_lookup, Icons.Default.Public, 1),
    SidebarItem(Destinations.DeviceInfo.route, R.string.nav_device_info, Icons.Default.Devices, 1),
    SidebarItem(Destinations.Profile.route, R.string.nav_profile, Icons.Default.Person, 2),
    SidebarItem(Destinations.Notifications.route, R.string.nav_notifications, Icons.Default.Notifications, 2),
    SidebarItem(Destinations.Community.route, R.string.nav_community, Icons.Default.Forum, 2)
)

private val adminItem = SidebarItem(Destinations.Admin.route, R.string.nav_admin, Icons.Default.AdminPanelSettings, 3)
private val settingsItem = SidebarItem(Destinations.Settings.route, R.string.nav_settings, Icons.Default.Settings, 3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    title: String,
    currentRoute: String,
    isAdmin: Boolean,
    displayName: String?,
    role: String?,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    actions: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SidebarContent(
                currentRoute = currentRoute,
                isAdmin = isAdmin,
                displayName = displayName,
                role = role,
                onItemClick = { route ->
                    scope.launch { drawerState.close() }
                    if (route != currentRoute) onNavigate(route)
                },
                onLogout = {
                    scope.launch { drawerState.close() }
                    onLogout()
                }
            )
        }
    ) {
        Scaffold(
            containerColor = colors.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            title,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = stringResource(R.string.nav_open_drawer),
                                tint = colors.onBackground
                            )
                        }
                    },
                    actions = { actions() },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = colors.background,
                        titleContentColor = colors.onBackground
                    )
                )
            }
        ) { padding -> content(padding) }
    }
}

@Composable
private fun SidebarContent(
    currentRoute: String,
    isAdmin: Boolean,
    displayName: String?,
    role: String?,
    onItemClick: (String) -> Unit,
    onLogout: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val ctx = LocalContext.current

    ModalDrawerSheet(
        drawerContainerColor = colors.surface,
        drawerContentColor = colors.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        colors.primary.copy(alpha = 0.15f)
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.primary.copy(alpha = 0.2f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = (displayName ?: "Z").take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = displayName ?: "ZeddiHub",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
                if (!role.isNullOrBlank()) {
                    Text(
                        text = role.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            val all = buildList {
                addAll(baseSidebarItems)
                if (isAdmin) add(adminItem)
                add(settingsItem)
            }
            val grouped = all.groupBy { it.section }
            val sectionLabels = mapOf(
                0 to null,
                1 to R.string.nav_section_tools,
                2 to R.string.nav_section_community,
                3 to R.string.nav_section_system
            )

            grouped.keys.sorted().forEach { sectionKey ->
                sectionLabels[sectionKey]?.let { labelRes ->
                    Text(
                        text = stringResource(labelRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(start = 24.dp, top = 14.dp, bottom = 4.dp)
                    )
                }
                grouped[sectionKey]?.forEach { item ->
                    NavItem(
                        item = item,
                        selected = item.route == currentRoute,
                        onClick = { onItemClick(item.route) }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLogout() }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
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
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "ZeddiHub v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun NavItem(
    item: SidebarItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val bg = if (selected) colors.primary.copy(alpha = 0.18f) else Color.Transparent
    val fg = if (selected) colors.primary else colors.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .background(bg, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(item.icon, contentDescription = null, tint = fg)
        Spacer(Modifier.width(16.dp))
        Text(
            text = stringResource(item.labelRes),
            color = fg,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
