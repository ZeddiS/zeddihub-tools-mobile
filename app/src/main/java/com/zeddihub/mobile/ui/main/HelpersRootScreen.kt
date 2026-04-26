package com.zeddihub.mobile.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhonelinkSetup
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zeddihub.mobile.R
import com.zeddihub.mobile.ui.navigation.Destinations

/**
 * Root screen for the "Pomůcky" tab. Reference material and calculators.
 * Items marked [stub] show a "Coming soon" screen on tap.
 */
@Composable
fun HelpersRootScreen(
    padding: PaddingValues,
    onNavigate: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(padding)
            .verticalScroll(rememberScrollState())
    ) {
        SectionHeader(stringResource(R.string.helpers_section_calc))
        HelperRow(
            HelperEntry(R.string.nav_currency, Icons.Default.CurrencyExchange, Destinations.CurrencyConverter.route),
            HelperEntry(R.string.nav_age_calc, Icons.Default.Cake, Destinations.AgeCalculator.route),
            onNavigate
        )
        HelperRow(
            HelperEntry(R.string.nav_electrician, Icons.Default.Bolt, Destinations.ElectricianCalc.route),
            null,
            onNavigate
        )

        SectionHeader(stringResource(R.string.helpers_section_reference))
        HelperRow(
            HelperEntry(R.string.nav_morse_braille, Icons.Default.Keyboard, Destinations.MorseBraille.route),
            HelperEntry(R.string.nav_bubble_level, Icons.Default.Straighten, Destinations.BubbleLevel.route),
            onNavigate
        )
        HelperRow(
            HelperEntry(R.string.nav_periodic_table, Icons.Default.Science, Destinations.PeriodicTable.route),
            HelperEntry(R.string.nav_school_tools, Icons.Default.School, Destinations.SchoolTools.route, stub = true),
            onNavigate
        )

        SectionHeader(stringResource(R.string.helpers_section_catalogs))
        HelperRow(
            HelperEntry(R.string.nav_holidays, Icons.Default.Event, Destinations.HolidaysCatalog.route),
            HelperEntry(R.string.nav_hazard_signs, Icons.Default.Warning, Destinations.HazardSigns.route, stub = true),
            onNavigate
        )
        HelperRow(
            HelperEntry(R.string.nav_traffic_signs, Icons.Default.DirectionsCar, Destinations.TrafficSigns.route, stub = true),
            HelperEntry(R.string.nav_clothes_sizes, Icons.Default.Checkroom, Destinations.ClothesSizes.route),
            onNavigate
        )

        // v0.8.0 productivity / multimedia helpers
        SectionHeader(stringResource(R.string.helpers_section_v080))
        HelperRow(
            HelperEntry(R.string.nav_reminders, Icons.Default.NotificationsActive, Destinations.SmartReminders.route),
            HelperEntry(R.string.nav_tuner, Icons.Default.GraphicEq, Destinations.Tuner.route),
            onNavigate
        )
        HelperRow(
            HelperEntry(R.string.nav_fullscreen_text, Icons.Default.TextFields, Destinations.FullscreenText.route),
            HelperEntry(R.string.nav_rubik, Icons.Default.Extension, Destinations.RubikSolver.route),
            onNavigate
        )
        HelperRow(
            HelperEntry(R.string.nav_phone_test, Icons.Default.PhonelinkSetup, Destinations.PhoneTest.route),
            HelperEntry(R.string.nav_voice_changer, Icons.Default.RecordVoiceOver, Destinations.VoiceChanger.route),
            onNavigate
        )

        // v0.9.0 hardware diagnostics + media tools
        SectionHeader(stringResource(R.string.helpers_section_v090))
        HelperRow(
            HelperEntry(R.string.nav_bt_tools, Icons.Default.Bluetooth, Destinations.BluetoothTools.route),
            HelperEntry(R.string.nav_usb_tools, Icons.Default.Usb, Destinations.UsbTools.route),
            onNavigate
        )
        HelperRow(
            HelperEntry(R.string.nav_usb_format, Icons.Default.FormatPaint, Destinations.UsbFormat.route),
            HelperEntry(R.string.nav_multi_remote, Icons.Default.SettingsRemote, Destinations.MultiRemote.route),
            onNavigate
        )
        HelperRow(
            HelperEntry(R.string.nav_music_tools, Icons.Default.LibraryMusic, Destinations.MusicTools.route),
            HelperEntry(R.string.nav_call_recorder, Icons.Default.Call, Destinations.CallRecorder.route),
            onNavigate
        )

        Spacer(Modifier.height(32.dp))
    }
}

private data class HelperEntry(
    val labelRes: Int,
    val icon: ImageVector,
    val route: String,
    val stub: Boolean = false
)

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
private fun HelperRow(
    left: HelperEntry,
    right: HelperEntry?,
    onNavigate: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HelperCard(left, onNavigate, Modifier.weight(1f))
        if (right != null) HelperCard(right, onNavigate, Modifier.weight(1f))
        else Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun HelperCard(
    entry: HelperEntry,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .height(96.dp)
            .clickable { onNavigate(entry.route) },
        color = colors.surface,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 2.dp
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            Icon(
                entry.icon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(26.dp).align(Alignment.TopStart)
            )
            if (entry.stub) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(
                            colors.tertiary.copy(alpha = 0.18f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "SOON",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.tertiary
                    )
                }
            }
            Text(
                text = stringResource(entry.labelRes),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
                maxLines = 2,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}
