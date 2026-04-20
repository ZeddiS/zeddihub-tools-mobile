package com.zeddihub.mobile.data.alerts

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.zeddihub.mobile.MainActivity
import com.zeddihub.mobile.R
import com.zeddihub.mobile.ZeddiHubApp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: AlertDao
) {

    fun observeRecent(): Flow<List<Alert>> = dao.observeRecent()
    fun observeUnreadCount(): Flow<Int> = dao.observeUnreadCount()

    suspend fun ingest(
        severity: String,
        source: String,
        title: String,
        body: String,
        timestamp: Long = System.currentTimeMillis()
    ): Long {
        val id = dao.insert(
            Alert(
                timestamp = timestamp,
                severity = severity,
                source = source,
                title = title,
                body = body
            )
        )
        postSystemNotification(id.toInt(), severity, title, body)
        return id
    }

    suspend fun markRead(id: Long) = dao.markRead(id)
    suspend fun markAllRead() = dao.markAllRead()

    private fun postSystemNotification(id: Int, severity: String, title: String, body: String) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val priority = when (severity) {
            "critical" -> NotificationCompat.PRIORITY_MAX
            "warn" -> NotificationCompat.PRIORITY_HIGH
            else -> NotificationCompat.PRIORITY_DEFAULT
        }
        val notif = NotificationCompat.Builder(context, ZeddiHubApp.CHANNEL_ALERTS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        nm.notify(id, notif)
    }
}
