package com.zeddihub.mobile.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ServerDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "game") val game: String,
    @Json(name = "status") val status: String,
    @Json(name = "playersOnline") val playersOnline: Int,
    @Json(name = "playersMax") val playersMax: Int,
    @Json(name = "fps") val fps: Double?,
    @Json(name = "tickRate") val tickRate: Double?,
    @Json(name = "cpuUsage") val cpuUsage: Double,
    @Json(name = "ramUsageMb") val ramUsageMb: Long,
    @Json(name = "ramTotalMb") val ramTotalMb: Long,
    @Json(name = "uptimeSeconds") val uptimeSeconds: Long,
    @Json(name = "map") val map: String?,
    @Json(name = "lastSeen") val lastSeen: String,
    @Json(name = "host") val host: String = "",
    @Json(name = "port") val port: Int = 0,
    @Json(name = "pingMs") val pingMs: Long? = null
) {
    val address: String get() = if (host.isNotEmpty() && port > 0) "$host:$port" else ""
}

@JsonClass(generateAdapter = true)
data class RconCommandRequest(
    @Json(name = "command") val command: String,
    @Json(name = "mfaToken") val mfaToken: String? = null
)

@JsonClass(generateAdapter = true)
data class RconCommandResponse(
    @Json(name = "ok") val ok: Boolean,
    @Json(name = "output") val output: String
)
