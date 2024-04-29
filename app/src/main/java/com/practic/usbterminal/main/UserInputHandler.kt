package com.practic.usbterminal.main

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.practic.usbterminal.settings.model.DefaultValues
import com.practic.usbterminal.settings.model.SettingsRepository
import com.practic.usbterminal.usbcommservice.UsbCommService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber


class UserInputHandler(
    settingsRepository: SettingsRepository,
    viewModelScope: CoroutineScope,
) {
    var usbCommService: UsbCommService? = null

    @Suppress("PrivatePropertyName")
    private val BASELINE_TXT_LEN = 5
    private val baselineTextToXmitCharByChar = arrayOf("1    ", "2    ")
    private val baselineMarks = arrayOf('1', '2')
    private var lastBaselineUsed = 0
    private val _textToXmitCharByChar = mutableStateOf(
        TextFieldValue(
            text = baselineTextToXmitCharByChar[lastBaselineUsed],
            selection = TextRange(baselineTextToXmitCharByChar[lastBaselineUsed].length)
        )
    )
    val textToXmitCharByChar: State<TextFieldValue> = _textToXmitCharByChar

    private val _ctrlButtonIsSelected = mutableStateOf(false)
    val ctrlButtonIsSelected: State<Boolean> = _ctrlButtonIsSelected

    private var bytesSentByEnterKey = DefaultValues.bytesSentByEnterKey

    init {
        val settingsStateFlow = settingsRepository.settingsStateFlow
        viewModelScope.launch {
            settingsStateFlow.collect {
                bytesSentByEnterKey = it.bytesSentByEnterKey
            }
        }
    }

    private var presetTextLen = baselineTextToXmitCharByChar[0].length
    fun onXmitCharByCharKBInput(tfv: TextFieldValue) {
        if (tfv.text.first() == baselineMarks[lastBaselineUsed]) {
            presetTextLen = BASELINE_TXT_LEN
        }
        val bytesToXmit: ByteArray = run {
            val nNewChars = tfv.text.length - presetTextLen
            if (nNewChars < 0) {
                ByteArray(-nNewChars) { '\b'.code.toByte() }
            } else if (nNewChars > 0) {
                ByteArray(nNewChars) { i ->
                    val b = tfv.text[presetTextLen + i].code.toByte()
                    if (_ctrlButtonIsSelected.value) {
                        when (b) {
                            in 64..95 -> {
                                _ctrlButtonIsSelected.value = false
                                (b - 64).toByte()
                            }

                            in 96..127 -> {
                                _ctrlButtonIsSelected.value = false
                                (b - 96).toByte()
                            }

                            else -> {
                                b
                            }
                        }
                    } else {
                        b
                    }
                }
            } else {
                ByteArray(0)
            }.also {
                presetTextLen = tfv.text.length
            }
        }
        if (bytesToXmit.isNotEmpty()) {
            usbCommService?.sendUsbData(processSendBuf(bytesToXmit))
        }
        lastBaselineUsed = 1 - lastBaselineUsed
        _textToXmitCharByChar.value = tfv.copy(
            text = baselineTextToXmitCharByChar[lastBaselineUsed],
            selection = TextRange(BASELINE_TXT_LEN)
        )
    }

    fun onCtrlKeyButtonClick() {
        Timber.d("onCtrlKeyButtonClick()")
        _ctrlButtonIsSelected.value = !_ctrlButtonIsSelected.value
    }

    fun onUpButtonClick() {
        Timber.d("onUpButtonClick")
        val bytesToXmit = byteArrayOf(0x1B, 0x5B, 0x41)
        usbCommService?.sendUsbData(bytesToXmit)
    }

    fun onDownButtonClick() {
        Timber.d("onDownButtonClick")
        val bytesToXmit = byteArrayOf(0x1B, 0x5B, 0x42)
        usbCommService?.sendUsbData(bytesToXmit)
    }

    fun onRightButtonClick() {
        Timber.d("onRightButtonClick")
        val bytesToXmit = byteArrayOf(0x1B, 0x5B, 0x43)
        usbCommService?.sendUsbData(bytesToXmit)
    }

    fun onLeftButtonClick() {
        Timber.d("onLeftButtonClick")
        val bytesToXmit = byteArrayOf(0x1B, 0x5B, 0x44)
        usbCommService?.sendUsbData(bytesToXmit)
    }

    fun onTabButtonClick() {
        Timber.d("onTabButtonClick")
        val bytesToXmit = byteArrayOf(0x09)
        usbCommService?.sendUsbData(bytesToXmit)
    }

    private fun processSendBuf(buf: ByteArray): ByteArray {
        @Suppress("CascadeIf")
        if (bytesSentByEnterKey == SettingsRepository.BytesSentByEnterKey.CR_LF) {
            val nExtraBytes = buf.count { it == 0x0A.toByte() }
            if (nExtraBytes == 0) {
                return buf
            }
            val result = ByteArray(buf.size + nExtraBytes)
            var i = 0
            buf.forEach {
                if (it == 0x0A.toByte()) {
                    result[i++] = 0x0D.toByte()
                    result[i++] = 0x0A.toByte()
                } else {
                    result[i++] = it
                }
            }
            return result
        } else if (bytesSentByEnterKey == SettingsRepository.BytesSentByEnterKey.CR) {
            val nNewLineChars = buf.count { it == 0x0A.toByte() }
            if (nNewLineChars == 0) {
                return buf
            }
            val result = ByteArray(buf.size)
            var i = 0
            buf.forEach {
                if (it == 0x0A.toByte()) {
                    result[i++] = 0x0D.toByte()
                } else {
                    result[i++] = it
                }
            }
            return result
        } else {
            return buf
        }
    }
}
