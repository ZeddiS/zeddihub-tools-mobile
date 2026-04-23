package com.zeddihub.mobile.ui.helpers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zeddihub.mobile.R
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyConverterScreen(
    padding: PaddingValues,
    viewModel: CurrencyConverterViewModel = hiltViewModel()
) {
    val colors = MaterialTheme.colorScheme
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadInitial() }

    var amountText by remember { mutableStateOf("100") }
    var from by remember { mutableStateOf("EUR") }
    var to by remember { mutableStateOf("CZK") }

    // Recompute on any input change.
    val amount = amountText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val result = state.rates?.let { rates -> convert(amount, from, to, rates.rates) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val dateText = state.rates?.date ?: "-"
            Text(
                text = stringResource(R.string.currency_rate_source, dateText),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewModel.refresh() }) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.currency_refresh))
            }
        }
        if (state.offline) {
            Text(
                text = stringResource(R.string.currency_offline_hint),
                style = MaterialTheme.typography.labelSmall,
                color = colors.tertiary
            )
        }
        if (state.loading) {
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
            label = { Text(stringResource(R.string.currency_amount)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        val currencies = remember(state.rates) {
            (state.rates?.rates?.keys?.toList() ?: listOf("EUR", "CZK", "USD")).sorted()
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            CurrencyDropdown(
                label = stringResource(R.string.currency_from),
                selected = from,
                options = currencies,
                onSelect = { from = it },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.size(8.dp))
            IconButton(onClick = {
                val tmp = from; from = to; to = tmp
            }) {
                Icon(Icons.Default.SwapVert, contentDescription = stringResource(R.string.currency_swap))
            }
            Spacer(Modifier.size(8.dp))
            CurrencyDropdown(
                label = stringResource(R.string.currency_to),
                selected = to,
                options = currencies,
                onSelect = { to = it },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(20.dp))

        // Result card
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    text = "${formatNumber(amount)} $from",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (result != null) "${formatNumber(result)} $to" else "—",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
            }
        }

        state.error?.let { err ->
            Spacer(Modifier.height(12.dp))
            Text(err, color = colors.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable { expanded = true },
            color = colors.surface,
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
                Text(
                    selected,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { code ->
                DropdownMenuItem(
                    text = { Text(code) },
                    onClick = {
                        onSelect(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun convert(amount: Double, from: String, to: String, rates: Map<String, Double>): Double? {
    if (amount == 0.0) return 0.0
    // Rates are stored as CZK per 1 unit of that currency. CZK itself has rate = 1.
    val rFrom = if (from == "CZK") 1.0 else rates[from] ?: return null
    val rTo = if (to == "CZK") 1.0 else rates[to] ?: return null
    val amountCzk = amount * rFrom
    return amountCzk / rTo
}

private fun formatNumber(value: Double): String {
    val df = DecimalFormat("#,##0.##")
    return df.format(value)
}
