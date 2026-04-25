package com.zeddihub.mobile.data.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.zeddihub.mobile.R
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules and delivers reminder notifications.
 *
 * Time triggers are wired through AlarmManager:
 *   • setExactAndAllowWhileIdle() so the user gets the alert even with
 *     the device dozing — required for medication / appointment style
 *     reminders. Asks for SCHEDULE_EXACT_ALARM on Android 12+.
 *   • One PendingIntent per reminder, requestCode = stable hash of id,
 *     so cancelling / rescheduling on edit is idempotent.
 *
 * Weekly recurring reminders re-arm themselves: the receiver fires,
 * delivers the notification, and computes the *next* qualifying
 * weekday + minute and schedules that. This keeps the AlarmManager
 * state minimal (one entry per rule) and resilient to reboots when
 * combined with BootReceiver.rescheduleAll().
 *
 * Other trigger kinds (geofence / wifi / bt / weather) are stored but
 * not yet armed in this iteration — they get their own delivery hooks
 * in v0.9.0 alongside the related hardware tools.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            val existing = nm.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.reminder_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.reminder_channel_desc)
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    /** Cancel + (re-)schedule the alarm for one reminder. */
    fun reschedule(r: Reminder) {
        cancel(r.id)
        if (!r.enabled) return
        val nextTime = nextTriggerEpoch(r.trigger) ?: return
        val intent = makeIntent(r.id)
        val canExact = if (Build.VERSION.SDK_INT >= 31) alarmManager.canScheduleExactAlarms() else true
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTime, intent)
        } else {
            // Fall back to inexact — better than nothing if the user denied
            // SCHEDULE_EXACT_ALARM. Drift up to ~10 min is acceptable.
            alarmManager.set(AlarmManager.RTC_WAKEUP, nextTime, intent)
        }
    }

    fun cancel(id: String) {
        alarmManager.cancel(makeIntent(id))
    }

    fun rescheduleAll(rules: List<Reminder>) {
        for (r in rules) reschedule(r)
    }

    private fun makeIntent(id: String): PendingIntent {
        val i = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_FIRE
            putExtra(EXTRA_ID, id)
        }
        return PendingIntent.getBroadcast(
            context, id.hashCode(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun postNotification(r: Reminder) {
        ensureChannel()
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(r.title.ifBlank { context.getString(R.string.reminder_default_title) })
            .setContentText(r.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(r.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(r.id.hashCode(), n)
        }
    }

    companion object {
        const val ACTION_FIRE = "com.zeddihub.mobile.REMINDER_FIRE"
        const val EXTRA_ID = "reminder_id"
        const val CHANNEL_ID = "zeddihub_reminders"

        /**
         * Compute the next epoch-ms instant when the trigger should fire,
         * or null if the trigger is not time-based (other triggers fire on
         * external events, not a schedule).
         *
         * For weekly: walks the days bitmask starting today, returning the
         * earliest future occurrence. If today's slot has already passed
         * we move on to the next set bit.
         */
        fun nextTriggerEpoch(t: ReminderTrigger): Long? {
            val now = System.currentTimeMillis()
            return when (t) {
                is ReminderTrigger.TimeAt -> if (t.epochMs > now) t.epochMs else null
                is ReminderTrigger.TimeWeekly -> {
                    val cal = Calendar.getInstance()
                    // Calendar.MONDAY = 2 .. SUNDAY = 1; we use bit0=Mon..bit6=Sun.
                    for (offset in 0..6) {
                        val c = (cal.clone() as Calendar).apply {
                            add(Calendar.DAY_OF_YEAR, offset)
                            set(Calendar.HOUR_OF_DAY, t.minuteOfDay / 60)
                            set(Calendar.MINUTE, t.minuteOfDay % 60)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val dow = c.get(Calendar.DAY_OF_WEEK)
                        val bit = when (dow) {
                            Calendar.MONDAY -> 0
                            Calendar.TUESDAY -> 1
                            Calendar.WEDNESDAY -> 2
                            Calendar.THURSDAY -> 3
                            Calendar.FRIDAY -> 4
                            Calendar.SATURDAY -> 5
                            Calendar.SUNDAY -> 6
                            else -> 0
                        }
                        if ((t.daysMask shr bit) and 1 == 1 && c.timeInMillis > now) {
                            return c.timeInMillis
                        }
                    }
                    null
                }
                else -> null
            }
        }
    }
}

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var store: ReminderStore
    @Inject lateinit var scheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(ReminderScheduler.EXTRA_ID) ?: return
        // Use a goAsync window so we can finish the disk read + notify
        // before the framework kills the receiver.
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val r = store.load().firstOrNull { it.id == id } ?: return@launch
                if (!r.enabled) return@launch
                scheduler.postNotification(r)
                // Re-arm weekly triggers; one-shots auto-disable.
                if (r.trigger is ReminderTrigger.TimeWeekly) {
                    scheduler.reschedule(r)
                } else if (r.trigger is ReminderTrigger.TimeAt) {
                    store.setEnabled(r.id, false)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
