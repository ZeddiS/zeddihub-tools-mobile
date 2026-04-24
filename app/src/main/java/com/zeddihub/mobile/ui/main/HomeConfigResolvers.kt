package com.zeddihub.mobile.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.zeddihub.mobile.ui.navigation.Destinations

/**
 * Maps the string `nav_id` values published by the admin panel (see
 * `tools/admin/home_mobile.php` $NAV_REGISTRY) to concrete nav routes.
 * Returns null for unknown ids so the Dashboard can silently skip them.
 */
fun resolveNavRoute(navId: String): String? = when (navId) {
    "speedtest"         -> Destinations.SpeedTest.route
    "mynetwork"         -> Destinations.MyNetwork.route
    "ip_lookup"         -> Destinations.IpLookup.route
    "device_info"       -> Destinations.DeviceInfo.route
    "cache_cleaner"     -> Destinations.CacheCleaner.route
    "app_finder"        -> Destinations.AppFinder.route
    "wifi_scan"         -> Destinations.WifiScanner.route
    "wifi_map"          -> Destinations.WifiMap.route
    "wifi_tools"        -> Destinations.WifiTools.route
    "pdf_scanner"       -> Destinations.PdfScanner.route
    "decibel"           -> Destinations.DecibelMeter.route
    "flashlight"        -> Destinations.Flashlight.route
    "storage"           -> Destinations.Storage.route
    "servers"           -> Destinations.Servers.route
    "freq_generator"    -> Destinations.FrequencyGenerator.route
    "speaker_cleaner"   -> Destinations.SpeakerCleaner.route
    "video_downloader"  -> Destinations.VideoDownloader.route
    "nfc"               -> Destinations.AdvancedNfc.route
    "qr"                -> Destinations.AdvancedQr.route
    "barcode"           -> Destinations.AdvancedBarcode.route
    "text_editor"       -> Destinations.AdvancedTextEditor.route
    "currency"          -> Destinations.CurrencyConverter.route
    "age"               -> Destinations.AgeCalculator.route
    "morse_braille"     -> Destinations.MorseBraille.route
    "bubble_level"      -> Destinations.BubbleLevel.route
    "holidays"          -> Destinations.HolidaysCatalog.route
    "hazard"            -> Destinations.HazardSigns.route
    "traffic"           -> Destinations.TrafficSigns.route
    "school"            -> Destinations.SchoolTools.route
    "school_grade"      -> Destinations.SchoolGrade.route
    "school_units"      -> Destinations.SchoolUnitConverter.route
    "school_formulas"   -> Destinations.SchoolMathFormulas.route
    "school_fractions"  -> Destinations.SchoolFractions.route
    "school_triangle"   -> Destinations.SchoolTriangle.route
    "school_stats"      -> Destinations.SchoolStatistics.route
    "school_time"       -> Destinations.SchoolTime.route
    "periodic"          -> Destinations.PeriodicTable.route
    "electrician"       -> Destinations.ElectricianCalc.route
    else                -> null
}

/**
 * Translates admin-picked icon names (string in $AVAILABLE_ICONS) into
 * concrete Material icons. Unknown names fall back to a generic
 * [Icons.Default.Star] so the tile still renders.
 */
fun resolveIcon(name: String): ImageVector = when (name) {
    "NetworkCheck"      -> Icons.Default.NetworkCheck
    "Wifi"              -> Icons.Default.Wifi
    "FlashlightOn"      -> Icons.Default.FlashlightOn
    "GraphicEq"         -> Icons.Default.GraphicEq
    "Speaker"           -> Icons.Default.Speaker
    "Storage"           -> Icons.Default.Storage
    "QrCode"            -> Icons.Default.QrCode
    "QrCode2"           -> Icons.Default.QrCode2
    "Nfc"               -> Icons.Default.Nfc
    "PictureAsPdf"      -> Icons.Default.PictureAsPdf
    "Edit"              -> Icons.Default.Edit
    "Download"          -> Icons.Default.Download
    "Translate"         -> Icons.Default.Translate
    "Cake"              -> Icons.Default.Cake
    "Paid"              -> Icons.Default.Paid
    "Straighten"        -> Icons.Default.Straighten
    "CalendarMonth"     -> Icons.Default.CalendarMonth
    "Warning"           -> Icons.Default.Warning
    "DirectionsCar"     -> Icons.Default.DirectionsCar
    "School"            -> Icons.Default.School
    "Science"           -> Icons.Default.Science
    "Bolt"              -> Icons.Default.Bolt
    "Apps"              -> Icons.Default.Apps
    "CleaningServices"  -> Icons.Default.CleaningServices
    "PhoneAndroid"      -> Icons.Default.PhoneAndroid
    "Public"            -> Icons.Default.Public
    "Map"               -> Icons.Default.Map
    "Bookmark"          -> Icons.Default.Bookmark
    "Star"              -> Icons.Default.Star
    "Build"             -> Icons.Default.Build
    "Settings"          -> Icons.Default.Settings
    "Folder"            -> Icons.Default.Folder
    "Calculate"         -> Icons.Default.Calculate
    "ChangeHistory"     -> Icons.Default.ChangeHistory
    "Functions"         -> Icons.Default.Functions
    "Percent"           -> Icons.Default.Percent
    "QueryStats"        -> Icons.Default.QueryStats
    "Schedule"          -> Icons.Default.Schedule
    "SwapHoriz"         -> Icons.Default.SwapHoriz
    else                -> Icons.Default.Star
}

/** Best-effort `#rrggbb` / `#aarrggbb` parsing. Invalid strings fall back to [fallback]. */
fun parseHexColor(hex: String, fallback: Color): Color {
    val raw = hex.trim()
    val withHash = if (raw.startsWith("#")) raw else "#$raw"
    return runCatching { Color(android.graphics.Color.parseColor(withHash)) }
        .getOrDefault(fallback)
}
