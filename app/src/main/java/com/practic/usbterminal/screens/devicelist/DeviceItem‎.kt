package com.practic.usbterminal.screens.devicelist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.practic.usbterminal.R
import com.practic.usbterminal.ui.theme.UsbTerminalTheme
import com.practic.usbterminal.usbserial.UsbSerialDevice
import com.practic.usbterminal.usbserial.UsbSerialPort

@Composable
fun DeviceItem(
    usbSerialPort: UsbSerialPort,
    itemId: Int,
    isConnected: Boolean,
    onConnectToPortClick: (itemId: Int) -> Unit,
    onSetDeviceTypeAndConnectToPortClick: (itemId: Int) -> Unit,
    onDisconnectFromPortClick: (itemId: Int) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(4.dp),
        elevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Row {
                Text(
                    text = stringResource(
                        id =
                        R.string.vid,
                        usbSerialPort.usbSerialDevice.vendorId,
                        usbSerialPort.usbSerialDevice.vendorId
                    ),
                    modifier = Modifier
                        .width(120.dp)
                        .padding(top = 4.dp),
                )
                Text(
                    text = stringResource(
                        id =
                        R.string.pid,
                        usbSerialPort.usbSerialDevice.productId,
                        usbSerialPort.usbSerialDevice.productId
                    ),
                    modifier = Modifier
                        .padding(top = 4.dp)
                )
            }
            Text(
                text = stringResource(id = R.string.portNumber, usbSerialPort.portNumber),
                modifier = Modifier
                    .padding(top = 4.dp)
            )
            Text(
                text = if (isConnected) stringResource(
                    R.string.statusConnected
                )
                else
                    stringResource(R.string.statusDisconnected),
                color = if (isConnected)
                    UsbTerminalTheme.extendedColors.textColorWhenConnected
                else
                    UsbTerminalTheme.extendedColors.textColorWhenDisconnected,
                fontWeight = if (isConnected)
                    FontWeight.Bold
                else
                    FontWeight.Normal,
                modifier = Modifier
                    .padding(top = 4.dp)
            )
            Text(
                text = stringResource(
                    R.string.deviceType,
                    usbSerialPort.usbSerialDevice.deviceTypeStr
                ),
                modifier = Modifier
                    .padding(top = 4.dp)
            )
            if (isConnected) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = { onDisconnectFromPortClick(itemId) },
                    ) {
                        Text(
                            text = stringResource(R.string.disconnect),
                            color = MaterialTheme.colors.onBackground,
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (usbSerialPort
                            .usbSerialDevice
                            .deviceType != UsbSerialDevice.DeviceType
                            .UNRECOGNIZED && !isConnected
                    ) {
                        OutlinedButton(
                            onClick = { onConnectToPortClick(itemId) },
                        ) {
                            Text(
                                text = stringResource(R.string.connect),
                                color = MaterialTheme.colors.onBackground,
                            )
                        }
                        Spacer(Modifier.size(16.dp))
                    }
                    OutlinedButton(
                        onClick = { onSetDeviceTypeAndConnectToPortClick(itemId) },
                    ) {
                        Text(
                            text = stringResource(R.string.set_type_and_connect),
                            color = MaterialTheme.colors.onBackground,
                        )
                    }
                }
            }
        }
    }
}