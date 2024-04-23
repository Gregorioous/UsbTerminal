package com.practic.usbterminal.main

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import java.lang.Integer.max
import java.lang.Integer.min
import java.util.concurrent.atomic.AtomicLong

class ScreenLine(
    private var maxLineLen: Int,
) {
    constructor(text: String) : this(text, text.length)
    constructor(text: String, maxLineLen: Int) : this(maxLineLen) {
        textLength = min(text.length, maxLineLen)
        repeat(textLength) { i ->
            textArray[i] = text[i]
        }
    }

    private var textArray: CharArray = CharArray(maxLineLen)
    var textLength: Int = 0
        private set
    private val spanStyles: MutableList<AnnotatedString.Range<SpanStyle>> = mutableListOf()
    private val activeSpanStyles: MutableList<MutableSpanStyleRange> = mutableListOf()
    private var annotatedString: AnnotatedString? = null


    private val _uid = ScreenLineUid()
    val uid: Long
        get() = _uid.toLong()

    fun setMaxLineLength(maxLen: Int) {
        maxLineLen = maxLen
        if (textLength > maxLineLen) textLength = maxLineLen
        textArray = textArray.copyOf(maxLineLen)
    }

    fun putCharAt(c: Char, index: Int, alsoExtendActiveSpanStyles: Boolean): Int {
        if (index >= maxLineLen) return -1
        var deltaSize = 0
        textArray[index] = c
        if (index >= textLength) {
            for (i in textLength until index) {
                textArray[i] = ' '
            }
            deltaSize = index + 1 - textLength
            textLength = min(index + 1, maxLineLen)
        }

        if (alsoExtendActiveSpanStyles) {
            activeSpanStyles.forEach {
                if (it.end < index + 1) {
                    it.end = index + 1
                }
            }
        }

        markThatWeHaveBeenChanged()
        return deltaSize
    }

    fun appendSpacesUpToLocation(location: Int) {
        val lastIndex = min(location, maxLineLen - 1)
        for (i in textLength..lastIndex) {
            textArray[i] = ' '
        }

        activeSpanStyles.forEach {
            if (it.end < lastIndex) {
                it.end = lastIndex
            }
        }
    }

    fun clear(): Int {
        val oldTextLength = textLength
        textArray[0] = ' '
        textLength = 1
        spanStyles.clear()
        activeSpanStyles.clear()
        markThatWeHaveBeenChanged()
        return oldTextLength - 1
    }

    fun clearAndTruncateTo(to: Int): Int {
        if (to >= maxLineLen) return 0
        for (i in 0..to) {
            textArray[i] = ' '
        }
        val oldTextLength = textLength
        textLength = to + 1
        markThatWeHaveBeenChanged()
        return oldTextLength - textLength
    }

    fun clearFromInclusive(from: Int): Int {
        if (from >= maxLineLen) return 0
        textArray[from] = ' '
        val oldTextLength = textLength
        textLength = from + 1
        markThatWeHaveBeenChanged()
        return oldTextLength - textLength
    }

    fun clearToInclusive(to: Int) {
        if (to >= maxLineLen) return
        for (i in 0..to) {
            textArray[i] = ' '
        }
        markThatWeHaveBeenChanged()
    }

    fun addSpanStyle(style: AnnotatedString.Range<SpanStyle>, alsoToggleUid: Boolean) {
        spanStyles.add(style)
        if (alsoToggleUid) {
            _uid.toggle()
        }
        markThatWeHaveBeenChanged()
    }

    fun removeSpanStyleByTag(tag: String, alsoToggleUid: Boolean) {
        spanStyles.removeAll { it.tag == tag }
        if (alsoToggleUid) {
            _uid.toggle()
        }
        markThatWeHaveBeenChanged()
    }

    fun startSpanStyle(style: SpanStyle, index: Int, tag: String) {
        val spanStyleRange = MutableSpanStyleRange(
            start = index,
            end = index,
            item = style,
            tag = tag
        )
        if (activeSpanStyles.size < 20) {
            activeSpanStyles.add(spanStyleRange)
        }
    }

    fun endAllSpanStyles(index: Int) {
        activeSpanStyles.forEach {
            it.end = max(index, it.end)
            if (it.end > it.start) {
                spanStyles.add(it.toSpanStyleRange())
            }
        }
        activeSpanStyles.clear()
        markThatWeHaveBeenChanged()
    }


    fun endAllSpanStylesByTag(index: Int, tag: String) {
        var weHaveBeenChanged = false
        val itr = activeSpanStyles.iterator()
        while (itr.hasNext()) {
            val spanStyleRange = itr.next()
            if (spanStyleRange.tag == tag) {
                spanStyleRange.end = index
                if (spanStyleRange.end > spanStyleRange.start) {
                    spanStyles.add(spanStyleRange.toSpanStyleRange())
                }
                itr.remove()
                weHaveBeenChanged = true
            }
        }
        if (weHaveBeenChanged) markThatWeHaveBeenChanged()
    }

    fun getAnnotatedString(): AnnotatedString {
        return annotatedString ?: run {
            val currentStyleSpans: MutableList<AnnotatedString.Range<SpanStyle>> =
                if (activeSpanStyles.size == 0) {
                    spanStyles
                } else {
                    val currentStyleSpans = mutableListOf<AnnotatedString.Range<SpanStyle>>()
                    spanStyles.forEach {
                        currentStyleSpans.add(it)
                    }
                    activeSpanStyles.forEach {
                        currentStyleSpans.add(it.toSpanStyleRange())
                    }
                    currentStyleSpans
                }
            annotatedString = AnnotatedString(
                text = String(textArray.copyOfRange(0, textLength)),
                spanStyles = currentStyleSpans
            )
            annotatedString!!
        }
    }

    private fun markThatWeHaveBeenChanged() {
        _uid.generateNewUid()
        annotatedString = null
    }

    private class ScreenLineUid {
        private val uid = AtomicLong(ScreenLineUidGenerator.getNextUid())
        private var dirty: Boolean = false
        fun toLong(): Long {
            if (dirty) {
                uid.set(ScreenLineUidGenerator.getNextUid())
                dirty = false
            }
            return uid.toLong()
        }

        fun generateNewUid() {
            dirty = true
        }

        fun toggle(): Long {
            if (dirty) {
                uid.set(ScreenLineUidGenerator.getNextUid())
                dirty = false
            }
            return uid.accumulateAndGet(0x4000_0000_0000_0000L) { a, b -> a xor b }
        }
    }

    private object ScreenLineUidGenerator {
        private var uid = AtomicLong(0L)


        fun getNextUid(): Long = uid.incrementAndGet()
    }

    private data class MutableSpanStyleRange(
        var start: Int,
        var end: Int,
        val item: SpanStyle,
        val tag: String,
    ) {
        fun toSpanStyleRange(): AnnotatedString.Range<SpanStyle> {
            return AnnotatedString.Range(
                start = start,
                end = end,
                item = item,
                tag = tag,
            )
        }
    }
}