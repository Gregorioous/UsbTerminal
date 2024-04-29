package com.practic.usbterminal.usbserial

import android.content.Context
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.Ch34xSerialDriver
import com.hoho.android.usbserial.driver.Cp21xxSerialDriver
import com.hoho.android.usbserial.driver.FtdiSerialDriver
import com.hoho.android.usbserial.driver.ProlificSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialProber

fun getSerialPortList(context: Context): List<UsbSerialPort> {
    val usbDefaultProber = UsbSerialProber.getDefaultProber()
    val portList = mutableListOf<UsbSerialPort>()

    for (device in (context.getSystemService(Context.USB_SERVICE) as UsbManager).deviceList.values) {

        val usbSerialDevice =
            UsbSerialDevice(device, UsbSerialDevice.DeviceType.AUTO_DETECT, usbDefaultProber)

        val driver = usbDefaultProber.probeDevice(device)

        if (driver != null) {
            for (portNumber in 0 until usbSerialDevice.nPorts) {
                portList.add(UsbSerialPort(usbSerialDevice, portNumber))
            }
        } else {
            portList.add(UsbSerialPort(usbSerialDevice))
        }
    }
    return portList
}

fun driverToDeviceType(driver: UsbSerialDriver?): UsbSerialDevice.DeviceType {
    return driver?.let {
        when (it) {
            is CdcAcmSerialDriver -> UsbSerialDevice.DeviceType.CDC_ACM
            is FtdiSerialDriver -> UsbSerialDevice.DeviceType.FTDI
            is Ch34xSerialDriver -> UsbSerialDevice.DeviceType.CH34X
            is Cp21xxSerialDriver -> UsbSerialDevice.DeviceType.CP21XX
            is ProlificSerialDriver -> UsbSerialDevice.DeviceType.PROLIFIC
            else -> UsbSerialDevice.DeviceType.UNRECOGNIZED
        }
    } ?: UsbSerialDevice.DeviceType.UNRECOGNIZED
}