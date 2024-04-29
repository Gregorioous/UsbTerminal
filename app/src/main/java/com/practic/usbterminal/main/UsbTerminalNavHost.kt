package com.practic.usbterminal.main

import androidx.activity.OnBackPressedDispatcher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.practic.usbterminal.screens.devicelist.DeviceListScreen
import com.practic.usbterminal.screens.devicelist.DeviceListScreenAttributes
import com.practic.usbterminal.screens.logfiles.LogFilesListScreen
import com.practic.usbterminal.screens.logfiles.LogFilesListScreenAttributes
import com.practic.usbterminal.screens.terminal.TerminalScreen
import com.practic.usbterminal.screens.terminal.TerminalScreenAttributes
import com.practic.usbterminal.settings.ui.SettingsScreen
import com.practic.usbterminal.settings.ui.SettingsScreenAttributes
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Composable
fun UsbTerminalNavHost(
    navController: NavHostController,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onBackPressedDispatcher: OnBackPressedDispatcher
) {

    LaunchedEffect("navigation") {
        UsbTerminalNavigator.navTargetsSharedFlow.onEach { navTarget ->
            when (navTarget) {
                UsbTerminalNavigator.NavTargetBack -> {
                    navController.popBackStackOrBackPressAction(onBackPressedDispatcher)
                }

                else -> {
                    navController.navigate(navTarget.route) {
                        if (navTarget.isTopInBackStack) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = true
                            }
                        }
                    }
                }
            }
        }.launchIn(this)
    }
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    }

    NavHost(
        navController = navController,
        startDestination = TerminalScreenAttributes.route,
        modifier = modifier
    ) {
        composable(TerminalScreenAttributes.route) {
            CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                TerminalScreen(viewModel)
            }
        }
        composable(DeviceListScreenAttributes.route) {
            CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                DeviceListScreen(viewModel)
            }
        }
        composable(LogFilesListScreenAttributes.route) {
            CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                LogFilesListScreen(viewModel)
            }
        }
        composable(SettingsScreenAttributes.route) {
            CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                SettingsScreen(viewModel)
            }
        }
    }
}

fun NavController.popBackStackOrBackPressAction(onBackPressedDispatcher: OnBackPressedDispatcher) {
    if (previousBackStackEntry == null) {
        onBackPressedDispatcher.onBackPressed()
    } else {
        popBackStack()
    }
}