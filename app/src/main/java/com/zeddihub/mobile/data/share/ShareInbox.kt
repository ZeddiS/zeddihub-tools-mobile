package com.zeddihub.mobile.data.share

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide single-slot inbox for inbound share-intent payloads.
 *
 * When the user picks ZeddiHub from another app's "Share" sheet
 * (TikTok, YouTube, etc.) the OS launches MainActivity with an
 * ACTION_SEND intent. We can't pass that arbitrarily through the
 * navigation graph (Compose Navigation doesn't have first-class
 * support for one-shot intent payloads), so we drop it in here and
 * AppNavGraph drains it on next composition.
 *
 * The holder is intentionally a simple `object` rather than a Hilt
 * singleton — this is genuinely process-global state (intents land
 * before any DI graph is touched) and a Singleton wouldn't gain us
 * anything except boilerplate.
 *
 * Single-slot semantics: the flow holds the most recent shared URL.
 * After [consume] returns the value the slot is cleared so the same
 * URL doesn't keep re-firing on every screen recomposition.
 */
object ShareInbox {

    private val _pendingUrl = MutableStateFlow<String?>(null)
    val pendingUrl: StateFlow<String?> = _pendingUrl.asStateFlow()

    fun submit(url: String) {
        _pendingUrl.value = url
    }

    fun consume(): String? {
        val v = _pendingUrl.value
        if (v != null) _pendingUrl.value = null
        return v
    }
}
