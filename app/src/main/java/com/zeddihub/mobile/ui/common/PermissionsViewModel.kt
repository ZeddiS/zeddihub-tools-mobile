package com.zeddihub.mobile.ui.common

import androidx.lifecycle.ViewModel
import com.zeddihub.mobile.data.remote.dto.PermissionsDto
import com.zeddihub.mobile.data.repository.FeatureState
import com.zeddihub.mobile.data.repository.PermissionsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Thin VM wrapper around PermissionsRepository so any Composable that
 * needs the role matrix can call hiltViewModel<PermissionsViewModel>()
 * without having to inject the repo directly. Exposes the StateFlow
 * verbatim — UI just `collectAsState()`s it.
 */
@HiltViewModel
class PermissionsViewModel @Inject constructor(
    private val repo: PermissionsRepository,
) : ViewModel() {
    val permissions: StateFlow<PermissionsDto> = repo.permissions
    fun stateOf(key: String): FeatureState = repo.stateOf(key)
}
