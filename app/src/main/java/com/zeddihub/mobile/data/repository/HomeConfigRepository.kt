package com.zeddihub.mobile.data.repository

import android.content.Context
import com.squareup.moshi.Moshi
import com.zeddihub.mobile.data.remote.ApiService
import com.zeddihub.mobile.data.remote.dto.HomeCategoryDto
import com.zeddihub.mobile.data.remote.dto.HomeConfigBackfill
import com.zeddihub.mobile.data.remote.dto.HomeConfigDto
import com.zeddihub.mobile.data.remote.dto.HomeItemDto
import com.zeddihub.mobile.data.remote.fetchHomeConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads the admin-managed home config (categories → folders/tiles +
 * news) from `https://zeddihub.eu/api/home-config.php` (MySQL-backed).
 *
 * Caching strategy:
 *   • Startup: emit the last-known JSON from SharedPreferences
 *     immediately so the Dashboard renders something without waiting
 *     for the network. If the cached payload is in the legacy v0.5.x
 *     flat-shortcuts shape, we transparently lift it into a synthetic
 *     "Rychlé zkratky" category so the UI never sees an empty screen
 *     mid-migration.
 *   • First usable fetch: overwrite the cache and emit the fresh
 *     config.
 *   • Network failure: keep the current cached value (never blank).
 *   • Empty cache + network failure: emit [DEFAULT_CONFIG] so the app
 *     still has the four built-in shortcuts.
 *   • Server returned ok=false (schema missing, DB down): treat as a
 *     fetch failure so we don't overwrite a good cache with junk.
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

            // If the server explicitly reports failure (schema missing,
            // DB down, etc.) keep the previous value instead of wiping
            // it with an empty list.
            if (!fresh.ok && fresh.categories.isEmpty()) return false

            val normalised = normaliseLegacy(fresh)

            // Don't replace a usable cached value with a totally empty
            // fresh response — most likely indicates a misconfigured
            // server returning an error page rather than a deliberate
            // "hide everything" instruction from the admin.
            if (normalised.categories.isEmpty() &&
                _config.value.categories.isNotEmpty()
            ) {
                return false
            }

            _config.value = normalised
            saveCached(normalised)
            true
        } catch (t: Throwable) {
            false
        }
    }

    private fun loadCached(): HomeConfigDto? {
        val raw = prefs.getString(KEY_CACHED_JSON, null) ?: return null
        val parsed = runCatching { adapter.fromJson(raw) }.getOrNull() ?: return null
        return normaliseLegacy(parsed)
    }

    private fun saveCached(cfg: HomeConfigDto) {
        runCatching {
            val json = adapter.toJson(cfg) ?: return
            prefs.edit().putString(KEY_CACHED_JSON, json).apply()
        }
    }

    /**
     * If the payload arrived in the old v0.5.x shape (flat `shortcuts`
     * array, no categories), wrap it into a single synthetic
     * "Rychlé zkratky" category. No-op for properly-formed hierarchical
     * responses.
     */
    private fun normaliseLegacy(cfg: HomeConfigDto): HomeConfigDto {
        if (cfg.categories.isNotEmpty()) return cfg
        val lifted = HomeConfigBackfill.liftLegacy(cfg.legacyShortcuts)
        if (lifted.isEmpty()) return cfg
        return cfg.copy(categories = lifted, legacyShortcuts = emptyList())
    }

    companion object {
        private const val PREFS_NAME = "zeddihub_home_config_cache"
        private const val KEY_CACHED_JSON = "home_android_json"

        /**
         * Safety net shown when we've never successfully loaded the admin
         * config (fresh install, offline on first launch, etc.). Mirrors
         * the four shortcuts the app shipped with before v0.6.0, plus
         * the new "Školní pomůcky" folder so the feature is discoverable
         * even before the server responds.
         */
        val DEFAULT_CONFIG: HomeConfigDto = HomeConfigDto(
            ok = true,
            categories = listOf(
                HomeCategoryDto(
                    slug = "quick",
                    nameCs = "Rychlé zkratky",
                    nameEn = "Quick actions",
                    icon = "Bookmark",
                    color = "#5b9cf6",
                    items = listOf(
                        HomeItemDto(
                            type = "tile", navId = "speedtest",
                            icon = "NetworkCheck", color = "#5b9cf6",
                            labelCs = "Speedtest", labelEn = "Speedtest",
                        ),
                        HomeItemDto(
                            type = "tile", navId = "wifi_scan",
                            icon = "Wifi", color = "#22c55e",
                            labelCs = "WiFi", labelEn = "WiFi",
                        ),
                        HomeItemDto(
                            type = "tile", navId = "flashlight",
                            icon = "FlashlightOn", color = "#f59e0b",
                            labelCs = "Baterka", labelEn = "Flashlight",
                        ),
                        HomeItemDto(
                            type = "tile", navId = "decibel",
                            icon = "GraphicEq", color = "#8b5cf6",
                            labelCs = "Decibely", labelEn = "Decibels",
                        ),
                    ),
                ),
                HomeCategoryDto(
                    slug = "helpers",
                    nameCs = "Pomůcky",
                    nameEn = "Helpers",
                    icon = "Apps",
                    color = "#6366f1",
                    items = listOf(
                        HomeItemDto(
                            type = "folder", slug = "school",
                            nameCs = "Školní pomůcky",
                            nameEn = "School tools",
                            icon = "School", color = "#0ea5e9",
                            tiles = listOf(
                                com.zeddihub.mobile.data.remote.dto.HomeShortcutDto(
                                    navId = "school_grade", icon = "Calculate",
                                    color = "#0ea5e9", visible = true,
                                    labelCs = "Známky", labelEn = "Grades",
                                ),
                                com.zeddihub.mobile.data.remote.dto.HomeShortcutDto(
                                    navId = "school_units", icon = "SwapHoriz",
                                    color = "#14b8a6", visible = true,
                                    labelCs = "Jednotky", labelEn = "Units",
                                ),
                                com.zeddihub.mobile.data.remote.dto.HomeShortcutDto(
                                    navId = "school_formulas", icon = "Functions",
                                    color = "#8b5cf6", visible = true,
                                    labelCs = "Vzorce", labelEn = "Formulas",
                                ),
                                com.zeddihub.mobile.data.remote.dto.HomeShortcutDto(
                                    navId = "school_fractions", icon = "Percent",
                                    color = "#f59e0b", visible = true,
                                    labelCs = "Zlomky", labelEn = "Fractions",
                                ),
                                com.zeddihub.mobile.data.remote.dto.HomeShortcutDto(
                                    navId = "school_triangle", icon = "ChangeHistory",
                                    color = "#ef4444", visible = true,
                                    labelCs = "Trojúhelník", labelEn = "Triangle",
                                ),
                                com.zeddihub.mobile.data.remote.dto.HomeShortcutDto(
                                    navId = "school_stats", icon = "QueryStats",
                                    color = "#22c55e", visible = true,
                                    labelCs = "Statistika", labelEn = "Statistics",
                                ),
                                com.zeddihub.mobile.data.remote.dto.HomeShortcutDto(
                                    navId = "school_time", icon = "Schedule",
                                    color = "#64748b", visible = true,
                                    labelCs = "Čas", labelEn = "Time",
                                ),
                                com.zeddihub.mobile.data.remote.dto.HomeShortcutDto(
                                    navId = "periodic", icon = "Science",
                                    color = "#ec4899", visible = true,
                                    labelCs = "Periodická", labelEn = "Periodic",
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            news = emptyList(),
            updatedAt = "",
        )
    }
}
