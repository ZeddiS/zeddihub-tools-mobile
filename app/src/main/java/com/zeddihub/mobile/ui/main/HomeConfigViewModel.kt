package com.zeddihub.mobile.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeddihub.mobile.data.local.AppPreferences
import com.zeddihub.mobile.data.local.LanguageCode
import com.zeddihub.mobile.data.remote.dto.HomeConfigDto
import com.zeddihub.mobile.data.repository.AppUpdateRepository
import com.zeddihub.mobile.data.repository.HomeConfigRepository
import com.zeddihub.mobile.data.repository.UpdateState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Thin wrapper around [HomeConfigRepository] for the Dashboard. On
 * creation we kick a background refresh so the user sees the most
 * recent shortcuts/news from the admin panel within one tab-open. The
 * repo already emits a cached value synchronously so the initial
 * render never shows an empty layout.
 *
 * Also surfaces the current UI language so the Dashboard can pick the
 * right localized title/body for each shortcut and news item without
 * importing AppPreferences directly, and wires the release-gating
 * update check so the Dashboard can show an "Nová verze" banner once
 * the admin publishes a pending release.
 */
@HiltViewModel
class HomeConfigViewModel @Inject constructor(
    private val repo: HomeConfigRepository,
    private val updateRepo: AppUpdateRepository,
    prefs: AppPreferences
) : ViewModel() {

    val config: StateFlow<HomeConfigDto> = repo.config
    val language: StateFlow<LanguageCode> = prefs.language
    val updateState: StateFlow<UpdateState> = updateRepo.state

    init {
        // Fire-and-forget. Failures silently retain the cached value.
        viewModelScope.launch { repo.refresh() }
        viewModelScope.launch { updateRepo.checkForUpdates() }
    }

    /** Explicit pull-to-refresh hook — also re-checks the update gate. */
    fun refresh() {
        viewModelScope.launch { repo.refresh() }
        viewModelScope.launch { updateRepo.checkForUpdates() }
    }

    /** Hide the update banner for this specific version_code. */
    fun dismissUpdate() {
        updateRepo.dismissCurrent()
    }
}
