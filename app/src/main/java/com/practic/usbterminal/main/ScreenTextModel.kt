package com.practic.usbterminal.main

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import com.practic.usbterminal.settings.model.DefaultValues
import com.practic.usbterminal.ui.theme.CursorColorForDarkTheme
import com.practic.usbterminal.ui.theme.CursorColorForLightTheme
import com.practic.usbterminal.usbcommservice.IOPacketsList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.lang.Integer.max
import java.lang.Integer.min
import java.util.concurrent.atomic.AtomicBoolean

class ScreenTextModel(
    private val coroutineScope: CoroutineScope,
    private var maxLineLen: Int,
    private val sendBuf: (buf: ByteArray) -> Unit,
    maxTotalSize: Int,
) {
    private companion object {
        const val TOTAL_SIZE_TRIMMING_HYSTERESIS = 1000
    }

    data class DisplayedCursorPosition(val line: Int, val column: Int)

    private var uid: Int = 1
        get() {
            field++; return field
        }

    data class ScreenState(
        val lines: Array<ScreenLine>,
        val displayedCursorPosition: DisplayedCursorPosition,
        val shouldScrollToBottom: Int,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ScreenState

            if (!(lines === other.lines)) return false
            if (displayedCursorPosition != other.displayedCursorPosition) return false
            if (shouldScrollToBottom != other.shouldScrollToBottom) return false

            return true
        }

        override fun hashCode(): Int {
            var result = lines.contentHashCode()
            result = 31 * result + displayedCursorPosition.hashCode()
            result = 31 * result + shouldScrollToBottom.hashCode()
            return result
        }
    }

    private val _screenState = mutableStateOf(
        ScreenState(
            lines = emptyArray(),
            displayedCursorPosition = DisplayedCursorPosition(0, 0),
            shouldScrollToBottom = 0,
        )
    )
    val screenState: State<ScreenState> = _screenState
    fun shouldScrollToBottom() {
        _screenState.value = _screenState.value.copy(shouldScrollToBottom = uid)
    }

    fun onScrolledToBottom(uid: Int) {
        if (uid == _screenState.value.shouldScrollToBottom) {
            _screenState.value = _screenState.value.copy(shouldScrollToBottom = 0)
        }
    }

    private val screenLines = mutableListOf<ScreenLine>()
    private var totalCharCount = 0

    var soundOn = DefaultValues.soundOn
    var silentlyDropUnrecognizedCtrlChars: Boolean = DefaultValues.silentlyDropUnrecognizedCtrlChars
        set(value) {
            field = value
            stateMachine.silentlyDropUnrecognizedCtrlChars = value
        }

    private val uiUpdateTriggered = AtomicBoolean(false)
    private val missedScrollToBottom = AtomicBoolean(false)
    private var screenHeight = 0
    private val currentGraphicRendition = mutableListOf<Int>()
    private val cursor = Cursor()
    private val stateMachine = ScreenTextModelStateMachine(this, cursor.position)
    private var totalSizeUpperLimit: Int = maxTotalSize + maxLineLen

    private val mutex = Mutex()

    init {
        clear()
    }

    fun onStart() {
        cursor.onStart()
    }

    fun onStop() {
        cursor.onStop()
        clear()
    }

    fun setMaxTotalSize(newMaxTotalSize: Int) {
        coroutineScope.launch {
            mutex.withLock {
                totalSizeUpperLimit = newMaxTotalSize + maxLineLen
                trimIfNeeded()
            }
        }
    }

    fun clear() {
        coroutineScope.launch {
            mutex.withLock {
                cursor.isBlinking = false
                cursor.hide()
                screenLines.clear()
                totalCharCount = 0
                appendNewLine()
                putCharAtCursorLocation(' ')
                _screenState.value = ScreenState(
                    lines = screenLines.toTypedArray(),
                    shouldScrollToBottom = 0,
                    displayedCursorPosition = DisplayedCursorPosition(
                        cursor.position.lineIndex + 1,
                        cursor.position.offsetInLine + 1
                    )
                )
                cursor.isBlinking = true
            }
        }
    }

    private val currentLine: ScreenLine
        get() = screenLines[cursor.position.lineIndex]

    fun setScreenDimensions(width: Int, height: Int) {
        screenHeight = height
        if (width != maxLineLen) {
            coroutineScope.launch {
                mutex.withLock {
                    maxLineLen = width
                    screenLines.forEach { screenLine ->
                        screenLine.setMaxLineLength(maxLineLen)
                    }
                }
            }
        }
    }

    fun setIsDarkTheme(isDarkTheme: Boolean) {
        val cursorColor = if (isDarkTheme) {
            CursorColorForDarkTheme
        } else {
            CursorColorForLightTheme
        }
        cursor.setColor(cursorColor)
    }

    fun onNewData(
        data: ByteArray,
        offset: Int,
        dataDirection: IOPacketsList.DataDirection,
        dataWasAlreadyProcessed: Boolean
    ) {
        if (dataDirection == IOPacketsList.DataDirection.OUT) {
            return
        }
        var remainingBytes = data.size - offset
        if (remainingBytes <= 0) {
            Timber.w("onNewData(): remainingBytes=$remainingBytes")
            return
        }
        var inputByteOffset = offset
        coroutineScope.launch {
            mutex.withLock {
                cursor.hide()
                while (remainingBytes > 0) {
                    processReceivedByte(data[inputByteOffset], dataWasAlreadyProcessed)

                    inputByteOffset++
                    remainingBytes--
                }
            }
            updateUi(alsoScrollToBottomOfScreen = true)
        }
    }

    private val resultOfByteProcessing = CharArray(64)
    private fun processReceivedByte(byte: Byte, dataWasAlreadyProcessed: Boolean) {
        val nCharsToDisplay = stateMachine.onNewByte(
            byte, dataWasAlreadyProcessed, resultOfByteProcessing
        )

        for (i in 0 until nCharsToDisplay) {
            putCharAtCursorLocation(resultOfByteProcessing[i], advanceCursorPosition = true)
        }
    }

    private fun putCharAtCursorLocation(c: Char, advanceCursorPosition: Boolean = false) {
        val lineSizeDelta =
            currentLine.putCharAt(c, cursor.position.offsetInLine, advanceCursorPosition)
        if (lineSizeDelta == -1) {
            onNewLine()
            currentLine.putCharAt(c, cursor.position.offsetInLine, advanceCursorPosition)
        } else {
            totalCharCount += lineSizeDelta
        }
        if (advanceCursorPosition) {
            cursor.position.offsetInLine++
            if (cursor.position.offsetInLine >= currentLine.textLength) {
                putCharAtCursorLocation(' ')
            }
        }
    }

    fun extendCurrentLineUpToCursor() {
        currentLine.appendSpacesUpToLocation(cursor.position.offsetInLine)
    }

    fun onNewLine() {
        cursor.position.onNewLine()
        if (cursor.position.lineIndex >= screenLines.size) {
            appendNewLine()
        }
    }

    private fun appendNewLine() {
        screenLines.add(ScreenLine(" ", maxLineLen))
        totalCharCount++
        trimIfNeeded()
        cursor.position.setPosition(lineIndex = screenLines.size - 1, offsetInLine = 0)
        selectGraphicRendition(currentGraphicRendition, false)
    }

    private fun trimIfNeeded() {
        if (totalCharCount > totalSizeUpperLimit + TOTAL_SIZE_TRIMMING_HYSTERESIS) {
            Timber.d("Trimming totalCharCount=$totalCharCount  totalSizeUpperLimit=$totalSizeUpperLimit  nLines=${screenLines.size}")
            while (totalCharCount - screenLines.first().textLength > totalSizeUpperLimit) {
                Timber.d("Deleting 1st line. txtLength=${screenLines.first().textLength}")
                totalCharCount -= screenLines.removeFirst().textLength
            }
        }
    }

    private val colors30to37 = arrayOf(
        0xFF000000,
        0xFFBB0000,
        0xFF00BB00,
        0xFFBBBB00,
        0xFF0000BB,
        0xFFBB00BB,
        0xFF00BBBB,
        0xFFBBBBBB
    )
    private val colors90to97 = arrayOf(
        0xFF555555,
        0xFFFF5555,
        0xFF55FF55,
        0xFFFFFF55,
        0xFF5555FF,
        0xFFFF55FF,
        0xFF55FFFF,
        0xFFFFFFFF
    )

    fun selectGraphicRendition(params: List<Int>, alsoRememberRendition: Boolean) {
        params.forEach {
            when (it) {
                0 -> {
                    currentLine.endAllSpanStyles(cursor.position.offsetInLine)
                    if (alsoRememberRendition) currentGraphicRendition.clear()
                }

                in 30..37 -> {
                    currentLine.endAllSpanStylesByTag(cursor.position.offsetInLine, "clr")
                    currentLine.startSpanStyle(
                        style = SpanStyle(color = Color(colors30to37[it - 30])),
                        index = cursor.position.offsetInLine,
                        tag = "clr",
                    )
                    if (alsoRememberRendition) currentGraphicRendition.add(it)
                }

                in 90..97 -> {
                    currentLine.endAllSpanStylesByTag(cursor.position.offsetInLine, "clr")
                    currentLine.startSpanStyle(
                        style = SpanStyle(color = Color(colors90to97[it - 90])),
                        index = cursor.position.offsetInLine,
                        tag = "clr",
                    )
                    if (alsoRememberRendition) currentGraphicRendition.add(it)
                }

                49 -> {
                }

                else -> Timber.w("selectGraphicRendition: Unsupported parameter: $it")
            }
        }
    }

    private suspend fun updateUi(alsoScrollToBottomOfScreen: Boolean) {
        if (uiUpdateTriggered.compareAndSet(false, true)) {
            coroutineScope.launch {
                delay(40)
                uiUpdateTriggered.set(false)
                mutex.withLock {
                    val shouldScrollToBottom =
                        if (alsoScrollToBottomOfScreen || missedScrollToBottom.getAndSet(false)) uid else _screenState.value.shouldScrollToBottom
                    _screenState.value = ScreenState(
                        lines = screenLines.toTypedArray(),
                        shouldScrollToBottom = shouldScrollToBottom,
                        displayedCursorPosition = DisplayedCursorPosition(
                            cursor.position.lineIndex + 1,
                            cursor.position.offsetInLine + 1
                        )
                    )
                }
            }
        } else {
            if (alsoScrollToBottomOfScreen) {
                missedScrollToBottom.set(true)
            }
        }
    }

    private inner class Cursor {
        val position = CursorPosition(lineIndex = 0, offsetInLine = 0)
        private val lastDisplayedPosition = CursorPosition(lineIndex = 0, offsetInLine = 0)
        private var cursorCurrentlyShown = false
        private var spanStyleUnderscore =
            SpanStyle(textDecoration = TextDecoration.Underline, color = Color.Transparent)
        var isBlinking = true

        fun setColor(color: Color) {
            spanStyleUnderscore =
                SpanStyle(textDecoration = TextDecoration.Underline, color = color)
        }

        private fun show() {
            if (position.lineIndex >= screenLines.size)
                return
            lastDisplayedPosition.setPosition(position)
            screenLines[position.lineIndex].addSpanStyle(
                AnnotatedString.Range(
                    start = position.offsetInLine,
                    end = position.offsetInLine + 1,
                    item = spanStyleUnderscore,
                    tag = "crsr"
                ),
                true
            )
            cursorCurrentlyShown = true
        }

        fun hide() {
            if (cursorCurrentlyShown) {
                screenLines[lastDisplayedPosition.lineIndex].removeSpanStyleByTag("crsr", true)
                cursorCurrentlyShown = false
            }
        }

        var job: Job? = null
        fun onStart() {
            job = coroutineScope.launch {
                while (isActive) {
                    if (isBlinking) {
                        if (cursorCurrentlyShown)
                            mutex.withLock { hide() }
                        else
                            mutex.withLock { show() }
                        updateUi(alsoScrollToBottomOfScreen = false)
                    }
                    delay(500)
                }
            }
        }

        fun onStop() {
            job?.cancel()
        }
    }

    inner class CursorPosition(
        var lineIndex: Int,
        var offsetInLine: Int,
    ) {
        fun setPosition(lineIndex: Int, offsetInLine: Int) {
            this.lineIndex = lineIndex; this.offsetInLine = offsetInLine
        }

        fun setPosition(other: CursorPosition) {
            this.lineIndex = other.lineIndex; this.offsetInLine = other.offsetInLine
        }

        fun onNewLine() {
            lineIndex++; offsetInLine = 0
        }

        fun onCarriageReturn() {
            offsetInLine = 0
        }

        fun onBackspace() {
            if (offsetInLine > 0) offsetInLine--
        }

        fun moveLeft(n: Int) {
            if (n < 0) {
                moveRight(-n)
            } else {
                offsetInLine = max(0, offsetInLine - n)
            }
        }

        fun moveRight(n: Int) {
            if (n < 0) {
                moveLeft(-n)
            } else {
                offsetInLine = min(maxLineLen - 1, offsetInLine + n)
            }
        }

        fun moveRightToNextTabStop() {
            offsetInLine = min(maxLineLen - 1, (offsetInLine + 8) and 0xFFF8)
        }

        fun moveToColumn(n: Int) {
            offsetInLine = min(maxLineLen - 1, n)
        }

        fun moveDown(n: Int) {
            lineIndex = min(screenLines.size - 1, lineIndex + n)
        }

        fun moveUp(n: Int) {
            lineIndex = max(0, lineIndex - n)
        }

        fun copy(): CursorPosition {
            return CursorPosition(lineIndex, offsetInLine)
        }
    }


    internal fun eraseLine(n: Int) {
        when (n) {
            0 -> {
                totalCharCount -= screenLines[cursor.position.lineIndex].clearFromInclusive(cursor.position.offsetInLine)
            }

            1 -> {
                screenLines[cursor.position.lineIndex].clearToInclusive(cursor.position.offsetInLine)
            }

            2 -> {
                totalCharCount -= screenLines[cursor.position.lineIndex].clearAndTruncateTo(cursor.position.offsetInLine)
            }
        }
    }

    internal fun eraseDisplay(ps: Int) {
        when (ps) {
            0 -> {
                eraseLine(0)
                for (i in (cursor.position.lineIndex + 1)..screenLines.lastIndex) {
                    totalCharCount -= screenLines[i].clear()
                }
            }

            2 -> {
                val currentCursorPosition = cursor.position.copy()
                positionCursorInDisplayedWindow(1, 1)
                eraseDisplay(0)
                totalCharCount -= screenLines[currentCursorPosition.lineIndex].clearAndTruncateTo(
                    currentCursorPosition.offsetInLine
                )
                cursor.position.setPosition(currentCursorPosition)
            }
        }
    }

    internal fun beep(durationMs: Int = 200) {
        if (soundOn) {
            try {
                ToneGenerator(AudioManager.STREAM_NOTIFICATION, ToneGenerator.MAX_VOLUME).startTone(
                    ToneGenerator.TONE_PROP_PROMPT,
                    durationMs
                )
            } catch (e: Exception) {
                Timber.e("beep(): ${e.message}")
            }
        }
    }

    private fun positionCursorInDisplayedWindow(nRow: Int, nCol: Int) {
        var row = nRow
        if (row < 1) row = 1 else if (row > screenLines.size) row = screenLines.size
        var col = nCol
        if (col < 1) col = 1 else if (col > maxLineLen) col = maxLineLen
        val firstDisplayedLine = max(0, screenLines.size - screenHeight)
        val targetLine = firstDisplayedLine + row - 1
        cursor.position.setPosition(targetLine, col - 1)
    }

    internal fun positionCursorInDisplayedWindow(params: List<Int>) {
        when {
            params.isEmpty() -> positionCursorInDisplayedWindow(1, 1)
            params.size == 1 -> positionCursorInDisplayedWindow(params[0], 1)
            params.size == 2 -> positionCursorInDisplayedWindow(params[0], params[1])
            else -> Timber.w("positionCursorInDisplayedWindow() params.size=${params.size} (should be 0,1 or 2)")
        }
    }

    internal fun doDeviceStatusReport(ps1: Int) {
        when (ps1) {
            6 -> {
                val cpr =
                    "\u001B[${cursor.position.lineIndex + 1};${cursor.position.offsetInLine + 1}R".toByteArray()
                sendBuf(cpr)
            }

            5 -> {
                val dsr = "\u001B[0n".toByteArray()
                sendBuf(dsr)
            }

            else -> {
                Timber.w("Received Device Status Report (DSR) sequence with unsupported Ps1: $ps1")
            }
        }
    }

}

fun StringBuilder.getNumericValue(): Int {
    return when {
        isEmpty() -> 0
        else -> {
            var nv = 0
            run breakSimulationLabel@{
                this.forEach {
                    if (it in '0'..'9') {
                        nv = 10 * nv + it.code - '0'.code
                    } else {
                        Timber.w("StringBuilder.getNumericValue(): Bad string: '$this'")
                        return@breakSimulationLabel
                    }
                }
            }
            nv
        }
    }
}
