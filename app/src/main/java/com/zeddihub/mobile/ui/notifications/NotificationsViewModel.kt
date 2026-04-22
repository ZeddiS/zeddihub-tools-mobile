package com.zeddihub.mobile.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeddihub.mobile.data.alerts.Alert
import com.zeddihub.mobile.data.alerts.AlertRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repo: AlertRepository
) : ViewModel() {

    val alerts: StateFlow<List<Alert>> = repo.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unreadCount: StateFlow<Int> = repo.observeUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh() = viewModelScope.launch {
        _isRefreshing.value = true
        // The alerts are fed by Room/FCM and updated asynchronously. Give
        // the UI a moment so the gesture feels acknowledged.
        delay(400)
        _isRefreshing.value = false
    }

    fun markRead(id: Long) = viewModelScope.launch { repo.markRead(id) }
    fun markAllRead() = viewModelScope.launch { repo.markAllRead() }

    fun seedDemoAlert() = viewModelScope.launch {
        repo.ingest(
            severity = "warn",
            source = "desktop",
            title = "Demo alert",
            body = "Toto je testovací upozornění z ZeddiHub."
        )
    }
}
