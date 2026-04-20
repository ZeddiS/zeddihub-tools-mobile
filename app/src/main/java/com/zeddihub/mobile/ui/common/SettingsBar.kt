package com.zeddihub.mobile.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeddihub.mobile.R
import com.zeddihub.mobile.data.local.LanguageCode
import com.zeddihub.mobile.data.local.ThemeMode

private fun flagFor(code: LanguageCode): String = when (code) {
    LanguageCode.CS -> "\uD83C\uDDE8\uD83C\uDDFF"
    LanguageCode.EN -> "\uD83C\uDDEC\uD83C\uDDE7"
}

@Composable
fun SettingsBar(
    modifier: Modifier = Modifier,
    currentLanguage: LanguageCode,
    currentTheme: ThemeMode,
    onLanguage: (LanguageCode) -> Unit,
    onTheme: (ThemeMode) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        color = colors.surface.copy(alpha = 0.7f),
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            LanguageMenu(currentLanguage = currentLanguage, onLanguage = onLanguage)
            ThemeToggle(currentTheme = currentTheme, onTheme = onTheme)
        }
    }
}

@Composable
private fun LanguageMenu(
    currentLanguage: LanguageCode,
    onLanguage: (LanguageCode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(
        onClick = { expanded = true },
        modifier = Modifier.size(44.dp)
    ) {
        Text(
            text = flagFor(currentLanguage),
            fontSize = 22.sp
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(flagFor(LanguageCode.CS), fontSize = 20.sp)
                    Text(
                        "  ${stringResource(R.string.lang_cs)}",
                        fontWeight = if (currentLanguage == LanguageCode.CS)
                            FontWeight.Bold else FontWeight.Normal
                    )
                }
            },
            onClick = {
                expanded = false
                onLanguage(LanguageCode.CS)
            }
        )
        DropdownMenuItem(
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(flagFor(LanguageCode.EN), fontSize = 20.sp)
                    Text(
                        "  ${stringResource(R.string.lang_en)}",
                        fontWeight = if (currentLanguage == LanguageCode.EN)
                            FontWeight.Bold else FontWeight.Normal
                    )
                }
            },
            onClick = {
                expanded = false
                onLanguage(LanguageCode.EN)
            }
        )
    }
}

@Composable
private fun ThemeToggle(
    currentTheme: ThemeMode,
    onTheme: (ThemeMode) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    IconButton(
        onClick = {
            val next = if (currentTheme == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
            onTheme(next)
        }
    ) {
        Icon(
            imageVector = if (currentTheme == ThemeMode.DARK)
                Icons.Default.LightMode else Icons.Default.DarkMode,
            contentDescription = stringResource(R.string.theme_toggle),
            tint = colors.primary
        )
    }
}
