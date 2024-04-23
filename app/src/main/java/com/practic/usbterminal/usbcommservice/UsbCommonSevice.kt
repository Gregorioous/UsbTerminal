package com.practic.usbterminal.usbcommservice

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MIN
import com.practic.usbterminal.R
import com.practic.usbterminal.main.MainActivity
import com.practic.usbterminal.settings.model.SettingsData
import com.practic.usbterminal.settings.model.SettingsRepository
import com.practic.usbterminal.usbserial.UsbSerialDevice
import com.practic.usbterminal.usbserial.UsbSerialPort
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*


@Suppress("DEPRECATION")
class UsbCommService : Service() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID_FOR_FOREGROUND_SERVICE = "UTFSChannelId"
        const val FOREGROUND_SERVICE_NOTIFICATION_ID = 7734
        const val STOP_SELF = "UTStopSelf"
    }

    enum class ErrorCode {
        OK,
        NOT_CONNECTED,
    }

    private val job = SupervisorJob()
    private val defaultScope = CoroutineScope(Dispatchers.Default + job)
    lateinit var settingsRepository: SettingsRepository
    private var settingsData = SettingsData()
    private var isStopping = false
    private var isForeground = false

    private lateinit var usbManager: UsbManager

    private var usbSerialPort: UsbSerialPort? = null

    val ioPacketsList = IOPacketsList(settingsData.maxBytesToRetainForBackScroll)

    private val observable = UsbCommServiceObservable()

    private class UsbCommServiceObservable : Observable() {
        @Deprecated("Deprecated in Java", ReplaceWith("super.setChanged()", "java.util.Observable"))
        public override fun setChanged() {
            super.setChanged()
        }
    }

    class Event(val eventType: Type, val obj: Any? = null) {
        enum class Type {
            CONNECTED,
            DISCONNECTED,
            CONNECTION_ERROR,
            NO_USB_PERMISSION,
            SHOULD_UNBIND_SERVICE,
            UNRECOGNIZED_DEVICE_TYPE,
        }

        class UsbConnectionParams(val usbSerialPort: UsbSerialPort)
        class UsbDisconnectionParams(val exception: Exception?)
        class UsbPermissionRequestParams(
            val usbDevice: UsbDevice,
            val portNumber: Int,
            val deviceType: UsbSerialDevice.DeviceType
        )
    }

    fun addObserver(observer: Observer) {
        observable.addObserver(observer)
    }

    fun deleteObserver(observer: Observer) {
        observable.deleteObserver(observer)
    }

    private val binder = CommunicationServiceBinder()
    private var logFile: LogFile? = null

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository.getInstance(application)
        defaultScope.launch {
            settingsRepository.settingsStateFlow.collect {
                if (it.maxBytesToRetainForBackScroll != settingsData.maxBytesToRetainForBackScroll) {
                    ioPacketsList.setMaxSize(it.maxBytesToRetainForBackScroll)
                }
                settingsData = it
                if (logFile == null && settingsData.logSessionDataToFile) {
                    val logFileResult = LogFile.Builder.getLogFileAsync(
                        context = application,
                        coroutineScope = defaultScope,
                        ioPacketsList = ioPacketsList
                    ).await()
                    logFile = logFileResult.getOrNull()
                    if (logFile == null) {
                        Timber.e("Can't create log-file. ${logFileResult.exceptionOrNull()?.message}")
                    }
                } else if (!settingsData.logSessionDataToFile) {
                    logFile?.close()
                    logFile = null
                }
                logFile?.updateSettings(settingsData)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.getBooleanExtra(STOP_SELF, false) == true) {
            Timber.d("onStartCommand(): Stopping self")
            defaultScope.launch {
                notifyObservers(Event(Event.Type.SHOULD_UNBIND_SERVICE))
                stopSelf()
            }
            return START_NOT_STICKY
        }

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        return START_STICKY
    }

    inner class CommunicationServiceBinder : Binder() {
        fun getService(): UsbCommService = this@UsbCommService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        logFile?.close()
        logFile = null
        job.cancel()
    }

    fun becomeForegroundService() {
        Timber.d("becomeForegroundService(): isStopping=$isStopping")
        if (isStopping) return
        createNotificationChannel()

        val openActivityIntent = Intent(this, MainActivity::class.java)
        val openActivityPendingIntent =
            PendingIntent.getActivity(this, 0, openActivityIntent, PendingIntent.FLAG_IMMUTABLE)

        val stopServiceIntent = Intent(this, UsbCommService::class.java).apply {
            putExtra(STOP_SELF, true)
        }
        val stopServicePendingIntent =
            PendingIntent.getService(this, 0, stopServiceIntent, PendingIntent.FLAG_IMMUTABLE)

        val notificationBuilder =
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_FOR_FOREGROUND_SERVICE)
        val notification = notificationBuilder
            .setContentTitle(getString(R.string.fg_service_notification_title))
            .setSmallIcon(R.drawable.ic_baseline_monitor_24)
            .setColor(resources.getColor(R.color.teal_900, theme))
            .setPriority(PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .setSilent(true)
            .addAction(
                R.drawable.ic_baseline_monitor_24,
                getString(R.string.open_app_all_caps),
                openActivityPendingIntent
            )
            .addAction(
                R.drawable.ic_baseline_clear_24,
                getString(R.string.stop_all_caps),
                stopServicePendingIntent
            )
            .build()
        startForeground(FOREGROUND_SERVICE_NOTIFICATION_ID, notification)
        isForeground = true
    }

    fun becomeBackgroundService() {
        Timber.d("becomeBackgroundService(): isForeground=$isForeground")
        if (isForeground) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            isForeground = false
        }
    }

    fun connectToUsbPort(
        usbDevice: UsbDevice,
        portNumber: Int,
        deviceType: UsbSerialDevice.DeviceType,
        serialCommunicationParams: SerialCommunicationParams,
        setDTROnConnect: Boolean,
        setRTSOnConnect: Boolean,
    ) {
        defaultScope.launch {
            if (!usbManager.hasPermission(usbDevice)) {
                notifyObservers(
                    Event(
                        eventType = Event.Type.NO_USB_PERMISSION,
                        obj = Event.UsbPermissionRequestParams(usbDevice, portNumber, deviceType)
                    )
                )
                return@launch
            }

            usbSerialPort?.let { currentlyConnectedPort ->
                if (currentlyConnectedPort.usbSerialDevice.usbDevice.deviceId == usbDevice.deviceId &&
                    currentlyConnectedPort.portNumber == portNumber
                ) {
                    Timber.d("connectToUsbPort(): Already connected to this port. Doing nothing.")
                    return@launch
                }
                currentlyConnectedPort.disconnect()
            }

            val usbSerialDevice = UsbSerialDevice(usbDevice, deviceType)
            Timber.d("connectToUsbPort(): DeviceType=${usbSerialDevice.deviceType.name} port#=$portNumber baud=${serialCommunicationParams.baudRate} nBits=${serialCommunicationParams.dataBits} nStopBits=${serialCommunicationParams.stopBits} parity=${serialCommunicationParams.parity}")
            if (usbSerialDevice.deviceType != UsbSerialDevice.DeviceType.UNRECOGNIZED) {
                usbSerialPort = UsbSerialPort(
                    usbSerialDevice = usbSerialDevice,
                    portNumber = portNumber,
                    serialCommunicationParams = serialCommunicationParams,
                    onDataReceived = ::onUsbDataReceived,
                    onError = ::onUsbError
                )
                usbSerialPort?.let { usbSerialPort ->
                    val connectResult = usbSerialPort.connect(application)
                    when (connectResult.statusCode) {
                        UsbSerialPort.ConnectStatusCode.CONNECTED -> {
                            Timber.d("connectToUsbPort(): Connected successfully")
                            setDTR(setDTROnConnect)
                            setRTS(setRTSOnConnect)
                            notifyObservers(
                                Event(
                                    Event.Type.CONNECTED,
                                    Event.UsbConnectionParams(usbSerialPort = usbSerialPort)
                                )
                            )
                        }

                        else -> {
                            Timber.w("connectToUsbPort(): Error in usbSerialPort.connect(). errCode=${connectResult.statusCode} usbDevice=$usbDevice")
                            notifyObservers(Event(Event.Type.CONNECTION_ERROR, connectResult))
                        }
                    }
                }
            } else {
                notifyObservers(Event(Event.Type.UNRECOGNIZED_DEVICE_TYPE))
            }
        }
    }

    fun disconnectFromUsbPort(exception: Exception?) {
        usbSerialPort?.let {
            usbSerialPort = null
            defaultScope.launch {
                Timber.d("disconnectFromUsbDevice()")
                it.disconnect()
                notifyObservers(
                    Event(
                        Event.Type.DISCONNECTED,
                        Event.UsbDisconnectionParams(exception)
                    )
                )
            }
        } ?: run {
            Timber.w("disconnectFromUsbPort(): usbSerialPort is null")
        }
    }

    private fun onUsbDataReceived(receivedData: ByteArray, inputPaused: Boolean) {

        ioPacketsList.appendData(receivedData, IOPacketsList.DataDirection.IN)
        if (inputPaused) {
            ioPacketsList.inputPaused()
        }
    }

    private fun onUsbError(exception: Exception) {
        Timber.e("onUsbError(): exception=${exception}")
        disconnectFromUsbPort(exception)
    }

    fun sendUsbData(data: ByteArray): ErrorCode {
        val isLoopBackOn = settingsData.loopBack
        var rc = ErrorCode.OK
        usbSerialPort?.let { port ->
            if (port.isConnected || isLoopBackOn) {
                defaultScope.launch {
                    if (port.isConnected) {
                        port.write(data)
                        ioPacketsList.appendData(data, IOPacketsList.DataDirection.OUT)
                    }

                    if (isLoopBackOn) {
                        ioPacketsList.appendData(data, IOPacketsList.DataDirection.IN)
                    }

                    ioPacketsList.inputPaused()
                }
            } else {
                Timber.w("sendUsbData(): usbSerialPort is not connected")
                rc = ErrorCode.NOT_CONNECTED
            }
        } ?: run {
            if (isLoopBackOn) {
                defaultScope.launch {
                    ioPacketsList.appendData(data, IOPacketsList.DataDirection.IN)
                    ioPacketsList.inputPaused()
                }
            } else {
                Timber.w("sendUsbData(): usbSerialPort is null")
                rc = ErrorCode.NOT_CONNECTED
            }
        }
        return rc
    }

    fun setDTR(value: Boolean) {
        usbSerialPort?.setDTR(value)
    }

    fun setRTS(value: Boolean) {
        usbSerialPort?.setRTS(value)
    }

    fun getDTR(): Boolean = usbSerialPort?.getDTR() ?: false
    fun getRTS(): Boolean = usbSerialPort?.getRTS() ?: false

    fun eraseBufferedData() {
        ioPacketsList.clear()
    }

    fun stop() {
        isStopping = true
        stopSelf()
    }


    private suspend fun notifyObservers(event: Event) {
        withContext(Dispatchers.Main) {
            Timber.d("notifyObservers() eventType=${event.eventType.name}")
            observable.setChanged()
            observable.notifyObservers(event)
        }
    }

    private fun createNotificationChannel() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (notificationManager.getNotificationChannel(
                NOTIFICATION_CHANNEL_ID_FOR_FOREGROUND_SERVICE
            ) == null
        ) {
            val srvNotificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID_FOR_FOREGROUND_SERVICE,
                getString(R.string.usb_comm_service_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)
                importance = NotificationManager.IMPORTANCE_DEFAULT
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(srvNotificationChannel)
        }
    }

    fun debug(param: Char) {
        usbCommServiceDebug(param, defaultScope, ioPacketsList)
    }
}