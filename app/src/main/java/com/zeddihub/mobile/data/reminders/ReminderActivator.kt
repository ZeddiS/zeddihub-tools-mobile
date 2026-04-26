package com.zeddihub.mobile.data.reminders

import android.Manifest
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cross-cutting activator that wires every non-time-based reminder
 * trigger up to whatever subsystem actually fires it.
 *
 * Architecture:
 *   • Time-based triggers (TimeAt / TimeWeekly) live in [ReminderScheduler]
 *     because they're a 1:1 mapping to AlarmManager and need exact-alarm
 *     plumbing that doesn't apply to anyone else.
 *   • Geofence — registered with GeofencingClient, fires through a
 *     dedicated [ReminderGeofenceReceiver].
 *   • WiFi SSID — one global ConnectivityManager.NetworkCallback owned
 *     by the activator, dispatches to whichever rules match the
 *     currently-connected SSID. Single callback (not per-rule) because
 *     the OS rate-limits NetworkCallback registrations.
 *   • Bluetooth device — one global ACL-connect / disconnect receiver,
 *     same dispatch model as WiFi.
 *   • Weather — periodic WorkManager job per rule (smallest interval the
 *     framework allows is 15 min) that hits Open-Meteo and notifies on
 *     condition match.
 *
 * Permission situation is messy: geofence needs ACCESS_BACKGROUND_LOCATION
 * on API 29+, and the rest need at least foreground location for SSID
 * disambiguation on API 26+. We don't request inside this class — the
 * editor screen drives the prompts so the user sees the rationale next
 * to the field they're editing.
 */
@Singleton
class ReminderActivator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: ReminderStore,
) {
    private val geofencingClient by lazy { LocationServices.getGeofencingClient(context) }
    private val cm by lazy { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
    private val wifi by lazy { context.getSystemService(Context.WIFI_SERVICE) as? WifiManager }

    private var wifiCallback: ConnectivityManager.NetworkCallback? = null
    private var btReceiver: BroadcastReceiver? = null

    /** Re-arm a single rule against its appropriate subsystem. */
    fun reschedule(r: Reminder) {
        // Always tear down first so flips between trigger kinds don't leak
        // ghost geofences / worker entries.
        cancel(r.id, r.trigger)
        if (!r.enabled) return
        when (val t = r.trigger) {
            is ReminderTrigger.Geofence -> installGeofence(r, t)
            is ReminderTrigger.WifiSsid -> ensureWifiCallback()
            is ReminderTrigger.BluetoothDevice -> ensureBtReceiver()
            is ReminderTrigger.Weather -> enqueueWeather(r)
            else -> { /* time-based: handled by ReminderScheduler */ }
        }
    }

    fun cancel(id: String, trigger: ReminderTrigger?) {
        when (trigger) {
            is ReminderTrigger.Geofence -> removeGeofence(id)
            is ReminderTrigger.Weather -> WorkManager.getInstance(context)
                .cancelUniqueWork(weatherWorkName(id))
            else -> { /* nothing to undo per-rule */ }
        }
    }

    /**
     * Walk every reminder and re-install. Called from the boot receiver
     * since geofences and the WiFi/BT registrations don't survive reboot.
     */
    suspend fun reactivateAll() {
        val rules = store.load()
        for (r in rules) reschedule(r)
        // After per-rule reschedule we also (re)evaluate the global
        // WiFi/BT registrations: if no rule needs them anymore, drop the
        // callback so we don't burn battery for nothing.
        val wantsWifi = rules.any { it.enabled && it.trigger is ReminderTrigger.WifiSsid }
        val wantsBt = rules.any { it.enabled && it.trigger is ReminderTrigger.BluetoothDevice }
        if (!wantsWifi) tearDownWifi()
        if (!wantsBt) tearDownBt()
    }

    // ── Geofence ────────────────────────────────────────────────────

    @Suppress("MissingPermission")
    private fun installGeofence(r: Reminder, t: ReminderTrigger.Geofence) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return
        val transitions =
            (if (t.onEnter) Geofence.GEOFENCE_TRANSITION_ENTER else 0) or
                (if (t.onExit) Geofence.GEOFENCE_TRANSITION_EXIT else 0)
        if (transitions == 0) return // no edge selected — nothing to fire
        val gf = Geofence.Builder()
            .setRequestId(r.id)
            .setCircularRegion(t.lat, t.lng, t.radiusM)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(transitions)
            .build()
        val request = GeofencingRequest.Builder()
            // INITIAL_TRIGGER_ENTER means "if we're already inside the fence
            // when the rule is added, fire ENTER right away". Without this,
            // a user who creates a 'when I'm at home' rule while at home
            // wouldn't see anything until they leave and come back.
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(gf)
            .build()
        runCatching { geofencingClient.addGeofences(request, geofencePendingIntent) }
    }

    @Suppress("MissingPermission")
    private fun removeGeofence(id: String) {
        runCatching { geofencingClient.removeGeofences(listOf(id)) }
    }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, ReminderGeofenceReceiver::class.java).apply {
            action = ACTION_GEOFENCE
        }
        PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    // ── WiFi SSID ───────────────────────────────────────────────────

    private fun ensureWifiCallback() {
        if (wifiCallback != null) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                // SSID lives on the *transport info* on API 31+ (WifiInfo
                // attached). On earlier versions we fall back to WifiManager
                // which reads the currently-connected network's SSID.
                val ssid = currentSsid(caps)
                if (ssid != null) onWifiConnected(ssid)
            }
            override fun onLost(network: Network) {
                onWifiDisconnected()
            }
        }
        val req = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        runCatching { cm.registerNetworkCallback(req, cb) }
        wifiCallback = cb
    }

    private fun tearDownWifi() {
        wifiCallback?.let { runCatching { cm.unregisterNetworkCallback(it) } }
        wifiCallback = null
    }

    private fun currentSsid(caps: NetworkCapabilities): String? {
        if (Build.VERSION.SDK_INT >= 29) {
            // Try TransportInfo first — needs NEARBY_WIFI_DEVICES on 33+ to
            // get the real SSID, otherwise it's the placeholder "<unknown ssid>".
            val ti = caps.transportInfo
            if (ti is android.net.wifi.WifiInfo) {
                return ti.ssid?.trim('"')?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
            }
        }
        @Suppress("DEPRECATION")
        return wifi?.connectionInfo?.ssid?.trim('"')
            ?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
    }

    private fun onWifiConnected(ssid: String) {
        deliverOnDispatcherThread {
            val rules = store.load().filter { it.enabled }
            for (r in rules) {
                val t = r.trigger as? ReminderTrigger.WifiSsid ?: continue
                if (t.onConnect && t.ssid.equals(ssid, ignoreCase = true)) {
                    fireById(r.id)
                }
            }
        }
    }

    private fun onWifiDisconnected() {
        deliverOnDispatcherThread {
            val rules = store.load().filter { it.enabled }
            for (r in rules) {
                val t = r.trigger as? ReminderTrigger.WifiSsid ?: continue
                if (t.onDisconnect) fireById(r.id)
            }
        }
    }

    // ── Bluetooth ACL ────────────────────────────────────────────────

    private fun ensureBtReceiver() {
        if (btReceiver != null) return
        val rec = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= 33)
                    i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                else
                    @Suppress("DEPRECATION") i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                val addr = device?.address ?: return
                val connected = i.action == BluetoothDevice.ACTION_ACL_CONNECTED
                deliverOnDispatcherThread {
                    val rules = store.load().filter { it.enabled }
                    for (r in rules) {
                        val t = r.trigger as? ReminderTrigger.BluetoothDevice ?: continue
                        if (!t.address.equals(addr, ignoreCase = true)) continue
                        if (connected && t.onConnect) fireById(r.id)
                        if (!connected && t.onDisconnect) fireById(r.id)
                    }
                }
            }
        }
        val f = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        ContextCompat.registerReceiver(context, rec, f, ContextCompat.RECEIVER_NOT_EXPORTED)
        btReceiver = rec
    }

    private fun tearDownBt() {
        btReceiver?.let { runCatching { context.unregisterReceiver(it) } }
        btReceiver = null
    }

    // ── Weather ─────────────────────────────────────────────────────

    private fun enqueueWeather(r: Reminder) {
        // 15 min is the minimum periodic interval the framework permits,
        // and weather doesn't change faster than that anyway.
        val req = PeriodicWorkRequestBuilder<ReminderWeatherWorker>(15, TimeUnit.MINUTES)
            .setInputData(androidx.work.workDataOf(ReminderWeatherWorker.KEY_ID to r.id))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            weatherWorkName(r.id),
            ExistingPeriodicWorkPolicy.UPDATE,
            req
        )
    }

    private fun weatherWorkName(id: String) = "rem_weather_$id"

    // ── Helpers ─────────────────────────────────────────────────────

    /** Fire the notification for [id] by delegating to [ReminderScheduler]
     *  via its existing pending intent so all paths converge to the same
     *  channel + body builder. */
    private fun fireById(id: String) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderScheduler.ACTION_FIRE
            putExtra(ReminderScheduler.EXTRA_ID, id)
        }
        context.sendBroadcast(intent)
    }

    private fun deliverOnDispatcherThread(block: suspend () -> Unit) {
        // Most callers come in on the main thread (NetworkCallback /
        // BroadcastReceiver are both main-thread by default). Move the
        // disk-touching parts off the UI thread.
        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try { block() } catch (_: Throwable) { }
        }
    }

    companion object {
        const val ACTION_GEOFENCE = "com.zeddihub.mobile.REMINDER_GEOFENCE"
    }
}
