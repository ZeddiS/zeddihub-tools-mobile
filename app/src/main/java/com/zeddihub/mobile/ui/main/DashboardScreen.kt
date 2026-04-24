package com.zeddihub.mobile.ui.main

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.zeddihub.mobile.R
import com.zeddihub.mobile.data.local.LanguageCode
import com.zeddihub.mobile.data.remote.dto.HomeNewsDto
import com.zeddihub.mobile.data.remote.dto.HomeShortcutDto
import com.zeddihub.mobile.ui.navigation.Destinations

@Composable
fun DashboardScreen(
    padding: PaddingValues,
    isAdmin: Boolean,
    onNavigate: (String) -> Unit,
    viewModel: HomeConfigViewModel = hiltViewModel()
) {
    val colors = MaterialTheme.colorScheme
    val ctx = LocalContext.current

    val homeConfig by viewModel.config.collectAsState()
    val language by viewModel.language.collectAsState()

    var ssid by remember { mutableStateOf<String?>(null) }
    var ipv4 by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val info = readNetworkInfo(ctx)
        ssid = info.first
        ipv4 = info.second
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(padding)
            .verticalScroll(rememberScrollState())
    ) {
        // Welcome hero
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(colors.primary.copy(alpha = 0.18f), Color.Transparent)
                    )
                )
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.dashboard_home_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground
                )
            }
        }

        // Network card
        SectionLabel(stringResource(R.string.dashboard_section_network))
        NetworkCard(
            ssid = ssid ?: stringResource(R.string.dashboard_network_unknown),
            ipv4 = ipv4 ?: stringResource(R.string.dashboard_network_unknown),
            onClick = { onNavigate(Destinations.MyNetwork.route) }
        )

        Spacer(Modifier.height(14.dp))

        // Quick shortcuts — rendered from the admin-managed home config.
        // Hidden/unresolvable entries are silently skipped; we only
        // show a section header if there's at least one usable item so
        // the admin can hide the whole row by emptying the list.
        val visibleShortcuts = homeConfig.shortcuts
            .filter { it.visible }
            .mapNotNull { sc ->
                resolveNavRoute(sc.navId)?.let { route -> sc to route }
            }
        if (visibleShortcuts.isNotEmpty()) {
            SectionLabel(stringResource(R.string.dashboard_section_shortcuts))
            DynamicShortcutsRow(
                items = visibleShortcuts,
                language = language,
                onNavigate = onNavigate
            )
        }

        Spacer(Modifier.height(14.dp))

        // Highlight row — showcases the new v0.5.8 tools
        SectionLabel(stringResource(R.string.tools_section_new))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HighlightTile(
                title = stringResource(R.string.nav_frequency_generator),
                icon = Icons.Default.GraphicEq,
                onClick = { onNavigate(Destinations.FrequencyGenerator.route) },
                modifier = Modifier.weight(1f)
            )
            HighlightTile(
                title = stringResource(R.string.nav_speaker_cleaner),
                icon = Icons.Default.Speaker,
                onClick = { onNavigate(Destinations.SpeakerCleaner.route) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(14.dp))

        // Servers row (compact)
        SectionLabel(stringResource(R.string.nav_servers))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { onNavigate(Destinations.Servers.route) },
            color = colors.surface,
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Storage, contentDescription = null, tint = colors.primary)
                Spacer(Modifier.size(12.dp))
                Text(
                    text = stringResource(R.string.nav_servers),
                    color = colors.onSurface,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // News — admin-managed. Pinned items float to the top; the
        // relative order of the rest is preserved.
        SectionLabel(stringResource(R.string.dashboard_section_news))
        val sortedNews = remember(homeConfig.news) {
            homeConfig.news.sortedByDescending { it.pinned }
        }
        if (sortedNews.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = colors.surface,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.dashboard_news_empty),
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sortedNews.forEach { item ->
                    NewsCard(item = item, language = language)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun NetworkCard(ssid: String, ipv4: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        color = colors.surface,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(colors.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Wifi, contentDescription = null, tint = colors.primary)
            }
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ssid,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${stringResource(R.string.dashboard_network_ip)}: $ipv4",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuickTile(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .height(92.dp)
            .clickable(onClick = onClick),
        color = colors.surface,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = tint ?: colors.primary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HighlightTile(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .height(108.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(colors.primary.copy(alpha = 0.30f), colors.tertiary.copy(alpha = 0.20f))
                    )
                )
                .padding(14.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(28.dp).align(Alignment.TopStart)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}

/**
 * Renders the admin-configured shortcut list as a horizontally-scrolling
 * row of tiles. Keeps each tile a fixed width so arbitrarily-long lists
 * still fit on narrow screens (4+ shortcuts used to squeeze into one
 * screen; now we scroll instead of shrinking).
 */
@Composable
private fun DynamicShortcutsRow(
    items: List<Pair<HomeShortcutDto, String>>,
    language: LanguageCode,
    onNavigate: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { (sc, route) ->
            val label = pickLocalized(language, sc.labelCs, sc.labelEn)
            QuickTile(
                label = label,
                icon = resolveIcon(sc.icon),
                tint = parseHexColor(sc.color, MaterialTheme.colorScheme.primary),
                onClick = { onNavigate(route) },
                modifier = Modifier.width(104.dp)
            )
        }
    }
}

/**
 * Single news card. Non-empty `url` makes the whole card clickable and
 * open in the system browser; otherwise the card is inert. Body text
 * is capped to avoid pushing the rest of the Dashboard off-screen.
 */
@Composable
private fun NewsCard(item: HomeNewsDto, language: LanguageCode) {
    val colors = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val title = pickLocalized(language, item.titleCs, item.titleEn)
    val body  = pickLocalized(language, item.bodyCs,  item.bodyEn)
    val hasUrl = item.url.isNotBlank()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (hasUrl) it.clickable { openUrl(ctx, item.url) } else it },
        color = colors.surface,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.pinned) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.size(6.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (item.date.isNotBlank()) {
                    Text(
                        text = item.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
            if (body.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = body.take(400),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

private fun openUrl(ctx: android.content.Context, url: String) {
    runCatching {
        ctx.startActivity(
            android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse(url)
            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

/** Returns the CS version when the app language is CS (and CS is non-empty),
 *  otherwise falls back to EN, and finally to whichever is non-empty. */
private fun pickLocalized(language: LanguageCode, cs: String, en: String): String {
    return when {
        language == LanguageCode.CS && cs.isNotBlank() -> cs
        language == LanguageCode.EN && en.isNotBlank() -> en
        cs.isNotBlank() -> cs
        else -> en
    }
}

/** Returns (ssid, ipv4) best-effort. Falls back to null for unknown. */
private fun readNetworkInfo(ctx: Context): Pair<String?, String?> {
    return try {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val active: Network? = cm?.activeNetwork
        val caps: NetworkCapabilities? = active?.let { cm.getNetworkCapabilities(it) }
        val link: LinkProperties? = active?.let { cm.getLinkProperties(it) }

        val ip = link?.linkAddresses?.firstOrNull { addr ->
            val host = addr.address?.hostAddress ?: return@firstOrNull false
            !host.contains(':') && host != "127.0.0.1"
        }?.address?.hostAddress

        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val ssid = if (isWifi) {
            @Suppress("DEPRECATION")
            val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wm?.connectionInfo?.ssid?.trim('"')?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
        } else {
            when {
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Mobilní data"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
                else -> null
            }
        }
        ssid to ip
    } catch (t: Throwable) {
        null to null
    }
}

@Suppress("unused")
private val buildFlagIsAtLeastR = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
