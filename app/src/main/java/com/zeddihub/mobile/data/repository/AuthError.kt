package com.zeddihub.mobile.data.repository

/**
 * Canonical taxonomy of errors returned by the `/api/auth/` endpoints.
 *
 * Mirrors the `_ERROR_CS` dict that the desktop client (`gui/auth.py`) uses,
 * and the `strings.xml` `err_*` resources referenced by the UI layer.
 *
 * Never widen this to accept arbitrary strings — if the server grows a new
 * error key, add a concrete subclass here **and** extend the server at the
 * same release boundary (see MOBILE_SYNC_v2.md §13).
 */
sealed class AuthError(
    val key: String,
    override val message: String?,
    val httpStatus: Int
) : Exception(message ?: key) {

    // Validation ---------------------------------------------------------
    class InvalidUsername(message: String? = null, status: Int = 400)
        : AuthError("invalid_username", message, status)
    class InvalidEmail(message: String? = null, status: Int = 400)
        : AuthError("invalid_email", message, status)
    class InvalidPassword(message: String? = null, status: Int = 400)
        : AuthError("invalid_password", message, status)
    class MissingIdentifier(message: String? = null, status: Int = 400)
        : AuthError("missing_identifier", message, status)
    class MissingPassword(message: String? = null, status: Int = 400)
        : AuthError("missing_password", message, status)

    // Captcha ------------------------------------------------------------
    class CaptchaRequired(message: String? = null, status: Int = 400)
        : AuthError("captcha_required", message, status)
    class CaptchaFailed(message: String? = null, status: Int = 400)
        : AuthError("captcha_failed", message, status)
    class CaptchaNetwork(message: String? = null, status: Int = 502)
        : AuthError("captcha_network", message, status)

    // Registration / login state ----------------------------------------
    class Taken(message: String? = null, status: Int = 409)
        : AuthError("taken", message, status)
    class BadCredentials(message: String? = null, status: Int = 401)
        : AuthError("bad_credentials", message, status)
    class Disabled(message: String? = null, status: Int = 403)
        : AuthError("disabled", message, status)

    // Rate limits --------------------------------------------------------
    class TooFast(message: String? = null, status: Int = 429)
        : AuthError("too_fast", message, status)
    class TooManyFails(message: String? = null, status: Int = 429)
        : AuthError("too_many_fails", message, status)
    class DailyLimit(message: String? = null, status: Int = 429)
        : AuthError("daily_limit", message, status)

    // Session ------------------------------------------------------------
    class AuthRequired(message: String? = null, status: Int = 401)
        : AuthError("auth_required", message, status)
    class AuthInvalid(message: String? = null, status: Int = 401)
        : AuthError("auth_invalid", message, status)

    // Admin --------------------------------------------------------------
    class Forbidden(message: String? = null, status: Int = 403)
        : AuthError("forbidden", message, status)
    class NotFound(message: String? = null, status: Int = 404)
        : AuthError("not_found", message, status)

    // Infra --------------------------------------------------------------
    class ServerError(message: String? = null, status: Int = 500)
        : AuthError("server_error", message, status)
    class Network(message: String? = null)
        : AuthError("network", message, 0)
    /** Caller did not receive ok:true but server did not send a known key. */
    class Unknown(keyValue: String?, message: String? = null, status: Int = 0)
        : AuthError(keyValue ?: "unknown", message, status)

    companion object {
        /** Map a server-supplied error key + optional message to a typed error. */
        fun fromKey(key: String?, message: String? = null, status: Int = 0): AuthError =
            when (key) {
                "invalid_username"    -> InvalidUsername(message, status.orDefault(400))
                "invalid_email"       -> InvalidEmail(message, status.orDefault(400))
                "invalid_password"    -> InvalidPassword(message, status.orDefault(400))
                "missing_identifier"  -> MissingIdentifier(message, status.orDefault(400))
                "missing_password"    -> MissingPassword(message, status.orDefault(400))
                "captcha_required"    -> CaptchaRequired(message, status.orDefault(400))
                "captcha_failed"      -> CaptchaFailed(message, status.orDefault(400))
                "captcha_network"     -> CaptchaNetwork(message, status.orDefault(502))
                "taken"               -> Taken(message, status.orDefault(409))
                "bad_credentials"     -> BadCredentials(message, status.orDefault(401))
                "disabled"            -> Disabled(message, status.orDefault(403))
                "too_fast"            -> TooFast(message, status.orDefault(429))
                "too_many_fails"      -> TooManyFails(message, status.orDefault(429))
                "daily_limit"         -> DailyLimit(message, status.orDefault(429))
                "auth_required"       -> AuthRequired(message, status.orDefault(401))
                "auth_invalid"        -> AuthInvalid(message, status.orDefault(401))
                "forbidden"           -> Forbidden(message, status.orDefault(403))
                "not_found"           -> NotFound(message, status.orDefault(404))
                "server_error"        -> ServerError(message, status.orDefault(500))
                null                  -> Unknown(null, message, status)
                else                  -> Unknown(key, message, status)
            }

        fun fromHttpCode(code: Int, message: String? = null): AuthError = when (code) {
            401 -> AuthRequired(message, code)
            403 -> Forbidden(message, code)
            404 -> NotFound(message, code)
            409 -> Taken(message, code)
            429 -> TooFast(message, code)
            in 500..599 -> ServerError(message, code)
            else -> Unknown(null, message, code)
        }

        private fun Int.orDefault(fallback: Int) = if (this > 0) this else fallback
    }
}
