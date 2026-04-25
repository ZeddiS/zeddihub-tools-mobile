package com.zeddihub.mobile.ui.helpers

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeddihub.mobile.R
import com.zeddihub.mobile.data.reminders.ComparisonOp
import com.zeddihub.mobile.data.reminders.Reminder
import com.zeddihub.mobile.data.reminders.ReminderScheduler
import com.zeddihub.mobile.data.reminders.ReminderStore
import com.zeddihub.mobile.data.reminders.ReminderTrigger
import com.zeddihub.mobile.data.reminders.WeatherCondition
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SmartRemindersViewModel @Inject constructor(
    private val store: ReminderStore,
    private val scheduler: ReminderScheduler,
) : ViewModel() {

    private val _items = MutableStateFlow<List<Reminder>>(emptyList())
    val items: StateFlow<List<Reminder>> = _items.asStateFlow()

    init {
        viewModelScope.launch {
            store.flow.collect { _items.value = it }
        }
        scheduler.ensureChannel()
    }

    fun upsert(r: Reminder) {
        viewModelScope.launch {
            store.upsert(r)
            scheduler.reschedule(r)
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            scheduler.cancel(id)
            store.remove(id)
        }
    }

    fun toggle(r: Reminder) {
        viewModelScope.launch {
            store.setEnabled(r.id, !r.enabled)
            scheduler.reschedule(r.copy(enabled = !r.enabled))
        }
    }
}

/**
 * UI for managing smart reminders. Time-based triggers are fully wired
 * to AlarmManager + a notification channel; the screen shows location /
 * WiFi / Bluetooth / weather options as well, but those rule kinds
 * become active in v0.9.0 (when the matching hardware tools land and
 * we wire the receivers into the Application's lifecycle).
 */
@Composable
fun SmartRemindersScreen(padding: PaddingValues) {
    val vm: SmartRemindersViewModel = hiltViewModel()
    val items by vm.items.collectAsState()
    var editing by remember { mutableStateOf<Reminder?>(null) }
    var creating by remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    // POST_NOTIFICATIONS for Android 13+
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* user choice handled by system */ }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        if (items.isEmpty()) {
            EmptyState(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items.size) { idx ->
                    val r = items[idx]
                    ReminderCard(
                        r = r,
                        onToggle = { vm.toggle(r) },
                        onClick = { editing = r },
                        onDelete = { vm.delete(r.id) }
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            text = { Text(stringResource(R.string.reminder_new)) },
            icon = { Icon(Icons.Default.Add, null) },
            onClick = { creating = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }

    if (creating || editing != null) {
        ReminderEditor(
            initial = editing,
            onCancel = { creating = false; editing = null },
            onSave = { r ->
                vm.upsert(r)
                creating = false
                editing = null
            }
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.reminder_empty_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.reminder_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReminderCard(
    r: Reminder,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (r.enabled) 3.dp else 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    r.title.ifBlank { stringResource(R.string.reminder_default_title) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (r.body.isNotBlank()) {
                    Text(r.body, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    triggerSummary(r.trigger),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = r.enabled, onCheckedChange = { onToggle() })
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null)
            }
        }
    }
}

private fun triggerSummary(t: ReminderTrigger): String = when (t) {
    is ReminderTrigger.TimeAt -> {
        val fmt = SimpleDateFormat("d.M.yyyy HH:mm", Locale.getDefault())
        "📅 " + fmt.format(Date(t.epochMs))
    }
    is ReminderTrigger.TimeWeekly -> {
        val days = listOf("Po", "Út", "St", "Čt", "Pá", "So", "Ne")
        val on = days.filterIndexed { i, _ -> (t.daysMask shr i) and 1 == 1 }.joinToString(",")
        "🔁 $on  %02d:%02d".format(t.minuteOfDay / 60, t.minuteOfDay % 60)
    }
    is ReminderTrigger.Geofence -> "📍 ${"%.4f".format(t.lat)}, ${"%.4f".format(t.lng)}"
    is ReminderTrigger.WifiSsid -> "📶 ${t.ssid}"
    is ReminderTrigger.BluetoothDevice -> "🔵 ${t.name.ifBlank { t.address }}"
    is ReminderTrigger.Weather -> "☁ ${t.condition} ${if (t.operator == ComparisonOp.GREATER_THAN) ">" else "<"} ${t.threshold}"
}

// ── Editor dialog ───────────────────────────────────────────────────

@Composable
private fun ReminderEditor(
    initial: Reminder?,
    onCancel: () -> Unit,
    onSave: (Reminder) -> Unit,
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var body by remember { mutableStateOf(initial?.body ?: "") }
    var enabled by remember { mutableStateOf(initial?.enabled ?: true) }
    var kind by remember {
        mutableStateOf(
            when (initial?.trigger) {
                is ReminderTrigger.TimeWeekly -> Kind.WEEKLY
                is ReminderTrigger.Weather -> Kind.WEATHER
                is ReminderTrigger.Geofence -> Kind.GEOFENCE
                is ReminderTrigger.WifiSsid -> Kind.WIFI
                is ReminderTrigger.BluetoothDevice -> Kind.BLUETOOTH
                else -> Kind.ONESHOT
            }
        )
    }

    // Time-at fields
    var atEpoch by remember {
        mutableStateOf(
            (initial?.trigger as? ReminderTrigger.TimeAt)?.epochMs
                ?: (System.currentTimeMillis() + 60 * 60 * 1000)
        )
    }

    // Weekly fields
    val weekly = initial?.trigger as? ReminderTrigger.TimeWeekly
    var daysMask by remember { mutableStateOf(weekly?.daysMask ?: 0b1111100) } // Mo-Fr default
    var minuteOfDay by remember { mutableStateOf(weekly?.minuteOfDay ?: (8 * 60)) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(if (initial == null) R.string.reminder_new else R.string.reminder_edit)) },
        confirmButton = {
            TextButton(onClick = {
                val trigger = when (kind) {
                    Kind.ONESHOT -> ReminderTrigger.TimeAt(atEpoch)
                    Kind.WEEKLY -> ReminderTrigger.TimeWeekly(daysMask, minuteOfDay)
                    // Other kinds — UI editor in v0.9.0; for now we keep
                    // the existing trigger if editing or fall back to a
                    // safe one-shot default if creating.
                    else -> initial?.trigger ?: ReminderTrigger.TimeAt(atEpoch)
                }
                onSave(
                    Reminder(
                        id = initial?.id ?: UUID.randomUUID().toString(),
                        title = title.trim(),
                        body = body.trim(),
                        enabled = enabled,
                        trigger = trigger,
                        createdAt = initial?.createdAt ?: System.currentTimeMillis(),
                    )
                )
            }) { Text(stringResource(R.string.reminder_save)) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.reminder_cancel)) }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text(stringResource(R.string.reminder_title_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = body, onValueChange = { body = it },
                    label = { Text(stringResource(R.string.reminder_body_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.reminder_enabled_label))
                }

                Text(stringResource(R.string.reminder_trigger_label),
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                // Kind picker
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Kind.values().take(2).forEach { k -> // Only OneShot + Weekly active in v0.8.0
                        FilterChip(
                            selected = kind == k,
                            onClick = { kind = k },
                            label = { Text(stringResource(k.labelRes)) }
                        )
                    }
                }
                Text(
                    stringResource(R.string.reminder_other_kinds_v090),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                when (kind) {
                    Kind.ONESHOT -> OneShotEditor(atEpoch) { atEpoch = it }
                    Kind.WEEKLY -> WeeklyEditor(daysMask, minuteOfDay) { mask, m ->
                        daysMask = mask; minuteOfDay = m
                    }
                    else -> {}
                }
            }
        }
    )
}

@Composable
private fun OneShotEditor(epoch: Long, onChange: (Long) -> Unit) {
    val ctx = LocalContext.current
    val cal = remember(epoch) { Calendar.getInstance().apply { timeInMillis = epoch } }
    val fmt = SimpleDateFormat("d.M.yyyy HH:mm", Locale.getDefault())

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(
            onClick = {
                android.app.DatePickerDialog(
                    ctx,
                    { _, y, m, d ->
                        val c = (cal.clone() as Calendar).apply {
                            set(Calendar.YEAR, y); set(Calendar.MONTH, m); set(Calendar.DAY_OF_MONTH, d)
                        }
                        onChange(c.timeInMillis)
                    },
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            label = { Text(SimpleDateFormat("d.M.yyyy", Locale.getDefault()).format(Date(epoch))) }
        )
        AssistChip(
            onClick = {
                TimePickerDialog(
                    ctx,
                    { _, h, m ->
                        val c = (cal.clone() as Calendar).apply {
                            set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m)
                            set(Calendar.SECOND, 0)
                        }
                        onChange(c.timeInMillis)
                    },
                    cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true
                ).show()
            },
            label = { Text("%02d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))) }
        )
    }
}

@Composable
private fun WeeklyEditor(daysMask: Int, minuteOfDay: Int, onChange: (Int, Int) -> Unit) {
    val ctx = LocalContext.current
    val days = listOf("Po", "Út", "St", "Čt", "Pá", "So", "Ne")
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        days.forEachIndexed { i, label ->
            val on = (daysMask shr i) and 1 == 1
            FilterChip(
                selected = on,
                onClick = { onChange(daysMask xor (1 shl i), minuteOfDay) },
                label = { Text(label) },
            )
        }
    }
    AssistChip(
        onClick = {
            TimePickerDialog(
                ctx,
                { _, h, m -> onChange(daysMask, h * 60 + m) },
                minuteOfDay / 60, minuteOfDay % 60, true
            ).show()
        },
        label = { Text("%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)) },
        leadingIcon = { Icon(Icons.Default.Schedule, null) }
    )
}

private enum class Kind(val labelRes: Int) {
    ONESHOT(R.string.reminder_kind_oneshot),
    WEEKLY(R.string.reminder_kind_weekly),
    GEOFENCE(R.string.reminder_kind_geofence),
    WIFI(R.string.reminder_kind_wifi),
    BLUETOOTH(R.string.reminder_kind_bt),
    WEATHER(R.string.reminder_kind_weather),
}
