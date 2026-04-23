package com.zeddihub.mobile.ui.helpers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeddihub.mobile.R

/**
 * Periodic-table helper.
 *
 *  - Top: search bar (matches by Czech/English name, symbol or Z).
 *  - Main 18×7 grid, horizontally scrollable (phones are narrower than
 *    the table looks good at a readable cell size).
 *  - Below the main grid: the lanthanide (period 6) and actinide
 *    (period 7) strips. The main grid reserves a placeholder at col 3
 *    for both rows pointing at the strip (traditional IUPAC layout).
 *  - Tap an element to show a bottom-sheet detail (IUPAC 2021 mass,
 *    Pauling electronegativity, melting/boiling point, density).
 *
 * When the user types in the search field, any element that does NOT
 * match is rendered dimmed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodicTableScreen(padding: PaddingValues) {
    val elements = remember { PeriodicTable.elements }

    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Element?>(null) }

    val matches: (Element) -> Boolean = remember(query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) { _ -> true } else { e ->
            e.symbol.lowercase() == q ||
                e.z.toString() == q ||
                e.nameCs.lowercase().contains(q) ||
                e.nameEn.lowercase().contains(q)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Clear, null)
                    }
                }
            },
            placeholder = { Text(stringResource(R.string.pt_search)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // Main 7×18 grid (horizontally scrollable).
        val mainGrid = remember(elements) {
            val map = HashMap<Long, Element>()
            elements.forEach { e ->
                if (e.col in 1..18) map[key(e.period, e.col)] = e
            }
            map
        }
        val lanthanides = remember(elements) { elements.filter { it.group == PeriodicGroup.LANTHANIDE } }
        val actinides = remember(elements) { elements.filter { it.group == PeriodicGroup.ACTINIDE } }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.Top
        ) {
            Column {
                for (period in 1..7) {
                    Row {
                        for (col in 1..18) {
                            val cell = mainGrid[key(period, col)]
                            if (cell != null) {
                                ElementCell(
                                    element = cell,
                                    matches = matches(cell),
                                    onClick = { selected = cell }
                                )
                            } else if ((period == 6 || period == 7) && col == 3) {
                                PlaceholderCell(
                                    label = if (period == 6) "57-71" else "89-103",
                                    group = if (period == 6) PeriodicGroup.LANTHANIDE else PeriodicGroup.ACTINIDE
                                )
                            } else {
                                EmptyCell()
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Lanthanide strip (indent by 3 cells to align under col 3).
                Row {
                    Spacer(Modifier.width((CELL_SIZE_DP * 3).dp))
                    lanthanides.forEach { e ->
                        ElementCell(element = e, matches = matches(e), onClick = { selected = e })
                    }
                }
                // Actinide strip.
                Row {
                    Spacer(Modifier.width((CELL_SIZE_DP * 3).dp))
                    actinides.forEach { e ->
                        ElementCell(element = e, matches = matches(e), onClick = { selected = e })
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        GroupLegend()

        Spacer(Modifier.height(24.dp))
    }

    if (selected != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { selected = null },
            sheetState = sheetState
        ) {
            ElementDetail(element = selected!!)
        }
    }
}

private const val CELL_SIZE_DP = 44

private fun key(period: Int, col: Int): Long = (period.toLong() shl 8) or col.toLong()

@Composable
private fun ElementCell(element: Element, matches: Boolean, onClick: () -> Unit) {
    val base = colorFor(element.group)
    val bg = if (matches) base else base.copy(alpha = 0.2f)
    val fg = if (matches) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)

    Surface(
        color = bg,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .padding(1.dp)
            .size(CELL_SIZE_DP.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = element.z.toString(),
                fontSize = 8.sp,
                color = fg,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Text(
                text = element.symbol,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = fg,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = if (element.mass.isNaN()) "—" else formatMass(element.mass),
                fontSize = 7.sp,
                color = fg,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PlaceholderCell(label: String, group: PeriodicGroup) {
    Surface(
        color = colorFor(group).copy(alpha = 0.5f),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .padding(1.dp)
            .size(CELL_SIZE_DP.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun EmptyCell() {
    Spacer(
        modifier = Modifier
            .padding(1.dp)
            .size(CELL_SIZE_DP.dp)
    )
}

@Composable
private fun GroupLegend() {
    val items = listOf(
        PeriodicGroup.ALKALI_METAL to R.string.pt_group_alkali,
        PeriodicGroup.ALKALINE_EARTH to R.string.pt_group_alkaline,
        PeriodicGroup.TRANSITION to R.string.pt_group_transition,
        PeriodicGroup.POST_TRANSITION to R.string.pt_group_post_transition,
        PeriodicGroup.METALLOID to R.string.pt_group_metalloid,
        PeriodicGroup.NONMETAL to R.string.pt_group_nonmetal,
        PeriodicGroup.HALOGEN to R.string.pt_group_halogen,
        PeriodicGroup.NOBLE_GAS to R.string.pt_group_noble,
        PeriodicGroup.LANTHANIDE to R.string.pt_group_lanthanide,
        PeriodicGroup.ACTINIDE to R.string.pt_group_actinide
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { (g, r) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(colorFor(g), RoundedCornerShape(3.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(r), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ElementDetail(element: Element) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = colorFor(element.group),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        element.symbol,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(element.nameCs, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(element.nameEn, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(16.dp))

        DetailRow(stringResource(R.string.pt_detail_z), element.z.toString())
        DetailRow(
            stringResource(R.string.pt_detail_mass),
            if (element.mass.isNaN()) "—" else "${formatMass(element.mass)} u"
        )
        DetailRow(stringResource(R.string.pt_detail_group), groupName(element.group))
        DetailRow(stringResource(R.string.pt_detail_phase), phaseName(element.phase))
        DetailRow(
            stringResource(R.string.pt_detail_electronegativity),
            if (element.en <= 0.0) "—" else "%.2f".format(element.en)
        )
        DetailRow(
            stringResource(R.string.pt_detail_density),
            if (element.density.isNaN()) "—" else "${formatNumber(element.density, 4)} g/cm³"
        )
        DetailRow(
            stringResource(R.string.pt_detail_melting),
            if (element.melt.isNaN()) "—" else "${formatNumber(element.melt, 2)} °C"
        )
        DetailRow(
            stringResource(R.string.pt_detail_boiling),
            if (element.boil.isNaN()) "—" else "${formatNumber(element.boil, 2)} °C"
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun groupName(g: PeriodicGroup): String = when (g) {
    PeriodicGroup.ALKALI_METAL    -> stringResource(R.string.pt_group_alkali)
    PeriodicGroup.ALKALINE_EARTH  -> stringResource(R.string.pt_group_alkaline)
    PeriodicGroup.TRANSITION      -> stringResource(R.string.pt_group_transition)
    PeriodicGroup.POST_TRANSITION -> stringResource(R.string.pt_group_post_transition)
    PeriodicGroup.METALLOID       -> stringResource(R.string.pt_group_metalloid)
    PeriodicGroup.NONMETAL        -> stringResource(R.string.pt_group_nonmetal)
    PeriodicGroup.HALOGEN         -> stringResource(R.string.pt_group_halogen)
    PeriodicGroup.NOBLE_GAS       -> stringResource(R.string.pt_group_noble)
    PeriodicGroup.LANTHANIDE      -> stringResource(R.string.pt_group_lanthanide)
    PeriodicGroup.ACTINIDE        -> stringResource(R.string.pt_group_actinide)
    PeriodicGroup.UNKNOWN         -> "—"
}

private fun phaseName(phase: String): String = when (phase) {
    "s" -> "solid"
    "l" -> "liquid"
    "g" -> "gas"
    else -> "—"
}

/**
 * Color palette for element groups. Soft pastel-ish colours that play
 * well with both light and dark themes since the cell text uses
 * `onSurface` from the current scheme.
 */
private fun colorFor(group: PeriodicGroup): Color = when (group) {
    PeriodicGroup.ALKALI_METAL    -> Color(0xFFFFCDD2)
    PeriodicGroup.ALKALINE_EARTH  -> Color(0xFFFFE0B2)
    PeriodicGroup.TRANSITION      -> Color(0xFFB3E5FC)
    PeriodicGroup.POST_TRANSITION -> Color(0xFFC5CAE9)
    PeriodicGroup.METALLOID       -> Color(0xFFD1C4E9)
    PeriodicGroup.NONMETAL        -> Color(0xFFC8E6C9)
    PeriodicGroup.HALOGEN         -> Color(0xFFDCEDC8)
    PeriodicGroup.NOBLE_GAS       -> Color(0xFFB2EBF2)
    PeriodicGroup.LANTHANIDE      -> Color(0xFFF8BBD0)
    PeriodicGroup.ACTINIDE        -> Color(0xFFFFCCBC)
    PeriodicGroup.UNKNOWN         -> Color(0xFFE0E0E0)
}

private fun formatMass(mass: Double): String {
    return if (mass >= 100) "%.0f".format(mass) else "%.2f".format(mass)
}

private fun formatNumber(v: Double, decimals: Int): String {
    // Trim trailing zeros to keep the detail sheet tidy.
    val raw = ("%." + decimals + "f").format(v)
    return raw.trimEnd('0').trimEnd('.', ',').ifEmpty { "0" }
}
