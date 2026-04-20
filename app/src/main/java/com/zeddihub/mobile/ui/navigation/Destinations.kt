package com.zeddihub.mobile.ui.navigation

sealed class Destinations(val route: String) {
    data object Login : Destinations("login")
    data object Home : Destinations("home")
    data object Servers : Destinations("servers")
    data object SpeedTest : Destinations("tools/speedtest")
    data object IpLookup : Destinations("tools/ip")
    data object DeviceInfo : Destinations("tools/device")
    data object CacheCleaner : Destinations("tools/cache")
    data object AppFinder : Destinations("tools/apps")
    data object Profile : Destinations("profile")
    data object Notifications : Destinations("notifications")
    data object Community : Destinations("community")
    data object Admin : Destinations("admin")
    data object Settings : Destinations("settings")
    data object ServerDetail : Destinations("server/{id}") {
        fun build(id: String) = "server/$id"
    }
}
