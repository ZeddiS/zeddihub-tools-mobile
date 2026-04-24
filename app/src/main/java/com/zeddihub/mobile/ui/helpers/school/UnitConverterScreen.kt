package com.zeddihub.mobile.ui.helpers.school

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType.Companion.Decimal
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions

/**
 * Převodník jednotek. Lineární faktory v Map; teplota má vlastní funkci.
 */
private data class UnitDef(val code: String, val label: String, val factor: Double)

private val lengthUnits = listOf(
    UnitDef("mm", "mm — milimetr", 0.001),
    UnitDef("cm", "cm — centimetr", 0.01),
    UnitDef("m", "m — metr", 1.0),
    UnitDef("km", "km — kilometr", 1000.0),
    UnitDef("in", "in — palec", 0.0254),
    UnitDef("ft", "ft — stopa", 0.3048),
    UnitDef("yd", "yd — yard", 0.9144),
    UnitDef("mi", "mi — míle", 1609.344),
    UnitDef("nmi", "nmi — námořní míle", 1852.0),
)

private val massUnits = listOf(
    UnitDef("mg", "mg — miligram", 1e-6),
    UnitDef("g", "g — gram", 1e-3),
    UnitDef("dkg", "dkg — dekagram", 1e-2),
    UnitDef("kg", "kg — kilogram", 1.0),
    UnitDef("t", "t — tuna", 1000.0),
    UnitDef("oz", "oz — unce", 0.0283495),
    UnitDef("lb", "lb — libra", 0.453592),
)

private val volumeUnits = listOf(
    UnitDef("ml", "ml — mililitr", 1e-3),
    UnitDef("cl", "cl — centilitr", 1e-2),
    UnitDef("dl", "dl — decilitr", 1e-1),
    UnitDef("l", "l — litr", 1.0),
    UnitDef("hl", "hl — hektolitr", 100.0),
    UnitDef("m3", "m³ — metr krychlový", 1000.0),
    UnitDef("tsp", "lžička (5 ml)", 0.005),
    UnitDef("tbsp", "lžíce (15 ml)", 0.015),
    UnitDef("cup", "šálek (240 ml)", 0.240),
    UnitDef("galUS", "galon US", 3.78541),
)

private val areaUnits = listOf(
    UnitDef("mm2", "mm²", 1e-6),
    UnitDef("cm2", "cm²", 1e-4),
    UnitDef("dm2", "dm²", 1e-2),
    UnitDef("m2", "m²", 1.0),
    UnitDef("a", "ar (100 m²)", 100.0),
    UnitDef("ha", "hektar", 10_000.0),
    UnitDef("km2", "km²", 1_000_000.0),
    UnitDef("acre", "akr", 4046.8564224),
)

private val speedUnits = listOf(
    UnitDef("ms", "m/s", 1.0),
    UnitDef("kmh", "km/h", 1000.0 / 3600.0),
    UnitDef("mph", "mph", 1609.344 / 3600.0),
    UnitDef("kn", "uzel (kn)", 1852.0 / 3600.0),
    UnitDef("fts", "ft/s", 0.3048),
)

private val pressureUnits = listOf(
    UnitDef("Pa", "Pa", 1.0),
    UnitDef("hPa", "hPa", 100.0),
    UnitDef("kPa", "kPa", 1000.0),
    UnitDef("MPa", "MPa", 1_000_000.0),
    UnitDef("bar", "bar", 100_000.0),
    UnitDef("atm", "atm", 101_325.0),
    UnitDef("mmHg", "mmHg (torr)", 133.322),
    UnitDef("psi", "psi", 6894.757),
)

private val energyUnits = listOf(
    UnitDef("J", "J — joule", 1.0),
    UnitDef("kJ", "kJ", 1000.0),
    UnitDef("MJ", "MJ", 1_000_000.0),
    UnitDef("cal", "cal", 4.184),
    UnitDef("kcal", "kcal", 4184.0),
    UnitDef("Wh", "Wh", 3600.0),
    UnitDef("kWh", "kWh", 3_600_000.0),
    UnitDef("eV", "eV", 1.602176634e-19),
)

private val timeUnits = listOf(
    UnitDef("ms", "ms — milisekunda", 0.001),
    UnitDef("s", "s — sekunda", 1.0),
    UnitDef("min", "min — minuta", 60.0),
    UnitDef("h", "h — hodina", 3600.0),
    UnitDef("d", "d — den", 86_400.0),
    UnitDef("wk", "týden", 604_800.0),
    UnitDef("mo", "měsíc (30 dní)", 2_592_000.0),
    UnitDef("yr", "rok (365,25 dne)", 31_557_600.0),
)

private val dataUnits = listOf(
    UnitDef("b", "bit", 1.0),
    UnitDef("B", "byte (B)", 8.0),
    UnitDef("KB", "KB (1000 B)", 8_000.0),
    UnitDef("KiB", "KiB (1024 B)", 8_192.0),
    UnitDef("MB", "MB", 8e6),
    UnitDef("MiB", "MiB", 8.0 * 1024 * 1024),
    UnitDef("GB", "GB", 8e9),
    UnitDef("GiB", "GiB", 8.0 * 1024 * 1024 * 1024),
    UnitDef("TB", "TB", 8e12),
)

private val tempUnits = listOf("°C", "°F", "K")

private data class Category(val name: String, val units: List<UnitDef>?)

private val categories = listOf(
    Category("Délka", lengthUnits),
    Category("Hmotnost", massUnits),
    Category("Objem", volumeUnits),
    Category("Plocha", areaUnits),
    Category("Rychlost", speedUnits),
    Category("Tlak", pressureUnits),
    Category("Energie", energyUnits),
    Category("Čas", timeUnits),
    Category("Data", dataUnits),
    Category("Teplota", null), // special
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitConverterScreen(padding: PaddingValues) {
    var catIdx by remember { mutableStateOf(0) }
    val category = categories[catIdx]
    // Reset selections when category changes
    var fromIdx by remember(catIdx) { mutableStateOf(0) }
    var toIdx by remember(catIdx) { mutableStateOf(1) }
    var input by remember(catIdx) { mutableStateOf("1") }

    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DropdownBox(
            label = "Kategorie",
            options = categories.map { it.name },
            selectedIndex = catIdx,
            onSelect = { catIdx = it },
        )

        val fromLabels = category.units?.map { it.label } ?: tempUnits
        val toLabels = fromLabels

        DropdownBox("Z jednotky", fromLabels, fromIdx) { fromIdx = it }
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Hodnota") },
            keyboardOptions = KeyboardOptions(keyboardType = Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        DropdownBox("Na jednotku", toLabels, toIdx) { toIdx = it }

        val value = input.replace(',', '.').toDoubleOrNull()
        val result: Double? = if (value == null) null else {
            if (category.units == null) {
                convertTemperature(value, tempUnits[fromIdx], tempUnits[toIdx])
            } else {
                val u = category.units
                value * u[fromIdx].factor / u[toIdx].factor
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Výsledek", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = result?.let { formatNumber(it) } ?: "—",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
                IconButton(
                    onClick = {
                        result?.let { clipboard.setText(AnnotatedString(formatNumber(it))) }
                    },
                ) { Icon(Icons.Filled.ContentCopy, contentDescription = "Kopírovat") }
            }
        }
    }
}

private fun formatNumber(d: Double): String {
    if (!d.isFinite()) return "—"
    val abs = kotlin.math.abs(d)
    return when {
        abs != 0.0 && (abs < 1e-3 || abs >= 1e9) -> "%.6e".format(d)
        else -> {
            val s = "%.6f".format(d)
            s.trimEnd('0').trimEnd('.', ',')
        }
    }
}

private fun convertTemperature(value: Double, from: String, to: String): Double {
    val celsius = when (from) {
        "°C" -> value
        "°F" -> (value - 32.0) * 5.0 / 9.0
        "K" -> value - 273.15
        else -> value
    }
    return when (to) {
        "°C" -> celsius
        "°F" -> celsius * 9.0 / 5.0 + 32.0
        "K" -> celsius + 273.15
        else -> celsius
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownBox(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = options.getOrNull(selectedIndex).orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEachIndexed { i, item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onSelect(i)
                        expanded = false
                    },
                )
            }
        }
    }
}
