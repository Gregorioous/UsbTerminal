package com.practic.usbterminal.settings.ui.lib

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.practic.usbterminal.settings.ui.lib.internal.SettingsTileAction
import com.practic.usbterminal.settings.ui.lib.internal.SettingsTileIcon
import com.practic.usbterminal.settings.ui.lib.internal.SettingsTileTexts

@Composable
fun SettingsSwitch(
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
                .clickable(onClick = { onCheckedChange(!checked) }),
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
                Switch(
                    enabled = enabled,
                    checked = checked,
                    onCheckedChange = { onCheckedChange(!checked) },
                )
            }
        }
    }
}


@Preview
@Composable
internal fun SettingsSwitchPreview() {
    MaterialTheme {
        var state by remember { mutableStateOf(true) }
        SettingsSwitch(
            icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Wifi") },
            title = { Text(text = "Hello") },
            subtitle = { Text(text = "This is a longer text") },
            checked = state,
            onCheckedChange = { state = it }
        )
    }
}
