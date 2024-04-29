package com.practic.usbterminal.usbserial

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import timber.log.Timber

class UsbPortBufferedIoManager(
    private val onDataReceived: (receivedData: ByteArray, inputPaused: Boolean) -> Unit,
    private val onError: (exception: Exception) -> Unit,
) {
    private val inputChannel: Channel<ByteArray> = Channel(100)
    private val outputChannel: Channel<ByteArray> = Channel(100)
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var inputDispatcherJob: Job? = null
    private var inputJob: Job? = null
    private var outputJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    fun connect(driverUsbSerialPort: com.hoho.android.usbserial.driver.UsbSerialPort) {
        Timber.d("UsbPortBufferedIoManager#connect")
        inputJob = ioScope.launch {
            val buf = ByteArray(driverUsbSerialPort.readEndpoint?.maxPacketSize ?: 1024)
            while (isActive) {
                try {
                    val len = driverUsbSerialPort.read(buf, 0)
                    if (len > 0) {
                        val data = buf.copyOfRange(0, len)
                        inputChannel.send(data)
                    }
                } catch (e: java.lang.Exception) {
                    if (isActive) {
                        Timber.e("Error reading from UsbSerialPort: $e")
                        onError.invoke(e)
                    }
                }
            }
        }

        inputDispatcherJob = ioScope.launch {
            while (isActive) {
                val data = inputChannel.receive()
                onDataReceived.invoke(data, inputChannel.isEmpty)
            }
        }

        outputJob = ioScope.launch {
            while (isActive) {
                val outputData = outputChannel.receive()
                try {
                    driverUsbSerialPort.write(outputData, 0)
                } catch (e: java.lang.Exception) {
                    if (isActive) {
                        Timber.e("Error writing to UsbSerialPort: ${e.message}")
                        onError.invoke(e)
                    }
                }
            }
        }
    }

    suspend fun write(data: ByteArray): UsbSerialPort.ErrorCode {
        outputChannel.send(data)
        return UsbSerialPort.ErrorCode.OK
    }

    fun disconnect() {
        Timber.d("UsbPortBufferedIoManager#Disconnect()")
        outputJob?.cancel(); outputJob = null
        inputJob?.cancel(); inputJob = null
        inputDispatcherJob?.cancel(); inputDispatcherJob = null
        inputChannel.close()
        outputChannel.close()
    }
}