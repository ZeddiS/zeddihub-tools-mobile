package com.zeddihub.mobile.data.repository

import android.content.Context
import com.squareup.moshi.Moshi
import com.zeddihub.mobile.data.remote.ApiService
import com.zeddihub.mobile.data.remote.dto.HomeConfigDto
import com.zeddihub.mobile.data.remote.dto.HomeShortcutDto
import com.zeddihub.mobile.data.remote.fetchHomeConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads the admin-managed home config (shortcuts + news) from
 * `https://zeddihub.eu/tools/data/home_android.json`.
 *
 * Caching strategy:
 *   • Startup: emit the last-known JSON from SharedPreferences
 *     immediately so the Dashboard renders something without waiting
 *     for the network.
 *   • First usable fetch: overwrite the cache and emit the fresh
 *     config.
 *   • Network failure: keep the current cached value (never blank).
 *   • Empty cache + network failure: emit [DEFAULT_CONFIG] so the app
 *     still has the four built-in shortcuts.
 */
@Singleton
class HomeConfigRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val api: ApiService,
    moshi: Moshi
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val adapter = moshi.adapter(HomeConfigDto::class.java)

    private val _config = MutableStateFlow(loadCached() ?: DEFAULT_CONFIG)
    val config: StateFlow<HomeConfigDto> = _config.asStateFlow()

    /**
     * Fetch the latest config from the server and update the cache.
     * Failures are swallowed — callers only care that the StateFlow
     * eventually reflects the truth, not that every call succeeds.
     */
    suspend fun refresh(): Boolean {
        return try {
            val fresh = api.fetchHomeConfig()
            _config.value = fresh
            saveCached(fresh)
            true
        } catch (t: Throwable) {
            false
        }
    }

    private fun loadCached(): HomeConfigDto? {
        val raw = prefs.getString(KEY_CACHED_JSON, null) ?: return null
        return runCatching { adapter.fromJson(raw) }.getOrNull()
    }

    private fun saveCached(cfg: HomeConfigDto) {
        runCatching {
            val json = adapter.toJson(cfg) ?: return
            prefs.edit().putString(KEY_CACHED_JSON, json).apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "zeddihub_home_config_cache"
        private const val KEY_CACHED_JSON = "home_android_json"

        /**
         * Safety net shown when we've never successfully loaded the admin
         * config (fresh install, offline on first launch, etc.). Mirrors
         * the four shortcuts the app shipped with before v0.6.0.
         */
        val DEFAULT_CONFIG = HomeConfigDto(
            shortcuts = listOf(
                HomeShortcutDto(
                    navId = "speedtest",
                    icon = "NetworkCheck",
                    color = "#5b9cf6",
                    visible = true,
                    labelCs = "Speedtest",
                    labelEn = "Speedtest"
                ),
                HomeShortcutDto(
                    navId = "wifi_scan",
                    icon = "Wifi",
                    color = "#22c55e",
                    visible = true,
                    labelCs = "WiFi",
                    labelEn = "WiFi"
                ),
                HomeShortcutDto(
                    navId = "flashlight",
                    icon = "FlashlightOn",
                    color = "#f59e0b",
                    visible = true,
                    labelCs = "Baterka",
                    labelEn = "Flashlight"
                ),
                HomeShortcutDto(
                    navId = "decibel",
                    icon = "GraphicEq",
                    color = "#8b5cf6",
                    visible = true,
                    labelCs = "Decibely",
                    labelEn = "Decibels"
                )
            ),
            news = emptyList(),
            updatedAt = ""
        )
    }
}
