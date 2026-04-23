package com.zeddihub.mobile.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.zeddihub.mobile.R

/**
 * Minimal top-bar scaffold for tool/helper detail screens.
 *
 * The tab-based navigation introduced in v0.5.8 replaces the hamburger
 * drawer used by [AppShell]. Detail screens get a back arrow + title and
 * are pushed on top of the Main tab container; tapping back returns to the
 * Nástroje / Pomůcky tab they came from, preserving its scroll position.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailShell(
    title: String,
    onBack: () -> Unit,
    currentRoute: String = title,
    actions: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    TrackScreen(route = currentRoute)

    Scaffold(
        containerColor = colors.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = colors.onBackground
                        )
                    }
                },
                actions = { actions() },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.onBackground
                )
            )
        }
    ) { padding -> content(padding) }
}
