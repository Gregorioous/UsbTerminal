package com.practic.usbterminal.settings.ui.lib

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.Checkbox
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.practic.usbterminal.settings.ui.lib.internal.SettingsTileAction
import com.practic.usbterminal.settings.ui.lib.internal.SettingsTileIcon
import com.practic.usbterminal.settings.ui.lib.internal.SettingsTileTexts

@Composable
fun SettingsCheckbox(
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable () -> Unit,
    subtitle: @Composable (() -> Unit)? = null,
    replaceIconWithSameSpace: Boolean = false,
    enabled: Boolean = true,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {

    Surface {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .then(if (enabled) Modifier.clickable(onClick = { onCheckedChange(!checked) }) else Modifier), // From: https://jetc.dev/slack/2020-12-13-conditional-modifiers.html
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (replaceIconWithSameSpace) {
                SettingsTileIcon(icon = null)
            } else {
                if (icon != null) {
                    SettingsTileIcon(icon = icon)
                } else {
                    Spacer(modifier = Modifier.width(20.dp))
                }
            }
            SettingsTileTexts(title = title, subtitle = subtitle, enabled = enabled)
            SettingsTileAction {
                Checkbox(
                    enabled = enabled,
                    checked = checked,
                    onCheckedChange = { onCheckedChange(!checked) },
                )
            }
        }
    }
}
