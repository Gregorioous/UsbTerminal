package com.practic.usbterminal.screens.devicelist

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.practic.usbterminal.main.MainViewModel
import com.practic.usbterminal.usbserial.UsbSerialDevice
import timber.log.Timber

class DeviceListViewModel(application: Application, private val mainViewModel: MainViewModel) :
    AndroidViewModel(application) {

    private var selectedPortIndex = -1

    private val _selectedDeviceType = mutableStateOf(UsbSerialDevice.DeviceType.UNRECOGNIZED)
    var selectedDeviceType: State<UsbSerialDevice.DeviceType> = _selectedDeviceType

    private val _shouldShowSelectDeviceTypeAndConnectDialog = mutableStateOf(false)
    val shouldShowSelectDeviceTypeAndConnectDialog: State<Boolean> =
        _shouldShowSelectDeviceTypeAndConnectDialog
    val deviceTypeStrings = listOf("CDC/ACM", "FTDI", "CH34X", "CP21XX", "Prolific")
    private val deviceTypes = listOf(
        UsbSerialDevice.DeviceType.CDC_ACM,
        UsbSerialDevice.DeviceType.FTDI,
        UsbSerialDevice.DeviceType.CH34X,
        UsbSerialDevice.DeviceType.CP21XX,
        UsbSerialDevice.DeviceType.PROLIFIC
    )

    fun deviceTypeToIndex(deviceType: UsbSerialDevice.DeviceType): Int {
        return when (deviceType) {
            UsbSerialDevice.DeviceType.CDC_ACM -> 0
            UsbSerialDevice.DeviceType.FTDI -> 1
            UsbSerialDevice.DeviceType.CH34X -> 2
            UsbSerialDevice.DeviceType.CP21XX -> 3
            UsbSerialDevice.DeviceType.PROLIFIC -> 4
            else -> -1
        }
    }

    private fun connectToPort(portIndex: Int, deviceType: UsbSerialDevice.DeviceType?) {
        val usbSerialPort = mainViewModel.portListState.value[portIndex]
        val portDeviceType = deviceType ?: usbSerialPort.usbSerialDevice.deviceType
        Timber.d("connectToPort(): portIndex=$portIndex deviceType=${portDeviceType.name} port#=${usbSerialPort.portNumber}")
        mainViewModel.connectToUsbPort(
            usbSerialPort.usbSerialDevice.usbDevice,
            usbSerialPort.portNumber,
            portDeviceType,
        )
    }

    fun onConnectToPortClick(itemIndex: Int) {
        connectToPort(itemIndex, null)
    }

    fun onConnectToPortWithSelectedDeviceTypeClick() {
        _shouldShowSelectDeviceTypeAndConnectDialog.value = false
        connectToPort(selectedPortIndex, selectedDeviceType.value)
    }

    fun onSetDeviceTypeAndConnectToPortClick(itemIndex: Int) {
        selectedPortIndex = itemIndex
        _selectedDeviceType.value =
            mainViewModel.portListState.value[itemIndex].usbSerialDevice.deviceType
        _shouldShowSelectDeviceTypeAndConnectDialog.value = true
    }

    fun onCancelSetDeviceTypeAndConnectToPortClick() {
        _shouldShowSelectDeviceTypeAndConnectDialog.value = false
    }

    fun onSelectDeviceType(deviceTypeIndex: Int) {
        _selectedDeviceType.value = deviceTypes[deviceTypeIndex]
    }

    fun onDisconnectFromPortClick(itemIndex: Int) {
        Timber.d("DLVM onDisconnectFromPortClick(): itemIndex=$itemIndex")
        mainViewModel.disconnectFromUsbPort()
    }


    class Factory(
        private val application: Application,
        private val mainViewModel: MainViewModel
    ) :
        ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DeviceListViewModel(application, mainViewModel) as T
        }
    }
}