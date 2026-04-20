package com.zeddihub.mobile.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthJsonDto(
    @Json(name = "users") val users: List<AuthUserDto> = emptyList(),
    @Json(name = "access_codes") val accessCodes: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AuthUserDto(
    @Json(name = "username") val username: String,
    @Json(name = "password") val password: String,
    @Json(name = "role") val role: String? = null,
    @Json(name = "display_name") val displayName: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null
)

data class AuthUser(
    val username: String,
    val role: String,
    val displayName: String?,
    val avatarUrl: String?
)
