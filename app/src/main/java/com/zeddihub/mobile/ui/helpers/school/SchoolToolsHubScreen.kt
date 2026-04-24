package com.zeddihub.mobile.ui.helpers.school

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private data class SchoolTile(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String,
)

private val tiles = listOf(
    SchoolTile(
        "Kalkulačka známek",
        "Vážený průměr + cíl",
        Icons.Filled.Calculate,
        "help/school/grade",
    ),
    SchoolTile(
        "Převodník jednotek",
        "Délka, hmotnost, teplota…",
        Icons.Filled.SwapHoriz,
        "help/school/units",
    ),
    SchoolTile(
        "Matematické vzorce",
        "Algebra, geometrie, derivace",
        Icons.Filled.Functions,
        "help/school/formulas",
    ),
    SchoolTile(
        "Zlomky a procenta",
        "Zlomky, poměry, %",
        Icons.Filled.Percent,
        "help/school/fractions",
    ),
    SchoolTile(
        "Trojúhelník",
        "Strany, úhly, obsah",
        Icons.Filled.ChangeHistory,
        "help/school/triangle",
    ),
    SchoolTile(
        "Statistika",
        "Průměr, medián, odchylka",
        Icons.Filled.QueryStats,
        "help/school/stats",
    ),
    SchoolTile(
        "Kalkulačka času",
        "Sčítání hodin a dat",
        Icons.Filled.Schedule,
        "help/school/time",
    ),
    SchoolTile(
        "Periodická tabulka",
        "Prvky a jejich vlastnosti",
        Icons.Filled.Science,
        "help/periodic",
    ),
)

@Composable
fun SchoolToolsHubScreen(padding: PaddingValues, onNavigate: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(tiles) { tile ->
            TileCard(tile = tile, onClick = { onNavigate(tile.route) })
        }
    }
}

@Composable
private fun TileCard(tile: SchoolTile, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = tile.icon,
                contentDescription = tile.title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = tile.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = tile.subtitle,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
