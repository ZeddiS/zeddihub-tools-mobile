package com.zeddihub.mobile.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeddihub.mobile.data.local.AppPreferences
import com.zeddihub.mobile.data.local.CredentialStore
import com.zeddihub.mobile.data.local.LanguageCode
import com.zeddihub.mobile.data.local.ThemeMode
import com.zeddihub.mobile.data.update.ReleaseInfo
import com.zeddihub.mobile.data.update.UpdateChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val theme: ThemeMode = ThemeMode.DARK,
    val language: LanguageCode = LanguageCode.CS,
    val autoUpdate: Boolean = true,
    val pushEnabled: Boolean = true,
    val updateCheckState: UpdateCheckState = UpdateCheckState.Idle
)

sealed class UpdateCheckState {
    data object Idle : UpdateCheckState()
    data object Checking : UpdateCheckState()
    data object UpToDate : UpdateCheckState()
    data class Available(val info: ReleaseInfo) : UpdateCheckState()
    data object Downloading : UpdateCheckState()
    data class Downloaded(val file: File, val info: ReleaseInfo) : UpdateCheckState()
    data class Error(val message: String?) : UpdateCheckState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val prefs: AppPreferences,
    private val credentialStore: CredentialStore,
    private val updateChecker: UpdateChecker
) : ViewModel() {

    private val _state = MutableStateFlow(
        SettingsUiState(
            theme = prefs.theme.value,
            language = prefs.language.value,
            autoUpdate = prefs.autoUpdate.value,
            pushEnabled = prefs.pushEnabled.value
        )
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun setTheme(mode: ThemeMode) {
        prefs.setTheme(mode)
        _state.value = _state.value.copy(theme = mode)
    }

    fun setLanguage(code: LanguageCode) {
        prefs.setLanguage(code)
        _state.value = _state.value.copy(language = code)
    }

    fun setAutoUpdate(enabled: Boolean) {
        prefs.setAutoUpdate(enabled)
        _state.value = _state.value.copy(autoUpdate = enabled)
    }

    fun setPush(enabled: Boolean) {
        prefs.setPushEnabled(enabled)
        _state.value = _state.value.copy(pushEnabled = enabled)
    }

    fun clearCache() {
        runCatching {
            appContext.cacheDir.deleteRecursively()
            appContext.externalCacheDir?.deleteRecursively()
        }
    }

    fun factoryReset() {
        credentialStore.clearAll()
        prefs.clearAll()
    }

    fun checkForUpdates() {
        _state.value = _state.value.copy(updateCheckState = UpdateCheckState.Checking)
        viewModelScope.launch {
            val info = updateChecker.fetchLatest()
            _state.value = if (info == null) {
                _state.value.copy(updateCheckState = UpdateCheckState.Error("network"))
            } else if (updateChecker.isNewer(info) && info.apkUrl.isNotEmpty()) {
                _state.value.copy(updateCheckState = UpdateCheckState.Available(info))
            } else {
                _state.value.copy(updateCheckState = UpdateCheckState.UpToDate)
            }
        }
    }

    fun downloadUpdate(info: ReleaseInfo) {
        _state.value = _state.value.copy(updateCheckState = UpdateCheckState.Downloading)
        viewModelScope.launch {
            val file = updateChecker.downloadApk(appContext, info.apkUrl)
            _state.value = if (file == null) {
                _state.value.copy(updateCheckState = UpdateCheckState.Error("download"))
            } else {
                _state.value.copy(updateCheckState = UpdateCheckState.Downloaded(file, info))
            }
        }
    }

    fun installUpdate(file: File) {
        updateChecker.installApk(appContext, file)
    }

    fun resetUpdateCheck() {
        _state.value = _state.value.copy(updateCheckState = UpdateCheckState.Idle)
    }
}
