package com.zeddihub.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.lifecycleScope
import com.zeddihub.mobile.data.local.AppPreferences
import com.zeddihub.mobile.data.local.LanguageCode
import com.zeddihub.mobile.data.local.ThemeMode
import com.zeddihub.mobile.data.repository.AuthRepository
import com.zeddihub.mobile.data.share.ShareInbox
import com.zeddihub.mobile.data.telemetry.TelemetryRecorder
import com.zeddihub.mobile.data.update.UpdateChecker
import com.zeddihub.mobile.ui.common.AppLockState
import kotlinx.coroutines.launch
import com.zeddihub.mobile.ui.common.BiometricLockGate
import com.zeddihub.mobile.ui.common.DuplicateInstallGate
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

    @Inject
    lateinit var authRepository: AuthRepository

    private var sessionStartNs: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleManager.apply(appPreferences.language.value)
        enableEdgeToEdge()

        sessionStartNs = System.nanoTime()
        telemetry.sessionStart()

        // Catch a Share intent on cold start. Anything coming from
        // TikTok / YouTube / browser "Share" sheets lands as ACTION_SEND
        // with a single text/plain URL. We stash it in ShareInbox so
        // the nav graph can route to the Video Downloader screen with
        // the URL pre-filled. onNewIntent below covers the warm path
        // (singleTask launchMode keeps re-using this activity).
        handleShareIntent(intent)

        // Only lock on a genuine cold start. Configuration changes
        // (e.g. rotation) keep the existing unlocked state.
        if (savedInstanceState == null) {
            appLockState.lock()
            // Cold-start session resume per MOBILE_SYNC_v2 §3.1: validate
            // cached token via /me, fall back to stored-password re-login,
            // or leave session cleared so the nav graph routes to Login.
            lifecycleScope.launch {
                runCatching { authRepository.resumeSession() }
            }
        }

        setContent {
            val themeMode by appPreferences.theme.collectAsState()
            val language by appPreferences.language.collectAsState()
            val appLockEnabled by appPreferences.appLockEnabled.collectAsState()
            val pinConfigured by appPreferences.pinConfigured.collectAsState()
            val unlocked by appLockState.unlocked.collectAsState()
            val session by authRepository.session.collectAsState()

            ZeddiHubTheme(darkTheme = themeMode == ThemeMode.DARK) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Biometric lock is only meaningful when:
                    //   • the user is actually logged in (session != null), AND
                    //   • the "remember me" flow is active (appLockEnabled is
                    //     set exclusively by successful login with remember=true).
                    // A logged-out user should never be blocked by a biometric
                    // prompt — they can't log in until they see LoginScreen.
                    val hasSession = session != null
                    val hasRemembered = remember(session) {
                        authRepository.rememberedCredentials() != null
                    }
                    val locked = appLockEnabled && hasSession && hasRemembered && !unlocked
                    DuplicateInstallGate {
                        BiometricLockGate(
                            locked = locked,
                            pinConfigured = pinConfigured,
                            onUnlocked = { appLockState.markUnlocked() },
                            verifyPin = { pin -> appPreferences.verifyPin(pin) }
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
                        }
                        // Update dialog lives outside BiometricLockGate so it
                        // is visible even for logged-out users on LoginScreen.
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
        // Ignore onStop triggered by configuration changes (rotation, etc.)
        // so they do not count as a background trip.
        if (!isChangingConfigurations) {
            appLockState.lastStoppedAt = System.currentTimeMillis()
        }
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

    /**
     * Warm-restart Share intent handler. launchMode="singleTask" routes
     * a second SEND through this method instead of starting a fresh
     * activity, so we re-call setIntent + drain into ShareInbox so the
     * nav graph picks the URL up.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        if (intent.type != "text/plain") return
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
        if (text.isEmpty()) return
        // Some apps share a caption + URL on separate lines (TikTok in
        // particular). Pick the first http(s) token so we don't try to
        // download the caption.
        val url = text.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("http://") || it.startsWith("https://") }
            ?: return
        ShareInbox.submit(url)
    }

    companion object {
        private const val BACKGROUND_LOCK_MS = 30_000L
    }
}
