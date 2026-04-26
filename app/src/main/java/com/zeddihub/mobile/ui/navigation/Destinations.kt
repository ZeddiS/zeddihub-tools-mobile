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
    data object Compass : Destinations("help/compass")
    data object BatteryInfo : Destinations("help/battery")
    data object QuickNote : Destinations("help/quick_note")

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

    // ── v0.8.0 helpers ────────────────────────────────────────────────────
    data object Tuner : Destinations("help/tuner")
    data object FullscreenText : Destinations("help/fst")
    data object ClothesSizes : Destinations("help/sizes")
    data object SmartReminders : Destinations("help/reminders")
    data object RubikSolver : Destinations("help/rubik")

    // ── v0.9.0 helpers ────────────────────────────────────────────────────
    data object PhoneTest : Destinations("help/phone_test")
    data object BluetoothTools : Destinations("help/bt")
    data object UsbTools : Destinations("help/usb")
    data object UsbFormat : Destinations("help/usb_format")
    data object MusicTools : Destinations("help/music")
    data object VoiceChanger : Destinations("help/voice")
    data object MultiRemote : Destinations("help/remote")
    data object CallRecorder : Destinations("help/call_recorder")
    // ── v1.8.5 helpers ────────────────────────────────────────────────────
    data object BluetoothAdvertise : Destinations("help/bt_advertise")

    // ── v0.7.8 helpers ────────────────────────────────────────────────────
    data object LicensePlate : Destinations("help/license_plate")
    data object PrankTools : Destinations("help/prank")

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
