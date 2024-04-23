package com.practic.usbterminal.ui.util

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.practic.usbterminal.settings.model.SettingsData
import com.practic.usbterminal.settings.model.SettingsRepository


@Composable
fun isDarkTheme(settingsData: SettingsData) = when (settingsData.themeType) {
    SettingsRepository.ThemeType.AS_SYSTEM -> isSystemInDarkTheme()
    SettingsRepository.ThemeType.DARK -> true
    else -> false
}