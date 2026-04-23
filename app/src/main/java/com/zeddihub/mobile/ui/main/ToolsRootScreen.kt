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
import androidx.compose.material.icons.automirrored.filled.ChromeReaderMode
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiPassword
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zeddihub.mobile.R
import com.zeddihub.mobile.ui.navigation.Destinations

/**
 * Root screen for the "Nástroje" tab. Lists all tool screens grouped
 * by section. New v0.5.8 features are surfaced at the top under
 * tools_section_new and also live in their natural category below.
 */
@Composable
fun ToolsRootScreen(
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
        SectionHeader(stringResource(R.string.tools_section_new), accent = true)
        ToolRow(
            ToolEntry(R.string.nav_frequency_generator, Icons.Default.GraphicEq, Destinations.FrequencyGenerator.route),
            ToolEntry(R.string.nav_speaker_cleaner, Icons.Default.Speaker, Destinations.SpeakerCleaner.route),
            onNavigate
        )
        ToolRow(
            ToolEntry(R.string.nav_video_downloader, Icons.Default.Download, Destinations.VideoDownloader.route),
            ToolEntry(R.string.nav_advanced_nfc, Icons.Default.Contactless, Destinations.AdvancedNfc.route),
            onNavigate
        )

        SectionHeader(stringResource(R.string.tools_section_network))
        ToolRow(
            ToolEntry(R.string.nav_my_network, Icons.Default.Wifi, Destinations.MyNetwork.route),
            ToolEntry(R.string.nav_speedtest, Icons.Default.NetworkCheck, Destinations.SpeedTest.route),
            onNavigate
        )
        ToolRow(
            ToolEntry(R.string.nav_ip_tracker, Icons.Default.Public, Destinations.IpLookup.route),
            null,
            onNavigate
        )

        SectionHeader(stringResource(R.string.tools_section_wifi))
        ToolRow(
            ToolEntry(R.string.nav_wifi_scanner, Icons.Default.Wifi, Destinations.WifiScanner.route),
            ToolEntry(R.string.nav_wifi_map, Icons.Default.Map, Destinations.WifiMap.route),
            onNavigate
        )
        ToolRow(
            ToolEntry(R.string.nav_wifi_tools, Icons.Default.WifiPassword, Destinations.WifiTools.route),
            null,
            onNavigate
        )

        SectionHeader(stringResource(R.string.tools_section_system))
        ToolRow(
            ToolEntry(R.string.nav_device_info, Icons.Default.Devices, Destinations.DeviceInfo.route),
            ToolEntry(R.string.nav_cache_cleaner, Icons.Default.CleaningServices, Destinations.CacheCleaner.route),
            onNavigate
        )
        ToolRow(
            ToolEntry(R.string.nav_storage, Icons.Default.Storage, Destinations.Storage.route),
            ToolEntry(R.string.nav_app_finder, Icons.Default.Apps, Destinations.AppFinder.route),
            onNavigate
        )

        SectionHeader(stringResource(R.string.tools_section_media))
        ToolRow(
            ToolEntry(R.string.nav_pdf_scanner, Icons.Default.PictureAsPdf, Destinations.PdfScanner.route),
            ToolEntry(R.string.nav_decibel_meter, Icons.Default.GraphicEq, Destinations.DecibelMeter.route),
            onNavigate
        )
        ToolRow(
            ToolEntry(R.string.nav_flashlight, Icons.Default.FlashlightOn, Destinations.Flashlight.route),
            null,
            onNavigate
        )

        SectionHeader(stringResource(R.string.tools_section_advanced))
        ToolRow(
            ToolEntry(R.string.nav_advanced_qr, Icons.Default.QrCode2, Destinations.AdvancedQr.route),
            ToolEntry(R.string.nav_advanced_barcode, Icons.Default.QrCodeScanner, Destinations.AdvancedBarcode.route, stub = true),
            onNavigate
        )
        ToolRow(
            ToolEntry(R.string.nav_advanced_text_editor, Icons.Default.TextFields, Destinations.AdvancedTextEditor.route),
            null,
            onNavigate
        )

        Spacer(Modifier.height(32.dp))
    }
}

private data class ToolEntry(
    val labelRes: Int,
    val icon: ImageVector,
    val route: String,
    val stub: Boolean = false
)

@Composable
private fun SectionHeader(text: String, accent: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = if (accent) colors.primary else colors.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
private fun ToolRow(
    left: ToolEntry,
    right: ToolEntry?,
    onNavigate: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ToolCard(left, onNavigate, Modifier.weight(1f))
        if (right != null) ToolCard(right, onNavigate, Modifier.weight(1f))
        else Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun ToolCard(
    entry: ToolEntry,
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
