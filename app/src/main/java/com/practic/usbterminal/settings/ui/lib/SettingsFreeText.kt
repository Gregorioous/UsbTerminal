package com.practic.usbterminal.settings.ui.lib

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.practic.usbterminal.R
import com.practic.usbterminal.settings.ui.lib.internal.FreeTextDialog
import com.practic.usbterminal.settings.ui.lib.internal.SettingsTileIcon
import com.practic.usbterminal.settings.ui.lib.internal.SettingsTileTexts
import timber.log.Timber

@Composable
fun SettingsFreeText(
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    title: @Composable () -> Unit,
    label: @Composable () -> Unit,
    previousText: String?,
    keyboardOptions: KeyboardOptions,
    onTextInput: (text: String) -> Unit,
) {
    var newText by remember { mutableStateOf(previousText ?: "") }
    var showDialog by remember { mutableStateOf(false) }

    Surface {
        Row(
            modifier = modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .clickable(onClick = { showDialog = true }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    SettingsTileIcon(icon = icon)
                } else {
                    Spacer(modifier = Modifier.width(20.dp))
                }

                val subtitle = if (previousText.isNullOrBlank()) {
                    stringResource(R.string.not_set)
                } else {
                    previousText
                }

                SettingsTileTexts(
                    title = title,
                    subtitle = { Text(text = subtitle) }
                )
            }
        }
    }

    if (showDialog) {
        FreeTextDialog(
            title = title,
            label = label,
            previousText = previousText ?: "",
            maxLines = 3,
            onTextChange = { newText = it },
            onCancel = { showDialog = false },
            onOk = {
                Timber.d("SettingsFreeText(): newText=$newText")
                onTextInput(newText)
                showDialog = false
            },
            keyboardActions = KeyboardActions(onDone = {
                Timber.d("SettingsFreeText(): newText=$newText")
                onTextInput(newText)
                showDialog = false
            }),
            keyboardOptions = keyboardOptions,
        )
    }
}

/*
@Preview
@Composable
fun SettingsFreeTextPreview() {
    MaterialTheme {
        Column {
            SettingsFreeText(
                icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
                title = { Text(text = "Free text") },
                label = { Text(text = "FreeText label") },
                previousText = "Not set",
                onTextInput = {},
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            )
            Spacer(modifier = Modifier.height(30.dp))
            SettingsFreeText(
                title = { Text(text = "Free text") },
                label = { Text(text = "FreeText label") },
                previousText = "lior.apps@gmail.com",
                onTextInput = {},
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            )
        }
    }
}*/
