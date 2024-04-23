package com.practic.usbterminal.main

import android.icu.text.SimpleDateFormat
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import com.hoho.android.usbserial.util.HexDump
import com.practic.usbterminal.ui.theme.ColorOfInputHexTextForDarkTheme
import com.practic.usbterminal.ui.theme.ColorOfInputHexTextForLightTheme
import com.practic.usbterminal.ui.theme.ColorOfOutputHexTextForDarkTheme
import com.practic.usbterminal.ui.theme.ColorOfOutputHexTextForLightTheme
import com.practic.usbterminal.ui.theme.TimestampColorForDarkTheme
import com.practic.usbterminal.ui.theme.TimestampColorForLightTheme
import com.practic.usbterminal.usbcommservice.IOPacketsList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class ScreenHexModel(
    private val coroutineScope: CoroutineScope,
    private var maxTotalSize: Int,
) {
    private companion object {
        const val TOTAL_SIZE_TRIMMING_HYSTERESIS = 1000

        var nextUid = 1L
            get() = field++
    }

    class HexTextBlock(
        var uid: Long = nextUid,
        var timeStamp: Long,
        var dataDirection: IOPacketsList.DataDirection,
        _annotatedString: AnnotatedString? = null,
    ) {
        var annotatedString: AnnotatedString? = _annotatedString
            set(value) {
                field = value
                uid = nextUid
            }
    }

    private val hexTextBlocks = mutableListOf<HexTextBlock>()
    private val _screenHexTextBlocksState = mutableStateOf<Array<HexTextBlock>>(emptyArray())
    val screenHexTextBlocksState: State<Array<HexTextBlock>> = _screenHexTextBlocksState
    private var totalCharCount = 0

    private val _shouldScrollToBottom = mutableStateOf(false)
    val shouldScrollToBottom: State<Boolean> = _shouldScrollToBottom
    fun setShouldScrollToBottom() {
        _shouldScrollToBottom.value = true
    }

    fun onScrolledToBottom() {
        _shouldScrollToBottom.value = false
    }

    init {
        hexTextBlocks.add(
            HexTextBlock(
                timeStamp = 0L,
                dataDirection = IOPacketsList.DataDirection.UNDEFINED
            )
        )
    }

    private val mutex = Mutex()

    private val latestDataBAOS = ByteArrayOutputStream(1000)
    private val timeFormatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val uiUpdateTriggered = AtomicBoolean(false)


    fun onStart() {}
    fun onStop() {
        clear()
    }

    private var spanStyleForInputDataText = SpanStyle(color = Color.Transparent)
    private var spanStyleForOutputDataText = SpanStyle(color = Color.Transparent)
    private var spanStyleForTimestamp = SpanStyle(color = Color.Transparent)
    fun setIsDarkTheme(isDarkTheme: Boolean) {
        if (isDarkTheme) {
            spanStyleForInputDataText = SpanStyle(color = ColorOfInputHexTextForDarkTheme)
            spanStyleForOutputDataText = SpanStyle(color = ColorOfOutputHexTextForDarkTheme)
            spanStyleForTimestamp = SpanStyle(color = TimestampColorForDarkTheme)
        } else {
            spanStyleForInputDataText = SpanStyle(color = ColorOfInputHexTextForLightTheme)
            spanStyleForOutputDataText = SpanStyle(color = ColorOfOutputHexTextForLightTheme)
            spanStyleForTimestamp = SpanStyle(color = TimestampColorForLightTheme)
        }
    }

    fun onNewData(
        data: ByteArray,
        offset: Int,
        dataDirection: IOPacketsList.DataDirection,
        timeStamp: Long
    ) {
        coroutineScope.launch {
            mutex.withLock {
                var lastHexTextBlock = hexTextBlocks.last()
                if (timeStamp != lastHexTextBlock.timeStamp ||
                    dataDirection != lastHexTextBlock.dataDirection ||
                    latestDataBAOS.size() >= 1024
                ) {
                    if (hexTextBlocks.size == 1 && lastHexTextBlock.dataDirection == IOPacketsList.DataDirection.UNDEFINED) {
                        lastHexTextBlock.timeStamp = timeStamp
                        lastHexTextBlock.dataDirection = dataDirection
                    } else {
                        if (latestDataBAOS.size() > 0) {
                            dumpLatestDataIntoTextBlock(lastHexTextBlock)
                            latestDataBAOS.reset()
                        }

                        lastHexTextBlock =
                            HexTextBlock(timeStamp = timeStamp, dataDirection = dataDirection)
                        hexTextBlocks.add(
                            lastHexTextBlock
                        )
                    }
                }
                latestDataBAOS.write(data, offset, data.size - offset)
                dumpLatestDataIntoTextBlock(lastHexTextBlock)
                trimIfNeeded()
            }
            updateUi()
        }
    }

    private fun dumpLatestDataIntoTextBlock(hexTextBlock: HexTextBlock) {
        val timeStampStr = "\n${timeFormatter.format(Date(hexTextBlock.timeStamp))}\n"
        val hexDumpStr =
            HexDump.dumpHexString(latestDataBAOS.toByteArray(), 0, latestDataBAOS.size())
        totalCharCount -= (hexTextBlock.annotatedString?.length ?: 0)
        hexTextBlock.annotatedString =
            with(AnnotatedString.Builder(timeStampStr.length + hexDumpStr.length)) {
                pushStyle(spanStyleForTimestamp)
                append(timeStampStr)
                pop()
                pushStyle(dataDirectionToSpanStyle(hexTextBlock.dataDirection))
                append(hexDumpStr)
                pop()
                toAnnotatedString()
            }
        totalCharCount += (hexTextBlock.annotatedString?.length ?: 0)
    }

    fun setMaxTotalSize(newMaxTotalSize: Int) {
        coroutineScope.launch {
            mutex.withLock {
                maxTotalSize = newMaxTotalSize
                trimIfNeeded()
            }
        }
    }

    private fun trimIfNeeded() {
        if (totalCharCount > maxTotalSize + TOTAL_SIZE_TRIMMING_HYSTERESIS) {
            while (totalCharCount - (hexTextBlocks.firstOrNull()?.annotatedString?.length
                    ?: 0) > maxTotalSize
            ) {
                totalCharCount -= (hexTextBlocks.removeFirst().annotatedString?.length ?: 0)
            }
        }
    }

    private val emptyBlock = HexTextBlock(
        timeStamp = 0L,
        dataDirection = IOPacketsList.DataDirection.UNDEFINED,
        _annotatedString = AnnotatedString("")
    )

    private suspend fun updateUi() {
        if (uiUpdateTriggered.compareAndSet(false, true)) {
            delay(40)
            uiUpdateTriggered.set(false)
            mutex.withLock {
                val screenHexTextBlocks = hexTextBlocks.toTypedArray(hexTextBlocks.size + 1)
                screenHexTextBlocks[hexTextBlocks.size] = emptyBlock
                _screenHexTextBlocksState.value = screenHexTextBlocks
                _shouldScrollToBottom.value = true
            }
        }
    }

    fun clear() {
        coroutineScope.launch {
            mutex.withLock {
                hexTextBlocks.clear()
                hexTextBlocks.add(
                    HexTextBlock(
                        timeStamp = 0L,
                        dataDirection = IOPacketsList.DataDirection.UNDEFINED
                    )
                )
                latestDataBAOS.reset()
                totalCharCount = 0
                _screenHexTextBlocksState.value = hexTextBlocks.toTypedArray()
            }
        }
    }

    private fun dataDirectionToSpanStyle(dd: IOPacketsList.DataDirection): SpanStyle {
        return when (dd) {
            IOPacketsList.DataDirection.OUT -> spanStyleForOutputDataText
            else -> spanStyleForInputDataText
        }
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> Collection<T>.toTypedArray(size: Int): Array<T> {
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    val thisCollection = this as java.util.Collection<T>
    return thisCollection.toArray(arrayOfNulls<T>(size)) as Array<T>
}