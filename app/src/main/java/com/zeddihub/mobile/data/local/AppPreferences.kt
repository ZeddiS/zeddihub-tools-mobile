package com.zeddihub.mobile.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.DARK.name)!!) }
            .getOrDefault(ThemeMode.DARK)

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
    }
}
