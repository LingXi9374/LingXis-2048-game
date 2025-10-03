package org.lingxi9374.game2048

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class LocaleManager(settingsManager: SettingsManager) {
    var locale by mutableStateOf(settingsManager.getLocale())
}

val LocalLocaleManager = compositionLocalOf<LocaleManager> { error("LocaleManager not provided") }