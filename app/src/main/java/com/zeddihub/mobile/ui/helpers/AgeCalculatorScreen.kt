package com.zeddihub.mobile.ui.helpers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

/**
 * Combined "Věk" + "Čas v rozmezí" calculator. Two sections on one screen:
 *
 *   1. Birth date → age breakdown at target date
 *      - y/m/d, total days, total hours, days until next birthday
 *   2. Date range → difference between any two dates
 *      - rendered as "Xy Ym Zd" plus total-days line
 *
 * Uses java.time (minSdk=26 already satisfied by app/build.gradle.kts).
 * No backend; pure arithmetic.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgeCalculatorScreen(padding: PaddingValues) {
    val colors = MaterialTheme.colorScheme
    val today = remember { LocalDate.now() }

    var birth by remember { mutableStateOf(LocalDate.of(2000, 1, 1)) }
    var target by remember { mutableStateOf(today) }

    var rangeFrom by remember { mutableStateOf(today.minusMonths(1)) }
    var rangeTo by remember { mutableStateOf(today) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ── Age section ────────────────────────────────────────────────
        Text(
            text = stringResource(R.string.nav_age_calc),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.onBackground
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateField(
                label = stringResource(R.string.age_birth_date),
                value = birth,
                onChange = { birth = it },
                modifier = Modifier.weight(1f)
            )
            DateField(
                label = stringResource(R.string.age_target_date),
                value = target,
                onChange = { target = it },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        val period = Period.between(birth, target)
        val totalDays = ChronoUnit.DAYS.between(birth, target)
        val totalHours = ChronoUnit.HOURS.between(birth.atStartOfDay(), target.atStartOfDay())

        // Next birthday is always measured from "today", not from the
        // target date — people usually want "how long till next birthday".
        val nextBirthdayYear = if (
            today.monthValue > birth.monthValue ||
            (today.monthValue == birth.monthValue && today.dayOfMonth >= birth.dayOfMonth)
        ) today.year + 1 else today.year
        val nextBirthday = safeDate(nextBirthdayYear, birth.monthValue, birth.dayOfMonth)
        val untilBirthday = ChronoUnit.DAYS.between(today, nextBirthday)

        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatColumn(
                        value = stringResource(R.string.age_years, period.years),
                        modifier = Modifier.weight(1f)
                    )
                    StatColumn(
                        value = stringResource(R.string.age_months, period.months),
                        modifier = Modifier.weight(1f)
                    )
                    StatColumn(
                        value = stringResource(R.string.age_days, period.days),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.age_total_days, totalDays), color = colors.onSurface)
                Text(stringResource(R.string.age_total_hours, totalHours), color = colors.onSurface)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.age_next_birthday, untilBirthday),
                    color = colors.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Range section ─────────────────────────────────────────────
        Text(
            text = stringResource(R.string.age_range_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.onBackground
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateField(
                label = stringResource(R.string.age_range_from),
                value = rangeFrom,
                onChange = { rangeFrom = it },
                modifier = Modifier.weight(1f)
            )
            DateField(
                label = stringResource(R.string.age_range_to),
                value = rangeTo,
                onChange = { rangeTo = it },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Normalize so Period doesn't go negative when user picks from > to.
        val (a, b) = if (rangeFrom.isAfter(rangeTo)) rangeTo to rangeFrom else rangeFrom to rangeTo
        val rangePeriod = Period.between(a, b)
        val rangeDays = ChronoUnit.DAYS.between(a, b)

        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(
                        R.string.age_range_result,
                        "${rangePeriod.years}y ${rangePeriod.months}m ${rangePeriod.days}d"
                    ),
                    color = colors.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.age_total_days, rangeDays), color = colors.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun DateField(
    label: String,
    value: LocalDate,
    onChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var showPicker by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .height(72.dp)
            .clickable { showPicker = true },
        color = colors.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    if (showPicker) {
        NativeDatePickerDialog(
            initial = value,
            onDismiss = { showPicker = false },
            onConfirm = {
                onChange(it)
                showPicker = false
            }
        )
    }
}

@Composable
private fun StatColumn(value: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier) {
        Text(
            text = value,
            color = colors.primary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge
        )
    }
}

/** Guards invalid dates like Feb 29 in non-leap years. */
private fun safeDate(year: Int, month: Int, day: Int): LocalDate {
    val ym = java.time.YearMonth.of(year, month)
    val clamped = day.coerceAtMost(ym.lengthOfMonth())
    return LocalDate.of(year, month, clamped)
}
