package com.zeddihub.mobile.data.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Re-arms all persisted reminders after device boot or app upgrade.
 *
 * AlarmManager loses every scheduled alarm across reboots, so without this
 * a user who restarts their phone would silently miss reminders until they
 * opened the app again. We listen for BOOT_COMPLETED *and*
 * LOCKED_BOOT_COMPLETED (direct-boot devices) plus MY_PACKAGE_REPLACED so
 * that an app update doesn't drop the schedule either.
 */
@AndroidEntryPoint
class ReminderBootReceiver : BroadcastReceiver() {

    @Inject lateinit var store: ReminderStore
    @Inject lateinit var scheduler: ReminderScheduler
    @Inject lateinit var activator: ReminderActivator

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val accepted = action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        if (!accepted) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                scheduler.ensureChannel()
                scheduler.rescheduleAll(store.load())
                // Geofence/WiFi/BT/weather subsystems forget their state
                // across reboots and app upgrades, so the activator has to
                // re-arm everything separately from the AlarmManager pass.
                activator.reactivateAll()
            } finally {
                pending.finish()
            }
        }
    }
}
