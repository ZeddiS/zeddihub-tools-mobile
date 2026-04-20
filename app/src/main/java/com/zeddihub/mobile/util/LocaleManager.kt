package com.zeddihub.mobile.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.zeddihub.mobile.data.local.LanguageCode

object LocaleManager {
    fun apply(code: LanguageCode) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(code.tag)
        )
    }
}
