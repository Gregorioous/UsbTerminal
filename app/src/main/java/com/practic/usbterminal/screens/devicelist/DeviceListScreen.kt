package com.practic.usbterminal.screens.devicelist

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.practic.usbterminal.R
import com.practic.usbterminal.main.MainViewModel
import com.practic.usbterminal.main.UsbTerminalScreenAttributes
import com.practic.usbterminal.usbserial.UsbSerialPort


object DeviceListScreenAttributes : UsbTerminalScreenAttributes(
    isTopInBackStack = false,
    route = "DeviceList",
) {
    override fun getTopAppBarActions(
        mainViewModel: MainViewModel,
        isTopBarInContextualMode: Boolean
    ): @Composable RowScope.() -> Unit =
        { DeviceListTopAppBarActions(mainViewModel) }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun DeviceListTopAppBarActions(mainViewModel: MainViewModel) {
}

@Composable
fun DeviceListScreen(
    mainViewModel: MainViewModel
) {
    LaunchedEffect(true) { mainViewModel.setTopBarTitle(R.string.device_list_screen_title) }

    val viewModel: DeviceListViewModel = viewModel(
        factory = DeviceListViewModel.Factory(
            application = LocalContext.current.applicationContext as Application,
            mainViewModel = mainViewModel
        )
    )
    val items by mainViewModel.portListState
    val usbConnectionState by mainViewModel.usbConnectionState
    val connectedUsbPort = usbConnectionState.connectedUsbPort

    if (items.isEmpty()) {
        EmptyDeviceListMessage()
    } else {
        DeviceList(
            items,
            connectedUsbPort,
            viewModel::onConnectToPortClick,
            viewModel::onSetDeviceTypeAndConnectToPortClick,
            viewModel::onDisconnectFromPortClick,
        )
    }
    if (viewModel.shouldShowSelectDeviceTypeAndConnectDialog.value) {
        SelectDeviceTypeAndConnectDialog(
            choices = viewModel.deviceTypeStrings,
            selectedIndex = viewModel.deviceTypeToIndex(viewModel.selectedDeviceType.value),
            onSelection = viewModel::onSelectDeviceType,
            onConnect = viewModel::onConnectToPortWithSelectedDeviceTypeClick,
            onCancel = viewModel::onCancelSetDeviceTypeAndConnectToPortClick
        )
    }
}

@Composable
private fun EmptyDeviceListMessage() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.no_usb_devices),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h6,
        )
    }
}

@Composable
private fun DeviceList(
    usbSerialPorts: List<UsbSerialPort>,
    connectedUsbPort: UsbSerialPort?,
    onConnectToPortClick: (itemId: Int) -> Unit,
    onSetDeviceTypeAndConnectToPortClick: (itemId: Int) -> Unit,
    onDisconnectFromPortClick: (itemId: Int) -> Unit,
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
    ) {
        items(count = usbSerialPorts.size) { itemIndex ->
            DeviceItem(
                usbSerialPort = usbSerialPorts[itemIndex],
                itemId = itemIndex,
                isConnected = usbSerialPorts[itemIndex].isEqual(connectedUsbPort),
                onConnectToPortClick = onConnectToPortClick,
                onSetDeviceTypeAndConnectToPortClick = onSetDeviceTypeAndConnectToPortClick,
                onDisconnectFromPortClick = onDisconnectFromPortClick,
            )
        }
    }
}