package com.practic.usbterminal.screens.help

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.practic.usbterminal.R
import com.practic.usbterminal.main.MainViewModel
import com.practic.usbterminal.main.UTTopAppBarNavigationIcon
import com.practic.usbterminal.main.UsbTerminalNavigator
import com.practic.usbterminal.main.UsbTerminalScreenAttributes
import com.practic.usbterminal.main.UsbTerminalTopAppBar
import com.practic.usbterminal.ui.util.ComposeWebView


object HelpScreenAttributes : UsbTerminalScreenAttributes(
    isTopInBackStack = false,
    route = "Help",
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HelpScreen(
    mainViewModel: MainViewModel = viewModel()
) {
    LaunchedEffect(true) { mainViewModel.setTopBarTitle(R.string.help_screen_title) }

    val mainPageUrl = "https://appassets.androidplatform.net/assets/help/help.html"

    Dialog(
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = true,
        ),
        onDismissRequest = { UsbTerminalNavigator.navigateBack() },
    ) {
        Column(
            modifier = Modifier
                .background(Color.Transparent)
                .padding(0.dp)
                .width(LocalConfiguration.current.screenWidthDp.dp)
        ) {
            UsbTerminalTopAppBar(
                navigationIcon = UTTopAppBarNavigationIcon.Back,
                onNavigationIconClick = { UsbTerminalNavigator.navigateBack() },
                title = "Help",
                isInContextualMode = false
            ) {

            }
            ComposeWebView(
                url = run {
                    val lastUrl =
                        mainViewModel.settingsRepository.settingsStateFlow.value.lastVisitedHelpUrl
                    lastUrl ?: mainPageUrl
                },
                onPageLoaded = { url ->
                    if (url != null) {
                        mainViewModel.settingsRepository.setLastVisitedHelpUrl(url)
                    }
                }
            )
        }
    }
}