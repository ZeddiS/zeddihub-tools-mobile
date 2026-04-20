package com.zeddihub.mobile

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ZeddiHubApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)

            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ALERTS,
                    "Server Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Kritické události serverů (pád, restart, update)"
                    enableVibration(true)
                }
            )

            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_UPDATES,
                    "Game / Mod Updates",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Nové verze her, Oxide, MetaMod"
                }
            )

            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_WATCH,
                    "Watch Mode",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Běžící dohledový foreground service"
                }
            )
        }
    }

    companion object {
        const val CHANNEL_ALERTS = "alerts"
        const val CHANNEL_UPDATES = "updates"
        const val CHANNEL_WATCH = "watch_mode"
    }
}
