package com.zeddihub.mobile.ui.helpers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zeddihub.mobile.R
import kotlin.math.sqrt

/**
 * Elektrikářské kalkulačky. Čtyři záložky:
 *  1. Ohmův zákon (U = I·R) — vyplň libovolné dvě a třetí se dopočítá.
 *  2. Výkon (P = U·I, P = I²·R, P = U²/R).
 *  3. Třífázový výkon (P = √3 · U · I · cosφ).
 *  4. Průřez vodiče podle úbytku napětí (ΔU%) pro Cu/Al, 1f/3f.
 *
 * Intentionally zero-persistence: vstupní pole jsou v lokálním
 * Composable state. Výpočty bez jakékoliv sítě.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElectricianScreen(padding: PaddingValues) {
    val colors = MaterialTheme.colorScheme
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.elec_tab_ohm),
        stringResource(R.string.elec_tab_power),
        stringResource(R.string.elec_tab_3ph),
        stringResource(R.string.elec_tab_wire)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(padding)
    ) {
        PrimaryScrollableTabRow(selectedTabIndex = tab) {
            tabs.forEachIndexed { i, label ->
                Tab(
                    selected = tab == i,
                    onClick = { tab = i },
                    text = { Text(label) }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            when (tab) {
                0 -> OhmPanel()
                1 -> PowerPanel()
                2 -> ThreePhasePanel()
                else -> WireGaugePanel()
            }
        }
    }
}

// ── Ohm's law ─────────────────────────────────────────────────────────────

@Composable
private fun OhmPanel() {
    val colors = MaterialTheme.colorScheme
    var u by remember { mutableStateOf("") }
    var i by remember { mutableStateOf("") }
    var r by remember { mutableStateOf("") }

    Text(
        stringResource(R.string.elec_hint_fill_two),
        style = MaterialTheme.typography.bodySmall,
        color = colors.onSurfaceVariant
    )
    Spacer(Modifier.padding(4.dp))

    val uVal = u.replace(',', '.').toDoubleOrNull()
    val iVal = i.replace(',', '.').toDoubleOrNull()
    val rVal = r.replace(',', '.').toDoubleOrNull()

    // Derive the missing one.
    val calcU = if (uVal == null && iVal != null && rVal != null) iVal * rVal else null
    val calcI = if (iVal == null && uVal != null && rVal != null && rVal != 0.0) uVal / rVal else null
    val calcR = if (rVal == null && uVal != null && iVal != null && iVal != 0.0) uVal / iVal else null

    NumberField(
        label = stringResource(R.string.elec_voltage),
        value = if (calcU != null) fmt(calcU) else u,
        onChange = { u = it },
        readOnly = calcU != null
    )
    Spacer(Modifier.padding(4.dp))
    NumberField(
        label = stringResource(R.string.elec_current),
        value = if (calcI != null) fmt(calcI) else i,
        onChange = { i = it },
        readOnly = calcI != null
    )
    Spacer(Modifier.padding(4.dp))
    NumberField(
        label = stringResource(R.string.elec_resistance),
        value = if (calcR != null) fmt(calcR) else r,
        onChange = { r = it },
        readOnly = calcR != null
    )

    if (uVal != null && iVal != null) {
        Spacer(Modifier.padding(8.dp))
        ResultSurface("P = U · I = ${fmt(uVal * iVal)} W")
    }
}

// ── Power ────────────────────────────────────────────────────────────────

@Composable
private fun PowerPanel() {
    var u by remember { mutableStateOf("") }
    var i by remember { mutableStateOf("") }
    var r by remember { mutableStateOf("") }

    Text(
        stringResource(R.string.elec_hint_fill_two),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.padding(4.dp))

    NumberField(stringResource(R.string.elec_voltage), u) { u = it }
    Spacer(Modifier.padding(4.dp))
    NumberField(stringResource(R.string.elec_current), i) { i = it }
    Spacer(Modifier.padding(4.dp))
    NumberField(stringResource(R.string.elec_resistance), r) { r = it }

    val uv = u.replace(',', '.').toDoubleOrNull()
    val iv = i.replace(',', '.').toDoubleOrNull()
    val rv = r.replace(',', '.').toDoubleOrNull()

    Spacer(Modifier.padding(8.dp))
    if (uv != null && iv != null) ResultSurface("P = U · I = ${fmt(uv * iv)} W")
    if (iv != null && rv != null) ResultSurface("P = I² · R = ${fmt(iv * iv * rv)} W")
    if (uv != null && rv != null && rv != 0.0) ResultSurface("P = U² / R = ${fmt(uv * uv / rv)} W")
}

// ── 3-phase power ─────────────────────────────────────────────────────────

@Composable
private fun ThreePhasePanel() {
    var u by remember { mutableStateOf("400") }
    var i by remember { mutableStateOf("") }
    var cosphi by remember { mutableStateOf("0.95") }

    NumberField(stringResource(R.string.elec_voltage), u) { u = it }
    Spacer(Modifier.padding(4.dp))
    NumberField(stringResource(R.string.elec_current), i) { i = it }
    Spacer(Modifier.padding(4.dp))
    NumberField(stringResource(R.string.elec_cosphi), cosphi) { cosphi = it }

    val uv = u.replace(',', '.').toDoubleOrNull()
    val iv = i.replace(',', '.').toDoubleOrNull()
    val cv = cosphi.replace(',', '.').toDoubleOrNull()

    if (uv != null && iv != null && cv != null) {
        val p = sqrt(3.0) * uv * iv * cv
        val s = sqrt(3.0) * uv * iv
        Spacer(Modifier.padding(8.dp))
        ResultSurface("P = √3 · U · I · cosφ = ${fmt(p)} W")
        Spacer(Modifier.padding(4.dp))
        ResultSurface("S = √3 · U · I = ${fmt(s)} VA")
        Spacer(Modifier.padding(4.dp))
        ResultSurface("Q = √(S² − P²) = ${fmt(sqrt((s * s - p * p).coerceAtLeast(0.0)))} var")
    }
}

// ── Wire gauge (conductor cross-section) ──────────────────────────────────

/**
 * ΔU = (k · L · I · cosφ) / (γ · S)  →  S = (k · L · I · cosφ) / (γ · ΔU)
 * where k = 2 for single-phase, √3 for three-phase,
 *       γ is the conductivity (Cu ≈ 56, Al ≈ 35 m/(Ω·mm²)),
 *       ΔU is the permitted voltage drop in volts.
 *
 * After the numeric result we round UP to the next standard CSA
 * from the IEC 60228 series.
 */
@Composable
private fun WireGaugePanel() {
    var voltage by remember { mutableStateOf("230") }
    var current by remember { mutableStateOf("16") }
    var length by remember { mutableStateOf("30") }
    var drop by remember { mutableStateOf("3") }
    var cosphi by remember { mutableStateOf("0.95") }
    var threePhase by remember { mutableStateOf(false) }
    var copper by remember { mutableStateOf(true) }

    NumberField(stringResource(R.string.elec_voltage), voltage) { voltage = it }
    Spacer(Modifier.padding(4.dp))
    NumberField(stringResource(R.string.elec_current), current) { current = it }
    Spacer(Modifier.padding(4.dp))
    NumberField(stringResource(R.string.elec_length), length) { length = it }
    Spacer(Modifier.padding(4.dp))
    NumberField(stringResource(R.string.elec_max_drop), drop) { drop = it }
    Spacer(Modifier.padding(4.dp))
    NumberField(stringResource(R.string.elec_cosphi), cosphi) { cosphi = it }

    Spacer(Modifier.padding(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ToggleChip(
            label = stringResource(R.string.elec_phase_single),
            selected = !threePhase,
            onClick = { threePhase = false }
        )
        ToggleChip(
            label = stringResource(R.string.elec_phase_three),
            selected = threePhase,
            onClick = { threePhase = true }
        )
    }
    Spacer(Modifier.padding(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ToggleChip(
            label = stringResource(R.string.elec_material_cu),
            selected = copper,
            onClick = { copper = true }
        )
        ToggleChip(
            label = stringResource(R.string.elec_material_al),
            selected = !copper,
            onClick = { copper = false }
        )
    }

    val uv = voltage.replace(',', '.').toDoubleOrNull()
    val iv = current.replace(',', '.').toDoubleOrNull()
    val lv = length.replace(',', '.').toDoubleOrNull()
    val dv = drop.replace(',', '.').toDoubleOrNull()
    val cv = cosphi.replace(',', '.').toDoubleOrNull()

    if (uv != null && iv != null && lv != null && dv != null && cv != null) {
        val k = if (threePhase) sqrt(3.0) else 2.0
        val gamma = if (copper) 56.0 else 35.0
        val deltaU = uv * dv / 100.0
        if (deltaU > 0.0) {
            val s = (k * lv * iv * cv) / (gamma * deltaU)
            val standard = nextStandardCsa(s)
            Spacer(Modifier.padding(10.dp))
            ResultSurface(
                stringResource(R.string.elec_suggested_cs, fmt(standard), fmt(s))
            )
        }
    }
}

private fun nextStandardCsa(csa: Double): Double {
    val standards = listOf(0.5, 0.75, 1.0, 1.5, 2.5, 4.0, 6.0, 10.0, 16.0, 25.0, 35.0, 50.0, 70.0, 95.0, 120.0, 150.0, 185.0, 240.0, 300.0, 400.0)
    return standards.firstOrNull { it >= csa } ?: standards.last()
}

// ── Shared UI helpers ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NumberField(
    label: String,
    value: String,
    readOnly: Boolean = false,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { c -> c.isDigit() || c == '.' || c == ',' || c == '-' }) },
        label = { Text(label) },
        readOnly = readOnly,
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToggleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
private fun ResultSurface(text: String) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surfaceVariant,
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(12.dp),
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface
        )
    }
}

private fun fmt(v: Double): String {
    if (!v.isFinite()) return "—"
    val abs = kotlin.math.abs(v)
    return when {
        abs >= 1000.0 -> "%.0f".format(v)
        abs >= 10.0 -> "%.2f".format(v)
        abs >= 1.0 -> "%.3f".format(v)
        else -> "%.4f".format(v)
    }
}
