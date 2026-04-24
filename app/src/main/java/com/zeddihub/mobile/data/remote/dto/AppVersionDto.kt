package com.zeddihub.mobile.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response from `GET /api/app-version.php?platform=mobile`.
 *
 * When no release has been published yet the server still returns
 * `ok=true` with `versionCode = 0`, so clients can treat a missing
 * published release as "you're already up-to-date" without having to
 * parse error codes.
 *
 * The client-side update banner fires when:
 *   - [ok] is true, AND
 *   - [versionCode] > BuildConfig.VERSION_CODE
 *
 * Any other case (network failure, server error, version_code == 0) is
 * treated as "no update to show" — this matches the server's fail-open
 * behaviour so a broken server doesn't pester the user.
 */
@JsonClass(generateAdapter = true)
data class AppVersionDto(
    @Json(name = "ok")               val ok: Boolean = false,
    @Json(name = "platform")         val platform: String = "",
    @Json(name = "version_code")     val versionCode: Int = 0,
    @Json(name = "version_name")     val versionName: String = "",
    @Json(name = "apk_url")          val apkUrl: String = "",
    @Json(name = "release_notes_cs") val releaseNotesCs: String = "",
    @Json(name = "release_notes_en") val releaseNotesEn: String = "",
    @Json(name = "min_sdk")          val minSdk: Int = 0,
    @Json(name = "published_at")     val publishedAt: Long? = null,
)
