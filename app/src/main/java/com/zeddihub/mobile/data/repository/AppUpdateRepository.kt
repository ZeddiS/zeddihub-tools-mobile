package com.zeddihub.mobile.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.zeddihub.mobile.BuildConfig
import com.zeddihub.mobile.data.remote.ApiService
import com.zeddihub.mobile.data.remote.dto.AppVersionDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Checks whether a newer *published* version of the app is available
 * via `GET /api/app-version.php`. The admin must explicitly publish a
 * pending release via the admin panel — that's the whole point of the
 * release-gating system.
 *
 * Behaviour:
 *   • On startup, fires [checkForUpdates] once (fire-and-forget).
 *   • On success, [state] holds `Available(...)` if the fetched
 *     `version_code > BuildConfig.VERSION_CODE`; otherwise `UpToDate`.
 *   • Any failure (network, DB down, schema missing) collapses to
 *     `UpToDate` — fail-open, no nagging.
 *   • A "dismissed" version is remembered in SharedPreferences so the
 *     user isn't pestered with the same banner every launch. Dismissing
 *     version N won't suppress the banner for N+1.
 */
@Singleton
class AppUpdateRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val api: ApiService,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Unknown)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    /**
     * One-shot fetch. Safe to call repeatedly — the StateFlow will
     * simply be overwritten. Swallows all errors (fail-open).
     */
    suspend fun checkForUpdates() {
        val fresh: AppVersionDto = runCatching { api.fetchAppVersion() }
            .getOrElse {
                _state.value = UpdateState.UpToDate
                return
            }

        if (!fresh.ok || fresh.versionCode <= 0) {
            _state.value = UpdateState.UpToDate
            return
        }

        if (fresh.versionCode <= BuildConfig.VERSION_CODE) {
            _state.value = UpdateState.UpToDate
            return
        }

        // Newer version exists. Respect a prior dismissal for the exact
        // same version_code — don't spam the user with the same banner.
        val dismissed = prefs.getInt(KEY_DISMISSED_CODE, 0)
        _state.value = UpdateState.Available(
            versionCode = fresh.versionCode,
            versionName = fresh.versionName,
            apkUrl = fresh.apkUrl,
            releaseNotesCs = fresh.releaseNotesCs,
            releaseNotesEn = fresh.releaseNotesEn,
            dismissed = dismissed == fresh.versionCode,
        )
    }

    /**
     * Remember that the user dismissed this specific version. The
     * banner will not reappear until a strictly newer version is
     * published.
     */
    fun dismissCurrent() {
        val current = _state.value
        if (current is UpdateState.Available) {
            prefs.edit().putInt(KEY_DISMISSED_CODE, current.versionCode).apply()
            _state.value = current.copy(dismissed = true)
        }
    }

    companion object {
        private const val PREFS = "zeddihub_app_update"
        private const val KEY_DISMISSED_CODE = "dismissed_version_code"
    }
}

sealed interface UpdateState {
    /** Haven't checked yet. */
    object Unknown : UpdateState
    /** Checked and we're on the latest published version (or check failed). */
    object UpToDate : UpdateState
    /** A newer published version is available. */
    data class Available(
        val versionCode: Int,
        val versionName: String,
        val apkUrl: String,
        val releaseNotesCs: String,
        val releaseNotesEn: String,
        /** True if the user has already tapped "Později" on this exact version. */
        val dismissed: Boolean,
    ) : UpdateState
}
