package com.zeddihub.mobile.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeddihub.mobile.data.local.AppPreferences
import com.zeddihub.mobile.data.local.LanguageCode
import com.zeddihub.mobile.data.remote.dto.HomeConfigDto
import com.zeddihub.mobile.data.repository.HomeConfigRepository
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
 * importing AppPreferences directly.
 */
@HiltViewModel
class HomeConfigViewModel @Inject constructor(
    private val repo: HomeConfigRepository,
    prefs: AppPreferences
) : ViewModel() {

    val config: StateFlow<HomeConfigDto> = repo.config
    val language: StateFlow<LanguageCode> = prefs.language

    init {
        // Fire-and-forget. Failures silently retain the cached value.
        viewModelScope.launch { repo.refresh() }
    }

    /** Explicit pull-to-refresh hook (not wired into UI yet). */
    fun refresh() {
        viewModelScope.launch { repo.refresh() }
    }
}
