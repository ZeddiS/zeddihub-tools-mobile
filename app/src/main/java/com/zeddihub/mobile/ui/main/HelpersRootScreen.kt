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
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DriveEta
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Compare
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zeddihub.mobile.R
import com.zeddihub.mobile.data.repository.FeatureKeys
import com.zeddihub.mobile.data.repository.FeatureState
import com.zeddihub.mobile.ui.common.PermissionsViewModel
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

    // Subscribe to the permission matrix so HIDDEN tiles get filtered
    // out and SOON tiles render with a non-clickable badge. The state
    // recomposes the whole screen when refresh() lands a new payload —
    // small enough surface that this is fine.
    val permsVm: PermissionsViewModel = hiltViewModel()
    val perms by permsVm.permissions.collectAsState()
    val stateFor: (String) -> FeatureState = remember(perms) { { key ->
        when (perms.states[key]) {
            "soon" -> FeatureState.SOON
            "hidden" -> FeatureState.HIDDEN
            else -> FeatureState.VISIBLE
        }
    } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(padding)
            .verticalScroll(rememberScrollState())
    ) {
        SectionHeader(stringResource(R.string.helpers_section_calc))
        HelperRow(
            HelperEntry(R.string.nav_currency, Icons.Default.CurrencyExchange, Destinations.CurrencyConverter.route, FeatureKeys.CURRENCY),
            HelperEntry(R.string.nav_age_calc, Icons.Default.Cake, Destinations.AgeCalculator.route, FeatureKeys.AGE_CALC),
            onNavigate, stateFor
        )
        HelperRow(
            HelperEntry(R.string.nav_electrician, Icons.Default.Bolt, Destinations.ElectricianCalc.route, FeatureKeys.ELECTRICIAN),
            null,
            onNavigate, stateFor
        )

        SectionHeader(stringResource(R.string.helpers_section_reference))
        HelperRow(
            HelperEntry(R.string.nav_morse_braille, Icons.Default.Keyboard, Destinations.MorseBraille.route, FeatureKeys.MORSE_BRAILLE),
            HelperEntry(R.string.nav_bubble_level, Icons.Default.Straighten, Destinations.BubbleLevel.route, FeatureKeys.BUBBLE_LEVEL),
            onNavigate, stateFor
        )
        HelperRow(
            HelperEntry(R.string.nav_periodic_table, Icons.Default.Science, Destinations.PeriodicTable.route, FeatureKeys.PERIODIC),
            HelperEntry(R.string.nav_school_tools, Icons.Default.School, Destinations.SchoolTools.route, FeatureKeys.SCHOOL_TOOLS, stub = true),
            onNavigate, stateFor
        )

        SectionHeader(stringResource(R.string.helpers_section_catalogs))
        HelperRow(
            HelperEntry(R.string.nav_holidays, Icons.Default.Event, Destinations.HolidaysCatalog.route, FeatureKeys.HOLIDAYS),
            HelperEntry(R.string.nav_hazard_signs, Icons.Default.Warning, Destinations.HazardSigns.route, FeatureKeys.HAZARD_SIGNS, stub = true),
            onNavigate, stateFor
        )
        HelperRow(
            HelperEntry(R.string.nav_traffic_signs, Icons.Default.DirectionsCar, Destinations.TrafficSigns.route, FeatureKeys.TRAFFIC_SIGNS, stub = true),
            HelperEntry(R.string.nav_clothes_sizes, Icons.Default.Checkroom, Destinations.ClothesSizes.route, FeatureKeys.CLOTHES_SIZES),
            onNavigate, stateFor
        )

        // v0.8.0 productivity / multimedia helpers
        SectionHeader(stringResource(R.string.helpers_section_v080))
        HelperRow(
            HelperEntry(R.string.nav_reminders, Icons.Default.NotificationsActive, Destinations.SmartReminders.route, FeatureKeys.REMINDERS),
            HelperEntry(R.string.nav_tuner, Icons.Default.GraphicEq, Destinations.Tuner.route, FeatureKeys.TUNER),
            onNavigate, stateFor
        )
        HelperRow(
            HelperEntry(R.string.nav_fullscreen_text, Icons.Default.TextFields, Destinations.FullscreenText.route, FeatureKeys.FULLSCREEN_TEXT),
            HelperEntry(R.string.nav_rubik, Icons.Default.Extension, Destinations.RubikSolver.route, FeatureKeys.RUBIK),
            onNavigate, stateFor
        )
        HelperRow(
            HelperEntry(R.string.nav_phone_test, Icons.Default.PhonelinkSetup, Destinations.PhoneTest.route, FeatureKeys.PHONE_TEST),
            HelperEntry(R.string.nav_voice_changer, Icons.Default.RecordVoiceOver, Destinations.VoiceChanger.route, FeatureKeys.VOICE_CHANGER),
            onNavigate, stateFor
        )

        // v0.9.0 hardware diagnostics + media tools
        SectionHeader(stringResource(R.string.helpers_section_v090))
        HelperRow(
            HelperEntry(R.string.nav_bt_tools, Icons.Default.Bluetooth, Destinations.BluetoothTools.route, FeatureKeys.BT_TOOLS),
            HelperEntry(R.string.nav_usb_tools, Icons.Default.Usb, Destinations.UsbTools.route, FeatureKeys.USB_TOOLS),
            onNavigate, stateFor
        )
        HelperRow(
            HelperEntry(R.string.nav_usb_format, Icons.Default.FormatPaint, Destinations.UsbFormat.route, FeatureKeys.USB_FORMAT),
            HelperEntry(R.string.nav_multi_remote, Icons.Default.SettingsRemote, Destinations.MultiRemote.route, FeatureKeys.MULTI_REMOTE),
            onNavigate, stateFor
        )
        HelperRow(
            HelperEntry(R.string.nav_music_tools, Icons.Default.LibraryMusic, Destinations.MusicTools.route, FeatureKeys.MUSIC_TOOLS),
            HelperEntry(R.string.nav_call_recorder, Icons.Default.Call, Destinations.CallRecorder.route, FeatureKeys.CALL_RECORDER),
            onNavigate, stateFor
        )
        HelperRow(
            HelperEntry(R.string.nav_bt_advertise, Icons.Default.BluetoothSearching, Destinations.BluetoothAdvertise.route, FeatureKeys.BT_ADVERTISE),
            HelperEntry(R.string.nav_license_plate, Icons.Default.DriveEta, Destinations.LicensePlate.route, FeatureKeys.LICENSE_PLATE),
            onNavigate, stateFor
        )
        HelperRow(
            HelperEntry(R.string.nav_prank, Icons.Default.Mood, Destinations.PrankTools.route, FeatureKeys.PRANK),
            HelperEntry(R.string.nav_before_after, Icons.Default.Compare, Destinations.BeforeAfter.route, FeatureKeys.BEFORE_AFTER),
            onNavigate, stateFor
        )

        Spacer(Modifier.height(32.dp))
    }
}

private data class HelperEntry(
    val labelRes: Int,
    val icon: ImageVector,
    val route: String,
    /** Server-controlled feature key. Null = always visible (legacy
     *  helpers that pre-date the matrix and stay always-on). */
    val featureKey: String? = null,
    /** Manual stub flag for screens we know aren't built yet. The
     *  matrix's SOON state overrides this if it's set. */
    val stub: Boolean = false,
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
    onNavigate: (String) -> Unit,
    stateFor: (String) -> FeatureState = { FeatureState.VISIBLE },
) {
    val leftState = left.featureKey?.let(stateFor) ?: FeatureState.VISIBLE
    val rightState = right?.featureKey?.let(stateFor) ?: FeatureState.VISIBLE

    // Skip the whole row if both sides are hidden — keeps the section
    // from leaving an empty band when admin hides matched-pair tiles.
    if (leftState == FeatureState.HIDDEN && (right == null || rightState == FeatureState.HIDDEN)) {
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (leftState != FeatureState.HIDDEN) {
            HelperCard(left, leftState, onNavigate, Modifier.weight(1f))
        } else {
            // Hidden left, visible right — keep right anchored to the
            // left column rather than letting it pop to the right.
            Spacer(Modifier.weight(1f))
        }
        if (right != null && rightState != FeatureState.HIDDEN) {
            HelperCard(right, rightState, onNavigate, Modifier.weight(1f))
        } else {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun HelperCard(
    entry: HelperEntry,
    state: FeatureState,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val isSoon = state == FeatureState.SOON || entry.stub
    Surface(
        modifier = modifier
            .height(96.dp)
            // SOON tiles look the same but aren't clickable. We don't
            // pop a toast on tap because that gets noisy when admin
            // SOON-locks a whole tab; the badge is enough.
            .let { if (isSoon) it else it.clickable { onNavigate(entry.route) } },
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
            if (isSoon) {
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
