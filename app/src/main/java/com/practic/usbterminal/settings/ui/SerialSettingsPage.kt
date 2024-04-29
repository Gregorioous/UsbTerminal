package com.practic.usbterminal.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.practic.usbterminal.R
import com.practic.usbterminal.main.MainViewModel
import com.practic.usbterminal.settings.model.SettingsData
import com.practic.usbterminal.settings.model.SettingsRepository
import com.practic.usbterminal.settings.ui.lib.SettingsSingleChoice
import com.practic.usbterminal.settings.ui.lib.SettingsSwitch

@Composable
fun SerialSettingsPage(
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
            title = { Text(text = stringResource(R.string.baud_rate)) },
            choices = SettingsRepository.BaudRateValues.preDefined.map { it.toString() },
            hasFreeInputField = true,
            freeInputFieldLabel = "Any baud rate",
            freeInputFieldValue = settingsData.baudRateFreeInput.let { if (it == -1) "" else it.toString() },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            preSelectedIndex = mainViewModel.settingsRepository.indexOfBaudRate(settingsData.baudRate),
        ) { choiceIndex, choiceValue ->
            mainViewModel.settingsRepository.setBaudRate(choiceValue)
        }

        SettingsSingleChoice(
            title = { Text(text = stringResource(R.string.data_bits)) },
            choices = stringArrayResource(R.array.data_bits).toList(),
            preSelectedIndex = SettingsRepository.indexOfDataBits(settingsData.dataBits),
        ) { choiceIndex, choiceValue ->
            mainViewModel.settingsRepository.setDataBits(choiceValue)
        }

        SettingsSingleChoice(
            title = { Text(text = stringResource(R.string.stop_bits)) },
            choices = stringArrayResource(R.array.stop_bits).toList(),
            preSelectedIndex = SettingsRepository.indexOfStopBits(settingsData.stopBits),
        ) { choiceIndex, choiceValue ->
            mainViewModel.settingsRepository.setStopBits(choiceIndex)
        }

        SettingsSingleChoice(
            title = { Text(text = stringResource(R.string.parity)) },
            choices = stringArrayResource(R.array.parity).toList(),
            preSelectedIndex = SettingsRepository.indexOfParity(settingsData.parity),
        ) { choiceIndex, choiceValue ->
            mainViewModel.settingsRepository.setParity(choiceIndex)
        }

        SettingsSwitch(
            title = { Text(text = stringResource(R.string.set_dtr_on_connect)) },
            checked = settingsData.setDTRTrueOnConnect
        ) { checked ->
            mainViewModel.settingsRepository.setSetDTROnConnect(checked)
        }

        SettingsSwitch(
            title = { Text(text = stringResource(R.string.set_rts_on_connect)) },
            checked = settingsData.setRTSTrueOnConnect
        ) { checked ->
            mainViewModel.settingsRepository.setSetRTSOnConnect(checked)
        }
    }
}