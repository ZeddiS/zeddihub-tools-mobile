package com.zeddihub.mobile.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ---------------------------------------------------------------------------
// Domain model used throughout the UI layer.
// ---------------------------------------------------------------------------

/** Public-facing authenticated user snapshot. */
data class AuthUser(
    val id: Long,
    val username: String,
    val email: String,
    val role: String,
    val isAdmin: Boolean,
    val displayName: String? = null,
    val avatarUrl: String? = null
)

// ---------------------------------------------------------------------------
// Wire DTOs matching /api/auth/* (see website/api/auth/CONTRACT.md).
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "identifier") val identifier: String,
    @Json(name = "password") val password: String,
    @Json(name = "captcha_token") val captchaToken: String? = null
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    @Json(name = "username") val username: String,
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "captcha_token") val captchaToken: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    @Json(name = "ok") val ok: Boolean = false,
    @Json(name = "user") val user: UserDto? = null,
    @Json(name = "token") val token: String? = null,
    @Json(name = "expires_at") val expiresAt: Long? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class UserDto(
    @Json(name = "id") val id: Long = 0L,
    @Json(name = "username") val username: String = "",
    @Json(name = "email") val email: String = "",
    @Json(name = "role") val role: String? = "user",
    @Json(name = "is_admin") val isAdmin: Boolean = false,
    @Json(name = "display_name") val displayName: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class MeResponse(
    @Json(name = "ok") val ok: Boolean = false,
    @Json(name = "user") val user: UserDto? = null,
    @Json(name = "expires_at") val expiresAt: Long? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class LogoutResponse(
    @Json(name = "ok") val ok: Boolean = false
)

// ---------------------------------------------------------------------------
// Legacy DTOs used by the auth.json fallback path. Kept so existing code
// that references them still compiles; the fallback itself is opt-in via
// [AuthRepository].
// ---------------------------------------------------------------------------

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
