package com.zeddihub.mobile.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypted on-device store for the authenticated session and optional
 * "remember me" credentials.
 *
 * The session is persisted as a [Session] snapshot (user + bearer token),
 * making token-based API calls usable synchronously from the
 * [com.zeddihub.mobile.data.remote.AppHeadersInterceptor].
 */
@Singleton
class CredentialStore @Inject constructor(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _session = MutableStateFlow(readSession())
    val session: StateFlow<Session?> = _session.asStateFlow()

    fun saveSession(
        userId: Long,
        username: String,
        email: String,
        role: String,
        isAdmin: Boolean,
        token: String,
        expiresAt: Long,
        displayName: String? = null,
        avatarUrl: String? = null
    ) {
        prefs.edit()
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .putString(KEY_EMAIL, email)
            .putString(KEY_ROLE, role)
            .putBoolean(KEY_IS_ADMIN, isAdmin)
            .putString(KEY_TOKEN, token)
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .putString(KEY_DISPLAY, displayName)
            .putString(KEY_AVATAR, avatarUrl)
            .apply()
        _session.value = Session(
            userId = userId,
            username = username,
            email = email,
            role = role,
            isAdmin = isAdmin,
            token = token,
            expiresAt = expiresAt,
            displayName = displayName,
            avatarUrl = avatarUrl
        )
    }

    fun updateTokenExpiry(expiresAt: Long) {
        val current = _session.value ?: return
        prefs.edit().putLong(KEY_EXPIRES_AT, expiresAt).apply()
        _session.value = current.copy(expiresAt = expiresAt)
    }

    fun saveRememberedCredentials(username: String, password: String) {
        prefs.edit()
            .putString(KEY_REMEMBER_USER, username)
            .putString(KEY_REMEMBER_PASS, password)
            .putBoolean(KEY_REMEMBER_ON, true)
            .apply()
    }

    fun clearRememberedCredentials() {
        prefs.edit()
            .remove(KEY_REMEMBER_USER)
            .remove(KEY_REMEMBER_PASS)
            .putBoolean(KEY_REMEMBER_ON, false)
            .apply()
    }

    fun rememberedCredentials(): Credentials? {
        if (!prefs.getBoolean(KEY_REMEMBER_ON, false)) return null
        val u = prefs.getString(KEY_REMEMBER_USER, null) ?: return null
        val p = prefs.getString(KEY_REMEMBER_PASS, null) ?: return null
        return Credentials(u, p)
    }

    fun isRememberEnabled(): Boolean = prefs.getBoolean(KEY_REMEMBER_ON, false)

    fun clearSession() {
        prefs.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_USERNAME)
            .remove(KEY_EMAIL)
            .remove(KEY_ROLE)
            .remove(KEY_IS_ADMIN)
            .remove(KEY_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .remove(KEY_DISPLAY)
            .remove(KEY_AVATAR)
            .apply()
        _session.value = null
    }

    fun clearAll() {
        prefs.edit().clear().apply()
        _session.value = null
    }

    private fun readSession(): Session? {
        val username = prefs.getString(KEY_USERNAME, null) ?: return null
        val token = prefs.getString(KEY_TOKEN, null).orEmpty()
        return Session(
            userId = prefs.getLong(KEY_USER_ID, 0L),
            username = username,
            email = prefs.getString(KEY_EMAIL, null).orEmpty(),
            role = prefs.getString(KEY_ROLE, "user") ?: "user",
            isAdmin = prefs.getBoolean(KEY_IS_ADMIN, false),
            token = token,
            expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L),
            displayName = prefs.getString(KEY_DISPLAY, null),
            avatarUrl = prefs.getString(KEY_AVATAR, null)
        )
    }

    data class Session(
        val userId: Long,
        val username: String,
        val email: String,
        val role: String,
        val isAdmin: Boolean,
        val token: String,
        val expiresAt: Long,
        val displayName: String?,
        val avatarUrl: String?
    )

    data class Credentials(val username: String, val password: String)

    companion object {
        private const val PREFS_NAME = "zeddihub_secure_prefs"
        private const val KEY_USER_ID = "session_user_id"
        private const val KEY_USERNAME = "session_username"
        private const val KEY_EMAIL = "session_email"
        private const val KEY_ROLE = "session_role"
        private const val KEY_IS_ADMIN = "session_is_admin"
        private const val KEY_TOKEN = "session_token"
        private const val KEY_EXPIRES_AT = "session_expires_at"
        private const val KEY_DISPLAY = "session_display"
        private const val KEY_AVATAR = "session_avatar"
        private const val KEY_REMEMBER_USER = "remember_username"
        private const val KEY_REMEMBER_PASS = "remember_password"
        private const val KEY_REMEMBER_ON = "remember_enabled"
    }
}
