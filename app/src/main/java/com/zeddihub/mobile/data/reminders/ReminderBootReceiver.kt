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
            } finally {
                pending.finish()
            }
        }
    }
}
