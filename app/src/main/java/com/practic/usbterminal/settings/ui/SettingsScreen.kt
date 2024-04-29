package com.practic.usbterminal.settings.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.google.accompanist.pager.*
import com.practic.usbterminal.R
import com.practic.usbterminal.main.MainViewModel
import com.practic.usbterminal.main.UsbTerminalScreenAttributes
import com.practic.usbterminal.settings.model.SettingsData
import com.practic.usbterminal.utill.collectAsStateLifecycleAware
import kotlinx.coroutines.launch

object SettingsScreenAttributes : UsbTerminalScreenAttributes(
    isTopInBackStack = false,
    route = "Settings",
) {
    override fun getTopAppBarActions(
        mainViewModel: MainViewModel,
        isTopBarInContextualMode: Boolean
    ): @Composable RowScope.() -> Unit =
        { SettingsTopAppBarActions(mainViewModel) }
}

sealed class TabItem(
    @StringRes val title: Int,
    val screen: @Composable () -> Unit,
    val mainViewModel: MainViewModel,
    val settingsData: SettingsData,
) {
    class General(mainViewModel: MainViewModel, settingsData: SettingsData) :
        TabItem(
            R.string.general,
            { GeneralSettingsPage(mainViewModel, settingsData) },
            mainViewModel,
            settingsData
        )

    class Terminal(mainViewModel: MainViewModel, settingsData: SettingsData) :
        TabItem(
            R.string.terminal,
            { TerminalSettingsPage(mainViewModel, settingsData) },
            mainViewModel,
            settingsData
        )

    class Serial(mainViewModel: MainViewModel, settingsData: SettingsData) :
        TabItem(
            R.string.serial,
            { SerialSettingsPage(mainViewModel, settingsData) },
            mainViewModel,
            settingsData
        )
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun SettingsScreen(mainViewModel: MainViewModel) {
    LaunchedEffect(true) { mainViewModel.setTopBarTitle(R.string.settings_screen_title) }
    val settingsData by mainViewModel.settingsRepository.settingsStateFlow.collectAsStateLifecycleAware()

    val tabs = listOf(
        TabItem.General(mainViewModel, settingsData),
        TabItem.Terminal(mainViewModel, settingsData),
        TabItem.Serial(mainViewModel, settingsData),
    )

    val pagerState = rememberPagerState()

    Column {
        Tabs(tabs = tabs, pagerState = pagerState)
        HorizontalPager(state = pagerState, count = tabs.size) { pageIndex ->
            tabs[pageIndex].screen()
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalPagerApi::class)
@Composable
fun Tabs(tabs: List<TabItem>, pagerState: PagerState) {
    val scope = rememberCoroutineScope()
    TabRow(
        selectedTabIndex = pagerState.currentPage,
        backgroundColor = MaterialTheme.colors.primaryVariant,
        contentColor = MaterialTheme.colors.onPrimary,
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                Modifier.pagerTabIndicatorOffset(pagerState, tabPositions)
            )
        }) {
        tabs.forEachIndexed { index, tab ->
            Tab(
                text = { Text(stringResource(tab.title)) },
                selected = pagerState.currentPage == index,
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
            )
        }
    }
}


@Composable
fun SettingsTopAppBarActions(@Suppress("UNUSED_PARAMETER") viewModel: MainViewModel) {
}
