package com.zeddihub.mobile.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Hierarchical home-screen configuration as served by
 * `https://zeddihub.eu/api/home-config.php` (MySQL-backed, edited in
 * Admin → Aplikace → Mobilní Domů).
 *
 * Structure:
 *   Kategorie (horizontal section) ── Položky:
 *     • Dlaždice  — single nav shortcut
 *     • Složka    — inline-expanding group of dlaždice (akordeon)
 *
 * All fields default to empty/safe values so a malformed or partial
 * response still deserializes cleanly; the Repository then falls back
 * to the bundled default when categories is empty.
 *
 * Backwards compat: we accept a legacy `shortcuts: [...]` array at the
 * top level via [HomeConfigBackfill] so old cached JSONs from v0.5.x
 * keep working after an upgrade without clearing the cache.
 */
@JsonClass(generateAdapter = true)
data class HomeConfigDto(
    @Json(name = "ok") val ok: Boolean = true,
    @Json(name = "categories") val categories: List<HomeCategoryDto> = emptyList(),
    @Json(name = "news") val news: List<HomeNewsDto> = emptyList(),
    @Json(name = "updated_at") val updatedAt: String = "",

    // Legacy v0.5.x fallback — present only in cached JSON from older
    // installs. Repository migrates these into a synthetic category.
    @Json(name = "shortcuts") val legacyShortcuts: List<HomeShortcutDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class HomeCategoryDto(
    @Json(name = "slug") val slug: String = "",
    @Json(name = "name_cs") val nameCs: String = "",
    @Json(name = "name_en") val nameEn: String = "",
    @Json(name = "icon") val icon: String = "Bookmark",
    @Json(name = "color") val color: String = "#5b9cf6",
    @Json(name = "items") val items: List<HomeItemDto> = emptyList(),
)

/**
 * Discriminated union: a home-item is either a single tile
 * (`type: "tile"`) or a folder that expands inline on tap
 * (`type: "folder"`). Moshi doesn't do polymorphic adapters without a
 * custom factory, so we model both shapes as flat fields with nullable
 * defaults and let [type] decide what the UI reads.
 */
@JsonClass(generateAdapter = true)
data class HomeItemDto(
    @Json(name = "type") val type: String = "tile",

    // tile-only
    @Json(name = "nav_id") val navId: String = "",

    // folder-only
    @Json(name = "slug") val slug: String = "",
    @Json(name = "name_cs") val nameCs: String = "",
    @Json(name = "name_en") val nameEn: String = "",
    @Json(name = "tiles") val tiles: List<HomeShortcutDto> = emptyList(),

    // shared
    @Json(name = "icon") val icon: String = "Star",
    @Json(name = "color") val color: String = "#5b9cf6",
    @Json(name = "label_cs") val labelCs: String = "",
    @Json(name = "label_en") val labelEn: String = "",
) {
    val isFolder: Boolean get() = type.equals("folder", ignoreCase = true)
    val isTile: Boolean get() = !isFolder
}

/**
 * Flat shortcut entry. Used for:
 *  • Tiles inside a folder (HomeItemDto.tiles[])
 *  • Legacy v0.5.x cached payloads (HomeConfigDto.legacyShortcuts)
 */
@JsonClass(generateAdapter = true)
data class HomeShortcutDto(
    @Json(name = "nav_id") val navId: String,
    @Json(name = "icon") val icon: String = "Star",
    @Json(name = "color") val color: String = "#5b9cf6",
    @Json(name = "visible") val visible: Boolean = true,
    @Json(name = "label_cs") val labelCs: String = "",
    @Json(name = "label_en") val labelEn: String = ""
)

@JsonClass(generateAdapter = true)
data class HomeNewsDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "date") val date: String = "",
    @Json(name = "pinned") val pinned: Boolean = false,
    @Json(name = "url") val url: String = "",
    @Json(name = "title_cs") val titleCs: String = "",
    @Json(name = "title_en") val titleEn: String = "",
    @Json(name = "body_cs") val bodyCs: String = "",
    @Json(name = "body_en") val bodyEn: String = ""
)

// Lightweight helper used by the Repository to synthesise a category
// from legacy `shortcuts: [...]` caches.
internal object HomeConfigBackfill {
    fun liftLegacy(legacy: List<HomeShortcutDto>): List<HomeCategoryDto> {
        if (legacy.isEmpty()) return emptyList()
        val items = legacy
            .filter { it.visible }
            .map { s ->
                HomeItemDto(
                    type = "tile",
                    navId = s.navId,
                    icon = s.icon,
                    color = s.color,
                    labelCs = s.labelCs,
                    labelEn = s.labelEn,
                )
            }
        return listOf(
            HomeCategoryDto(
                slug = "quick",
                nameCs = "Rychlé zkratky",
                nameEn = "Quick actions",
                icon = "Bookmark",
                color = "#5b9cf6",
                items = items,
            )
        )
    }
}
