package com.zeddihub.mobile.ui.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds whether the biometric app-lock has been passed for the current
 * foreground session. A cold start (or a long background period) resets it
 * back to locked.
 */
@Singleton
class AppLockState @Inject constructor() {
    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    /** Last moment the activity was stopped — used to decide whether a short
     *  background trip (e.g. biometric prompt itself) should re-lock. */
    var lastStoppedAt: Long = 0L

    fun markUnlocked() {
        _unlocked.value = true
        // Reset the background timestamp so a stale value (e.g. from a
        // previous backgrounding) cannot trigger a re-lock at the next
        // onStart when no real background trip happened after unlocking.
        lastStoppedAt = 0L
    }
    fun lock() { _unlocked.value = false }
}
