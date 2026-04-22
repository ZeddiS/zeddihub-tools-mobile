package com.zeddihub.mobile.data.repository

import android.content.Context
import androidx.annotation.StringRes
import com.zeddihub.mobile.R

/**
 * Translates an [AuthError] into a user-visible message.
 *
 * Resolution order:
 * 1. Lookup the error [AuthError.key] in the canonical `err_*` string
 *    resources (matches desktop `_ERROR_CS` dict).
 * 2. If no mapping exists, fall back to the server-provided message
 *    (if non-blank).
 * 3. Finally, format `err_unknown_fallback` with the raw key.
 */
@StringRes
fun AuthError.toStringRes(): Int? = when (key) {
    "invalid_username"   -> R.string.err_invalid_username
    "invalid_email"      -> R.string.err_invalid_email
    "invalid_password"   -> R.string.err_invalid_password
    "captcha_required"   -> R.string.err_captcha_required
    "captcha_failed"     -> R.string.err_captcha_failed
    "captcha_network"    -> R.string.err_captcha_network
    "taken"              -> R.string.err_taken
    "bad_credentials"    -> R.string.err_bad_credentials
    "disabled"           -> R.string.err_disabled
    "too_fast"           -> R.string.err_too_fast
    "too_many_fails"     -> R.string.err_too_many_fails
    "daily_limit"        -> R.string.err_daily_limit
    "auth_required"      -> R.string.err_auth_required
    "auth_invalid"       -> R.string.err_auth_invalid
    "forbidden"          -> R.string.err_forbidden
    "not_found"          -> R.string.err_not_found
    "server_error"       -> R.string.err_server_error
    "missing_identifier" -> R.string.err_missing_identifier
    "missing_password"   -> R.string.err_missing_password
    "network"            -> R.string.err_network
    else -> null
}

fun AuthError.localize(context: Context): String {
    toStringRes()?.let { return context.getString(it) }
    if (!message.isNullOrBlank()) return message!!
    return context.getString(R.string.err_unknown_fallback, key)
}
