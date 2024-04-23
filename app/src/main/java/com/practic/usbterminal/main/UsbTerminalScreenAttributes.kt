package com.practic.usbterminal.main

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import com.practic.usbterminal.screens.devicelist.DeviceListScreenAttributes
import com.practic.usbterminal.screens.logfiles.LogFilesListScreenAttributes
import com.practic.usbterminal.screens.terminal.TerminalScreenAttributes
import com.practic.usbterminal.settings.ui.SettingsScreenAttributes


abstract class UsbTerminalScreenAttributes(
    override val isTopInBackStack: Boolean,
    override val route: String
) : UsbTerminalNavigator.NavTarget {
    open fun getTopAppBarActions(
        mainViewModel: MainViewModel,
        isTopBarInContextualMode: Boolean
    ): @Composable RowScope.() -> Unit = {}

    open fun getFab(viewModel: MainViewModel): @Composable () -> Unit = {}

    companion object {
        fun fromRoute(route: String?): UsbTerminalScreenAttributes =
            when (route?.substringBefore("/")) {
                TerminalScreenAttributes.route -> TerminalScreenAttributes
                DeviceListScreenAttributes.route -> DeviceListScreenAttributes
                LogFilesListScreenAttributes.route -> LogFilesListScreenAttributes
                SettingsScreenAttributes.route -> SettingsScreenAttributes
                else -> throw IllegalArgumentException("Route $route is not recognized.")
            }
    }
}