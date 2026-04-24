package com.zeddihub.mobile.ui.helpers.school

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.sqrt

@Composable
fun StatisticsCalculatorScreen(padding: PaddingValues) {
    var input by remember {
        mutableStateOf("1, 2, 2, 3, 4, 4, 4, 5, 7, 10, 11, 12, 12, 15")
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Zadej čísla oddělená čárkou, mezerou nebo novým řádkem.",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Data") },
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            minLines = 4,
        )

        val values: List<Double> = input
            .split(',', ' ', '\n', '\t', ';')
            .mapNotNull { it.trim().replace(',', '.').toDoubleOrNull() }

        if (values.isEmpty()) {
            Text("Žádná platná čísla.")
        } else {
            val sorted = values.sorted()
            val n = sorted.size
            val sum = sorted.sum()
            val mean = sum / n
            val median = if (n % 2 == 1) sorted[n / 2]
            else (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
            val minV = sorted.first()
            val maxV = sorted.last()
            val range = maxV - minV
            val variance = if (n > 1) sorted.sumOf { (it - mean) * (it - mean) } / (n - 1) else 0.0
            val sd = sqrt(variance)
            // Modus: hodnoty s nejvyšší frekvencí
            val groups = sorted.groupingBy { it }.eachCount()
            val maxFreq = groups.values.max()
            val modes = if (maxFreq == 1) emptyList()
            else groups.filter { it.value == maxFreq }.keys.sorted()

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Statistika", fontWeight = FontWeight.SemiBold)
                    Row { Label("Počet"); Value("$n") }
                    Row { Label("Součet"); Value("%.4f".format(sum)) }
                    Row { Label("Minimum"); Value("%.4f".format(minV)) }
                    Row { Label("Maximum"); Value("%.4f".format(maxV)) }
                    Row { Label("Průměr"); Value("%.4f".format(mean)) }
                    Row { Label("Medián"); Value("%.4f".format(median)) }
                    Row {
                        Label("Modus")
                        Value(if (modes.isEmpty()) "(žádný)" else modes.joinToString { "%.4g".format(it) })
                    }
                    Row { Label("Rozpětí"); Value("%.4f".format(range)) }
                    Row { Label("Rozptyl (vzorek)"); Value("%.4f".format(variance)) }
                    Row { Label("Směrod. odchylka"); Value("%.4f".format(sd)) }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Histogram (10 binů)", fontWeight = FontWeight.SemiBold)
                    Histogram(
                        values = sorted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Label(s: String) {
    Text(s, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(end = 8.dp))
}

@Composable
private fun Value(s: String) {
    Text(s, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun Histogram(values: List<Double>, modifier: Modifier) {
    val barColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas
        val minV = values.first()
        val maxV = values.last()
        val binsCount = 10
        val span = max(maxV - minV, 1e-9)
        val bins = IntArray(binsCount)
        for (v in values) {
            var idx = (((v - minV) / span) * binsCount).toInt()
            if (idx >= binsCount) idx = binsCount - 1
            if (idx < 0) idx = 0
            bins[idx]++
        }
        val maxBin = bins.max().coerceAtLeast(1)
        val w = size.width
        val h = size.height
        val pad = 14f
        val barAreaW = w - 2 * pad
        val barAreaH = h - 2 * pad
        val bw = barAreaW / binsCount
        for (i in 0 until binsCount) {
            val bh = (bins[i].toFloat() / maxBin) * barAreaH
            val x = pad + i * bw
            val y = h - pad - bh
            drawRect(
                color = barColor,
                topLeft = Offset(x + bw * 0.08f, y),
                size = Size(bw * 0.84f, bh),
            )
        }
        // baseline
        drawLine(
            color = axisColor,
            start = Offset(pad, h - pad),
            end = Offset(w - pad, h - pad),
            strokeWidth = 2f,
        )
    }
}
