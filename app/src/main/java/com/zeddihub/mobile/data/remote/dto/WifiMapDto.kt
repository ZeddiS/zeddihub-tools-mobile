package com.zeddihub.mobile.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WifiMapListDto(
    @Json(name = "entries") val entries: List<WifiMapEntryDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class WifiMapEntryDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "ssid") val ssid: String,
    @Json(name = "password") val password: String? = null,
    @Json(name = "is_open") val isOpen: Boolean = false,
    @Json(name = "security") val security: String? = null,
    @Json(name = "lat") val lat: Double,
    @Json(name = "lon") val lon: Double,
    @Json(name = "venue") val venue: String? = null,
    @Json(name = "note") val note: String? = null,
    @Json(name = "verified") val verified: Boolean = false,
    @Json(name = "created_at") val createdAt: Long? = null
)

@JsonClass(generateAdapter = true)
data class WifiMapSubmitRequest(
    @Json(name = "ssid") val ssid: String,
    @Json(name = "password") val password: String? = null,
    @Json(name = "is_open") val isOpen: Boolean = false,
    @Json(name = "security") val security: String? = null,
    @Json(name = "lat") val lat: Double,
    @Json(name = "lon") val lon: Double,
    @Json(name = "venue") val venue: String? = null,
    @Json(name = "note") val note: String? = null
)

@JsonClass(generateAdapter = true)
data class WifiMapSubmitResponse(
    @Json(name = "ok") val ok: Boolean = false,
    @Json(name = "id") val id: String? = null,
    @Json(name = "message") val message: String? = null
)
