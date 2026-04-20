package com.zeddihub.mobile.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

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
        username: String,
        role: String,
        displayName: String?,
        avatarUrl: String?
    ) {
        prefs.edit()
            .putString(KEY_USERNAME, username)
            .putString(KEY_ROLE, role)
            .putString(KEY_DISPLAY, displayName)
            .putString(KEY_AVATAR, avatarUrl)
            .apply()
        _session.value = Session(username, role, displayName, avatarUrl)
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
            .remove(KEY_USERNAME)
            .remove(KEY_ROLE)
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
        val u = prefs.getString(KEY_USERNAME, null) ?: return null
        val r = prefs.getString(KEY_ROLE, null) ?: "user"
        return Session(
            username = u,
            role = r,
            displayName = prefs.getString(KEY_DISPLAY, null),
            avatarUrl = prefs.getString(KEY_AVATAR, null)
        )
    }

    data class Session(
        val username: String,
        val role: String,
        val displayName: String?,
        val avatarUrl: String?
    )

    data class Credentials(val username: String, val password: String)

    companion object {
        private const val PREFS_NAME = "zeddihub_secure_prefs"
        private const val KEY_USERNAME = "session_username"
        private const val KEY_ROLE = "session_role"
        private const val KEY_DISPLAY = "session_display"
        private const val KEY_AVATAR = "session_avatar"
        private const val KEY_REMEMBER_USER = "remember_username"
        private const val KEY_REMEMBER_PASS = "remember_password"
        private const val KEY_REMEMBER_ON = "remember_enabled"
    }
}
