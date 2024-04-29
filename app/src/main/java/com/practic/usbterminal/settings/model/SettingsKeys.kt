package com.practic.usbterminal.settings.model

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object SettingsKeys {
    val THEME_TYPE_KEY = intPreferencesKey("themeType")
    val INPUT_MODE_KEY = intPreferencesKey("inputMode")
    val SEND_INPUT_LINE_ON_ENTER_KEY_KEY = booleanPreferencesKey("siloek")
    val BYTES_SENT_BY_ENTER_KEY_KEY = intPreferencesKey("bsbek")
    val LOOPBACK_KEY = booleanPreferencesKey("loopback")
    val SOUND_ON_KEY = booleanPreferencesKey("sound")
    val DROP_UNRECOGNIZED_CTRL_CHARS_KEY = booleanPreferencesKey("ducc")
    val FONT_SIZE_KEY = intPreferencesKey("fontSize")
    val DEFAULT_TEXT_COLOR_KEY = intPreferencesKey("defaultTextColorDialogParams")
    val DEFAULT_TEXT_COLOR_FREE_INPUT_KEY = intPreferencesKey("defaultTextColorFreeInput")
    val BAUD_RATE_KEY = intPreferencesKey("baudRate")
    val BAUD_RATE_FREE_INPUT_KEY = intPreferencesKey("baudRateFreeInput")
    val DATA_BITS_KEY = intPreferencesKey("dataBits")
    val STOP_BITS_KEY = intPreferencesKey("atopBits")
    val PARITY_KEY = intPreferencesKey("parity")
    val SET_DTR_KEY = booleanPreferencesKey("setDtr")
    val SET_RTS_KEY = booleanPreferencesKey("setRts")
    val LOG_SESSION_DATA_TO_FILE_KEY = booleanPreferencesKey("logToFile")
    val ALSO_LOG_OUTGOING_DATA_KEY = booleanPreferencesKey("logOutgoing")
    val MARK_LOGGED_OUTGOING_DATA_KEY = booleanPreferencesKey("markOutgoing")
    val ZIP_LOG_FILES_WHEN_SHARING_KEY = booleanPreferencesKey("zipShrdLogs")
    val CONNECT_TO_DEVICE_ON_START_KEY = booleanPreferencesKey("cos")
    val EMAIL_ADDR_FOR_SHARING_KEY = stringPreferencesKey("eafs")
    val WORK_ALSO_IN_BACKGROUND_KEY = booleanPreferencesKey("bg")
    val LOG_FILES_SORT_KEY = intPreferencesKey("logFilesSort")
    val SHOWED_EULA_V1_KEY = booleanPreferencesKey("seulav1")
    val SHOWED_V2_WELCOME_MSG_KEY = booleanPreferencesKey("sv2wm")
    val DISPLAY_TYPE_KEY = intPreferencesKey("dt")
    val SHOW_CTRL_BUTTON_KEY = booleanPreferencesKey("scb")
    val MAX_BYTES_TO_RETAIN_FOR_BACK_SCROLL_KEY = intPreferencesKey("bbs")
    val LAST_VISITED_HELP_URL_KEY = stringPreferencesKey("lvhu")
}