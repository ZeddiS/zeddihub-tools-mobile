package com.zeddihub.mobile.ui.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zeddihub.mobile.ui.theme.StateDanger
import com.zeddihub.mobile.ui.theme.StateSuccess
import com.zeddihub.mobile.ui.theme.StateWarning

@Composable
fun StatusDot(
    status: String,
    modifier: Modifier = Modifier
) {
    val color = when (status.lowercase()) {
        "online" -> StateSuccess
        "starting" -> StateWarning
        "crashed" -> StateDanger
        else -> Color(0xFF6B7280)
    }

    val transition = rememberInfiniteTransition(label = "pulse")
    val radius by transition.animateFloat(
        initialValue = 5f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseRadius"
    )

    Canvas(modifier = modifier.size(14.dp)) {
        drawCircle(color = color.copy(alpha = 0.25f), radius = radius + 4f)
        drawCircle(color = color, radius = radius)
    }
}
