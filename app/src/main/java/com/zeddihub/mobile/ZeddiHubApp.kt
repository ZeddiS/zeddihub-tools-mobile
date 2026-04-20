package com.zeddihub.mobile

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.zeddihub.mobile.data.local.AppPreferences
import com.zeddihub.mobile.data.telemetry.TelemetryRecorder
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ZeddiHubApp : Application() {

    @Inject lateinit var appPreferences: AppPreferences
    @Inject lateinit var telemetry: TelemetryRecorder

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(appPreferences.language.value.tag)
        )
        createNotificationChannels()
        installCrashHandler()
    }

    private fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { telemetry.crash("${throwable.javaClass.simpleName}: ${throwable.message}") }
            previous?.uncaughtException(thread, throwable)
        }
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
