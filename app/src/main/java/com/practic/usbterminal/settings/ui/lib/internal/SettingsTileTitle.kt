package com.practic.usbterminal.settings.ui.lib.internal

import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.runtime.Composable

@Composable
internal fun SettingsTileTitle(
    title: @Composable () -> Unit,
    enabled: Boolean = true,
) {
    val textStyle = if (enabled) {
        MaterialTheme.typography.subtitle1
    } else {
        MaterialTheme.typography.subtitle1.copy(
            color = MaterialTheme.colors.onBackground.copy(alpha = 0.5f)
        )
    }
    ProvideTextStyle(value = textStyle) {
        title()
    }
}