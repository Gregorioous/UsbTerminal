package com.practic.usbterminal.settings.model

import android.annotation.SuppressLint
import android.content.Context
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.migrations.SharedPreferencesView
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.hoho.android.usbserial.driver.UsbSerialPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.roundToInt

val Context.settingsDataStore by preferencesDataStore(
    name = SettingsRepository.SETTINGS_DATASTORE_NAME,
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                context = context,
                sharedPreferencesName = context.packageName + "_preferences",
                migrate = ::doMigration
            )
        )
    }
)

class SettingsRepository private constructor(private val context: Context) {

    private fun mapPreferencesToModel(preferences: Preferences): SettingsData =
        SettingsData(

            themeType = preferences[SettingsKeys.THEME_TYPE_KEY] ?: DefaultValues.themeType,
            logSessionDataToFile = preferences[SettingsKeys.LOG_SESSION_DATA_TO_FILE_KEY]
                ?: DefaultValues.logSessionDataToFile,
            alsoLogOutgoingData = preferences[SettingsKeys.ALSO_LOG_OUTGOING_DATA_KEY]
                ?: DefaultValues.alsoLogOutgoingData,
            markLoggedOutgoingData = preferences[SettingsKeys.MARK_LOGGED_OUTGOING_DATA_KEY]
                ?: DefaultValues.markLoggedOutgoingData,
            zipLogFilesWhenSharing = preferences[SettingsKeys.ZIP_LOG_FILES_WHEN_SHARING_KEY]
                ?: DefaultValues.zipLogFilesWhenSharing,
            connectToDeviceOnStart = preferences[SettingsKeys.CONNECT_TO_DEVICE_ON_START_KEY]
                ?: DefaultValues.connectToDeviceOnStart,
            emailAddressForSharing = preferences[SettingsKeys.EMAIL_ADDR_FOR_SHARING_KEY]
                ?: DefaultValues.emailAddressForSharing,
            workAlsoInBackground = preferences[SettingsKeys.WORK_ALSO_IN_BACKGROUND_KEY]
                ?: DefaultValues.workAlsoInBackground,
            maxBytesToRetainForBackScroll = preferences[SettingsKeys.MAX_BYTES_TO_RETAIN_FOR_BACK_SCROLL_KEY]
                ?: DefaultValues.maxBytesToRetainForBackScroll,

            inputMode = preferences[SettingsKeys.INPUT_MODE_KEY] ?: DefaultValues.inputMode,
            sendInputLineOnEnterKey = preferences[SettingsKeys.SEND_INPUT_LINE_ON_ENTER_KEY_KEY]
                ?: DefaultValues.sendInputLineOnEnterKey,
            bytesSentByEnterKey = preferences[SettingsKeys.BYTES_SENT_BY_ENTER_KEY_KEY]
                ?: DefaultValues.bytesSentByEnterKey,
            loopBack = preferences[SettingsKeys.LOOPBACK_KEY] ?: DefaultValues.loopBack,
            fontSize = preferences[SettingsKeys.FONT_SIZE_KEY] ?: DefaultValues.fontSize,
            defaultTextColor = preferences[SettingsKeys.DEFAULT_TEXT_COLOR_KEY]
                ?: DefaultValues.defaultTextColor,
            defaultTextColorFreeInput = preferences[SettingsKeys.DEFAULT_TEXT_COLOR_FREE_INPUT_KEY]
                ?: DefaultValues.defaultTextColorFreeInput,
            soundOn = preferences[SettingsKeys.SOUND_ON_KEY] ?: DefaultValues.soundOn,
            silentlyDropUnrecognizedCtrlChars = preferences[SettingsKeys.DROP_UNRECOGNIZED_CTRL_CHARS_KEY]
                ?: DefaultValues.silentlyDropUnrecognizedCtrlChars,

            baudRate = preferences[SettingsKeys.BAUD_RATE_KEY] ?: DefaultValues.baudRate,
            baudRateFreeInput = preferences[SettingsKeys.BAUD_RATE_FREE_INPUT_KEY]
                ?: DefaultValues.baudRateFreeInput,
            dataBits = preferences[SettingsKeys.DATA_BITS_KEY] ?: DefaultValues.dataBits,
            stopBits = preferences[SettingsKeys.STOP_BITS_KEY] ?: DefaultValues.stopBits,
            parity = preferences[SettingsKeys.PARITY_KEY] ?: DefaultValues.parity,
            setDTRTrueOnConnect = preferences[SettingsKeys.SET_DTR_KEY]
                ?: DefaultValues.setDTRTrueOnConnect,
            setRTSTrueOnConnect = preferences[SettingsKeys.SET_RTS_KEY]
                ?: DefaultValues.setRTSTrueOnConnect,

            isDefaultValues = false,
            showedEulaV1 = preferences[SettingsKeys.SHOWED_EULA_V1_KEY]
                ?: DefaultValues.showedEulaV1,
            showedV2WelcomeMsg = preferences[SettingsKeys.SHOWED_V2_WELCOME_MSG_KEY]
                ?: DefaultValues.showedV2WelcomeMsg,
            displayType = DisplayType.fromInt(preferences[SettingsKeys.DISPLAY_TYPE_KEY])
                ?: DefaultValues.displayType,
            showCtrlButtonsRow = preferences[SettingsKeys.SHOW_CTRL_BUTTON_KEY]
                ?: DefaultValues.showCtrlButtonsRow,
            logFilesListSortingOrder = preferences[SettingsKeys.LOG_FILES_SORT_KEY]
                ?: DefaultValues.logFilesListSortingOrder,
            lastVisitedHelpUrl = preferences[SettingsKeys.LAST_VISITED_HELP_URL_KEY],
        )

    object ThemeType {
        @Suppress("unused")
        const val LIGHT = 0
        const val DARK = 1
        const val AS_SYSTEM = 2
    }

    object InputMode {
        const val CHAR_BY_CHAR = 0
        const val WHOLE_LINE = 1
    }

    object BytesSentByEnterKey {
        const val CR = 0
        const val LF = 1
        const val CR_LF = 2
    }

    object BaudRateValues {
        val preDefined =
            arrayOf(300, 600, 1200, 2400, 4800, 9600, 19200, 28800, 38400, 57600, 115200)

        fun isPreDefined(baud: Int): Boolean = preDefined.contains(baud)
    }

    object DataBits {
        val values = arrayOf(8, 7, 6)
    }

    object StopBits {
        val values = arrayOf(
            UsbSerialPort.STOPBITS_1,
            UsbSerialPort.STOPBITS_1_5,
            UsbSerialPort.STOPBITS_2
        )
    }

    object Parity {
        val values = arrayOf(
            UsbSerialPort.PARITY_NONE,
            UsbSerialPort.PARITY_ODD,
            UsbSerialPort.PARITY_EVEN,
            UsbSerialPort.PARITY_MARK,
            UsbSerialPort.PARITY_SPACE
        )
    }

    object LogFilesListSortingOrder {
        const val ASCENDING = 0
        const val DESCENDING = 1
    }

    enum class DisplayType(val value: Int) {
        TEXT(0),
        HEX(1);

        companion object {
            fun fromInt(value: Int?) = values().firstOrNull { it.value == value }
        }
    }

    object FontSize {
        val values = arrayOf(8, 9, 10, 11, 12, 14, 16, 18, 20, 22, 24, 28)
    }

    object DefaultTextColorValues {
        val preDefined = arrayOf(
            0xeeeeee,
            0x66ff66,
            0xffc600,
        )

        fun isPreDefined(color: Int): Boolean = preDefined.contains(color)
    }

    private val settingsFlow: Flow<Preferences> = context.settingsDataStore.data
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    val settingsStateFlow: StateFlow<SettingsData> = settingsFlow
        .map { mapPreferencesToModel(it) }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = SettingsData(),
        )

    fun setBaudRate(newBaudRateValue: String) {
        val newBaud = newBaudRateValue.toIntOrNull() ?: 0
        if (newBaud == 0) {
            Timber.w("setBaudRate() Illegal baud rate: '$newBaudRateValue'")
        } else {
            updateSettingsDataStore(SettingsKeys.BAUD_RATE_KEY, newBaud)
            if (!BaudRateValues.isPreDefined(newBaud)) {
                updateSettingsDataStore(SettingsKeys.BAUD_RATE_FREE_INPUT_KEY, newBaud)
            }
        }
    }

    fun setDataBits(newDataBitsValue: String) {
        val newDataBits = newDataBitsValue.toIntOrNull() ?: 0
        if (newDataBits == 0) {
            Timber.w("setDataBits() Illegal value: '$newDataBitsValue'")
        } else {
            updateSettingsDataStore(SettingsKeys.DATA_BITS_KEY, newDataBits)
        }
    }

    fun setStopBits(newStopBitsIndex: Int) {
        if (newStopBitsIndex < 0 || newStopBitsIndex >= StopBits.values.size) {
            Timber.w("setStopBits() Illegal index value: '$newStopBitsIndex'")
        } else {
            val newStopBitsValue = StopBits.values[newStopBitsIndex]
            updateSettingsDataStore(SettingsKeys.STOP_BITS_KEY, newStopBitsValue)
        }
    }

    fun setParity(newValue: Int) {
        if (!Parity.values.contains(newValue)) {
            Timber.w("setParity() Illegal value: '$newValue'")
        } else {
            updateSettingsDataStore(SettingsKeys.PARITY_KEY, newValue)
        }
    }

    fun setFontSize(newFontSizeValue: String) {
        val newFontSize = newFontSizeValue.toIntOrNull() ?: 0
        if (newFontSize == 0) {
            Timber.w("setFontSize() Illegal value: '$newFontSize'")
        } else {
            updateSettingsDataStore(SettingsKeys.FONT_SIZE_KEY, newFontSize)
        }
    }

    fun setDefaultTextColor(newTextColor: Int) {
        updateSettingsDataStore(SettingsKeys.DEFAULT_TEXT_COLOR_KEY, newTextColor)
        if (!DefaultTextColorValues.isPreDefined(newTextColor)) {
            updateSettingsDataStore(SettingsKeys.DEFAULT_TEXT_COLOR_FREE_INPUT_KEY, newTextColor)
        }
    }

    fun setThemeType(themeType: Int) {
        updateSettingsDataStore(SettingsKeys.THEME_TYPE_KEY, themeType)
    }

    fun setInputMode(inputMode: Int) {
        updateSettingsDataStore(SettingsKeys.INPUT_MODE_KEY, inputMode)
    }

    fun setBytesSentByEnterKey(bytesSentByEnterKey: Int) {
        updateSettingsDataStore(SettingsKeys.BYTES_SENT_BY_ENTER_KEY_KEY, bytesSentByEnterKey)
    }

    fun setLoopBack(newValue: Boolean) {
        updateSettingsDataStore(SettingsKeys.LOOPBACK_KEY, newValue)
    }

    fun setSoundOn(newValue: Boolean) {
        updateSettingsDataStore(SettingsKeys.SOUND_ON_KEY, newValue)
    }

    fun setSilentlyDropUnrecognizedCtrlChars(newValue: Boolean) {
        updateSettingsDataStore(
            SettingsKeys.DROP_UNRECOGNIZED_CTRL_CHARS_KEY, newValue
        )
    }

    fun setSetDTROnConnect(newValue: Boolean) {
        updateSettingsDataStore(SettingsKeys.SET_DTR_KEY, newValue)
    }

    fun setSetRTSOnConnect(newValue: Boolean) {
        updateSettingsDataStore(SettingsKeys.SET_RTS_KEY, newValue)
    }

    fun setLogSessionDataToFile(newValue: Boolean) {
        updateSettingsDataStore(SettingsKeys.LOG_SESSION_DATA_TO_FILE_KEY, newValue)
    }

    fun setAlsoLogOutgoingData(newValue: Boolean) {
        updateSettingsDataStore(SettingsKeys.ALSO_LOG_OUTGOING_DATA_KEY, newValue)
    }

    fun setMarkLoggedOutgoingData(newValue: Boolean) {
        updateSettingsDataStore(SettingsKeys.MARK_LOGGED_OUTGOING_DATA_KEY, newValue)
    }

    fun setZipLogFilesWhenSharing(newValue: Boolean) {
        updateSettingsDataStore(SettingsKeys.ZIP_LOG_FILES_WHEN_SHARING_KEY, newValue)
    }

    fun setConnectToDeviceOnStart(newValue: Boolean) {
        updateSettingsDataStore(SettingsKeys.CONNECT_TO_DEVICE_ON_START_KEY, newValue)
    }

    fun setEmailAddressForSharing(emailAddr: String) {
        updateSettingsDataStore(SettingsKeys.EMAIL_ADDR_FOR_SHARING_KEY, emailAddr)
    }

    fun setWorkAlsoInBackground(newValue: Boolean) {
        updateSettingsDataStore(SettingsKeys.WORK_ALSO_IN_BACKGROUND_KEY, newValue)
    }

    fun setLogFilesSortingOrder(sortingOrder: Int) {
        updateSettingsDataStore(SettingsKeys.LOG_FILES_SORT_KEY, sortingOrder)
    }

    fun setDisplayType(displayType: Int) {
        updateSettingsDataStore(SettingsKeys.DISPLAY_TYPE_KEY, displayType)
    }

    fun setShowCtrlButtonsRow(show: Boolean) {
        updateSettingsDataStore(SettingsKeys.SHOW_CTRL_BUTTON_KEY, show)
    }

    fun setMaxBytesToRetainForBackScroll(nMaxBytes: Int) {
        updateSettingsDataStore(SettingsKeys.MAX_BYTES_TO_RETAIN_FOR_BACK_SCROLL_KEY, nMaxBytes)
    }

    fun setShowedV2WelcomeMsg(showedMsg: Boolean) {
        updateSettingsDataStore(SettingsKeys.SHOWED_V2_WELCOME_MSG_KEY, showedMsg)
    }

    fun setSendInputLineOnEnterKey(send: Boolean) {
        updateSettingsDataStore(SettingsKeys.SEND_INPUT_LINE_ON_ENTER_KEY_KEY, send)
    }

    fun setLastVisitedHelpUrl(url: String) {
        updateSettingsDataStore(SettingsKeys.LAST_VISITED_HELP_URL_KEY, url)
    }


    private fun <T> updateSettingsDataStore(key: Preferences.Key<T>, value: T) {
        coroutineScope.launch {
            context.settingsDataStore.edit { settings ->
                settings[key] = value
            }
        }
    }

    fun indexOfTextColor(color: Int): Int {
        var index = DefaultTextColorValues.preDefined.indexOfFirst { it == color }
        if (index == -1) {
            index = DefaultTextColorValues.preDefined.size
        }
        return index
    }

    fun indexOfBaudRate(baud: Int): Int {
        var index = BaudRateValues.preDefined.indexOfFirst { it == baud }
        if (index == -1) {
            val freeInputBaudRate = settingsStateFlow.value.baudRateFreeInput
            if (freeInputBaudRate == baud) {
                index = BaudRateValues.preDefined.size
            }
        }
        return index
    }


    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE?.let {
                    return it
                }

                val instance = SettingsRepository(context)
                INSTANCE = instance
                instance
            }
        }

        @Suppress("unused")
        fun getInstance(): SettingsRepository {
            return INSTANCE
                ?: throw (IllegalStateException("The first call to SettingsRepository.getInstance() must pass a context object"))
        }

        const val SETTINGS_DATASTORE_NAME = "settings"

        fun indexOfFontSize(fontSize: Int): Int = FontSize.values.indexOfFirst { it == fontSize }

        fun indexOfParity(parity: Int): Int = Parity.values.indexOfFirst { it == parity }

        fun indexOfStopBits(stopBits: Int): Int = StopBits.values.indexOfFirst { it == stopBits }

        fun indexOfDataBits(dataBits: Int): Int = DataBits.values.indexOfFirst { it == dataBits }
    }
}

private fun doMigration(sharedPrefs: SharedPreferencesView, currentData: Preferences): Preferences {
    val currentKeys = currentData.asMap().keys.map { it.name }

    val filteredSharedPreferences =
        sharedPrefs.getAll().filter { (key, _) -> key !in currentKeys }

    val mutablePreferences = currentData.toMutablePreferences()
    for ((key, value) in filteredSharedPreferences) {
        if (key == "showed_eula_1") {
            mutablePreferences[SettingsKeys.SHOWED_EULA_V1_KEY] = value as Boolean
        } else if (key == "pref_baudrate") {
            val baudRate = (value as String).toIntOrNull() ?: 0
            if (baudRate != 0) {
                mutablePreferences[SettingsKeys.BAUD_RATE_KEY] = baudRate
                if (!SettingsRepository.BaudRateValues.isPreDefined(baudRate)) {
                    mutablePreferences[SettingsKeys.BAUD_RATE_FREE_INPUT_KEY] = baudRate
                }
            }
        } else if (key == "pref_data_bits") {
            val dataBits = (value as String).toIntOrNull() ?: 0
            if (dataBits != 0) {
                mutablePreferences[SettingsKeys.DATA_BITS_KEY] = dataBits
            }
        } else if (key == "pref_parity") {
            when (value as String) {
                "none" -> mutablePreferences[SettingsKeys.PARITY_KEY] = UsbSerialPort.PARITY_NONE
                "even" -> mutablePreferences[SettingsKeys.PARITY_KEY] = UsbSerialPort.PARITY_EVEN
                "odd" -> mutablePreferences[SettingsKeys.PARITY_KEY] = UsbSerialPort.PARITY_ODD
                "mark" -> mutablePreferences[SettingsKeys.PARITY_KEY] = UsbSerialPort.PARITY_MARK
                "space" -> mutablePreferences[SettingsKeys.PARITY_KEY] = UsbSerialPort.PARITY_SPACE
            }
        } else if (key == "pref_stop_bits") {
            when (value as String) {
                "1" -> mutablePreferences[SettingsKeys.STOP_BITS_KEY] = UsbSerialPort.STOPBITS_1
                "1.5" -> mutablePreferences[SettingsKeys.STOP_BITS_KEY] = UsbSerialPort.STOPBITS_1_5
                "2" -> mutablePreferences[SettingsKeys.STOP_BITS_KEY] = UsbSerialPort.STOPBITS_2
            }
        } else if (key == "pref_enter_key_sends") {
            when (value as String) {
                "CR" -> mutablePreferences[SettingsKeys.BYTES_SENT_BY_ENTER_KEY_KEY] =
                    SettingsRepository.BytesSentByEnterKey.CR

                "LF" -> mutablePreferences[SettingsKeys.BYTES_SENT_BY_ENTER_KEY_KEY] =
                    SettingsRepository.BytesSentByEnterKey.LF

                "CR_CR" -> mutablePreferences[SettingsKeys.BYTES_SENT_BY_ENTER_KEY_KEY] =
                    SettingsRepository.BytesSentByEnterKey.CR_LF
            }
        } else if (key == "pref_text_size") {
            val fontSizeFloat = (value as String).toFloatOrNull() ?: 0f
            val fontSizeInt = fontSizeFloat.roundToInt()
            if (fontSizeFloat > 0.1f && SettingsRepository.indexOfFontSize(fontSizeInt) > 0) {
                mutablePreferences[SettingsKeys.FONT_SIZE_KEY] = fontSizeInt
            }
        } else if (key == "pref_use_dedicated_field_for_kb_input") {
            if (value as Boolean) {
                mutablePreferences[SettingsKeys.INPUT_MODE_KEY] =
                    SettingsRepository.InputMode.WHOLE_LINE
            } else {
                mutablePreferences[SettingsKeys.INPUT_MODE_KEY] =
                    SettingsRepository.InputMode.CHAR_BY_CHAR
            }
        } else if (key == "pref_send_when_enter_key_pressed") {
            mutablePreferences[SettingsKeys.SEND_INPUT_LINE_ON_ENTER_KEY_KEY] = value as Boolean
        } else if (key == "pref_log_session") {
            mutablePreferences[SettingsKeys.LOG_SESSION_DATA_TO_FILE_KEY] = value as Boolean
        } else if (key == "pref_local_echo") {
            mutablePreferences[SettingsKeys.LOOPBACK_KEY] = value as Boolean
        } else if (key == "pref_scrollback_buffer_size") {
            val scrollBackBufferSize = (value as String).toIntOrNull() ?: 0
            if (scrollBackBufferSize != 0) {
                mutablePreferences[SettingsKeys.MAX_BYTES_TO_RETAIN_FOR_BACK_SCROLL_KEY] =
                    1000 * scrollBackBufferSize
            }
        }
    }

    return mutablePreferences.toPreferences()
}
