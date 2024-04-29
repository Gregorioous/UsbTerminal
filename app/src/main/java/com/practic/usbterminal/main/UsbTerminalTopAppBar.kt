package com.practic.usbterminal.main

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.practic.usbterminal.R
import com.practic.usbterminal.ui.theme.UsbTerminalTheme


enum class UTTopAppBarNavigationIcon {
    Menu, Back, Clear
}

@Composable
fun UsbTerminalTopAppBar(
    navigationIcon: UTTopAppBarNavigationIcon,
    onNavigationIconClick: () -> Unit,
    title: String,
    isInContextualMode: Boolean,
    actions: @Composable RowScope.() -> Unit
) {
    val onSurfaceColor = if (isInContextualMode) {
        UsbTerminalTheme.extendedColors.contextualAppBarOnBackground
    } else {
        MaterialTheme.colors.onPrimary
    }
    TopAppBar(
        title = {
            Text(
                text = title,
                fontSize = 18.sp,
                color = onSurfaceColor,
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigationIconClick) {
                when (navigationIcon) {
                    UTTopAppBarNavigationIcon.Menu -> Icon(
                        Icons.Filled.Menu,
                        stringResource(id = R.string.menu),
                        tint = onSurfaceColor
                    )

                    UTTopAppBarNavigationIcon.Back -> Icon(
                        Icons.Filled.ArrowBack,
                        stringResource(id = R.string.back),
                        tint = onSurfaceColor
                    )

                    UTTopAppBarNavigationIcon.Clear -> Icon(
                        Icons.Filled.Clear,
                        stringResource(id = R.string.clear),
                        tint = onSurfaceColor
                    )
                }
            }
        },
        actions = actions,
        elevation = 4.dp,
        backgroundColor = when (isInContextualMode) {
            true -> UsbTerminalTheme.extendedColors.contextualAppBarBackground
            false -> MaterialTheme.colors.primary
        },
    )
}