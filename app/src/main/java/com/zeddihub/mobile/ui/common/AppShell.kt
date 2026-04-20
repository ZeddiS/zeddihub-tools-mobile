package com.zeddihub.mobile.ui.common

import android.os.Build
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    SidebarItem(Destinations.SpeedTest.route, R.string.nav_speedtest, Icons.Default.NetworkCheck, 1),
    SidebarItem(Destinations.IpLookup.route, R.string.nav_ip_lookup, Icons.Default.Public, 1),
    SidebarItem(Destinations.DeviceInfo.route, R.string.nav_device_info, Icons.Default.Devices, 1),
    SidebarItem(Destinations.CacheCleaner.route, R.string.nav_cache_cleaner, Icons.Default.CleaningServices, 1),
    SidebarItem(Destinations.AppFinder.route, R.string.nav_app_finder, Icons.Default.Apps, 1),
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
    gesturesEnabled: Boolean = true,
    actions: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    TrackScreen(route = currentRoute)

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = gesturesEnabled,
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

    // Glassmorphism gradient — deep vertical gradient with surface tint blend.
    val glassGradient = Brush.verticalGradient(
        colors = listOf(
            colors.surface.copy(alpha = 0.92f),
            colors.surfaceVariant.copy(alpha = 0.86f),
            colors.surface.copy(alpha = 0.94f)
        )
    )

    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        Modifier.blur(0.dp) else Modifier // blur is applied to backdrop, drawer stays crisp

    ModalDrawerSheet(
        drawerContainerColor = Color.Transparent,
        drawerContentColor = colors.onSurface,
        drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
        modifier = Modifier.width(304.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(304.dp)
                .background(glassGradient)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            colors.primary.copy(alpha = 0.35f),
                            colors.primary.copy(alpha = 0.0f)
                        )
                    ),
                    shape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp)
            ) {
                // ── Account header with gradient accent ────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    colors.primary.copy(alpha = 0.22f),
                                    colors.tertiary.copy(alpha = 0.14f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(colors.primary, colors.tertiary)
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (displayName ?: "Z").take(1).uppercase(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.onPrimary
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = displayName ?: "ZeddiHub",
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

                Spacer(Modifier.height(8.dp))

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
                        SectionHeader(stringResource(labelRes))
                    }
                    grouped[sectionKey]?.forEach { item ->
                        NavItem(
                            item = item,
                            selected = item.route == currentRoute,
                            onClick = { onItemClick(item.route) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Gradient divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    colors.onSurface.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .background(
                            colors.error.copy(alpha = 0.0f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onLogout() }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
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

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "ZeddiHub Mobile · v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
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

@Composable
private fun SectionHeader(label: String) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = colors.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.padding(start = 24.dp, end = 20.dp, top = 18.dp, bottom = 6.dp)
    )
}

@Composable
private fun NavItem(
    item: SidebarItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val accentAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        label = "nav_accent"
    )
    val indicatorWidth by animateDpAsState(
        targetValue = if (selected) 4.dp else 0.dp,
        label = "nav_indicator"
    )

    val fg = if (selected) colors.primary else colors.onSurface.copy(alpha = 0.86f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        colors.primary.copy(alpha = 0.20f * accentAlpha),
                        colors.primary.copy(alpha = 0.06f * accentAlpha),
                        Color.Transparent
                    )
                ),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // left accent pill
        Box(
            modifier = Modifier
                .padding(start = 0.dp)
                .width(indicatorWidth)
                .height(22.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(colors.primary, colors.tertiary)
                    ),
                    RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
                )
        )
        Spacer(Modifier.width(if (selected) 10.dp else 14.dp))
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
