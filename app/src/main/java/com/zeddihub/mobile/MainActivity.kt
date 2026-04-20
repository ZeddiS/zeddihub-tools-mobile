package com.zeddihub.mobile

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.zeddihub.mobile.data.local.AppPreferences
import com.zeddihub.mobile.data.local.LanguageCode
import com.zeddihub.mobile.data.local.ThemeMode
import com.zeddihub.mobile.data.update.UpdateChecker
import com.zeddihub.mobile.ui.navigation.AppNavGraph
import com.zeddihub.mobile.ui.theme.ZeddiHubTheme
import com.zeddihub.mobile.util.LocaleManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var updateChecker: UpdateChecker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleManager.apply(appPreferences.language.value)
        enableEdgeToEdge()

        if (appPreferences.autoUpdate.value) {
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { updateChecker.fetchLatest() }
            }
        }

        setContent {
            val themeMode by appPreferences.theme.collectAsState()
            val language by appPreferences.language.collectAsState()

            ZeddiHubTheme(darkTheme = themeMode == ThemeMode.DARK) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavGraph(
                        navController = navController,
                        currentLanguage = language,
                        currentTheme = themeMode,
                        onLanguage = { code: LanguageCode ->
                            appPreferences.setLanguage(code)
                            LocaleManager.apply(code)
                        },
                        onTheme = { mode: ThemeMode ->
                            appPreferences.setTheme(mode)
                        }
                    )
                }
            }
        }
    }
}
