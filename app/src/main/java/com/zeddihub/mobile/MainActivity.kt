package com.zeddihub.mobile

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.compose.rememberNavController
import com.zeddihub.mobile.data.local.AppPreferences
import com.zeddihub.mobile.data.local.LanguageCode
import com.zeddihub.mobile.data.local.ThemeMode
import com.zeddihub.mobile.data.telemetry.TelemetryRecorder
import com.zeddihub.mobile.data.update.UpdateChecker
import com.zeddihub.mobile.ui.common.AppLockState
import com.zeddihub.mobile.ui.common.BiometricLockGate
import com.zeddihub.mobile.ui.common.StartupUpdateDialog
import com.zeddihub.mobile.ui.navigation.AppNavGraph
import com.zeddihub.mobile.ui.theme.ZeddiHubTheme
import com.zeddihub.mobile.util.LocaleManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var updateChecker: UpdateChecker

    @Inject
    lateinit var telemetry: TelemetryRecorder

    @Inject
    lateinit var appLockState: AppLockState

    private var sessionStartNs: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleManager.apply(appPreferences.language.value)
        enableEdgeToEdge()

        sessionStartNs = System.nanoTime()
        telemetry.sessionStart()

        // Cold start / process recreation always starts locked.
        appLockState.lock()

        setContent {
            val themeMode by appPreferences.theme.collectAsState()
            val language by appPreferences.language.collectAsState()
            val appLockEnabled by appPreferences.appLockEnabled.collectAsState()
            val unlocked by appLockState.unlocked.collectAsState()

            ZeddiHubTheme(darkTheme = themeMode == ThemeMode.DARK) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val locked = appLockEnabled && !unlocked
                    BiometricLockGate(
                        locked = locked,
                        onUnlocked = { appLockState.markUnlocked() }
                    ) {
                        val navController = rememberNavController()
                        AppNavGraph(
                            navController = navController,
                            currentLanguage = language,
                            currentTheme = themeMode,
                            onLanguage = { code: LanguageCode ->
                                if (code != appPreferences.language.value) {
                                    appPreferences.setLanguage(code)
                                    LocaleManager.apply(code)
                                    recreate()
                                }
                            },
                            onTheme = { mode: ThemeMode ->
                                appPreferences.setTheme(mode)
                            }
                        )
                        if (appPreferences.autoUpdate.value) {
                            StartupUpdateDialog(updateChecker = updateChecker)
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        appLockState.lastStoppedAt = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        // Re-lock if the app was backgrounded for longer than the grace
        // window. Short trips (system dialogs, biometric prompt flicker)
        // stay unlocked to avoid a prompt loop.
        val last = appLockState.lastStoppedAt
        if (last != 0L && System.currentTimeMillis() - last > BACKGROUND_LOCK_MS) {
            appLockState.lock()
        }
    }

    override fun onDestroy() {
        val elapsed = (System.nanoTime() - sessionStartNs) / 1_000_000L
        telemetry.sessionEnd(elapsed)
        super.onDestroy()
    }

    companion object {
        private const val BACKGROUND_LOCK_MS = 30_000L
    }
}
