package com.zeddihub.mobile.data.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single shared geofence receiver — one per process, dispatches every
 * triggered fence to the matching reminder (we use the reminder ID as
 * the geofence requestId so the lookup is direct).
 */
@AndroidEntryPoint
class ReminderGeofenceReceiver : BroadcastReceiver() {

    @Inject lateinit var store: ReminderStore
    @Inject lateinit var scheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        val triggered = event.triggeringGeofences ?: return
        val transition = event.geofenceTransition

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val rules = store.load().filter { it.enabled }.associateBy { it.id }
                for (gf in triggered) {
                    val r = rules[gf.requestId] ?: continue
                    val t = r.trigger as? ReminderTrigger.Geofence ?: continue
                    val match = when (transition) {
                        Geofence.GEOFENCE_TRANSITION_ENTER -> t.onEnter
                        Geofence.GEOFENCE_TRANSITION_EXIT -> t.onExit
                        else -> false
                    }
                    if (match) scheduler.postNotification(r)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
