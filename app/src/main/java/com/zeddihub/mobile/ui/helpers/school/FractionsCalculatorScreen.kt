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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType.Companion.Decimal
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FractionsCalculatorScreen(padding: PaddingValues) {
    var tab by remember { mutableStateOf(0) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
    ) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Zlomky") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Poměr") })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Procenta") })
        }
        when (tab) {
            0 -> FractionsTab()
            1 -> RatioTab()
            else -> PercentTab()
        }
    }
}

/* ---------- Fractions ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FractionsTab() {
    var n1 by remember { mutableStateOf("1") }
    var d1 by remember { mutableStateOf("2") }
    var n2 by remember { mutableStateOf("1") }
    var d2 by remember { mutableStateOf("3") }
    val ops = listOf("+", "−", "×", "÷")
    var opIdx by remember { mutableStateOf(0) }
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Zadej čitatele a jmenovatele obou zlomků.", style = MaterialTheme.typography.bodyMedium)

        FractionInput("Zlomek 1", n1, d1, { n1 = it }, { d1 = it })

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = ops[opIdx],
                onValueChange = {},
                readOnly = true,
                label = { Text("Operace") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                ops.forEachIndexed { i, op ->
                    DropdownMenuItem(text = { Text(op) }, onClick = {
                        opIdx = i; expanded = false
                    })
                }
            }
        }

        FractionInput("Zlomek 2", n2, d2, { n2 = it }, { d2 = it })

        val a = n1.toLongOrNull()
        val b = d1.toLongOrNull()
        val c = n2.toLongOrNull()
        val d = d2.toLongOrNull()

        val result: Pair<Long, Long>? = if (a != null && b != null && c != null && d != null && b != 0L && d != 0L) {
            when (opIdx) {
                0 -> a * d + c * b to b * d
                1 -> a * d - c * b to b * d
                2 -> a * c to b * d
                else -> if (c != 0L) a * d to b * c else null
            }?.let { simplify(it.first, it.second) }
        } else null

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Výsledek", fontWeight = FontWeight.SemiBold)
                if (result == null) Text("—", style = MaterialTheme.typography.headlineSmall)
                else {
                    val (rn, rd) = result
                    val mixed = if (rd != 0L && abs(rn) >= abs(rd)) {
                        val whole = rn / rd
                        val rem = rn % rd
                        if (rem == 0L) "$whole" else "$whole a ${abs(rem)}/${abs(rd)}"
                    } else null
                    Text("$rn / $rd", style = MaterialTheme.typography.headlineSmall)
                    if (mixed != null) Text("= $mixed")
                    if (rd != 0L) Text("≈ %.6f".format(rn.toDouble() / rd.toDouble()))
                }
            }
        }
    }
}

@Composable
private fun FractionInput(
    label: String,
    n: String,
    d: String,
    onN: (String) -> Unit,
    onD: (String) -> Unit,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = n, onValueChange = onN,
                label = { Text("Čitatel") },
                keyboardOptions = KeyboardOptions(keyboardType = Decimal),
                modifier = Modifier.weight(1f), singleLine = true,
            )
            OutlinedTextField(
                value = d, onValueChange = onD,
                label = { Text("Jmenovatel") },
                keyboardOptions = KeyboardOptions(keyboardType = Decimal),
                modifier = Modifier.weight(1f), singleLine = true,
            )
        }
    }
}

private fun gcd(a: Long, b: Long): Long {
    var x = abs(a); var y = abs(b)
    while (y != 0L) { val t = y; y = x % y; x = t }
    return if (x == 0L) 1 else x
}

private fun simplify(n: Long, d: Long): Pair<Long, Long> {
    if (d == 0L) return n to d
    val g = gcd(n, d)
    val sign = if (d < 0) -1 else 1
    return (n / g) * sign to (d / g) * sign
}

/* ---------- Ratio ---------- */

@Composable
private fun RatioTab() {
    var a by remember { mutableStateOf("2") }
    var b by remember { mutableStateOf("3") }
    var c by remember { mutableStateOf("10") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("a : b = c : ?  —  zadej a, b, c a spočítám čtvrté číslo.", style = MaterialTheme.typography.bodyMedium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = a, onValueChange = { a = it }, label = { Text("a") },
                keyboardOptions = KeyboardOptions(keyboardType = Decimal),
                modifier = Modifier.weight(1f), singleLine = true,
            )
            OutlinedTextField(
                value = b, onValueChange = { b = it }, label = { Text("b") },
                keyboardOptions = KeyboardOptions(keyboardType = Decimal),
                modifier = Modifier.weight(1f), singleLine = true,
            )
        }
        OutlinedTextField(
            value = c, onValueChange = { c = it }, label = { Text("c") },
            keyboardOptions = KeyboardOptions(keyboardType = Decimal),
            modifier = Modifier.fillMaxWidth(), singleLine = true,
        )
        val av = a.replace(',', '.').toDoubleOrNull()
        val bv = b.replace(',', '.').toDoubleOrNull()
        val cv = c.replace(',', '.').toDoubleOrNull()
        val dv = if (av != null && bv != null && cv != null && av != 0.0) cv * bv / av else null

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Čtvrté číslo (d)", fontWeight = FontWeight.SemiBold)
                Text(dv?.let { "%.6g".format(it) } ?: "—", style = MaterialTheme.typography.headlineSmall)
                Text("Platí $a : $b = $c : ${dv?.let { "%.4f".format(it) } ?: "?"}")
            }
        }
    }
}

/* ---------- Percent ---------- */

@Composable
private fun PercentTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PercentBlock1()
        PercentBlock2()
        PercentBlock3()
        PercentBlock4()
    }
}

@Composable
private fun PercentBlock1() {
    var base by remember { mutableStateOf("200") }
    var pct by remember { mutableStateOf("15") }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Kolik je X % z Y?", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pct, onValueChange = { pct = it }, label = { Text("X %") },
                    keyboardOptions = KeyboardOptions(keyboardType = Decimal),
                    modifier = Modifier.weight(1f), singleLine = true,
                )
                OutlinedTextField(
                    value = base, onValueChange = { base = it }, label = { Text("Y") },
                    keyboardOptions = KeyboardOptions(keyboardType = Decimal),
                    modifier = Modifier.weight(1f), singleLine = true,
                )
            }
            val p = pct.replace(',', '.').toDoubleOrNull()
            val y = base.replace(',', '.').toDoubleOrNull()
            val r = if (p != null && y != null) p * y / 100.0 else null
            Text("= ${r?.let { "%.4f".format(it) } ?: "—"}")
        }
    }
}

@Composable
private fun PercentBlock2() {
    var part by remember { mutableStateOf("30") }
    var whole by remember { mutableStateOf("120") }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Kolik % je X z Y?", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = part, onValueChange = { part = it }, label = { Text("X") },
                    keyboardOptions = KeyboardOptions(keyboardType = Decimal),
                    modifier = Modifier.weight(1f), singleLine = true,
                )
                OutlinedTextField(
                    value = whole, onValueChange = { whole = it }, label = { Text("Y") },
                    keyboardOptions = KeyboardOptions(keyboardType = Decimal),
                    modifier = Modifier.weight(1f), singleLine = true,
                )
            }
            val x = part.replace(',', '.').toDoubleOrNull()
            val y = whole.replace(',', '.').toDoubleOrNull()
            val r = if (x != null && y != null && y != 0.0) x / y * 100.0 else null
            Text("= ${r?.let { "%.4f".format(it) } ?: "—"} %")
        }
    }
}

@Composable
private fun PercentBlock3() {
    var a by remember { mutableStateOf("80") }
    var b by remember { mutableStateOf("100") }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Změna v %", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = a, onValueChange = { a = it }, label = { Text("Z hodnoty") },
                    keyboardOptions = KeyboardOptions(keyboardType = Decimal),
                    modifier = Modifier.weight(1f), singleLine = true,
                )
                OutlinedTextField(
                    value = b, onValueChange = { b = it }, label = { Text("Na hodnotu") },
                    keyboardOptions = KeyboardOptions(keyboardType = Decimal),
                    modifier = Modifier.weight(1f), singleLine = true,
                )
            }
            val av = a.replace(',', '.').toDoubleOrNull()
            val bv = b.replace(',', '.').toDoubleOrNull()
            val r = if (av != null && bv != null && av != 0.0) (bv - av) / av * 100.0 else null
            Text("= ${r?.let { "%+.4f".format(it) } ?: "—"} %")
        }
    }
}

@Composable
private fun PercentBlock4() {
    var base by remember { mutableStateOf("500") }
    var pct by remember { mutableStateOf("20") }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Přidej / uber %", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = base, onValueChange = { base = it }, label = { Text("Hodnota") },
                    keyboardOptions = KeyboardOptions(keyboardType = Decimal),
                    modifier = Modifier.weight(1f), singleLine = true,
                )
                OutlinedTextField(
                    value = pct, onValueChange = { pct = it }, label = { Text("±%") },
                    keyboardOptions = KeyboardOptions(keyboardType = Decimal),
                    modifier = Modifier.weight(1f), singleLine = true,
                )
            }
            val v = base.replace(',', '.').toDoubleOrNull()
            val p = pct.replace(',', '.').toDoubleOrNull()
            val plus = if (v != null && p != null) v * (1 + p / 100.0) else null
            val minus = if (v != null && p != null) v * (1 - p / 100.0) else null
            Text("+%: ${plus?.let { "%.4f".format(it) } ?: "—"}")
            Text("−%: ${minus?.let { "%.4f".format(it) } ?: "—"}")
        }
    }
}
