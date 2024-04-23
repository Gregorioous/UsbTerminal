package com.practic.usbterminal.settings.ui.lib

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.practic.usbterminal.settings.ui.lib.internal.SettingsTileAction
import com.practic.usbterminal.settings.ui.lib.internal.SettingsTileIcon
import com.practic.usbterminal.settings.ui.lib.internal.SettingsTileTexts

@Composable
fun SettingsMenuLink(
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    title: @Composable () -> Unit,
    subtitle: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Surface {
        Row(
            modifier = modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingsTileIcon(icon = icon)
                SettingsTileTexts(title = title, subtitle = subtitle)
            }
            if (action != null) {
                Divider(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .height(56.dp)
                        .width(1.dp),
                )
                SettingsTileAction {
                    action.invoke()
                }
            }
        }
    }
}

/*
@Preview
@Composable
internal fun SettingsMenuLinkPreview() {
    MaterialTheme {
        SettingsMenuLink(
            icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Wifi") },
            title = { Text(text = "Hello") },
            subtitle = { Text(text = "This is a longer text") },
        ) {

        }
    }
}

@Preview
@Composable
internal fun SettingsMenuLinkActionPreview() {
    var rememberCheckBoxState by remember { mutableStateOf(true) }
    MaterialTheme {
        SettingsMenuLink(
            icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Wifi") },
            title = { Text(text = "Hello") },
            subtitle = { Text(text = "This is a longer text") },
            action = {
                Checkbox(checked = rememberCheckBoxState, onCheckedChange = { newState ->
                    rememberCheckBoxState = newState
                })
            },
        ) {

        }
    }
}*/
