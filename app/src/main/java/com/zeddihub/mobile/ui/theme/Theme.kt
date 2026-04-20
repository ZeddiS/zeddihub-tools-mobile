package com.zeddihub.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ZeddiDarkColors = darkColorScheme(
    primary = AccentOrange,
    onPrimary = BgBase,
    primaryContainer = AccentOrangeDeep,
    onPrimaryContainer = TextPrimary,
    secondary = AccentOrangeSoft,
    onSecondary = BgBase,
    background = BgBase,
    onBackground = TextPrimary,
    surface = BgSurface,
    onSurface = TextPrimary,
    surfaceVariant = BgElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    error = StateDanger,
    onError = TextPrimary
)

private val ZeddiLightColors = lightColorScheme(
    primary = AccentOrangeDeep,
    onPrimary = BgSurfaceLight,
    primaryContainer = AccentOrangeSoft,
    onPrimaryContainer = TextPrimaryLight,
    secondary = AccentOrange,
    onSecondary = BgSurfaceLight,
    background = BgBaseLight,
    onBackground = TextPrimaryLight,
    surface = BgSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = BgElevatedLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderSubtleLight,
    error = StateDanger,
    onError = BgSurfaceLight
)

@Composable
fun ZeddiHubTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) ZeddiDarkColors else ZeddiLightColors,
        typography = ZeddiTypography,
        content = content
    )
}
