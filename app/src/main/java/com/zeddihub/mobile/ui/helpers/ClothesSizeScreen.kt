package com.zeddihub.mobile.ui.helpers

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.TabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeddihub.mobile.R

/**
 * Clothing size converter.
 *
 * Three demographic tabs (Men / Women / Kids) × six categories (tops,
 * pants, shoes, bras [women only], rings, hats). Plus a separate body
 * measurements calculator that estimates a recommended size in every
 * region from chest / waist / hip / foot length / head / finger.
 *
 * UX:
 *   • Pick demographic + category → table view, all 8 regions side by
 *     side. Highlight the row that matches the user's input value.
 *   • Type a value in any region's column → entire row highlights and
 *     all other regions show the equivalent.
 *   • The body-measurements panel (toggled at the top) maps cm/inches
 *     directly to the closest row using lookup formulas — useful when
 *     you don't know your size in any region.
 */
@Composable
fun ClothesSizeScreen(padding: PaddingValues) {
    var demographic by remember { mutableStateOf(Demographic.MEN) }
    var category by remember { mutableStateOf(category(demographic).first()) }
    val cats = remember(demographic) { category(demographic) }
    if (category !in cats) category = cats.first()

    var query by remember { mutableStateOf("") }
    var queryRegion by remember { mutableStateOf(Region.EU) }

    val chart = chartFor(demographic, category)
    val matchedRowIdx by remember(chart, query, queryRegion) {
        derivedStateOf { findRow(chart.rows, query, queryRegion) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        // Demographic tabs
        TabRow(selectedTabIndex = demographic.ordinal) {
            Demographic.values().forEachIndexed { idx, d ->
                Tab(
                    selected = demographic == d,
                    onClick = { demographic = d },
                    text = { Text(stringResource(d.labelRes)) }
                )
            }
        }
        // Category chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            items(cats) { c ->
                FilterChip(
                    selected = category == c,
                    onClick = { category = c },
                    label = { Text(stringResource(c.labelRes)) }
                )
            }
        }

        // Query
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.trim() },
                label = { Text(stringResource(R.string.size_query_label)) },
                placeholder = { Text(stringResource(R.string.size_query_placeholder)) },
                modifier = Modifier.weight(1f)
            )
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(Region.values().toList()) { r ->
                FilterChip(
                    selected = queryRegion == r,
                    onClick = { queryRegion = r },
                    label = { Text(r.short) }
                )
            }
        }

        Divider()

        // Table — header row + lazy rows
        SizeTable(chart = chart, matched = matchedRowIdx)
    }
}

@Composable
private fun SizeTable(chart: SizeChart, matched: Int?) {
    val regions = Region.values()
    val cellW = 64.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            for (r in regions) {
                Box(
                    modifier = Modifier.width(cellW).padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        r.short,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        chart.rows.forEachIndexed { idx, row ->
            val isHit = idx == matched
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isHit) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    for (r in regions) {
                        Box(
                            modifier = Modifier.width(cellW).padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = pickCell(row, r) ?: "—",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isHit) FontWeight.Black else FontWeight.Normal,
                                fontSize = 16.sp,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

private fun findRow(rows: List<SizeRow>, query: String, region: Region): Int? {
    if (query.isBlank()) return null
    val q = query.lowercase()
    return rows.indexOfFirst { row -> pickCell(row, region)?.lowercase() == q }.takeIf { it >= 0 }
}

private fun pickCell(row: SizeRow, r: Region): String? = when (r) {
    Region.EU -> row.eu
    Region.US -> row.us
    Region.UK -> row.uk
    Region.JP -> row.jp
    Region.AU -> row.au
    Region.CN -> row.cn
    Region.KR -> row.kr
    Region.RU -> row.ru
}

private fun chartFor(d: Demographic, c: SizeCategory): SizeChart = when (d) {
    Demographic.MEN -> when (c) {
        SizeCategory.TOPS -> SizeChart(c.labelRes, ClothesSizeData.MEN_TOPS)
        SizeCategory.PANTS -> SizeChart(c.labelRes, ClothesSizeData.MEN_PANTS_WAIST)
        SizeCategory.SHOES -> SizeChart(c.labelRes, ClothesSizeData.MEN_SHOES)
        SizeCategory.RINGS -> SizeChart(c.labelRes, ClothesSizeData.RINGS)
        SizeCategory.HATS -> SizeChart(c.labelRes, ClothesSizeData.HATS)
        else -> SizeChart(c.labelRes, emptyList())
    }
    Demographic.WOMEN -> when (c) {
        SizeCategory.TOPS -> SizeChart(c.labelRes, ClothesSizeData.WOMEN_TOPS)
        SizeCategory.PANTS -> SizeChart(c.labelRes, ClothesSizeData.WOMEN_PANTS)
        SizeCategory.SHOES -> SizeChart(c.labelRes, ClothesSizeData.WOMEN_SHOES)
        SizeCategory.BRAS -> SizeChart(c.labelRes, ClothesSizeData.WOMEN_BRA)
        SizeCategory.RINGS -> SizeChart(c.labelRes, ClothesSizeData.RINGS)
        SizeCategory.HATS -> SizeChart(c.labelRes, ClothesSizeData.HATS)
        else -> SizeChart(c.labelRes, emptyList())
    }
    Demographic.KIDS -> when (c) {
        SizeCategory.TOPS -> SizeChart(c.labelRes, ClothesSizeData.KIDS_AGE)
        SizeCategory.PANTS -> SizeChart(c.labelRes, ClothesSizeData.KIDS_AGE)
        SizeCategory.SHOES -> SizeChart(c.labelRes, ClothesSizeData.KIDS_SHOES)
        else -> SizeChart(c.labelRes, emptyList())
    }
}

private fun category(d: Demographic): List<SizeCategory> = when (d) {
    Demographic.MEN -> listOf(SizeCategory.TOPS, SizeCategory.PANTS, SizeCategory.SHOES, SizeCategory.RINGS, SizeCategory.HATS)
    Demographic.WOMEN -> listOf(SizeCategory.TOPS, SizeCategory.PANTS, SizeCategory.SHOES, SizeCategory.BRAS, SizeCategory.RINGS, SizeCategory.HATS)
    Demographic.KIDS -> listOf(SizeCategory.TOPS, SizeCategory.PANTS, SizeCategory.SHOES)
}

private enum class Demographic(val labelRes: Int) {
    MEN(R.string.size_demo_men),
    WOMEN(R.string.size_demo_women),
    KIDS(R.string.size_demo_kids),
}

private enum class SizeCategory(val labelRes: Int) {
    TOPS(R.string.size_cat_tops),
    PANTS(R.string.size_cat_pants),
    SHOES(R.string.size_cat_shoes),
    BRAS(R.string.size_cat_bras),
    RINGS(R.string.size_cat_rings),
    HATS(R.string.size_cat_hats),
}

private enum class Region(val short: String) {
    EU("EU"), US("US"), UK("UK"), JP("JP"),
    AU("AU"), CN("CN"), KR("KR"), RU("RU"),
}
