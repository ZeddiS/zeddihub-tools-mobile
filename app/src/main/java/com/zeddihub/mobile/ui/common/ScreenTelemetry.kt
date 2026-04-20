package com.zeddihub.mobile.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.zeddihub.mobile.data.telemetry.TelemetryRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TelemetryHolder @Inject constructor(
    val telemetry: TelemetryRecorder
) : ViewModel()

@Composable
fun TrackScreen(route: String) {
    val holder: TelemetryHolder = hiltViewModel()
    DisposableEffect(route) {
        val start = System.currentTimeMillis()
        onDispose {
            holder.telemetry.screenView(route, System.currentTimeMillis() - start)
        }
    }
}
