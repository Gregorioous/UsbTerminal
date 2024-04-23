package com.practic.usbterminal.settings.model

data class SettingsData(

    val themeType: Int = DefaultValues.themeType,
    val logSessionDataToFile: Boolean = DefaultValues.logSessionDataToFile,
    val alsoLogOutgoingData: Boolean = DefaultValues.alsoLogOutgoingData,
    val markLoggedOutgoingData: Boolean = DefaultValues.markLoggedOutgoingData,
    val zipLogFilesWhenSharing: Boolean = DefaultValues.zipLogFilesWhenSharing,
    val connectToDeviceOnStart: Boolean = DefaultValues.connectToDeviceOnStart,
    val emailAddressForSharing: String = DefaultValues.emailAddressForSharing,
    val workAlsoInBackground: Boolean = DefaultValues.workAlsoInBackground,
    val maxBytesToRetainForBackScroll: Int = DefaultValues.maxBytesToRetainForBackScroll,

    val inputMode: Int = DefaultValues.inputMode,
    val sendInputLineOnEnterKey: Boolean = DefaultValues.sendInputLineOnEnterKey,
    val bytesSentByEnterKey: Int = DefaultValues.bytesSentByEnterKey,
    val loopBack: Boolean = DefaultValues.loopBack,
    val fontSize: Int = DefaultValues.fontSize,
    val defaultTextColor: Int = DefaultValues.defaultTextColor,
    val defaultTextColorFreeInput: Int = DefaultValues.defaultTextColorFreeInput,
    val soundOn: Boolean = DefaultValues.soundOn,
    val silentlyDropUnrecognizedCtrlChars: Boolean = DefaultValues.silentlyDropUnrecognizedCtrlChars,

    val baudRate: Int = DefaultValues.baudRate,
    val baudRateFreeInput: Int = DefaultValues.baudRateFreeInput,
    val dataBits: Int = DefaultValues.dataBits,
    val stopBits: Int = DefaultValues.stopBits,
    val parity: Int = DefaultValues.parity,
    val setDTRTrueOnConnect: Boolean = DefaultValues.setDTRTrueOnConnect,
    val setRTSTrueOnConnect: Boolean = DefaultValues.setRTSTrueOnConnect,

    val isDefaultValues: Boolean = DefaultValues.isDefaultValues,
    val showedEulaV1: Boolean = DefaultValues.showedEulaV1,
    val showedV2WelcomeMsg: Boolean = DefaultValues.showedV2WelcomeMsg,
    val displayType: SettingsRepository.DisplayType = DefaultValues.displayType,
    val showCtrlButtonsRow: Boolean = DefaultValues.showCtrlButtonsRow,
    val logFilesListSortingOrder: Int = DefaultValues.logFilesListSortingOrder,
    val lastVisitedHelpUrl: String? = null
)