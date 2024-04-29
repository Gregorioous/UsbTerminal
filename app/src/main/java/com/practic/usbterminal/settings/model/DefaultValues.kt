package com.practic.usbterminal.settings.model

import com.hoho.android.usbserial.driver.UsbSerialPort

object DefaultValues {

    const val themeType = SettingsRepository.ThemeType.AS_SYSTEM
    const val logSessionDataToFile = false
    const val alsoLogOutgoingData = false
    const val markLoggedOutgoingData = true
    const val zipLogFilesWhenSharing = true
    const val connectToDeviceOnStart = true
    const val emailAddressForSharing = ""
    const val workAlsoInBackground = true
    const val maxBytesToRetainForBackScroll = 100_000

    const val inputMode = SettingsRepository.InputMode.CHAR_BY_CHAR
    const val sendInputLineOnEnterKey: Boolean = true
    const val bytesSentByEnterKey = SettingsRepository.BytesSentByEnterKey.CR
    const val loopBack = false
    const val fontSize = 16
    const val defaultTextColor = 0xeeeeee
    const val defaultTextColorFreeInput = -1
    const val soundOn = true
    const val silentlyDropUnrecognizedCtrlChars = true

    const val baudRate = 115200
    const val baudRateFreeInput = -1
    const val dataBits = 8
    const val stopBits = UsbSerialPort.STOPBITS_1
    const val parity = UsbSerialPort.PARITY_NONE
    const val setDTRTrueOnConnect = true
    const val setRTSTrueOnConnect = true


    const val isDefaultValues = true
    const val showedEulaV1 = false
    const val showedV2WelcomeMsg = false
    val displayType = SettingsRepository.DisplayType.TEXT
    const val showCtrlButtonsRow = false
    const val logFilesListSortingOrder = SettingsRepository.LogFilesListSortingOrder.ASCENDING
}