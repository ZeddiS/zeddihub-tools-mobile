package com.zeddihub.mobile.ui.helpers.school

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardType.Companion.Decimal
import androidx.compose.ui.text.input.KeyboardType.Companion.Number
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions

private data class GradeEntry(val grade: String, val weight: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeCalculatorScreen(padding: PaddingValues) {
    var tab by remember { mutableStateOf(0) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
    ) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Průměr") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Co potřebuju") })
        }
        when (tab) {
            0 -> AverageSection()
            else -> NeededSection()
        }
    }
}

@Composable
private fun AverageSection() {
    val entries = remember {
        mutableStateListOf(
            GradeEntry("1", "1"),
            GradeEntry("2", "1"),
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Zadej známky (1–5) a jejich váhy (výchozí 1).",
            style = MaterialTheme.typography.bodyMedium,
        )
        entries.forEachIndexed { index, entry ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = entry.grade,
                    onValueChange = { entries[index] = entry.copy(grade = it.take(3)) },
                    label = { Text("Známka") },
                    keyboardOptions = KeyboardOptions(keyboardType = Decimal),
                    modifier = Modifier.width(130.dp),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = entry.weight,
                    onValueChange = { entries[index] = entry.copy(weight = it.take(3)) },
                    label = { Text("Váha") },
                    keyboardOptions = KeyboardOptions(keyboardType = Decimal),
                    modifier = Modifier.width(110.dp),
                    singleLine = true,
                )
                IconButton(
                    onClick = { if (entries.size > 1) entries.removeAt(index) },
                ) { Icon(Icons.Filled.Close, contentDescription = "Odebrat") }
            }
        }
        FilledTonalButton(
            onClick = { entries.add(GradeEntry("", "1")) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(" Přidat známku")
        }
        val parsed = entries.mapNotNull {
            val g = it.grade.replace(',', '.').toDoubleOrNull()
            val w = it.weight.replace(',', '.').toDoubleOrNull() ?: 1.0
            if (g != null && g in 1.0..5.0 && w > 0) g to w else null
        }
        val average = if (parsed.isEmpty()) null else {
            val top = parsed.sumOf { it.first * it.second }
            val bot = parsed.sumOf { it.second }
            top / bot
        }
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Vážený průměr", fontWeight = FontWeight.SemiBold)
                Text(
                    text = average?.let { "%.3f".format(it) } ?: "—",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    "Zaokrouhlená výsledná známka: " +
                        (average?.let { roundedCzechGrade(it).toString() } ?: "—"),
                )
                Text(
                    "Započítáno zadání: ${parsed.size} z ${entries.size}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun roundedCzechGrade(avg: Double): Int {
    // Czech convention: 1.00-1.50 → 1, 1.51-2.50 → 2, etc.
    return when {
        avg < 1.5 -> 1
        avg < 2.5 -> 2
        avg < 3.5 -> 3
        avg < 4.5 -> 4
        else -> 5
    }
}

@Composable
private fun NeededSection() {
    var current by remember { mutableStateOf("2.3") }
    var target by remember { mutableStateOf("2.0") }
    var existingWeight by remember { mutableStateOf("5") }
    var remaining by remember { mutableStateOf("2") }
    var remWeight by remember { mutableStateOf("1") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Spočítej, jaký průměr potřebuješ ze zbývajících testů, abys dosáhl cíle.",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = current, onValueChange = { current = it },
            label = { Text("Aktuální průměr") },
            keyboardOptions = KeyboardOptions(keyboardType = Decimal),
            modifier = Modifier.fillMaxWidth(), singleLine = true,
        )
        OutlinedTextField(
            value = existingWeight, onValueChange = { existingWeight = it },
            label = { Text("Součet vah stávajících známek") },
            keyboardOptions = KeyboardOptions(keyboardType = Decimal),
            modifier = Modifier.fillMaxWidth(), singleLine = true,
        )
        OutlinedTextField(
            value = target, onValueChange = { target = it },
            label = { Text("Cílový průměr") },
            keyboardOptions = KeyboardOptions(keyboardType = Decimal),
            modifier = Modifier.fillMaxWidth(), singleLine = true,
        )
        OutlinedTextField(
            value = remaining, onValueChange = { remaining = it },
            label = { Text("Počet zbývajících testů") },
            keyboardOptions = KeyboardOptions(keyboardType = Number),
            modifier = Modifier.fillMaxWidth(), singleLine = true,
        )
        OutlinedTextField(
            value = remWeight, onValueChange = { remWeight = it },
            label = { Text("Váha každého zbývajícího testu") },
            keyboardOptions = KeyboardOptions(keyboardType = Decimal),
            modifier = Modifier.fillMaxWidth(), singleLine = true,
        )

        val c = current.replace(',', '.').toDoubleOrNull()
        val t = target.replace(',', '.').toDoubleOrNull()
        val ew = existingWeight.replace(',', '.').toDoubleOrNull()
        val rc = remaining.toIntOrNull()
        val rw = remWeight.replace(',', '.').toDoubleOrNull()

        val result: String = if (c != null && t != null && ew != null && rc != null && rc > 0 && rw != null && rw > 0) {
            val totalWeight = ew + rc * rw
            val needed = (t * totalWeight - c * ew) / (rc * rw)
            when {
                needed > 5.0 -> "Nedosažitelné — potřebuješ průměr %.2f".format(needed)
                needed < 1.0 -> "Snadno dosažitelné — i průměr 1 stačí (vychází %.2f)".format(needed)
                else -> "Potřebuješ průměr %.2f z každého zbývajícího testu".format(needed)
            }
        } else "Zadej všechna čísla."

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(Modifier.padding(16.dp)) { Text(result, style = MaterialTheme.typography.titleMedium) }
        }
    }
}
