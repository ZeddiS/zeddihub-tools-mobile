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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Schedule
import android.annotation.SuppressLint
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
import com.zeddihub.mobile.data.reminders.ReminderActivator
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
    private val activator: ReminderActivator,
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
            // Time triggers go to the alarm scheduler; everything else
            // (geofence/wifi/bt/weather) goes to the activator. Calling
            // both is safe: each is a no-op when the trigger isn't its
            // kind.
            scheduler.reschedule(r)
            activator.reschedule(r)
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            // We need the trigger kind to know what subsystem to clean up,
            // so look the rule up *before* we remove it from the store.
            val rule = store.load().firstOrNull { it.id == id }
            scheduler.cancel(id)
            activator.cancel(id, rule?.trigger)
            store.remove(id)
        }
    }

    fun toggle(r: Reminder) {
        viewModelScope.launch {
            val flipped = r.copy(enabled = !r.enabled)
            store.setEnabled(r.id, flipped.enabled)
            scheduler.reschedule(flipped)
            activator.reschedule(flipped)
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

@OptIn(ExperimentalLayoutApi::class)
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

    // Geofence fields
    val geo = initial?.trigger as? ReminderTrigger.Geofence
    var geoLat by remember { mutableStateOf(geo?.lat ?: 50.0755) }
    var geoLng by remember { mutableStateOf(geo?.lng ?: 14.4378) }
    var geoRadius by remember { mutableStateOf(geo?.radiusM ?: 200f) }
    var geoOnEnter by remember { mutableStateOf(geo?.onEnter ?: true) }
    var geoOnExit by remember { mutableStateOf(geo?.onExit ?: false) }

    // WiFi fields
    val wifi = initial?.trigger as? ReminderTrigger.WifiSsid
    var wifiSsid by remember { mutableStateOf(wifi?.ssid ?: "") }
    var wifiOnConnect by remember { mutableStateOf(wifi?.onConnect ?: true) }
    var wifiOnDisconnect by remember { mutableStateOf(wifi?.onDisconnect ?: false) }

    // Bluetooth fields
    val bt = initial?.trigger as? ReminderTrigger.BluetoothDevice
    var btAddress by remember { mutableStateOf(bt?.address ?: "") }
    var btName by remember { mutableStateOf(bt?.name ?: "") }
    var btOnConnect by remember { mutableStateOf(bt?.onConnect ?: true) }
    var btOnDisconnect by remember { mutableStateOf(bt?.onDisconnect ?: false) }

    // Weather fields
    val wx = initial?.trigger as? ReminderTrigger.Weather
    var wxLat by remember { mutableStateOf(wx?.lat ?: 50.0755) }
    var wxLng by remember { mutableStateOf(wx?.lng ?: 14.4378) }
    var wxCondition by remember { mutableStateOf(wx?.condition ?: WeatherCondition.TEMP_C) }
    var wxOp by remember { mutableStateOf(wx?.operator ?: ComparisonOp.GREATER_THAN) }
    var wxThreshold by remember { mutableStateOf(wx?.threshold ?: 25f) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(if (initial == null) R.string.reminder_new else R.string.reminder_edit)) },
        confirmButton = {
            TextButton(onClick = {
                val trigger = when (kind) {
                    Kind.ONESHOT -> ReminderTrigger.TimeAt(atEpoch)
                    Kind.WEEKLY -> ReminderTrigger.TimeWeekly(daysMask, minuteOfDay)
                    Kind.GEOFENCE -> ReminderTrigger.Geofence(
                        geoLat, geoLng, geoRadius.coerceAtLeast(50f),
                        geoOnEnter, geoOnExit
                    )
                    Kind.WIFI -> ReminderTrigger.WifiSsid(
                        wifiSsid.trim(), wifiOnConnect, wifiOnDisconnect
                    )
                    Kind.BLUETOOTH -> ReminderTrigger.BluetoothDevice(
                        btAddress.trim().uppercase(), btName.trim(),
                        btOnConnect, btOnDisconnect
                    )
                    Kind.WEATHER -> ReminderTrigger.Weather(
                        wxLat, wxLng, wxCondition, wxOp, wxThreshold
                    )
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
                // Kind picker — all six kinds active from v1.8.5 onward.
                // The dialog scrolls vertically because six trigger UIs
                // can stack tall (Geofence + Weather both render lat/lng
                // sliders and a condition row).
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Kind.values().forEach { k ->
                        FilterChip(
                            selected = kind == k,
                            onClick = { kind = k },
                            label = { Text(stringResource(k.labelRes)) }
                        )
                    }
                }

                when (kind) {
                    Kind.ONESHOT -> OneShotEditor(atEpoch) { atEpoch = it }
                    Kind.WEEKLY -> WeeklyEditor(daysMask, minuteOfDay) { mask, m ->
                        daysMask = mask; minuteOfDay = m
                    }
                    Kind.GEOFENCE -> GeofenceEditor(
                        lat = geoLat, lng = geoLng, radius = geoRadius,
                        onEnter = geoOnEnter, onExit = geoOnExit,
                        onChange = { lat, lng, r, e, x ->
                            geoLat = lat; geoLng = lng; geoRadius = r
                            geoOnEnter = e; geoOnExit = x
                        }
                    )
                    Kind.WIFI -> WifiEditor(
                        ssid = wifiSsid, onConn = wifiOnConnect, onDisc = wifiOnDisconnect,
                        onChange = { s, c, d -> wifiSsid = s; wifiOnConnect = c; wifiOnDisconnect = d }
                    )
                    Kind.BLUETOOTH -> BluetoothEditor(
                        address = btAddress, name = btName,
                        onConn = btOnConnect, onDisc = btOnDisconnect,
                        onChange = { a, n, c, d ->
                            btAddress = a; btName = n; btOnConnect = c; btOnDisconnect = d
                        }
                    )
                    Kind.WEATHER -> WeatherEditor(
                        lat = wxLat, lng = wxLng,
                        condition = wxCondition, op = wxOp, threshold = wxThreshold,
                        onChange = { lat, lng, c, o, t ->
                            wxLat = lat; wxLng = lng; wxCondition = c; wxOp = o; wxThreshold = t
                        }
                    )
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

// ── New trigger editors ────────────────────────────────────────────

@Composable
private fun GeofenceEditor(
    lat: Double, lng: Double, radius: Float,
    onEnter: Boolean, onExit: Boolean,
    onChange: (Double, Double, Float, Boolean, Boolean) -> Unit
) {
    val ctx = LocalContext.current
    var latText by remember(lat) { mutableStateOf("%.5f".format(lat)) }
    var lngText by remember(lng) { mutableStateOf("%.5f".format(lng)) }
    var fetching by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = latText,
                onValueChange = {
                    latText = it
                    it.toDoubleOrNull()?.let { v -> onChange(v, lng, radius, onEnter, onExit) }
                },
                label = { Text("Lat") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = lngText,
                onValueChange = {
                    lngText = it
                    it.toDoubleOrNull()?.let { v -> onChange(lat, v, radius, onEnter, onExit) }
                },
                label = { Text("Lng") },
                modifier = Modifier.weight(1f)
            )
        }
        AssistChip(
            onClick = {
                if (fetching) return@AssistChip
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        ctx, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED) return@AssistChip
                fetching = true
                fetchLastLocation(ctx) { loc ->
                    fetching = false
                    if (loc != null) {
                        latText = "%.5f".format(loc.latitude)
                        lngText = "%.5f".format(loc.longitude)
                        onChange(loc.latitude, loc.longitude, radius, onEnter, onExit)
                    }
                }
            },
            label = {
                Text(
                    if (fetching) stringResource(R.string.reminder_geo_locating)
                    else stringResource(R.string.reminder_geo_use_current)
                )
            },
            leadingIcon = {
                Icon(Icons.Default.MyLocation, null)
            }
        )
        Text("Radius: ${radius.toInt()} m",
            style = MaterialTheme.typography.bodyMedium)
        androidx.compose.material3.Slider(
            value = radius,
            onValueChange = { onChange(lat, lng, it, onEnter, onExit) },
            valueRange = 50f..2000f,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = onEnter,
                onClick = { onChange(lat, lng, radius, !onEnter, onExit) },
                label = { Text(stringResource(R.string.reminder_geo_on_enter)) }
            )
            FilterChip(
                selected = onExit,
                onClick = { onChange(lat, lng, radius, onEnter, !onExit) },
                label = { Text(stringResource(R.string.reminder_geo_on_exit)) }
            )
        }
        Text(
            stringResource(R.string.reminder_geo_perm_note),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@SuppressLint("MissingPermission")
private fun fetchLastLocation(
    ctx: android.content.Context,
    onResult: (android.location.Location?) -> Unit,
) {
    // Caller has already checked ACCESS_FINE_LOCATION; SuppressLint is
    // safe here because this function is only ever invoked from inside
    // that permission gate.
    com.google.android.gms.location.LocationServices
        .getFusedLocationProviderClient(ctx)
        .lastLocation
        .addOnSuccessListener { onResult(it) }
        .addOnFailureListener { onResult(null) }
}

@Composable
private fun WifiEditor(
    ssid: String, onConn: Boolean, onDisc: Boolean,
    onChange: (String, Boolean, Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = ssid,
            onValueChange = { onChange(it, onConn, onDisc) },
            label = { Text("SSID") },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = onConn,
                onClick = { onChange(ssid, !onConn, onDisc) },
                label = { Text(stringResource(R.string.reminder_wifi_on_connect)) }
            )
            FilterChip(
                selected = onDisc,
                onClick = { onChange(ssid, onConn, !onDisc) },
                label = { Text(stringResource(R.string.reminder_wifi_on_disconnect)) }
            )
        }
    }
}

@Composable
private fun BluetoothEditor(
    address: String, name: String,
    onConn: Boolean, onDisc: Boolean,
    onChange: (String, String, Boolean, Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { onChange(address, it, onConn, onDisc) },
            label = { Text(stringResource(R.string.reminder_bt_name)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = address,
            onValueChange = { onChange(it, name, onConn, onDisc) },
            label = { Text(stringResource(R.string.reminder_bt_address)) },
            placeholder = { Text("AA:BB:CC:DD:EE:FF") },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = onConn,
                onClick = { onChange(address, name, !onConn, onDisc) },
                label = { Text(stringResource(R.string.reminder_bt_on_connect)) }
            )
            FilterChip(
                selected = onDisc,
                onClick = { onChange(address, name, onConn, !onDisc) },
                label = { Text(stringResource(R.string.reminder_bt_on_disconnect)) }
            )
        }
    }
}

@Composable
private fun WeatherEditor(
    lat: Double, lng: Double,
    condition: WeatherCondition, op: ComparisonOp, threshold: Float,
    onChange: (Double, Double, WeatherCondition, ComparisonOp, Float) -> Unit
) {
    var latText by remember(lat) { mutableStateOf("%.5f".format(lat)) }
    var lngText by remember(lng) { mutableStateOf("%.5f".format(lng)) }
    var thrText by remember(threshold) { mutableStateOf("%.1f".format(threshold)) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = latText,
                onValueChange = {
                    latText = it
                    it.toDoubleOrNull()?.let { v -> onChange(v, lng, condition, op, threshold) }
                },
                label = { Text("Lat") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = lngText,
                onValueChange = {
                    lngText = it
                    it.toDoubleOrNull()?.let { v -> onChange(lat, v, condition, op, threshold) }
                },
                label = { Text("Lng") },
                modifier = Modifier.weight(1f)
            )
        }
        // Condition picker
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WeatherCondition.values().forEach { c ->
                FilterChip(
                    selected = condition == c,
                    onClick = { onChange(lat, lng, c, op, threshold) },
                    label = { Text(c.shortLabel()) }
                )
            }
        }
        // Operator picker
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = op == ComparisonOp.GREATER_THAN,
                onClick = { onChange(lat, lng, condition, ComparisonOp.GREATER_THAN, threshold) },
                label = { Text("> ${stringResource(R.string.reminder_wx_greater)}") }
            )
            FilterChip(
                selected = op == ComparisonOp.LESS_THAN,
                onClick = { onChange(lat, lng, condition, ComparisonOp.LESS_THAN, threshold) },
                label = { Text("< ${stringResource(R.string.reminder_wx_less)}") }
            )
        }
        OutlinedTextField(
            value = thrText,
            onValueChange = {
                thrText = it
                it.toFloatOrNull()?.let { v -> onChange(lat, lng, condition, op, v) }
            },
            label = { Text(stringResource(R.string.reminder_wx_threshold) + " (${condition.unit()})") },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            stringResource(R.string.reminder_wx_note),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun WeatherCondition.shortLabel() = when (this) {
    WeatherCondition.TEMP_C -> "🌡 °C"
    WeatherCondition.RAIN_MM -> "🌧 mm"
    WeatherCondition.WIND_KMH -> "💨 km/h"
    WeatherCondition.HUMIDITY_PCT -> "💧 %"
}

private fun WeatherCondition.unit() = when (this) {
    WeatherCondition.TEMP_C -> "°C"
    WeatherCondition.RAIN_MM -> "mm"
    WeatherCondition.WIND_KMH -> "km/h"
    WeatherCondition.HUMIDITY_PCT -> "%"
}
