package com.zeddihub.mobile.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Raw JSON shape of `tools/data/home_android.json` managed in the admin
 * panel. Served statically from the website as a plain HTTPS GET.
 *
 * All fields have lenient defaults so a missing/empty file (or one still
 * being authored) degrades gracefully to the hardcoded fallback on the
 * mobile side.
 */
@JsonClass(generateAdapter = true)
data class HomeConfigDto(
    @Json(name = "shortcuts") val shortcuts: List<HomeShortcutDto> = emptyList(),
    @Json(name = "news") val news: List<HomeNewsDto> = emptyList(),
    @Json(name = "updated_at") val updatedAt: String = ""
)

@JsonClass(generateAdapter = true)
data class HomeShortcutDto(
    @Json(name = "nav_id") val navId: String,
    @Json(name = "icon") val icon: String = "NetworkCheck",
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
