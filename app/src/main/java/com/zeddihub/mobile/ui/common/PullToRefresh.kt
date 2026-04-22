package com.zeddihub.mobile.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll

/**
 * Drop-in wrapper that gives any screen a Material3 pull-to-refresh behaviour.
 *
 * Usage:
 * ```
 * PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = vm::refresh) {
 *     // original screen content
 * }
 * ```
 *
 * The [content] should be fully scrollable (LazyColumn, Column with verticalScroll,
 * etc.) so the nested-scroll connection can detect the pull.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun PullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val state = rememberPullToRefreshState()

    // When user releases the gesture over the threshold.
    if (state.isRefreshing) {
        LaunchedEffect(true) {
            onRefresh()
        }
    }

    // When external refresh state flips to false → reset the indicator.
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) state.startRefresh() else state.endRefresh()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(state.nestedScrollConnection)
    ) {
        content()
        PullToRefreshContainer(
            state = state,
            modifier = Modifier.align(Alignment.TopCenter),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        )
    }
}
