package com.practic.usbterminal.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.practic.usbterminal.R
import com.practic.usbterminal.main.MainViewModel
import com.practic.usbterminal.settings.model.SettingsData
import com.practic.usbterminal.settings.ui.lib.SettingsFreeText
import com.practic.usbterminal.settings.ui.lib.SettingsSingleChoice
import com.practic.usbterminal.settings.ui.lib.SettingsSwitch
import com.practic.usbterminal.ui.util.GeneralDialog

@Composable
fun GeneralSettingsPage(
    mainViewModel: MainViewModel,
    settingsData: SettingsData,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colors.surface)
    ) {

        SettingsSingleChoice(
            title = { Text(text = stringResource(R.string.theme)) },
            icon = {
                Icon(
                    painterResource(R.drawable.ic_baseline_brightness_medium_24),
                    contentDescription = stringResource(R.string.theme),
                )
            },
            choices = listOf(
                stringResource(R.string.light),
                stringResource(R.string.dark),
                stringResource(R.string.same_as_system),
            ),
            hasFreeInputField = false,
            preSelectedIndex = settingsData.themeType
        ) { choiceIndex, choiceValue ->
            mainViewModel.settingsRepository.setThemeType(choiceIndex)
        }

        SettingsSwitch(
            title = { Text(text = stringResource(R.string.log_session_data_to_file)) },
            icon = {
                Icon(
                    painterResource(R.drawable.ic_baseline_history_edu_24),
                    contentDescription = stringResource(R.string.log_session_data_to_file),
                )
            },
            checked = settingsData.logSessionDataToFile
        ) { checked ->
            mainViewModel.settingsRepository.setLogSessionDataToFile(checked)
        }

        SettingsSwitch(
            title = { Text(text = stringResource(R.string.also_log_outgoing_data)) },
            checked = settingsData.alsoLogOutgoingData,
            enabled = settingsData.logSessionDataToFile,
            replaceIconWithSameSpace = true,
        ) { checked ->
            mainViewModel.settingsRepository.setAlsoLogOutgoingData(checked)
        }

        SettingsSwitch(
            title = { Text(text = stringResource(R.string.mark_logged_outgoing_data)) },
            checked = settingsData.markLoggedOutgoingData,
            enabled = (settingsData.logSessionDataToFile && settingsData.alsoLogOutgoingData),
            replaceIconWithSameSpace = true,
        ) { checked ->
            mainViewModel.settingsRepository.setMarkLoggedOutgoingData(checked)
        }

        SettingsSwitch(
            title = { Text(text = stringResource(R.string.zip_log_files_when_sharing)) },
            checked = settingsData.zipLogFilesWhenSharing,
            enabled = (settingsData.logSessionDataToFile),
            icon = {
                Icon(
                    painterResource(R.drawable.folder_zip_outline),
                    contentDescription = stringResource(R.string.zip_log_files_when_sharing),
                )
            },
        ) { checked ->
            mainViewModel.settingsRepository.setZipLogFilesWhenSharing(checked)
        }

        var shouldDisplayWorkInBGDialog by remember { mutableStateOf(false) }
        SettingsSwitch(
            title = { Text(text = stringResource(R.string.work_also_in_the_background)) },
            icon = {
                Icon(
                    painterResource(R.drawable.ic_baseline_content_copy_24),
                    contentDescription = stringResource(R.string.work_also_in_the_background),
                )
            },
            checked = settingsData.workAlsoInBackground
        ) { checked ->
            if (checked) {
                shouldDisplayWorkInBGDialog = true
            } else {
                mainViewModel.settingsRepository.setWorkAlsoInBackground(false)
            }
        }
        if (shouldDisplayWorkInBGDialog) {
            GeneralDialog(
                titleText = stringResource(R.string.work_also_in_the_background),
                onPositiveText = stringResource(R.string.ok),
                onPositiveClick = {
                    shouldDisplayWorkInBGDialog = false
                    mainViewModel.settingsRepository.setWorkAlsoInBackground(true)
                },
                onDismissText = stringResource(R.string.cancel_all_caps),
                onDismissClick = { shouldDisplayWorkInBGDialog = false }
            ) {
                Text(
                    modifier = Modifier.padding(20.dp),
                    text = stringResource(R.string.work_also_in_the_background_explanation),
                )
            }
        }

        SettingsSwitch(
            title = { Text(text = stringResource(R.string.connect_to_usb_device_on_start)) },
            icon = {
                Icon(
                    painterResource(R.drawable.ic_baseline_compare_arrows_24),
                    contentDescription = stringResource(R.string.connect_to_usb_device_on_start),
                )
            },
            checked = settingsData.connectToDeviceOnStart
        ) { checked ->
            mainViewModel.settingsRepository.setConnectToDeviceOnStart(checked)
        }

        SettingsFreeText(
            title = { Text(text = stringResource(R.string.default_sharing_email_addresses)) },
            icon = {
                Icon(
                    painterResource(R.drawable.ic_baseline_mail_outline_24),
                    contentDescription = stringResource(R.string.default_sharing_email_addresses),
                )
            },
            label = { Text(text = stringResource(R.string.email_address)) },
            previousText = settingsData.emailAddressForSharing,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
        ) { emailAddress ->
            mainViewModel.settingsRepository.setEmailAddressForSharing(emailAddress)
        }
    }
}