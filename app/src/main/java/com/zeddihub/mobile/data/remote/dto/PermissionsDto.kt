package com.zeddihub.mobile.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Per-feature permission matrix for the calling user.
 *
 * Matches the JSON returned by /api/permissions.php on the web. The
 * `states` map is keyed by canonical feature key (`tools.servers`,
 * `help.call_recorder`, …) — admin maintains the full list at
 * Admin → Mobilní aplikace → Role a oprávnění.
 *
 * Three possible state values:
 *   • "visible" — feature shown normally
 *   • "soon"    — feature shown with a "SOON" badge, not clickable
 *   • "hidden"  — feature dropped from the listing entirely
 *
 * Unknown feature keys returned by the server are forward-compatible:
 * the app just ignores them. Likewise, features the server hasn't
 * heard of yet (because the build is newer than the admin matrix)
 * default to visible client-side.
 */
@JsonClass(generateAdapter = true)
data class PermissionsDto(
    @Json(name = "ok") val ok: Boolean = true,
    @Json(name = "role") val role: String = "guest",
    @Json(name = "ad_free") val adFree: Boolean = false,
    @Json(name = "states") val states: Map<String, String> = emptyMap(),
    @Json(name = "fetched_at") val fetchedAt: Long = 0,
)
