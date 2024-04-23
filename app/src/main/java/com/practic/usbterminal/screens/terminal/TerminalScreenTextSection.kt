package com.practic.usbterminal.screens.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.practic.usbterminal.main.MainViewModel
import com.practic.usbterminal.main.ScreenLine
import com.practic.usbterminal.main.ScreenTextModel
import com.practic.usbterminal.ui.util.isKeyboardOpenAsState


@Composable
fun ColumnScope.TerminalScreenTextSection(
    screenState: State<ScreenTextModel.ScreenState>,
    shouldMeasureScreenDimensions: MainViewModel.ScreenMeasurementCommand,
    requestUID: Int,
    onScreenDimensionsMeasured: (MainViewModel.ScreenDimensions, MainViewModel.ScreenMeasurementCommand) -> Unit,
    shouldReportIfAtBottom: Boolean,
    onReportIfAtBottom: (Boolean) -> Unit,
    onScrolledToBottom: (Int) -> Unit,
    fontSize: Int,
    textColor: Color,
    shouldRespondToClicks: Boolean,
    mainFocusRequester: FocusRequester,
    auxFocusRequester: FocusRequester,
    onKeyboardStateChange: () -> Unit,
) {
    val lines = screenState.value.lines
    val shouldScrollToBottom = screenState.value.shouldScrollToBottom
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = true)
    ) {
        if (shouldMeasureScreenDimensions != MainViewModel.ScreenMeasurementCommand.NOOP) {
            MeasureScreenDimensions(
                onScreenDimensionsMeasured,
                fontSize,
                shouldMeasureScreenDimensions,
                requestUID
            )
        }
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
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
                items = lines,
                key = { line -> line.uid },
            ) { line ->
                TerminalScreenLine(line, fontSize, textColor)
            }
        }
    }
    LaunchedEffect(key1 = isKeyboardOpen) {
        if (isKeyboardOpen && atBottomBeforeKBWasOpened) {
            lazyListState.scrollToItem(lines.lastIndex)
        }
        onKeyboardStateChange()
    }
    LaunchedEffect(key1 = shouldScrollToBottom) {
        if (shouldScrollToBottom != 0) {
            lazyListState.scrollToItem(lazyListState.layoutInfo.totalItemsCount - 1)
            onScrolledToBottom(shouldScrollToBottom)
        }
    }
}

@Composable
fun TerminalScreenLine(
    line: ScreenLine,
    fontSize: Int,
    textColor: Color,
    modifier: Modifier = Modifier,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    Text(
        text = line.getAnnotatedString(),
        color = textColor,
        fontFamily = FontFamily.Monospace,
        fontSize = fontSize.sp,
        overflow = TextOverflow.Clip,
        maxLines = 1,
        onTextLayout = onTextLayout,
        modifier = modifier,
    )
}
