package com.zeddihub.mobile.data.repository

/**
 * Canonical feature keys, one per togglable tile in the app.
 *
 * **Must stay in sync with** the server-side list in
 * `tools/admin/roles.php → roles_features()` and
 * `api/permissions.php → pm_known_features()`. Adding a new key here
 * without adding it on the web means the matrix can't gate it; adding
 * one server-side without bumping this file means the app silently
 * ignores it (defaults to visible).
 *
 * Keys use `tools.*` for the Nástroje tab and `help.*` for the
 * Pomůcky tab. The dot-separated prefix lets server-side filters
 * scope a permission change to a whole tab without listing every
 * tile (`SET state='soon' WHERE feature_key LIKE 'help.%'`).
 */
object FeatureKeys {
    // Tools
    const val SERVERS = "tools.servers"
    const val MY_NETWORK = "tools.my_network"
    const val SPEEDTEST = "tools.speedtest"
    const val IP_LOOKUP = "tools.ip_lookup"
    const val DEVICE_INFO = "tools.device_info"
    const val CACHE_CLEANER = "tools.cache_cleaner"
    const val APP_FINDER = "tools.app_finder"
    const val WIFI_SCANNER = "tools.wifi_scanner"
    const val WIFI_MAP = "tools.wifi_map"
    const val WIFI_TOOLS = "tools.wifi_tools"
    const val PDF_SCANNER = "tools.pdf_scanner"
    const val DECIBEL_METER = "tools.decibel_meter"
    const val FLASHLIGHT = "tools.flashlight"
    const val STORAGE = "tools.storage"
    const val FREQ_GEN = "tools.frequency_gen"
    const val SPEAKER_CLEANER = "tools.speaker_cleaner"
    const val VIDEO_DL = "tools.video_dl"
    const val ADV_NFC = "tools.advanced_nfc"
    const val ADV_QR = "tools.advanced_qr"
    const val ADV_BARCODE = "tools.advanced_barcode"
    const val ADV_TEXT = "tools.advanced_text"

    // Helpers
    const val CURRENCY = "help.currency"
    const val AGE_CALC = "help.age_calc"
    const val MORSE_BRAILLE = "help.morse_braille"
    const val BUBBLE_LEVEL = "help.bubble_level"
    const val COMPASS = "help.compass"
    const val BATTERY_INFO = "help.battery_info"
    const val QUICK_NOTE = "help.quick_note"
    const val HOLIDAYS = "help.holidays"
    const val HAZARD_SIGNS = "help.hazard_signs"
    const val TRAFFIC_SIGNS = "help.traffic_signs"
    const val SCHOOL_TOOLS = "help.school_tools"
    const val PERIODIC = "help.periodic"
    const val ELECTRICIAN = "help.electrician"
    const val TUNER = "help.tuner"
    const val FULLSCREEN_TEXT = "help.fullscreen_text"
    const val CLOTHES_SIZES = "help.clothes_sizes"
    const val REMINDERS = "help.reminders"
    const val RUBIK = "help.rubik"
    const val PHONE_TEST = "help.phone_test"
    const val BT_TOOLS = "help.bt_tools"
    const val BT_ADVERTISE = "help.bt_advertise"
    const val USB_TOOLS = "help.usb_tools"
    const val USB_FORMAT = "help.usb_format"
    const val MUSIC_TOOLS = "help.music_tools"
    const val VOICE_CHANGER = "help.voice_changer"
    const val MULTI_REMOTE = "help.multi_remote"
    const val CALL_RECORDER = "help.call_recorder"
    const val LICENSE_PLATE = "help.license_plate"
    const val PRANK = "help.prank"
    const val BEFORE_AFTER = "help.before_after" // v0.8.0
}
