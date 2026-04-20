package com.zeddihub.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.zeddihub.mobile.R
import com.zeddihub.mobile.data.local.CredentialStore
import com.zeddihub.mobile.data.local.LanguageCode
import com.zeddihub.mobile.data.local.ThemeMode
import com.zeddihub.mobile.ui.admin.AdminScreen
import com.zeddihub.mobile.ui.common.AppShell
import com.zeddihub.mobile.ui.community.CommunityScreen
import com.zeddihub.mobile.ui.home.HomeScreen
import com.zeddihub.mobile.ui.login.LoginScreen
import com.zeddihub.mobile.ui.login.LoginViewModel
import com.zeddihub.mobile.ui.notifications.NotificationsScreen
import com.zeddihub.mobile.ui.profile.ProfileScreen
import com.zeddihub.mobile.ui.servers.ServersScreen
import com.zeddihub.mobile.ui.settings.SettingsScreen
import com.zeddihub.mobile.ui.tools.AppFinderScreen
import com.zeddihub.mobile.ui.tools.CacheCleanerScreen
import com.zeddihub.mobile.ui.tools.DeviceInfoScreen
import com.zeddihub.mobile.ui.tools.IpLookupScreen
import com.zeddihub.mobile.ui.tools.SpeedTestScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    currentLanguage: LanguageCode,
    currentTheme: ThemeMode,
    onLanguage: (LanguageCode) -> Unit,
    onTheme: (ThemeMode) -> Unit
) {
    val gateVm: LoginViewModel = hiltViewModel()
    val loggedIn by gateVm.isLoggedIn.collectAsState()
    val session by gateVm.sessionFlow.collectAsState()

    val startRoute = if (loggedIn) Destinations.Home.route else Destinations.Login.route

    NavHost(navController = navController, startDestination = startRoute) {

        composable(Destinations.Login.route) {
            LoginScreen(
                currentLanguage = currentLanguage,
                currentTheme = currentTheme,
                onLanguage = onLanguage,
                onTheme = onTheme,
                onLoggedIn = {
                    navController.navigate(Destinations.Home.route) {
                        popUpTo(Destinations.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Destinations.Home.route) {
            Shell(navController, session, Destinations.Home.route, stringResource(R.string.home_title)) { padding ->
                HomeScreen(
                    padding = padding,
                    isAdmin = session?.role?.equals("admin", ignoreCase = true) == true,
                    onNavigate = { route -> navTo(navController, route) }
                )
            }
        }

        composable(Destinations.Servers.route) {
            Shell(navController, session, Destinations.Servers.route, stringResource(R.string.nav_servers)) { padding ->
                ServersScreen(padding = padding)
            }
        }

        composable(Destinations.SpeedTest.route) {
            Shell(navController, session, Destinations.SpeedTest.route, stringResource(R.string.nav_speedtest)) { padding ->
                SpeedTestScreen(padding = padding)
            }
        }

        composable(Destinations.IpLookup.route) {
            Shell(navController, session, Destinations.IpLookup.route, stringResource(R.string.nav_ip_lookup)) { padding ->
                IpLookupScreen(padding = padding)
            }
        }

        composable(Destinations.DeviceInfo.route) {
            Shell(navController, session, Destinations.DeviceInfo.route, stringResource(R.string.nav_device_info)) { padding ->
                DeviceInfoScreen(padding = padding)
            }
        }

        composable(Destinations.CacheCleaner.route) {
            Shell(navController, session, Destinations.CacheCleaner.route, stringResource(R.string.nav_cache_cleaner)) { padding ->
                CacheCleanerScreen(padding = padding)
            }
        }

        composable(Destinations.AppFinder.route) {
            Shell(navController, session, Destinations.AppFinder.route, stringResource(R.string.nav_app_finder)) { padding ->
                AppFinderScreen(padding = padding)
            }
        }

        composable(Destinations.Profile.route) {
            Shell(navController, session, Destinations.Profile.route, stringResource(R.string.nav_profile)) { padding ->
                ProfileScreen(
                    padding = padding,
                    session = session,
                    onLogout = {
                        gateVm.logout()
                        navController.navigate(Destinations.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(Destinations.Notifications.route) {
            Shell(navController, session, Destinations.Notifications.route, stringResource(R.string.nav_notifications)) { padding ->
                NotificationsScreen(padding = padding)
            }
        }

        composable(Destinations.Community.route) {
            Shell(navController, session, Destinations.Community.route, stringResource(R.string.nav_community)) { padding ->
                CommunityScreen(padding = padding)
            }
        }

        composable(Destinations.Admin.route) {
            Shell(
                navController = navController,
                session = session,
                currentRoute = Destinations.Admin.route,
                title = stringResource(R.string.nav_admin),
                gesturesEnabled = false
            ) { padding ->
                AdminScreen(
                    padding = padding,
                    credentials = gateVm.credentials()
                )
            }
        }

        composable(Destinations.Settings.route) {
            Shell(navController, session, Destinations.Settings.route, stringResource(R.string.nav_settings)) { padding ->
                SettingsScreen(
                    padding = padding,
                    onLanguageChange = onLanguage,
                    onThemeChange = onTheme,
                    onFactoryReset = {
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
private fun Shell(
    navController: NavHostController,
    session: CredentialStore.Session?,
    currentRoute: String,
    title: String,
    gesturesEnabled: Boolean = true,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit
) {
    val gateVm: LoginViewModel = hiltViewModel()
    AppShell(
        title = title,
        currentRoute = currentRoute,
        isAdmin = session?.role?.equals("admin", ignoreCase = true) == true,
        displayName = session?.displayName ?: session?.username,
        role = session?.role,
        gesturesEnabled = gesturesEnabled,
        onNavigate = { route -> navTo(navController, route) },
        onLogout = {
            gateVm.logout()
            navController.navigate(Destinations.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        },
        content = content
    )
}

private fun navTo(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
