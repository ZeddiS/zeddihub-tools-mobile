package com.zeddihub.mobile.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { DARK, LIGHT }

enum class LanguageCode(val tag: String) {
    CS("cs"),
    EN("en");

    companion object {
        fun fromTag(tag: String?): LanguageCode =
            values().firstOrNull { it.tag.equals(tag, ignoreCase = true) } ?: CS
    }
}

@Singleton
class AppPreferences @Inject constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(readTheme())
    val theme: StateFlow<ThemeMode> = _theme.asStateFlow()

    private val _language = MutableStateFlow(readLanguage())
    val language: StateFlow<LanguageCode> = _language.asStateFlow()

    private val _biometricEnabled = MutableStateFlow(readBiometric())
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled.asStateFlow()

    private val _autoUpdate = MutableStateFlow(prefs.getBoolean(KEY_AUTO_UPDATE, true))
    val autoUpdate: StateFlow<Boolean> = _autoUpdate.asStateFlow()

    private val _pushEnabled = MutableStateFlow(prefs.getBoolean(KEY_PUSH, true))
    val pushEnabled: StateFlow<Boolean> = _pushEnabled.asStateFlow()

    private val _appLockEnabled = MutableStateFlow(prefs.getBoolean(KEY_APP_LOCK, true))
    val appLockEnabled: StateFlow<Boolean> = _appLockEnabled.asStateFlow()

    private val _pinConfigured = MutableStateFlow(prefs.contains(KEY_PIN_HASH))
    val pinConfigured: StateFlow<Boolean> = _pinConfigured.asStateFlow()

    /**
     * Store a new PIN. Uses a per-user random salt and SHA-256 so the raw
     * PIN never touches disk. 4–12 digits accepted; caller must enforce
     * UX rules (matching confirm field, length range, etc.).
     */
    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = sha256(salt + pin.toByteArray(Charsets.UTF_8))
        prefs.edit()
            .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PIN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
        _pinConfigured.value = true
    }

    /** Clear the stored PIN (used when user disables it or resets the app). */
    fun clearPin() {
        prefs.edit().remove(KEY_PIN_HASH).remove(KEY_PIN_SALT).apply()
        _pinConfigured.value = false
    }

    /** Verify an entered PIN against the stored hash. */
    fun verifyPin(pin: String): Boolean {
        val saltB64 = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val hashB64 = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val salt = runCatching { Base64.decode(saltB64, Base64.NO_WRAP) }.getOrNull() ?: return false
        val expected = runCatching { Base64.decode(hashB64, Base64.NO_WRAP) }.getOrNull() ?: return false
        val actual = sha256(salt + pin.toByteArray(Charsets.UTF_8))
        return MessageDigest.isEqual(expected, actual)
    }

    private fun sha256(input: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(input)

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCK, enabled).apply()
        _appLockEnabled.value = enabled
    }

    fun setAutoUpdate(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_UPDATE, enabled).apply()
        _autoUpdate.value = enabled
    }

    fun setPushEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PUSH, enabled).apply()
        _pushEnabled.value = enabled
    }

    fun clearAll() {
        prefs.edit().clear().apply()
        _theme.value = ThemeMode.DARK
        _language.value = LanguageCode.CS
        _biometricEnabled.value = false
        _autoUpdate.value = true
        _pushEnabled.value = true
        _appLockEnabled.value = true
        _pinConfigured.value = false
    }

    fun setTheme(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _theme.value = mode
    }

    fun setLanguage(code: LanguageCode) {
        prefs.edit().putString(KEY_LANGUAGE, code.tag).apply()
        _language.value = code
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
        _biometricEnabled.value = enabled
    }

    private fun readTheme(): ThemeMode =
        runCatching {
            val stored = prefs.getString(KEY_THEME, ThemeMode.DARK.name) ?: ThemeMode.DARK.name
            ThemeMode.valueOf(stored)
        }.getOrDefault(ThemeMode.DARK)

    private fun readLanguage(): LanguageCode =
        LanguageCode.fromTag(prefs.getString(KEY_LANGUAGE, LanguageCode.CS.tag))

    private fun readBiometric(): Boolean = prefs.getBoolean(KEY_BIOMETRIC, false)

    companion object {
        private const val PREFS_NAME = "zeddihub_app_prefs"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_LANGUAGE = "language_code"
        private const val KEY_BIOMETRIC = "biometric_enabled"
        private const val KEY_AUTO_UPDATE = "auto_update"
        private const val KEY_PUSH = "push_enabled"
        private const val KEY_APP_LOCK = "app_lock_enabled"
        private const val KEY_PIN_HASH = "app_lock_pin_hash"
        private const val KEY_PIN_SALT = "app_lock_pin_salt"
    }
}
