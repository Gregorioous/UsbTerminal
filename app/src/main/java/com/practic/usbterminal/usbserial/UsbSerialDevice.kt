package com.practic.usbterminal.usbserial

import android.hardware.usb.UsbDevice
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.Ch34xSerialDriver
import com.hoho.android.usbserial.driver.Cp21xxSerialDriver
import com.hoho.android.usbserial.driver.FtdiSerialDriver
import com.hoho.android.usbserial.driver.ProlificSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialProber

class UsbSerialDevice(
    val usbDevice: UsbDevice,
    var deviceType: DeviceType,
    private var usbSerialProber: UsbSerialProber? = null
) {
    val id: Int
        get() = usbDevice.deviceId
    val name: String
        get() = usbDevice.deviceName
    val vendorId: Int
        get() = usbDevice.vendorId
    val productId: Int
        get() = usbDevice.productId

    var nPorts = 0
    val deviceTypeStr: String
        get() = getDeviceTypeStr(deviceType)

    var driver: UsbSerialDriver? = null

    enum class DeviceType {
        AUTO_DETECT, CDC_ACM, FTDI, CH34X, CP21XX, PROLIFIC, UNRECOGNIZED
    }

    init {
        if (deviceType == DeviceType.AUTO_DETECT) {
            if (usbSerialProber == null) {
                usbSerialProber = UsbSerialProber.getDefaultProber()
            }
            driver = usbSerialProber?.probeDevice(usbDevice)
            deviceType = driverToDeviceType(driver)
        } else {
            driver = getDriverForDeviceType(deviceType, usbDevice)
        }
        nPorts = driver?.ports?.size ?: 0
    }

    companion object {
        fun getDriverForDeviceType(deviceType: DeviceType, usbDevice: UsbDevice): UsbSerialDriver? {
            return when (deviceType) {
                DeviceType.CDC_ACM -> CdcAcmSerialDriver(usbDevice)
                DeviceType.FTDI -> FtdiSerialDriver(usbDevice)
                DeviceType.CH34X -> Ch34xSerialDriver(usbDevice)
                DeviceType.CP21XX -> Cp21xxSerialDriver(usbDevice)
                DeviceType.PROLIFIC -> ProlificSerialDriver(usbDevice)
                else -> null
            }
        }
    }

    private fun getDeviceTypeStr(deviceType: DeviceType): String {
        return when (deviceType) {
            DeviceType.CDC_ACM -> "CDC/ACM"
            DeviceType.FTDI -> "FTDI"
            DeviceType.CH34X -> "CH34x"
            DeviceType.CP21XX -> "CP21xx"
            DeviceType.PROLIFIC -> "Prolific"
            else -> "Unrecognized"
        }
    }
}