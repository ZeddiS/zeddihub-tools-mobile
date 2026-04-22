package com.zeddihub.mobile.ui.navigation

sealed class Destinations(val route: String) {
    data object Login : Destinations("login")
    data object Register : Destinations("register")
    data object Home : Destinations("home")
    data object Servers : Destinations("servers")
    data object SpeedTest : Destinations("tools/speedtest")
    data object IpLookup : Destinations("tools/ip")
    data object DeviceInfo : Destinations("tools/device")
    data object CacheCleaner : Destinations("tools/cache")
    data object AppFinder : Destinations("tools/apps")
    data object WifiScanner : Destinations("tools/wifi/scan")
    data object WifiMap : Destinations("tools/wifi/map")
    data object WifiTools : Destinations("tools/wifi/tools")
    data object PdfScanner : Destinations("tools/pdf")
    data object DecibelMeter : Destinations("tools/decibel")
    data object Flashlight : Destinations("tools/flashlight")
    data object Profile : Destinations("profile")
    data object Notifications : Destinations("notifications")
    data object Community : Destinations("community")
    data object Admin : Destinations("admin")
    data object Settings : Destinations("settings")
    data object ServerDetail : Destinations("server/{id}") {
        fun build(id: String) = "server/$id"
    }
}
