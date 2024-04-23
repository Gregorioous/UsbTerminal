package com.practic.usbterminal.screens.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import com.practic.usbterminal.main.MainViewModel
import com.practic.usbterminal.main.ScreenLine


@Composable
fun MeasureScreenDimensions(
    onMeasuredScreenDimensions: (MainViewModel.ScreenDimensions, MainViewModel.ScreenMeasurementCommand) -> Unit,
    fontSize: Int,
    shouldMeasureScreenDimensions: MainViewModel.ScreenMeasurementCommand,
    requestUID: Int,
) {

    var screenDimensions = MainViewModel.ScreenDimensions(0, 0)
    val longLine =
        ScreenLine(text = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx${requestUID}")
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        TerminalScreenLine(
            line = longLine,
            fontSize = fontSize,
            textColor = Color.Black,
            onTextLayout = { textLayoutResult ->
                val lineEndIndex = textLayoutResult.getLineEnd(lineIndex = 0, visibleEnd = true)
                screenDimensions = screenDimensions.copy(width = lineEndIndex)
            },
            modifier = Modifier
                .fillMaxWidth()
                .alpha(0f)
        )


        val line = ScreenLine(text = if (requestUID == 999999999) " " else "")
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
        ) {
            items(100) {
                TerminalScreenLine(
                    line = line,
                    fontSize = fontSize,
                    textColor = Color.Black,
                    modifier = Modifier.alpha(0f),
                )
            }
        }
        LaunchedEffect(key1 = requestUID) {
            screenDimensions =
                screenDimensions.copy(height = listState.layoutInfo.visibleItemsInfo.size - 1)
            onMeasuredScreenDimensions(screenDimensions, shouldMeasureScreenDimensions)
        }
    }
}