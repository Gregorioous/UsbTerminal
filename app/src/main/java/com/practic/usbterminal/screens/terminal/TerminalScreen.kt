package com.practic.usbterminal.screens.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.practic.usbterminal.R
import com.practic.usbterminal.main.MainViewModel
import com.practic.usbterminal.main.ScreenTextModel
import com.practic.usbterminal.main.UsbTerminalScreenAttributes
import com.practic.usbterminal.settings.model.SettingsRepository
import com.practic.usbterminal.ui.theme.UsbTerminalTheme
import com.practic.usbterminal.utill.collectAsStateLifecycleAware
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object TerminalScreenAttributes : UsbTerminalScreenAttributes(
    isTopInBackStack = true,
    route = "Terminal",
) {
    override fun getTopAppBarActions(
        mainViewModel: MainViewModel,
        isTopBarInContextualMode: Boolean
    ): @Composable RowScope.() -> Unit = {
        TerminalScreenTopAppBarActions(mainViewModel)
    }
}

@Composable
fun TerminalScreen(
    mainViewModel: MainViewModel
) {
    LaunchedEffect(true) { mainViewModel.setTopBarTitle(R.string.terminal_screen_title) }
    val textToXmit = mainViewModel.textToXmit.collectAsStateLifecycleAware()
    val textToXmitCharByChar = mainViewModel.userInputHandler.textToXmitCharByChar
    val usbConnectionState by mainViewModel.usbConnectionState
    val settingsData by mainViewModel.settingsRepository.settingsStateFlow.collectAsStateLifecycleAware()
    val displayType = settingsData.displayType
    val screenDimensions by mainViewModel.screenDimensions
    val cursorPosition = if (displayType == SettingsRepository.DisplayType.TEXT) {
        mainViewModel.textScreenState.value.displayedCursorPosition
    } else {
        ScreenTextModel.DisplayedCursorPosition(0, 0)
    }
    val fontSize = settingsData.fontSize
    val textColor = Color(settingsData.defaultTextColor).copy(alpha = 1f)
    val mainScreenShouldRespondToClicks =
        settingsData.inputMode == SettingsRepository.InputMode.CHAR_BY_CHAR
    val shouldShowUpgradeFromV1Msg by mainViewModel.shouldShowUpgradeFromV1Msg
    val shouldMeasureScreenDimensions by mainViewModel.shouldMeasureScreenDimensions
    val shouldReportIfAtBottom by mainViewModel.shouldReportIfAtBottom

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val mainFocusRequester = remember { FocusRequester() }
        val auxFocusRequester = remember { FocusRequester() }

        if (displayType == SettingsRepository.DisplayType.HEX) {
            TerminalScreenHexSection(
                textBlocks = mainViewModel.screenHexTextBlocksState,
                shouldScrollToBottom = mainViewModel.screenHexShouldScrollToBottom,
                shouldReportIfAtBottom = shouldReportIfAtBottom,
                onReportIfAtBottom = mainViewModel::onReportIfAtBottom,
                onScrolledToBottom = mainViewModel.onScreenHexScrolledToBottom,
                fontSize = fontSize,
                shouldRespondToClicks = mainScreenShouldRespondToClicks,
                mainFocusRequester = mainFocusRequester,
                auxFocusRequester = auxFocusRequester,
                onKeyboardStateChange = mainViewModel::remeasureScreenDimensions
            )
        } else {
            TerminalScreenTextSection(
                screenState = mainViewModel.textScreenState,
                shouldMeasureScreenDimensions = shouldMeasureScreenDimensions.cmd,
                requestUID = shouldMeasureScreenDimensions.uid,
                onScreenDimensionsMeasured = mainViewModel::onScreenDimensionsMeasured,
                shouldReportIfAtBottom = shouldReportIfAtBottom,
                onReportIfAtBottom = mainViewModel::onReportIfAtBottom,
                onScrolledToBottom = mainViewModel.onScreenTxtScrolledToBottom,
                fontSize = fontSize,
                textColor = textColor,
                shouldRespondToClicks = mainScreenShouldRespondToClicks,
                mainFocusRequester = mainFocusRequester,
                auxFocusRequester = auxFocusRequester,
                onKeyboardStateChange = mainViewModel::remeasureScreenDimensions
            )
        }
        TextToXmitInputField(
            settingsData.inputMode,
            textToXmit,
            textToXmitCharByChar,
            mainViewModel.userInputHandler::onXmitCharByCharKBInput,
            mainViewModel::setTextToXmit,
            mainViewModel::onXmitButtonClick,
            mainFocusRequester,
            auxFocusRequester,
        )
        if (mainViewModel.ctrlButtonsRowVisible.value) {
            CtrlButtonsRow(mainViewModel)
        }
        Divider(color = UsbTerminalTheme.extendedColors.statusLineDividerColor, thickness = 1.dp)
        StatusLine(usbConnectionState, screenDimensions, cursorPosition, displayType)

        if (shouldShowUpgradeFromV1Msg) {
            UpgradeFromV1MsgDialog(
                mainViewModel::onUserAcceptedWelcomeOrUpgradeMsg,
                mainViewModel::onUserDeclinedWelcomeOrUpgradeMsg,
            )
        }
    }
}

fun openSoftKeyboard(
    coroutineScope: CoroutineScope,
    mainFocusRequester: FocusRequester,
    auxFocusRequester: FocusRequester,
) {
    coroutineScope.launch {
        auxFocusRequester.requestFocus()
        delay(50)
        mainFocusRequester.requestFocus()
    }
}