package com.practic.usbterminal.screens.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.practic.usbterminal.main.ScreenHexModel
import com.practic.usbterminal.ui.util.isKeyboardOpenAsState


@Composable
fun ColumnScope.TerminalScreenHexSection(
    textBlocks: State<Array<ScreenHexModel.HexTextBlock>>,
    shouldScrollToBottom: State<Boolean>,
    shouldReportIfAtBottom: Boolean,
    onReportIfAtBottom: (Boolean) -> Unit,
    onScrolledToBottom: () -> Unit,
    fontSize: Int,
    shouldRespondToClicks: Boolean,
    mainFocusRequester: FocusRequester,
    auxFocusRequester: FocusRequester,
    onKeyboardStateChange: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val isKeyboardOpen by isKeyboardOpenAsState()
    var atBottomBeforeKBWasOpened by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = shouldReportIfAtBottom) {
        if (shouldReportIfAtBottom) {
            onReportIfAtBottom(
                lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == lazyListState.layoutInfo.totalItemsCount - 1
            )
        }
    }


    val interactionSource = remember { MutableInteractionSource() }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .weight(1f, true)
            .background(Color.Black)
            .clickable(interactionSource, indication = null) {
                if (shouldRespondToClicks) {
                    atBottomBeforeKBWasOpened =
                        lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == lazyListState.layoutInfo.totalItemsCount - 1
                    openSoftKeyboard(
                        coroutineScope = coroutineScope,
                        mainFocusRequester = mainFocusRequester,
                        auxFocusRequester = auxFocusRequester,
                    )
                }
            },
    ) {
        items(
            items = textBlocks.value,
            key = { textBlock -> textBlock.uid },
        ) { textBlock ->
            Text(
                text = textBlock.annotatedString ?: AnnotatedString(""),
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize.sp,
                overflow = TextOverflow.Clip,
            )
        }
    }
    LaunchedEffect(key1 = isKeyboardOpen) {
        if (isKeyboardOpen && atBottomBeforeKBWasOpened) {
            val lastIndex = textBlocks.value.lastIndex
            if (lastIndex >= 0) {
                lazyListState.scrollToItem(lastIndex)
            }
        }
        onKeyboardStateChange()
    }
    if (shouldScrollToBottom.value) {
        LaunchedEffect(textBlocks.value) {
            val lastIndex = textBlocks.value.lastIndex
            if (lastIndex >= 0) {
                lazyListState.scrollToItem(lastIndex)
            }
            onScrolledToBottom()
        }
    }
}