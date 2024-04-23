package com.practic.usbterminal.usbserial

import android.content.Context
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.*
import com.practic.usbterminal.usbcommservice.SerialCommunicationParams
import timber.log.Timber
import java.io.IOException
import com.hoho.android.usbserial.driver.UsbSerialPort as DriverUsbSerialPort


class UsbSerialPort(
    val usbSerialDevice: UsbSerialDevice,
    val portNumber: Int = 0,
    private val baudRate: Int = 9600,
    private val dataBits: Int = 8,
    private val stopBits: Int = STOPBITS_1,
    private val parity: Int = PARITY_NONE,
    private val onDataReceived: ((receivedData: ByteArray, inputPaused: Boolean) -> Unit)? = null,
    private val onError: ((exception: Exception) -> Unit)? = null,
) {

    constructor(
        usbSerialDevice: UsbSerialDevice,
        portNumber: Int,
        serialCommunicationParams: SerialCommunicationParams,
        onDataReceived: (receivedData: ByteArray, inputPaused: Boolean) -> Unit,
        onError: (exception: Exception) -> Unit,
    ) : this(
        usbSerialDevice = usbSerialDevice,
        portNumber = portNumber,
        baudRate = serialCommunicationParams.baudRate,
        dataBits = serialCommunicationParams.dataBits,
        stopBits = serialCommunicationParams.stopBits,
        parity = serialCommunicationParams.parity,
        onDataReceived = onDataReceived,
        onError = onError,
    )

    @Suppress("unused")
    companion object {
        const val PARITY_NONE = DriverUsbSerialPort.PARITY_NONE
        const val PARITY_ODD = DriverUsbSerialPort.PARITY_ODD
        const val PARITY_EVEN = DriverUsbSerialPort.PARITY_EVEN
        const val PARITY_MARK = DriverUsbSerialPort.PARITY_MARK
        const val PARITY_SPACE = DriverUsbSerialPort.PARITY_SPACE
        const val STOPBITS_1 = DriverUsbSerialPort.STOPBITS_1
        const val STOPBITS_1_5 = DriverUsbSerialPort.STOPBITS_1_5
        const val STOPBITS_2 = DriverUsbSerialPort.STOPBITS_2
    }

    enum class ErrorCode {
        OK,
        NOT_CONNECTED,
    }

    enum class ConnectStatusCode {
        IDLE,
        CONNECTED,
        ERR_NO_PERMISSION,
        ERR_ALREADY_CONNECTED,
        ERR_UNRECOGNIZED_DEVICE_TYPE,
        ERR_NO_SUCH_PORT_NUMBER,
        ERR_OPEN_DEVICE_FAILED,
        ERR_OPEN_PORT_FAILED,
        ERR_SET_PORT_PARAMETERS_FAILED,
        ERR_MISSING_HANDLERS,
    }

    class ConnectResult(val statusCode: ConnectStatusCode, val msg: String)

    var isConnected = false

    private var driverUsbSerialPort: DriverUsbSerialPort? = null

    private var usbPortBufferedIoManager: UsbPortBufferedIoManager? = null


    fun isEqual(that: UsbSerialPort?): Boolean {
        if (that == null) return false
        return this.portNumber == that.portNumber &&
                this.usbSerialDevice.usbDevice.deviceId == that.usbSerialDevice.usbDevice.deviceId
    }

    fun connect(context: Context): ConnectResult {
        if (isConnected) {
            return ConnectResult(ConnectStatusCode.ERR_ALREADY_CONNECTED, "Already connected")
        }
        if (onDataReceived == null || onError == null) {
            return ConnectResult(
                ConnectStatusCode.ERR_MISSING_HANDLERS,
                "Unspecified onDataReceived() or onError()"
            )
        }

        val usbDevice = usbSerialDevice.usbDevice

        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

        if (!usbManager.hasPermission(usbDevice)) {
            return ConnectResult(
                ConnectStatusCode.ERR_NO_PERMISSION,
                "No permission granted to access USB device: $usbDevice"
            )
        }

        val usbSerialDriver = usbSerialDevice.driver
            ?: return ConnectResult(
                ConnectStatusCode.ERR_UNRECOGNIZED_DEVICE_TYPE,
                "Unrecognized device type. Device=$usbDevice"
            )

        if (usbSerialDriver.ports.size < portNumber) {
            return ConnectResult(
                ConnectStatusCode.ERR_NO_SUCH_PORT_NUMBER,
                "Invalid port number: $portNumber. Device has only ${usbSerialDriver.ports.size} ports."
            )
        }

        val usbConnection = usbManager.openDevice(usbDevice)
            ?: return ConnectResult(
                ConnectStatusCode.ERR_OPEN_DEVICE_FAILED,
                "UsbManager#openDevice() failed for device: $portNumber."
            )

        driverUsbSerialPort = usbSerialDriver.ports[portNumber]
        try {
            driverUsbSerialPort?.open(usbConnection)
        } catch (e: Exception) {
            Timber.e(e, "driverUsbSerialPort#open() failed: ${e.message}")
            disconnect()
            return ConnectResult(
                ConnectStatusCode.ERR_OPEN_PORT_FAILED,
                "Open port failed. Port number: $portNumber."
            )
        }
        try {
            driverUsbSerialPort?.setParameters(baudRate, dataBits, stopBits, parity)
        } catch (e: Exception) {
            Timber.e(e, "driverUsbSerialPort#setParameters() failed: ${e.message}")
            disconnect()
            return ConnectResult(
                ConnectStatusCode.ERR_SET_PORT_PARAMETERS_FAILED,
                "Set port parameters failed. Port number: $portNumber."
            )
        }
        usbPortBufferedIoManager = UsbPortBufferedIoManager(onDataReceived, onError)
        driverUsbSerialPort?.let {
            usbPortBufferedIoManager?.connect(it)
        }

        isConnected = true

        return ConnectResult(ConnectStatusCode.CONNECTED, "OK")
    }

    fun disconnect() {
        try {
            driverUsbSerialPort?.close()
        } catch (ignored: IOException) {
        }
        driverUsbSerialPort = null

        usbPortBufferedIoManager?.disconnect()

        isConnected = false
    }

    suspend fun write(data: ByteArray): ErrorCode {
        if (!isConnected) {
            Timber.e("write(): Illegal state: Not-Connected")
            return ErrorCode.NOT_CONNECTED
        }
        usbPortBufferedIoManager?.write(data)
        return ErrorCode.OK
    }

    fun setDTR(value: Boolean) {
        driverUsbSerialPort?.dtr = value
    }

    fun setRTS(value: Boolean) {
        driverUsbSerialPort?.rts = value
    }

    fun getDTR(): Boolean = driverUsbSerialPort?.dtr ?: false
    fun getRTS(): Boolean = driverUsbSerialPort?.rts ?: false

    @Suppress("unused")
    fun getCD(): Boolean = driverUsbSerialPort?.cd ?: false

    @Suppress("unused")
    fun getCTS(): Boolean = driverUsbSerialPort?.cts ?: false

    @Suppress("unused")
    fun getDSR(): Boolean = driverUsbSerialPort?.dsr ?: false

    @Suppress("unused")
    fun getRI(): Boolean = driverUsbSerialPort?.ri ?: false
}
