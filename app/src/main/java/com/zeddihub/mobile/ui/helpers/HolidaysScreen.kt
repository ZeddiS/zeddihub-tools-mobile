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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zeddihub.mobile.R
import java.time.LocalDate
import java.time.Month
import java.time.MonthDay
import java.time.format.TextStyle
import java.util.Locale

/**
 * Holidays helper.
 *
 * Three tabs:
 *  - Dnes: today's name-day and state-holiday summary.
 *  - Kalendář: scrollable 366-day list of Czech name-days grouped
 *    by month.
 *  - Státní: list of state holidays for the current and next year.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolidaysScreen(padding: PaddingValues) {
    val today = remember { LocalDate.now() }
    var tab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        PrimaryTabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }) {
                Text(stringResource(R.string.holidays_tab_today), modifier = Modifier.padding(12.dp))
            }
            Tab(selected = tab == 1, onClick = { tab = 1 }) {
                Text(stringResource(R.string.holidays_tab_all), modifier = Modifier.padding(12.dp))
            }
            Tab(selected = tab == 2, onClick = { tab = 2 }) {
                Text(stringResource(R.string.holidays_tab_state), modifier = Modifier.padding(12.dp))
            }
        }
        when (tab) {
            0 -> TodayTab(today)
            1 -> CalendarTab()
            2 -> StateTab(today)
        }
    }
}

@Composable
private fun TodayTab(today: LocalDate) {
    val md = MonthDay.from(today)
    val name = NameDays.forMonthDay(md)
    val tomorrow = today.plusDays(1)
    val tomorrowName = NameDays.forMonthDay(MonthDay.from(tomorrow))
    val holiday = Holidays.forDate(today)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Big card: today's name-day.
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    formatDate(today),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.holidays_nameday_today, name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.holidays_tomorrow, tomorrowName),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Holiday card.
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (holiday != null)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                if (holiday != null) {
                    Text(
                        stringResource(R.string.holidays_state_today, holiday.name),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (holiday.free) stringResource(R.string.holidays_free_day)
                        else stringResource(R.string.holidays_working_day),
                        style = MaterialTheme.typography.labelMedium
                    )
                } else {
                    Text(stringResource(R.string.holidays_no_state_today))
                }
            }
        }
    }
}

@Composable
private fun CalendarTab() {
    val entries = remember { NameDays.orderedEntries }
    val today = remember { MonthDay.from(LocalDate.now()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Group by month, emit a header row per month.
        var currentMonth: Month? = null
        entries.forEach { (md, name) ->
            if (md.month != currentMonth) {
                currentMonth = md.month
                item("hdr-${md.monthValue}") {
                    Column(Modifier.padding(top = 12.dp, bottom = 4.dp)) {
                        Text(
                            md.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale("cs"))
                                .replaceFirstChar { it.titlecase(Locale("cs")) },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        HorizontalDivider(Modifier.padding(top = 4.dp))
                    }
                }
            }
            item("${md.monthValue}-${md.dayOfMonth}") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (md == today)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    md.dayOfMonth.toString(),
                                    color = if (md == today)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (md == today) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun StateTab(today: LocalDate) {
    val years = listOf(today.year, today.year + 1)
    val items = remember(years) {
        years.flatMap { y -> Holidays.forYear(y).map { y to it } }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        var lastYear: Int? = null
        items.forEach { (year, h) ->
            if (year != lastYear) {
                lastYear = year
                item("year-$year") {
                    Text(
                        year.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                    HorizontalDivider()
                }
            }
            item("h-$year-${h.date}") {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (h.date == today)
                        MaterialTheme.colorScheme.tertiaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            formatShortDate(h.date),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(96.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(h.name, style = MaterialTheme.typography.bodyMedium)
                        }
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = {
                                Text(
                                    if (h.free) stringResource(R.string.holidays_free_day)
                                    else stringResource(R.string.holidays_working_day)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                disabledContainerColor = if (h.free)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun formatDate(date: LocalDate): String {
    val cs = Locale("cs")
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL_STANDALONE, cs)
    val monthName = date.month.getDisplayName(TextStyle.FULL_STANDALONE, cs)
    return "$dayName, ${date.dayOfMonth}. $monthName ${date.year}"
        .replaceFirstChar { it.titlecase(cs) }
}

private fun formatShortDate(date: LocalDate): String {
    val cs = Locale("cs")
    val monthName = date.month.getDisplayName(TextStyle.SHORT_STANDALONE, cs)
    return "${date.dayOfMonth}. $monthName"
}
