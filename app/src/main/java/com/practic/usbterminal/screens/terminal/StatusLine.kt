package com.practic.usbterminal.screens.terminal

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.practic.usbterminal.R
import com.practic.usbterminal.main.MainViewModel
import com.practic.usbterminal.main.ScreenTextModel
import com.practic.usbterminal.settings.model.SettingsRepository
import com.practic.usbterminal.ui.theme.LedColorWhenConnected
import com.practic.usbterminal.ui.theme.LedColorWhenDisconnected
import com.practic.usbterminal.ui.theme.LedColorWhenError
import com.practic.usbterminal.ui.theme.UsbTerminalTheme
import com.practic.usbterminal.usbserial.UsbSerialPort

@Composable
fun StatusLine(
    usbConnectionState: MainViewModel.UsbConnectionState,
    screenDimensions: MainViewModel.ScreenDimensions,
    cursorPosition: ScreenTextModel.DisplayedCursorPosition,
    displayType: SettingsRepository.DisplayType,
) {
    val scroll = rememberScrollState()

    val connectionStatusLEDColor = when (usbConnectionState.statusCode) {
        UsbSerialPort.ConnectStatusCode.CONNECTED -> LedColorWhenConnected
        UsbSerialPort.ConnectStatusCode.IDLE -> LedColorWhenDisconnected
        else -> LedColorWhenError
    }
    val connectionStatusMsg = usbConnectionState.msg.ifBlank {
        when (usbConnectionState.statusCode) {
            UsbSerialPort.ConnectStatusCode.CONNECTED -> stringResource(R.string.connected)
            UsbSerialPort.ConnectStatusCode.IDLE -> stringResource(R.string.disconnected)
            else -> usbConnectionState.statusCode.name
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = UsbTerminalTheme.extendedColors.statusLineBackgroundColor)
            .padding(1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_baseline_wb_sunny_24),
            contentDescription = "",
            colorFilter = ColorFilter.tint(connectionStatusLEDColor),
            modifier = Modifier
                .padding(start = 2.dp),
        )
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp, end = 10.dp)
                .horizontalScroll(scroll),
            text = connectionStatusMsg,
            color = UsbTerminalTheme.extendedColors.statusLineTextColor,
        )
        if (displayType == SettingsRepository.DisplayType.TEXT) {
            Text(
                modifier = Modifier
                    .padding(end = 6.dp),
                text = "${cursorPosition.line}:${cursorPosition.column}",
                fontSize = 14.sp,
                color = UsbTerminalTheme.extendedColors.statusLineTextColor,
            )
            Text(
                modifier = Modifier
                    .padding(end = 6.dp),
                text = "${screenDimensions.height}x${screenDimensions.width}",
                fontSize = 14.sp,
                color = UsbTerminalTheme.extendedColors.statusLineTextColor,
            )
        }
        Text(
            modifier = Modifier
                .padding(end = 4.dp),
            text = when (displayType) {
                SettingsRepository.DisplayType.TEXT -> stringResource(R.string.display_type_indicator_txt)
                SettingsRepository.DisplayType.HEX -> stringResource(R.string.display_type_indicator_hex)
            },
            fontSize = 14.sp,
            color = UsbTerminalTheme.extendedColors.statusLineTextColor,
        )
    }
}