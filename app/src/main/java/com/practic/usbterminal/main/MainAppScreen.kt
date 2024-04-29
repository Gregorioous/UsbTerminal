package com.practic.usbterminal.main

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DrawerValue
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberDrawerState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.practic.usbterminal.utill.collectAsStateLifecycleAware
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun MainAppScreen(viewModel: MainViewModel, onBackPressedDispatcher: OnBackPressedDispatcher) {
    val navHostController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
    val backstackEntry by navHostController.currentBackStackEntryAsState()
    val currentScreenAttributes =
        UsbTerminalScreenAttributes.fromRoute(backstackEntry?.destination?.route)
    val isTopBarInContextualMode by viewModel.isTopBarInContextualMode.collectAsStateLifecycleAware()
    val topBarTitleParams by viewModel.topBarTitle.collectAsStateLifecycleAware()
    val topBarTitle = stringResource(topBarTitleParams.fmtResId, *topBarTitleParams.params)

    SystemBackButtonHandler(
        enabled = !currentScreenAttributes.isTopInBackStack
                || scaffoldState.drawerState.isOpen
                || isTopBarInContextualMode,
        coroutineScope = coroutineScope,
        scaffoldState = scaffoldState,
        navHostController = navHostController,
        mainViewModel = viewModel,
    )

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            UsbTerminalTopAppBar(
                navigationIcon = getTopAppBarNavigationIcon(
                    currentScreenAttributes,
                    isTopBarInContextualMode
                ),
                onNavigationIconClick = {
                    onTopAppBarNavigationIconClick(
                        currentScreenAttributes = currentScreenAttributes,
                        navHostController = navHostController,
                        scaffoldState = scaffoldState,
                        scope = coroutineScope,
                        isTopBarInContextualMode = isTopBarInContextualMode,
                        onClearButtonClicked = viewModel::onTopBarClearButtonClicked
                    )
                },
                isInContextualMode = isTopBarInContextualMode,
                title = topBarTitle,
                actions = currentScreenAttributes.getTopAppBarActions(
                    viewModel,
                    isTopBarInContextualMode
                )
            )
        },
        floatingActionButton = currentScreenAttributes.getFab(viewModel),
        drawerContent = {
            UsbTerminalNavDrawer(
                coroutineScope = coroutineScope,
                scaffoldState = scaffoldState,
                navHostController = navHostController
            )
        },
    ) { contentPadding ->
        UsbTerminalNavHost(
            navController = navHostController,
            viewModel = viewModel,
            modifier = Modifier.padding(contentPadding),
            onBackPressedDispatcher = onBackPressedDispatcher
        )
    }
}

@Composable
fun SystemBackButtonHandler(
    enabled: Boolean,
    coroutineScope: CoroutineScope,
    scaffoldState: ScaffoldState,
    navHostController: NavHostController,
    mainViewModel: MainViewModel,
) {
    navHostController.enableOnBackPressed(false)
    BackHandler(
        enabled = enabled,
        onBack = {
            if (scaffoldState.drawerState.isOpen) {
                coroutineScope.launch { scaffoldState.drawerState.close() }
            } else {
                mainViewModel.setIsTopBarInContextualMode(false)
                UsbTerminalNavigator.navigateBack()
            }
        }
    )
}

fun onTopAppBarNavigationIconClick(
    currentScreenAttributes: UsbTerminalScreenAttributes,
    navHostController: NavHostController,
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    isTopBarInContextualMode: Boolean,
    onClearButtonClicked: () -> Unit
) {
    when {
        isTopBarInContextualMode -> onClearButtonClicked()
        currentScreenAttributes.isTopInBackStack -> {
            scope.launch {
                scaffoldState.drawerState.open()
            }
        }
        else -> navHostController.popBackStack()
    }
}

fun getTopAppBarNavigationIcon(
    currentScreenAttributes: UsbTerminalScreenAttributes,
    isTopBarInContextualMode: Boolean,
): UTTopAppBarNavigationIcon {
    return when {
        isTopBarInContextualMode -> UTTopAppBarNavigationIcon.Clear
        currentScreenAttributes.isTopInBackStack -> UTTopAppBarNavigationIcon.Menu
        else -> UTTopAppBarNavigationIcon.Back
    }
}