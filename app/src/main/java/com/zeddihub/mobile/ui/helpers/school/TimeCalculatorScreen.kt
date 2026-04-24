package com.zeddihub.mobile.ui.helpers.school

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
import androidx.compose.ui.text.input.KeyboardType.Companion.Number
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeCalculatorScreen(padding: PaddingValues) {
    var tab by remember { mutableStateOf(0) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
    ) {
        ScrollableTabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Sčítání") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Odčítání") })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Od kdy/dokdy") })
            Tab(selected = tab == 3, onClick = { tab = 3 }, text = { Text("Délka") })
        }
        when (tab) {
            0 -> SumTab()
            1 -> DiffTab()
            2 -> StartEndTab()
            else -> DurationTab()
        }
    }
}

private data class TimeRow(val h: String, val m: String, val s: String)

private fun parseSeconds(h: String, m: String, s: String): Long? {
    val hh = h.ifBlank { "0" }.toLongOrNull() ?: return null
    val mm = m.ifBlank { "0" }.toLongOrNull() ?: return null
    val ss = s.ifBlank { "0" }.toLongOrNull() ?: return null
    if (hh < 0 || mm < 0 || ss < 0) return null
    return hh * 3600 + mm * 60 + ss
}

private fun formatHMS(totalSec: Long): String {
    val sign = if (totalSec < 0) "-" else ""
    val t = abs(totalSec)
    val h = t / 3600
    val m = (t % 3600) / 60
    val s = t % 60
    return "%s%02d:%02d:%02d".format(sign, h, m, s)
}

@Composable
private fun TimeRowInput(
    row: TimeRow,
    onChange: (TimeRow) -> Unit,
    onRemove: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = row.h,
            onValueChange = { onChange(row.copy(h = it.filter { c -> c.isDigit() }.take(4))) },
            label = { Text("h") },
            keyboardOptions = KeyboardOptions(keyboardType = Number),
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
        OutlinedTextField(
            value = row.m,
            onValueChange = { onChange(row.copy(m = it.filter { c -> c.isDigit() }.take(2))) },
            label = { Text("m") },
            keyboardOptions = KeyboardOptions(keyboardType = Number),
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
        OutlinedTextField(
            value = row.s,
            onValueChange = { onChange(row.copy(s = it.filter { c -> c.isDigit() }.take(2))) },
            label = { Text("s") },
            keyboardOptions = KeyboardOptions(keyboardType = Number),
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = "Odebrat")
            }
        }
    }
}

@Composable
private fun SumTab() {
    val rows = remember {
        mutableStateListOf(TimeRow("1", "30", "0"), TimeRow("0", "45", "0"))
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        rows.forEachIndexed { i, r ->
            TimeRowInput(
                row = r,
                onChange = { rows[i] = it },
                onRemove = if (rows.size > 1) ({ rows.removeAt(i) }) else null,
            )
        }
        FilledTonalButton(onClick = { rows.add(TimeRow("0", "0", "0")) }) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(" Přidat řádek")
        }
        val total = rows.sumOf { parseSeconds(it.h, it.m, it.s) ?: 0L }
        ResultCard("Součet", formatHMS(total), "Celkem ${total} s")
    }
}

@Composable
private fun DiffTab() {
    var a by remember { mutableStateOf(TimeRow("2", "15", "30")) }
    var b by remember { mutableStateOf(TimeRow("0", "45", "10")) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("A − B", fontWeight = FontWeight.SemiBold)
        Text("A:")
        TimeRowInput(a, { a = it }, null)
        Text("B:")
        TimeRowInput(b, { b = it }, null)
        val av = parseSeconds(a.h, a.m, a.s)
        val bv = parseSeconds(b.h, b.m, b.s)
        val diff = if (av != null && bv != null) av - bv else null
        ResultCard(
            title = "Rozdíl",
            main = diff?.let { formatHMS(it) } ?: "—",
            sub = diff?.let { "${it} s" } ?: "",
        )
    }
}

@Composable
private fun StartEndTab() {
    var start by remember { mutableStateOf(TimeRow("9", "00", "00")) }
    var end by remember { mutableStateOf(TimeRow("17", "30", "00")) }
    var duration by remember { mutableStateOf(TimeRow("1", "30", "00")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("1) Start + délka → konec", fontWeight = FontWeight.SemiBold)
        Text("Start:")
        TimeRowInput(start, { start = it }, null)
        Text("Délka:")
        TimeRowInput(duration, { duration = it }, null)
        val s = parseSeconds(start.h, start.m, start.s)
        val d = parseSeconds(duration.h, duration.m, duration.s)
        val endCalc = if (s != null && d != null) (s + d) else null
        val (endH, endM, endS, dayOverflow) = if (endCalc != null) {
            val total = endCalc % 86400
            val overflow = endCalc / 86400
            Quad((total / 3600).toInt(), ((total % 3600) / 60).toInt(), (total % 60).toInt(), overflow.toInt())
        } else Quad(-1, -1, -1, 0)
        ResultCard(
            title = "Konec",
            main = if (endCalc != null) "%02d:%02d:%02d".format(endH, endM, endS) else "—",
            sub = if (dayOverflow > 0) "(+$dayOverflow den/dní)" else "",
        )

        Text("2) Start + konec → délka", fontWeight = FontWeight.SemiBold)
        Text("Start:")
        TimeRowInput(start, { start = it }, null)
        Text("Konec:")
        TimeRowInput(end, { end = it }, null)
        val e = parseSeconds(end.h, end.m, end.s)
        val s2 = parseSeconds(start.h, start.m, start.s)
        val durCalc = if (s2 != null && e != null) {
            val raw = e - s2
            if (raw < 0) raw + 86400 else raw
        } else null
        ResultCard(
            title = "Délka",
            main = durCalc?.let { formatHMS(it) } ?: "—",
            sub = durCalc?.let { "${it} s" } ?: "",
        )
    }
}

private data class Quad(val a: Int, val b: Int, val c: Int, val d: Int)

@Composable
private fun DurationTab() {
    var sDate by remember { mutableStateOf("2026-01-01") }
    var sTime by remember { mutableStateOf("08:00") }
    var eDate by remember { mutableStateOf("2026-04-24") }
    var eTime by remember { mutableStateOf("17:30") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Formát: datum YYYY-MM-DD, čas HH:MM", style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = sDate, onValueChange = { sDate = it },
                label = { Text("Start datum") },
                modifier = Modifier.weight(1f), singleLine = true,
            )
            OutlinedTextField(
                value = sTime, onValueChange = { sTime = it },
                label = { Text("Start čas") },
                modifier = Modifier.weight(1f), singleLine = true,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = eDate, onValueChange = { eDate = it },
                label = { Text("Konec datum") },
                modifier = Modifier.weight(1f), singleLine = true,
            )
            OutlinedTextField(
                value = eTime, onValueChange = { eTime = it },
                label = { Text("Konec čas") },
                modifier = Modifier.weight(1f), singleLine = true,
            )
        }

        val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE
        val timeFmt = DateTimeFormatter.ofPattern("H:mm")
        val (resTitle, resMain, resSub) = runCatching {
            val s = LocalDateTime.of(
                LocalDate.parse(sDate.trim(), dateFmt),
                LocalTime.parse(sTime.trim(), timeFmt),
            )
            val e = LocalDateTime.of(
                LocalDate.parse(eDate.trim(), dateFmt),
                LocalTime.parse(eTime.trim(), timeFmt),
            )
            val secs = java.time.Duration.between(s, e).seconds
            val absSec = abs(secs)
            val days = absSec / 86400
            val rem = absSec % 86400
            val h = rem / 3600
            val m = (rem % 3600) / 60
            val sec = rem % 60
            val sign = if (secs < 0) "-" else ""
            val main = "$sign${days} dní, ${h}h ${m}m ${sec}s"
            val sub = "Celkem: ${absSec / 60} min  |  ${absSec / 3600} h  |  $secs s"
            Triple("Rozdíl", main, sub)
        }.getOrElse { Triple("Rozdíl", "—", "Chybný formát data/času.") }

        ResultCard(resTitle, resMain, resSub)
    }
}

@Composable
private fun ResultCard(title: String, main: String, sub: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(main, style = MaterialTheme.typography.headlineSmall)
            if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall)
        }
    }
}
