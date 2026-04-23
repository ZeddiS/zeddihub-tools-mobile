package com.zeddihub.mobile.ui.helpers

import android.app.DatePickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate

/**
 * Thin wrapper around the platform DatePickerDialog that returns a
 * LocalDate. Compose M3 has its own DatePickerDialog but it pulls in
 * extra state machinery that's unnecessary for helpers like
 * AgeCalculator; the native dialog is enough.
 */
@Composable
fun NativeDatePickerDialog(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    val ctx = LocalContext.current
    DisposableEffect(initial) {
        val dlg = DatePickerDialog(
            ctx,
            { _, y, m, d -> onConfirm(LocalDate.of(y, m + 1, d)) },
            initial.year,
            initial.monthValue - 1,
            initial.dayOfMonth
        )
        dlg.setOnCancelListener { onDismiss() }
        dlg.setOnDismissListener { onDismiss() }
        dlg.show()
        onDispose { dlg.dismiss() }
    }
}
