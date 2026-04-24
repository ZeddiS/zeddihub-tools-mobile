package com.zeddihub.mobile.ui.helpers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zeddihub.mobile.data.local.LanguageCode

/**
 * GHS hazard pictogram reference (per Regulation (EC) 1272/2008 CLP).
 *
 * Displays the nine standard pictograms as a 3×3 grid; tapping any tile
 * opens a bottom sheet with: code, full hazard class, and typical
 * example materials. Purely a read-only educational tool — no network
 * or permissions.
 *
 * We do not ship the official red-diamond SVGs (license considerations
 * + APK size); instead each tile uses a Material icon on the canonical
 * red/white diamond background so the pictogram is still recognizable.
 */
@Composable
fun HazardSignsScreen(
    padding: PaddingValues,
    language: LanguageCode = LanguageCode.CS
) {
    val colors = MaterialTheme.colorScheme
    var selected by remember { mutableStateOf<GhsPictogram?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(padding)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (language == LanguageCode.CS)
                "Piktogramy nebezpečí GHS (9 kategorií). Klepnutím zobrazíš detail."
            else
                "GHS hazard pictograms (9 categories). Tap a tile for details.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().height(360.dp)
        ) {
            items(GhsPictograms) { p ->
                PictogramTile(p = p, language = language) { selected = p }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    selected?.let { p ->
        PictogramDetailSheet(
            p = p,
            language = language,
            onDismiss = { selected = null }
        )
    }
}

@Composable
private fun PictogramTile(
    p: GhsPictogram,
    language: LanguageCode,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val title = if (language == LanguageCode.CS) p.titleCs else p.titleEn
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = colors.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Canonical GHS red diamond — rotated square with red border
            // and white fill. Holds the Material icon at its center.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                DiamondBadge(icon = p.icon)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = p.code,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurface,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

/** Red-diamond GHS-style badge. Rotated 45°, outlined red, white inside. */
@Composable
private fun DiamondBadge(icon: ImageVector) {
    val red = Color(0xFFE11D2E)
    Box(
        modifier = Modifier
            .size(68.dp)
            .graphicsLayerRotate(45f)
            .background(Color.White, RoundedCornerShape(6.dp))
            .border(4.dp, red, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Counter-rotate the icon so it sits upright inside the diamond.
        Box(modifier = Modifier.graphicsLayerRotate(-45f)) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/** Convenience wrapper since `.graphicsLayer(rotationZ = ...)` reads less nicely inline. */
private fun Modifier.graphicsLayerRotate(deg: Float): Modifier =
    this.graphicsLayer(rotationZ = deg)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PictogramDetailSheet(
    p: GhsPictogram,
    language: LanguageCode,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = MaterialTheme.colorScheme
    val title = if (language == LanguageCode.CS) p.titleCs else p.titleEn
    val hazardClass = if (language == LanguageCode.CS) p.hazardClassCs else p.hazardClassEn
    val examples = if (language == LanguageCode.CS) p.examplesCs else p.examplesEn

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                    DiamondBadge(icon = p.icon)
                }
                Spacer(Modifier.size(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = p.code,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = if (language == LanguageCode.CS) "Třída nebezpečí" else "Hazard class",
                style = MaterialTheme.typography.labelLarge,
                color = colors.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = hazardClass,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (language == LanguageCode.CS) "Typické příklady" else "Typical examples",
                style = MaterialTheme.typography.labelLarge,
                color = colors.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = examples,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ────────────────────────────────────────────────────────────────────────
// Data (static — no network). All text from UN GHS Rev.9 (2021).
// ────────────────────────────────────────────────────────────────────────

private data class GhsPictogram(
    val code: String,
    val icon: ImageVector,
    val titleCs: String,
    val titleEn: String,
    val hazardClassCs: String,
    val hazardClassEn: String,
    val examplesCs: String,
    val examplesEn: String
)

private val GhsPictograms = listOf(
    GhsPictogram(
        code = "GHS01",
        icon = Icons.Default.Bolt,
        titleCs = "Výbušniny",
        titleEn = "Explosive",
        hazardClassCs = "Výbušné látky a předměty; samovolně reagující látky typu A a B; organické peroxidy typu A a B.",
        hazardClassEn = "Explosives; self-reactive substances type A or B; organic peroxides type A or B.",
        examplesCs = "TNT, dynamit, roznětky, pyrotechnika, některé azidy.",
        examplesEn = "TNT, dynamite, detonators, fireworks, some azides."
    ),
    GhsPictogram(
        code = "GHS02",
        icon = Icons.Default.LocalFireDepartment,
        titleCs = "Hořlavé",
        titleEn = "Flammable",
        hazardClassCs = "Hořlavé plyny, aerosoly, kapaliny nebo tuhé látky; samozápalné látky; látky uvolňující hořlavé plyny při styku s vodou.",
        hazardClassEn = "Flammable gases, aerosols, liquids, or solids; pyrophoric substances; substances that emit flammable gas in contact with water.",
        examplesCs = "Benzín, etanol, aceton, propan, bílý fosfor, sodík.",
        examplesEn = "Gasoline, ethanol, acetone, propane, white phosphorus, sodium."
    ),
    GhsPictogram(
        code = "GHS03",
        icon = Icons.Default.PriorityHigh,
        titleCs = "Oxidační",
        titleEn = "Oxidizer",
        hazardClassCs = "Oxidující plyny, kapaliny nebo tuhé látky — zvyšují riziko požáru jiných látek tím, že uvolňují kyslík.",
        hazardClassEn = "Oxidizing gases, liquids, or solids — may enhance the fire of other substances by yielding oxygen.",
        examplesCs = "Peroxid vodíku (koncentrovaný), chlornan sodný, dusičnany, chlorečnany.",
        examplesEn = "Hydrogen peroxide (concentrated), sodium hypochlorite, nitrates, chlorates."
    ),
    GhsPictogram(
        code = "GHS04",
        icon = Icons.Default.Compress,
        titleCs = "Plyny pod tlakem",
        titleEn = "Gas under pressure",
        hazardClassCs = "Stlačené, zkapalněné nebo rozpuštěné plyny. Při zahřátí mohou vybuchnout; zkapalněné plyny mohou způsobit popáleniny chladem.",
        hazardClassEn = "Compressed, liquefied or dissolved gases. May explode when heated; liquefied gases may cause cold burns.",
        examplesCs = "Kyslíkové lahve, propan-butan, oxid uhličitý v láhvi, acetylen.",
        examplesEn = "Oxygen cylinders, LPG, CO₂ cylinders, acetylene."
    ),
    GhsPictogram(
        code = "GHS05",
        icon = Icons.Default.Science,
        titleCs = "Žíravé",
        titleEn = "Corrosive",
        hazardClassCs = "Žíravé pro kovy; způsobují vážné poleptání kůže a poškození očí.",
        hazardClassEn = "Corrosive to metals; causes severe skin burns and eye damage.",
        examplesCs = "Kyselina sírová, hydroxid sodný, kyselina chlorovodíková, čpavek (koncentrovaný).",
        examplesEn = "Sulfuric acid, sodium hydroxide, hydrochloric acid, concentrated ammonia."
    ),
    GhsPictogram(
        code = "GHS06",
        icon = Icons.Default.Dangerous,
        titleCs = "Akutní toxicita",
        titleEn = "Acute toxicity",
        hazardClassCs = "Akutní toxicita kategorie 1–3 — smrtelné nebo toxické při požití, styku s kůží nebo vdechnutí.",
        hazardClassEn = "Acute toxicity categories 1–3 — fatal or toxic if swallowed, in contact with skin, or inhaled.",
        examplesCs = "Kyanid draselný, arsen, methanol, fosgen, některé pesticidy.",
        examplesEn = "Potassium cyanide, arsenic, methanol, phosgene, some pesticides."
    ),
    GhsPictogram(
        code = "GHS07",
        icon = Icons.Default.Warning,
        titleCs = "Dráždivé / škodlivé",
        titleEn = "Irritant / Harmful",
        hazardClassCs = "Dráždí kůži a oči; zdraví škodlivé; senzibilizující pro kůži; akutní toxicita kategorie 4; narkotické účinky.",
        hazardClassEn = "Skin/eye irritation; harmful if swallowed/inhaled; skin sensitizer; acute toxicity cat. 4; narcotic effects.",
        examplesCs = "Čisticí prostředky, ředidla, některé laky, formaldehyd (nízká koncentrace).",
        examplesEn = "Cleaning products, thinners, some varnishes, formaldehyde (low concentration)."
    ),
    GhsPictogram(
        code = "GHS08",
        icon = Icons.Default.HealthAndSafety,
        titleCs = "Dlouhodobé zdravotní riziko",
        titleEn = "Serious health hazard",
        hazardClassCs = "Karcinogenita, mutagenita, toxicita pro reprodukci, senzibilizace dýchacích cest, aspirační toxicita, STOT (toxicita pro cílové orgány).",
        hazardClassEn = "Carcinogen, mutagen, reproductive toxicity, respiratory sensitization, aspiration toxicity, STOT (target-organ toxicity).",
        examplesCs = "Benzen, azbest, olovo (anorganické sloučeniny), terpentýn, některá rozpouštědla.",
        examplesEn = "Benzene, asbestos, lead (inorganic compounds), turpentine, some solvents."
    ),
    GhsPictogram(
        code = "GHS09",
        icon = Icons.Default.Eco,
        titleCs = "Nebezpečné pro životní prostředí",
        titleEn = "Environmental hazard",
        hazardClassCs = "Nebezpečné pro vodní prostředí (akutní nebo chronické) — toxické pro ryby, korýše, řasy.",
        hazardClassEn = "Hazardous to the aquatic environment (acute or chronic) — toxic to fish, crustaceans, algae.",
        examplesCs = "Některé pesticidy, rtuť, sloučeniny mědi, ropné produkty.",
        examplesEn = "Some pesticides, mercury, copper compounds, petroleum products."
    )
)
