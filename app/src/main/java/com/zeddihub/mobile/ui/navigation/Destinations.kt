package com.zeddihub.mobile.ui.navigation

sealed class Destinations(val route: String) {
    // Auth
    data object Login : Destinations("login")
    data object Register : Destinations("register")

    // Main tab container (replaces old Home root).
    // Sub-tabs Dashboard/Tools/Helpers/Account are HorizontalPager pages,
    // not individual routes — they share this container.
    data object Main : Destinations("main")

    // ── Tool detail screens (Nástroje tab) ────────────────────────────────
    data object Servers : Destinations("servers")
    data object MyNetwork : Destinations("tools/mynetwork")
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
    data object Storage : Destinations("tools/storage")

    // New tools (v0.5.8)
    data object FrequencyGenerator : Destinations("tools/freq")
    data object SpeakerCleaner : Destinations("tools/speaker_cleaner")
    data object VideoDownloader : Destinations("tools/video_dl")
    data object AdvancedNfc : Destinations("tools/nfc")
    data object AdvancedQr : Destinations("tools/qr")
    data object AdvancedBarcode : Destinations("tools/barcode")
    data object AdvancedTextEditor : Destinations("tools/text_editor")

    // ── Helper detail screens (Pomůcky tab) ───────────────────────────────
    data object CurrencyConverter : Destinations("help/currency")
    data object AgeCalculator : Destinations("help/age")
    data object MorseBraille : Destinations("help/morse_braille")
    data object BubbleLevel : Destinations("help/level")

    // Stub helpers (will be fully implemented in later releases)
    data object HolidaysCatalog : Destinations("help/holidays")
    data object HazardSigns : Destinations("help/hazard")
    data object TrafficSigns : Destinations("help/traffic")
    // Školní pomůcky — hub + 7 sub-tools. Hub je rozcestník (tiles), každý
    // nástroj vlastní route. Osmá "dlaždice" v hubu je zkratka na existující
    // PeriodicTable (sdílený nav route).
    data object SchoolTools : Destinations("help/school")
    data object SchoolGrade : Destinations("help/school/grade")
    data object SchoolUnitConverter : Destinations("help/school/units")
    data object SchoolMathFormulas : Destinations("help/school/formulas")
    data object SchoolFractions : Destinations("help/school/fractions")
    data object SchoolTriangle : Destinations("help/school/triangle")
    data object SchoolStatistics : Destinations("help/school/stats")
    data object SchoolTime : Destinations("help/school/time")
    data object PeriodicTable : Destinations("help/periodic")
    data object ElectricianCalc : Destinations("help/electrician")

    // ── Account detail screens (Účet tab) ─────────────────────────────────
    data object Profile : Destinations("profile")
    data object Notifications : Destinations("notifications")
    data object Community : Destinations("community")
    data object Admin : Destinations("admin")
    data object Settings : Destinations("settings")

    data object ServerDetail : Destinations("server/{id}") {
        fun build(id: String) = "server/$id"
    }

    // Legacy alias: kept so any stored state that still expects "home"
    // transparently lands on the new Main tab container.
    data object Home : Destinations("main")
}
