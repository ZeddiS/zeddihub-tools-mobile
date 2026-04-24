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
import com.zeddihub.mobile.data.local.LanguageCode
import com.zeddihub.mobile.data.local.ThemeMode
import com.zeddihub.mobile.ui.admin.AdminScreen
import com.zeddihub.mobile.ui.common.DetailShell
import com.zeddihub.mobile.ui.community.CommunityScreen
import com.zeddihub.mobile.ui.helpers.AgeCalculatorScreen
import com.zeddihub.mobile.ui.helpers.BubbleLevelScreen
import com.zeddihub.mobile.ui.helpers.CurrencyConverterScreen
import com.zeddihub.mobile.ui.helpers.ElectricianScreen
import com.zeddihub.mobile.ui.helpers.HazardSignsScreen
import com.zeddihub.mobile.ui.helpers.HolidaysScreen
import com.zeddihub.mobile.ui.helpers.MorseBrailleScreen
import com.zeddihub.mobile.ui.helpers.PeriodicTableScreen
import com.zeddihub.mobile.ui.helpers.StubScreen
import com.zeddihub.mobile.ui.helpers.TrafficSignsScreen
import com.zeddihub.mobile.ui.helpers.school.FractionsCalculatorScreen
import com.zeddihub.mobile.ui.helpers.school.GradeCalculatorScreen
import com.zeddihub.mobile.ui.helpers.school.MathFormulasScreen
import com.zeddihub.mobile.ui.helpers.school.SchoolToolsHubScreen
import com.zeddihub.mobile.ui.helpers.school.StatisticsCalculatorScreen
import com.zeddihub.mobile.ui.helpers.school.TimeCalculatorScreen
import com.zeddihub.mobile.ui.helpers.school.TriangleCalculatorScreen
import com.zeddihub.mobile.ui.helpers.school.UnitConverterScreen
import com.zeddihub.mobile.ui.login.LoginScreen
import com.zeddihub.mobile.ui.login.LoginViewModel
import com.zeddihub.mobile.ui.login.RegisterScreen
import com.zeddihub.mobile.ui.main.MainScaffold
import com.zeddihub.mobile.ui.notifications.NotificationsScreen
import com.zeddihub.mobile.ui.profile.ProfileScreen
import com.zeddihub.mobile.ui.servers.ServersScreen
import com.zeddihub.mobile.ui.settings.SettingsScreen
import com.zeddihub.mobile.ui.tools.AdvancedBarcodeScreen
import com.zeddihub.mobile.ui.tools.AdvancedNfcScreen
import com.zeddihub.mobile.ui.tools.AdvancedQrScreen
import com.zeddihub.mobile.ui.tools.AdvancedTextEditorScreen
import com.zeddihub.mobile.ui.tools.AppFinderScreen
import com.zeddihub.mobile.ui.tools.CacheCleanerScreen
import com.zeddihub.mobile.ui.tools.DecibelMeterScreen
import com.zeddihub.mobile.ui.tools.DeviceInfoScreen
import com.zeddihub.mobile.ui.tools.FlashlightScreen
import com.zeddihub.mobile.ui.tools.FrequencyGeneratorScreen
import com.zeddihub.mobile.ui.tools.IpLookupScreen
import com.zeddihub.mobile.ui.tools.MyNetworkScreen
import com.zeddihub.mobile.ui.tools.PdfScannerScreen
import com.zeddihub.mobile.ui.tools.SpeakerCleanerScreen
import com.zeddihub.mobile.ui.tools.SpeedTestScreen
import com.zeddihub.mobile.ui.tools.StorageManagerScreen
import com.zeddihub.mobile.ui.tools.VideoDownloaderScreen
import com.zeddihub.mobile.ui.tools.WifiMapScreen
import com.zeddihub.mobile.ui.tools.WifiScannerScreen
import com.zeddihub.mobile.ui.tools.WifiToolsScreen

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

    val startRoute = if (loggedIn) Destinations.Main.route else Destinations.Login.route

    NavHost(navController = navController, startDestination = startRoute) {

        composable(Destinations.Login.route) {
            LoginScreen(
                currentLanguage = currentLanguage,
                currentTheme = currentTheme,
                onLanguage = onLanguage,
                onTheme = onTheme,
                onLoggedIn = {
                    navController.navigate(Destinations.Main.route) {
                        popUpTo(Destinations.Login.route) { inclusive = true }
                    }
                },
                onGoToRegister = {
                    navController.navigate(Destinations.Register.route)
                }
            )
        }

        composable(Destinations.Register.route) {
            RegisterScreen(
                onRegistered = {
                    navController.navigate(Destinations.Main.route) {
                        popUpTo(Destinations.Login.route) { inclusive = true }
                    }
                },
                onBackToLogin = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Destinations.Login.route) {
                            popUpTo(Destinations.Register.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        // ── Main tab container ────────────────────────────────────────────
        composable(Destinations.Main.route) {
            MainScaffold(navController = navController)
        }

        // ── Tool detail screens ───────────────────────────────────────────
        detail(Destinations.Servers.route, R.string.nav_servers, navController) { padding ->
            ServersScreen(padding = padding)
        }
        detail(Destinations.MyNetwork.route, R.string.nav_my_network, navController) { padding ->
            MyNetworkScreen(padding = padding, navController = navController)
        }
        detail(Destinations.SpeedTest.route, R.string.nav_speedtest, navController) { padding ->
            SpeedTestScreen(padding = padding)
        }
        detail(Destinations.IpLookup.route, R.string.nav_ip_lookup, navController) { padding ->
            IpLookupScreen(padding = padding)
        }
        detail(Destinations.DeviceInfo.route, R.string.nav_device_info, navController) { padding ->
            DeviceInfoScreen(padding = padding)
        }
        detail(Destinations.CacheCleaner.route, R.string.nav_cache_cleaner, navController) { padding ->
            CacheCleanerScreen(padding = padding)
        }
        detail(Destinations.AppFinder.route, R.string.nav_app_finder, navController) { padding ->
            AppFinderScreen(padding = padding)
        }
        detail(Destinations.WifiScanner.route, R.string.nav_wifi_scanner, navController) { padding ->
            WifiScannerScreen(padding = padding)
        }
        detail(Destinations.WifiMap.route, R.string.nav_wifi_map, navController) { padding ->
            WifiMapScreen(padding = padding)
        }
        detail(Destinations.WifiTools.route, R.string.nav_wifi_tools, navController) { padding ->
            WifiToolsScreen(padding = padding)
        }
        detail(Destinations.PdfScanner.route, R.string.nav_pdf_scanner, navController) { padding ->
            PdfScannerScreen(padding = padding)
        }
        detail(Destinations.DecibelMeter.route, R.string.nav_decibel_meter, navController) { padding ->
            DecibelMeterScreen(padding = padding)
        }
        detail(Destinations.Flashlight.route, R.string.nav_flashlight, navController) { padding ->
            FlashlightScreen(padding = padding)
        }
        detail(Destinations.Storage.route, R.string.nav_storage, navController) { padding ->
            StorageManagerScreen(padding = padding)
        }

        // New v0.5.8 tools
        detail(Destinations.FrequencyGenerator.route, R.string.nav_frequency_generator, navController) { padding ->
            FrequencyGeneratorScreen(padding = padding)
        }
        detail(Destinations.SpeakerCleaner.route, R.string.nav_speaker_cleaner, navController) { padding ->
            SpeakerCleanerScreen(padding = padding)
        }
        detail(Destinations.VideoDownloader.route, R.string.nav_video_downloader, navController) { padding ->
            VideoDownloaderScreen(padding = padding)
        }
        detail(Destinations.AdvancedNfc.route, R.string.nav_advanced_nfc, navController) { padding ->
            AdvancedNfcScreen(padding = padding)
        }
        // Stub advanced tools
        detail(Destinations.AdvancedQr.route, R.string.nav_advanced_qr, navController) { padding ->
            AdvancedQrScreen(padding = padding)
        }
        detail(Destinations.AdvancedBarcode.route, R.string.nav_advanced_barcode, navController) { padding ->
            AdvancedBarcodeScreen(padding = padding)
        }
        detail(Destinations.AdvancedTextEditor.route, R.string.nav_advanced_text_editor, navController) { padding ->
            AdvancedTextEditorScreen(padding = padding)
        }

        // ── Helper detail screens ─────────────────────────────────────────
        detail(Destinations.CurrencyConverter.route, R.string.nav_currency, navController) { padding ->
            CurrencyConverterScreen(padding = padding)
        }
        detail(Destinations.AgeCalculator.route, R.string.nav_age_calc, navController) { padding ->
            AgeCalculatorScreen(padding = padding)
        }
        detail(Destinations.MorseBraille.route, R.string.nav_morse_braille, navController) { padding ->
            MorseBrailleScreen(padding = padding)
        }
        detail(Destinations.BubbleLevel.route, R.string.nav_bubble_level, navController) { padding ->
            BubbleLevelScreen(padding = padding)
        }
        // Stub helpers
        detail(Destinations.HolidaysCatalog.route, R.string.nav_holidays, navController) { padding ->
            HolidaysScreen(padding = padding)
        }
        detail(Destinations.HazardSigns.route, R.string.nav_hazard_signs, navController) { padding ->
            HazardSignsScreen(padding = padding, language = currentLanguage)
        }
        detail(Destinations.TrafficSigns.route, R.string.nav_traffic_signs, navController) { padding ->
            TrafficSignsScreen(padding = padding, language = currentLanguage)
        }
        detail(Destinations.SchoolTools.route, R.string.nav_school_tools, navController) { padding ->
            SchoolToolsHubScreen(padding = padding, onNavigate = { navController.navigate(it) })
        }
        detail(Destinations.SchoolGrade.route, R.string.nav_school_grade, navController) { padding ->
            GradeCalculatorScreen(padding = padding)
        }
        detail(Destinations.SchoolUnitConverter.route, R.string.nav_school_units, navController) { padding ->
            UnitConverterScreen(padding = padding)
        }
        detail(Destinations.SchoolMathFormulas.route, R.string.nav_school_formulas, navController) { padding ->
            MathFormulasScreen(padding = padding)
        }
        detail(Destinations.SchoolFractions.route, R.string.nav_school_fractions, navController) { padding ->
            FractionsCalculatorScreen(padding = padding)
        }
        detail(Destinations.SchoolTriangle.route, R.string.nav_school_triangle, navController) { padding ->
            TriangleCalculatorScreen(padding = padding)
        }
        detail(Destinations.SchoolStatistics.route, R.string.nav_school_stats, navController) { padding ->
            StatisticsCalculatorScreen(padding = padding)
        }
        detail(Destinations.SchoolTime.route, R.string.nav_school_time, navController) { padding ->
            TimeCalculatorScreen(padding = padding)
        }
        detail(Destinations.PeriodicTable.route, R.string.nav_periodic_table, navController) { padding ->
            PeriodicTableScreen(padding = padding)
        }
        detail(Destinations.ElectricianCalc.route, R.string.nav_electrician, navController) { padding ->
            ElectricianScreen(padding = padding)
        }

        // ── Account detail screens ────────────────────────────────────────
        composable(Destinations.Profile.route) {
            val vm: LoginViewModel = hiltViewModel()
            val session by vm.sessionFlow.collectAsState()
            DetailShell(
                title = stringResource(R.string.nav_profile),
                currentRoute = Destinations.Profile.route,
                onBack = { if (!navController.popBackStack()) navController.navigate(Destinations.Main.route) }
            ) { padding ->
                ProfileScreen(
                    padding = padding,
                    session = session,
                    onLogout = {
                        vm.logout()
                        navController.navigate(Destinations.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
        detail(Destinations.Notifications.route, R.string.nav_notifications, navController) { padding ->
            NotificationsScreen(padding = padding)
        }
        detail(Destinations.Community.route, R.string.nav_community, navController) { padding ->
            CommunityScreen(padding = padding)
        }
        detail(Destinations.Admin.route, R.string.nav_admin, navController) { padding ->
            AdminScreen(
                padding = padding,
                credentials = gateVm.credentials()
            )
        }
        detail(Destinations.Settings.route, R.string.nav_settings, navController) { padding ->
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

/**
 * Helper that wraps a detail route in [DetailShell] (top bar + back arrow)
 * and registers it on the NavGraphBuilder. Pulled out to keep AppNavGraph
 * compact — each detail becomes a single line above instead of 6-line
 * boilerplate.
 */
private fun androidx.navigation.NavGraphBuilder.detail(
    route: String,
    titleRes: Int,
    navController: NavHostController,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit
) {
    composable(route) {
        DetailShell(
            title = stringResource(titleRes),
            currentRoute = route,
            onBack = { if (!navController.popBackStack()) navController.navigate(Destinations.Main.route) }
        ) { padding -> content(padding) }
    }
}
