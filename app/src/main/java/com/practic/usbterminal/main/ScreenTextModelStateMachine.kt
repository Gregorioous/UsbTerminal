package com.practic.usbterminal.main

import com.practic.usbterminal.settings.model.DefaultValues
import timber.log.Timber
import java.lang.Integer.max

enum class StateOfStateMachine {
    IDLE,
    RECEIVED_ESC,
    RECEIVED_ESC_AND_BRACKET,
}

class ScreenTextModelStateMachine(
    private val screenTextModel: ScreenTextModel,
    private val cursorPosition: ScreenTextModel.CursorPosition,
) {
    private var state: StateOfStateMachine = StateOfStateMachine.IDLE
    private var param1 = StringBuilder(4)
    private var params = mutableListOf<Int>()
    private var keepParams = false
    var silentlyDropUnrecognizedCtrlChars = DefaultValues.silentlyDropUnrecognizedCtrlChars

    fun onNewByte(byte: Byte, dataWasAlreadyProcessed: Boolean, result: CharArray): Int {
        val c = iso_8859_1[byte.toUByte().toInt()]

        return when (state) {
            StateOfStateMachine.IDLE -> {
                if (c.code >= 32) {
                    result[0] = c
                    1
                } else {
                    onNewControlCharWhenIdle(c, result)
                }
            }

            StateOfStateMachine.RECEIVED_ESC -> onNewCharWhenReceivedEsc(c, result)
            StateOfStateMachine.RECEIVED_ESC_AND_BRACKET -> onNewCharWhenReceivedEscAndBracket(
                c,
                dataWasAlreadyProcessed,
                result
            )
        }
    }

    private fun onNewCharWhenIdle(c: Char, result: CharArray): Int {
        return if (c.code < 32) {
            onNewControlCharWhenIdle(c, result)
        } else {
            result[0] = c
            1
        }
    }

    private fun onNewControlCharWhenIdle(c: Char, result: CharArray): Int {
        return when (c) {
            '\u001B' -> {
                state = StateOfStateMachine.RECEIVED_ESC
                0
            }

            '\n' -> {
                screenTextModel.onNewLine()
                0
            }

            '\b' -> {
                cursorPosition.onBackspace()
                0
            }

            '\r' -> {
                cursorPosition.onCarriageReturn()
                0
            }

            '\t' -> {
                cursorPosition.moveRightToNextTabStop()
                screenTextModel.extendCurrentLineUpToCursor()
                0
            }

            '\u0007' -> {
                screenTextModel.beep()
                0
            }

            else -> {
                if (silentlyDropUnrecognizedCtrlChars) {
                    0
                } else {
                    result[0] = '\u2e2e'
                    1
                }
            }
        }
    }

    private fun onNewCharWhenReceivedEsc(c: Char, result: CharArray): Int {
        return if (c == '[') {
            state = StateOfStateMachine.RECEIVED_ESC_AND_BRACKET
            0
        } else if (c == 'H') {
            state = StateOfStateMachine.IDLE
            screenTextModel.positionCursorInDisplayedWindow(listOf(0, 0))
            0
        } else {
            state = StateOfStateMachine.IDLE
            result[0] = '^'
            result[1] = '['
            val tmp = CharArray(1)
            val l = onNewCharWhenIdle(c, tmp)
            if (l > 0) {
                result[2] = tmp[0]
                3
            } else {
                2
            }
        }
    }

    private fun onNewCharWhenReceivedEscAndBracket(
        c: Char, dataWasAlreadyProcessed: Boolean, result: CharArray
    ): Int {
        return if ((c in '0'..'9' || (c == '-' && param1.isEmpty())) && param1.length <= 3) {
            param1.append(c)
            0
        } else {
            val pn1 = param1.getNumericValue()
            keepParams = false
            state = StateOfStateMachine.IDLE
            var rc = 0
            when (c) {
                'A' -> {
                    cursorPosition.moveUp(max(1, pn1))
                    screenTextModel.extendCurrentLineUpToCursor()
                }

                'B' -> {
                    cursorPosition.moveDown(max(1, pn1))
                    screenTextModel.extendCurrentLineUpToCursor()
                }

                'C' -> {
                    cursorPosition.moveRight(max(1, pn1))
                    screenTextModel.extendCurrentLineUpToCursor()
                }

                'D' -> {
                    cursorPosition.moveLeft(max(1, pn1))
                }

                'G' -> {
                    cursorPosition.moveToColumn(max(1, pn1) - 1)
                    screenTextModel.extendCurrentLineUpToCursor()
                }

                'H' -> {
                    params.add(pn1)
                    screenTextModel.positionCursorInDisplayedWindow(params)
                }

                'J' -> {
                    screenTextModel.eraseDisplay(pn1)
                }

                'K' -> {
                    screenTextModel.eraseLine(pn1)
                }

                ';' -> {
                    params.add(pn1)
                    if (params.size > 10) {
                        Timber.w("onNewCharWhenReceivedEscAndBracket() Too many parameters")
                        params.clear()
                    } else {
                        keepParams = true
                        state = StateOfStateMachine.RECEIVED_ESC_AND_BRACKET
                    }
                }

                'm' -> {
                    params.add(pn1)
                    screenTextModel.selectGraphicRendition(params, alsoRememberRendition = true)
                }

                'n' -> {
                    if (!dataWasAlreadyProcessed) {
                        screenTextModel.doDeviceStatusReport(pn1)
                    }
                }

                else -> {
                    result[0] = '^'; result[1] = '['
                    result[2] = '['
                    var i = 3
                    param1.forEach { c2 ->
                        result[i++] = c2
                    }
                    result[i++] = c
                    rc = i
                }
            }
            param1.clear()
            if (!keepParams) params.clear()
            rc
        }
    }
}
