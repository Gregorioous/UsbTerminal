package com.practic.usbterminal.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.practic.usbterminal.R
import com.practic.usbterminal.screens.devicelist.DeviceListScreenAttributes
import com.practic.usbterminal.screens.logfiles.LogFilesListScreenAttributes
import com.practic.usbterminal.settings.ui.SettingsScreenAttributes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class UsbTerminalNavDrawerItemDefinition(
    @DrawableRes var icon: Int,
    @StringRes var text: Int,
    var route: String,
    val isTopInBackStack: Boolean = false
)

object NavDrawerItems {
    val items = listOf(
        UsbTerminalNavDrawerItemDefinition(
            icon = R.drawable.ic_baseline_usb_24,
            text = R.string.device_list_screen_title,
            route = DeviceListScreenAttributes.route,
            isTopInBackStack = DeviceListScreenAttributes.isTopInBackStack
        ),
        UsbTerminalNavDrawerItemDefinition(
            icon = R.drawable.ic_baseline_list_24,
            text = R.string.log_files_screen_top_appbar_normal_title,
            route = LogFilesListScreenAttributes.route,
            isTopInBackStack = LogFilesListScreenAttributes.isTopInBackStack
        ),
        UsbTerminalNavDrawerItemDefinition(
            icon = R.drawable.ic_baseline_settings_24,
            text = R.string.settings_screen_title,
            route = SettingsScreenAttributes.route,
            isTopInBackStack = SettingsScreenAttributes.isTopInBackStack
        )
    )
}

@Composable
fun UsbTerminalNavDrawer(
    coroutineScope: CoroutineScope,
    scaffoldState: ScaffoldState,
    navHostController: NavHostController
) {
    Column {
        NavDrawerHeader()

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .background(MaterialTheme.colors.primary)
        ) {
            Spacer(modifier = Modifier.height(5.dp))
            val navBackStackEntry by navHostController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            NavDrawerItems.items.forEach { item ->
                UsbTerminalNavDrawerItem(
                    navDrawerItemDefinition = item,
                    selected = currentRoute == item.route,
                    onItemClick = {
                        navHostController.navigate(item.route) {
                            navHostController.graph.startDestinationRoute?.let { route ->
                                popUpTo(route) {
                                    saveState = true
                                    if (item.isTopInBackStack) inclusive = true
                                }
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                        coroutineScope.launch {
                            scaffoldState.drawerState.close()
                        }
                    })
            }
        }
    }
}

/*@Preview(showBackground = false)
@Composable
fun UsbTerminalNavDrawerPreview() {
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
    val navHostController = rememberNavController()
    UsbTerminalNavDrawer(coroutineScope = scope, scaffoldState = scaffoldState, navHostController = navHostController)
}*/

@Composable
fun NavDrawerHeader() {
    Row(
        modifier = Modifier
            .height(140.dp)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colors.primary,
                        MaterialTheme.colors.primary.copy(alpha = 0.2f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1000f)
                )
            )
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_round),
            contentDescription = "",
            modifier = Modifier
                .height(120.dp)
                .width(120.dp)
                .padding(start = 20.dp),
        )
        Column(
            modifier = Modifier
                .padding()
                .align(Alignment.CenterVertically)
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.h6,
                textAlign = TextAlign.Start,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(start = 14.dp, top = 14.dp)
                    .align(Alignment.Start)
            )
        }
    }
}

@Composable
fun UsbTerminalNavDrawerItem(
    navDrawerItemDefinition: UsbTerminalNavDrawerItemDefinition,
    selected: Boolean,
    onItemClick: (UsbTerminalNavDrawerItemDefinition) -> Unit
) {
    val borderColor =
        if (selected) MaterialTheme.colors.secondary else MaterialTheme.colors.primary.copy(alpha = 0f)
    val fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
    Surface(color = MaterialTheme.colors.primary.copy(alpha = 0f)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { onItemClick(navDrawerItemDefinition) })
                .height(45.dp)
                .padding(horizontal = 5.dp)
                .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(10.dp))
        ) {
            Spacer(modifier = Modifier.width(5.dp))
            Icon(
                painterResource(id = navDrawerItemDefinition.icon),
                contentDescription = stringResource(id = navDrawerItemDefinition.text),
                tint = MaterialTheme.colors.onPrimary
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = stringResource(id = navDrawerItemDefinition.text),
                fontSize = 18.sp,
                fontWeight = fontWeight,
                color = MaterialTheme.colors.onPrimary
            )
        }
    }
}

/*
@Preview(showBackground = false)
@Composable
fun UsbTerminalNavDrawerItemPreview() {
    UsbTerminalNavDrawerItem(
        navDrawerItemDefinition = UsbTerminalNavDrawerItemDefinition(
            R.drawable.ic_baseline_settings_24,
            R.string.settings_screen_title,
            SettingsScreenAttributes.route),
        selected = false,
        onItemClick = {}
    )
}*/
