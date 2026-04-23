package com.zeddihub.mobile.ui.main

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.zeddihub.mobile.R
import com.zeddihub.mobile.data.local.CredentialStore
import com.zeddihub.mobile.ui.common.TrackScreen
import com.zeddihub.mobile.ui.login.LoginViewModel
import com.zeddihub.mobile.ui.navigation.Destinations
import kotlinx.coroutines.launch

private enum class MainTab(
    val titleRes: Int,
    val icon: ImageVector,
    val route: String
) {
    Home(R.string.tab_home, Icons.Default.Home, "main/home"),
    Tools(R.string.tab_tools, Icons.Default.Build, "main/tools"),
    Helpers(R.string.tab_helpers, Icons.Default.Widgets, "main/helpers"),
    Account(R.string.tab_account, Icons.Default.AccountCircle, "main/account")
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    navController: NavHostController
) {
    val colors = MaterialTheme.colorScheme
    val tabs = remember { MainTab.values().toList() }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    val gateVm: LoginViewModel = hiltViewModel()
    val session by gateVm.sessionFlow.collectAsState()
    val displayName = session?.displayName ?: session?.username
    val role = session?.role
    val isAdmin = role?.equals("admin", ignoreCase = true) == true

    // Track tab changes as screen views so telemetry keeps working under
    // the new tab layout.
    val activeTab = tabs[pagerState.currentPage]
    TrackScreen(route = activeTab.route)

    Scaffold(
        containerColor = colors.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(activeTab.titleRes),
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                },
                navigationIcon = {
                    // Small profile avatar on the left — tap jumps to Account tab.
                    IconButton(onClick = {
                        scope.launch { pagerState.animateScrollToPage(MainTab.Account.ordinal) }
                    }) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
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
                                color = colors.onPrimary,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.onBackground
                )
            )
        },
        bottomBar = {
            BottomNav(
                tabs = tabs,
                activeIndex = pagerState.currentPage,
                onTabSelect = { idx ->
                    scope.launch { pagerState.animateScrollToPage(idx) }
                }
            )
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { page ->
            when (tabs[page]) {
                MainTab.Home -> DashboardScreen(
                    padding = PaddingValues(),
                    isAdmin = isAdmin,
                    onNavigate = { route -> navTo(navController, route) }
                )
                MainTab.Tools -> ToolsRootScreen(
                    padding = PaddingValues(),
                    onNavigate = { route -> navTo(navController, route) }
                )
                MainTab.Helpers -> HelpersRootScreen(
                    padding = PaddingValues(),
                    onNavigate = { route -> navTo(navController, route) }
                )
                MainTab.Account -> AccountRootScreen(
                    padding = PaddingValues(),
                    session = session,
                    isAdmin = isAdmin,
                    onNavigate = { route -> navTo(navController, route) },
                    onLogout = {
                        gateVm.logout()
                        navController.navigate(Destinations.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun BottomNav(
    tabs: List<MainTab>,
    activeIndex: Int,
    onTabSelect: (Int) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    NavigationBar(
        containerColor = colors.surface,
        tonalElevation = 6.dp
    ) {
        tabs.forEachIndexed { idx, tab ->
            val selected = idx == activeIndex
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelect(idx) },
                icon = {
                    Icon(tab.icon, contentDescription = null)
                },
                label = {
                    Text(
                        text = stringResource(tab.titleRes),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.onPrimary,
                    selectedTextColor = colors.primary,
                    indicatorColor = colors.primary,
                    unselectedIconColor = colors.onSurfaceVariant,
                    unselectedTextColor = colors.onSurfaceVariant
                )
            )
        }
    }
}

private fun navTo(navController: NavHostController, route: String) {
    navController.navigate(route) {
        launchSingleTop = true
    }
}
