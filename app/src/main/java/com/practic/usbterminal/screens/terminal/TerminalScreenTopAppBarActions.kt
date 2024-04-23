package com.practic.usbterminal.screens.terminal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.practic.usbterminal.R
import com.practic.usbterminal.main.MainViewModel
import com.practic.usbterminal.usbserial.UsbSerialPort
import kotlinx.coroutines.launch

@Composable
fun TerminalScreenTopAppBarActions(mainViewModel: MainViewModel) {

    val usbConnectionState by mainViewModel.usbConnectionState
    val showOverflowMenu = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()



    IconButton(
        onClick = { coroutineScope.launch { mainViewModel.clearScreen() } },
    ) {
        Icon(
            modifier = Modifier.padding(start = 0.dp, end = 0.dp),
            painter = painterResource(id = R.drawable.ic_baseline_delete_24),
            contentDescription = stringResource(R.string.clear_screen)
        )
    }
    IconButton(
        onClick = { mainViewModel.onToggleHexTxtButtonClick() },
    ) {
        Icon(
            modifier = Modifier.padding(start = 0.dp, end = 0.dp),
            painter = painterResource(id = R.drawable.ic_baseline_txt_hex_24),
            contentDescription = stringResource(R.string.toggle_hex_text)
        )
    }
    IconButton(
        onClick = { mainViewModel.onToggleShowCtrlButtonsRowButtonClick() },
    ) {
        Icon(
            modifier = Modifier.padding(start = 0.dp, end = 0.dp),
            painter = painterResource(id = R.drawable.ic_baseline_open_with_24),
            contentDescription = stringResource(R.string.arrows_keypad)
        )
    }

    Box {
        IconButton(
            onClick = { showOverflowMenu.value = !showOverflowMenu.value }
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.more)
            )
        }
        DropdownMenu(
            expanded = showOverflowMenu.value,
            onDismissRequest = { showOverflowMenu.value = false },
            offset = DpOffset((0).dp, (-60).dp),
        ) {

            DropdownMenuItem(
                onClick = { mainViewModel.setDTR(true); showOverflowMenu.value = false },
                enabled = usbConnectionState.connectedUsbPort?.getDTR() == false,
            ) {
                Text(
                    text = stringResource(R.string.set_dtr),
                    modifier = Modifier
                        .clickable { mainViewModel.setDTR(true); showOverflowMenu.value = false }
                )
            }
            DropdownMenuItem(
                onClick = { mainViewModel.setDTR(false); showOverflowMenu.value = false },
                enabled = usbConnectionState.connectedUsbPort?.getDTR() == true,
            ) {
                Text(
                    text = stringResource(R.string.clear_dtr),
                    modifier = Modifier
                        .clickable { mainViewModel.setDTR(false); showOverflowMenu.value = false }
                )
            }

            DropdownMenuItem(
                onClick = { mainViewModel.setRTS(true); showOverflowMenu.value = false },
                enabled = usbConnectionState.connectedUsbPort?.getRTS() == false,
            ) {
                Text(
                    text = stringResource(R.string.set_rts),
                    modifier = Modifier
                        .clickable { mainViewModel.setRTS(true); showOverflowMenu.value = false }
                )
            }
            DropdownMenuItem(
                onClick = { mainViewModel.setRTS(false); showOverflowMenu.value = false },
                enabled = usbConnectionState.connectedUsbPort?.getRTS() == true,
            ) {
                Text(
                    text = stringResource(R.string.clear_rts),
                    modifier = Modifier
                        .clickable { mainViewModel.setRTS(false); showOverflowMenu.value = false }
                )
            }

            DropdownMenuItem(
                onClick = { mainViewModel.esp32BootloaderReset(); showOverflowMenu.value = false },
                enabled = usbConnectionState.statusCode == UsbSerialPort.ConnectStatusCode.CONNECTED
            ) {
                Text(
                    text = stringResource(R.string.esp32_boot_reset),
                    modifier = Modifier
                        .clickable {
                            mainViewModel.esp32BootloaderReset(); showOverflowMenu.value = false
                        }
                )
            }

            DropdownMenuItem(
                onClick = { mainViewModel.arduinoReset(); showOverflowMenu.value = false },
                enabled = usbConnectionState.statusCode == UsbSerialPort.ConnectStatusCode.CONNECTED
            ) {
                Text(
                    text = stringResource(R.string.arduino_reset),
                    modifier = Modifier
                        .clickable {
                            mainViewModel.arduinoReset(); showOverflowMenu.value = false
                        }
                )
            }
            DropdownMenuDebugSection(showOverflowMenu, mainViewModel)
        }
    }
}