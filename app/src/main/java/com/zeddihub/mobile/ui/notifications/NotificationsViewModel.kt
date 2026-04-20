package com.zeddihub.mobile.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeddihub.mobile.data.alerts.Alert
import com.zeddihub.mobile.data.alerts.AlertRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
