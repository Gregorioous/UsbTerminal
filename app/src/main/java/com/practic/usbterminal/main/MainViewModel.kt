package com.practic.usbterminal.main

import android.annotation.SuppressLint
import android.app.Application
import android.content.*
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.IBinder
import androidx.annotation.StringRes
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.practic.usbterminal.R
import com.practic.usbterminal.UsbTerminalApplication
import com.practic.usbterminal.settings.model.SettingsData
import com.practic.usbterminal.settings.model.SettingsRepository
import com.practic.usbterminal.usbcommservice.IOPacketsList
import com.practic.usbterminal.usbcommservice.SerialCommunicationParams
import com.practic.usbterminal.usbcommservice.UsbCommService
import com.practic.usbterminal.usbserial.UsbSerialDevice
import com.practic.usbterminal.usbserial.UsbSerialPort
import com.practic.usbterminal.usbserial.getSerialPortList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.*

@Suppress("DEPRECATION")
class MainViewModel(
    application: Application,
    initialIntent: Intent
) : AndroidViewModel(application) {


    val settingsRepository = SettingsRepository.getInstance(application)
    private var settingsData = SettingsData()

    data class UsbConnectionState(
        val statusCode: UsbSerialPort.ConnectStatusCode,
        val connectedUsbPort: UsbSerialPort? = null,
        val msg: String = ""
    )

    private val _usbConnectionState =
        mutableStateOf(UsbConnectionState(UsbSerialPort.ConnectStatusCode.IDLE))
    val usbConnectionState: State<UsbConnectionState> = _usbConnectionState

    private val _portListState: MutableState<List<UsbSerialPort>> = mutableStateOf(emptyList())
    val portListState: State<List<UsbSerialPort>> = _portListState

    private val _shouldRequestUsbPermissionFlow =
        MutableStateFlow(UsbPermissionRequester.PermissionRequestParams(false))
    val shouldRequestUsbPermissionFlow: StateFlow<UsbPermissionRequester.PermissionRequestParams> =
        _shouldRequestUsbPermissionFlow.asStateFlow()

    fun usbPermissionWasRequested() {
        _shouldRequestUsbPermissionFlow.value =
            _shouldRequestUsbPermissionFlow.value.copy(shouldRequestPermission = false)
    }

    private val screenTextModel =
        ScreenTextModel(viewModelScope, 80, ::sendBuf, settingsData.maxBytesToRetainForBackScroll)
    val textScreenState = screenTextModel.screenState
    val onScreenTxtScrolledToBottom = screenTextModel::onScrolledToBottom

    private val screenHexModel = ScreenHexModel(viewModelScope, 10_000)
    val screenHexTextBlocksState = screenHexModel.screenHexTextBlocksState
    val screenHexShouldScrollToBottom = screenHexModel.shouldScrollToBottom
    val onScreenHexScrolledToBottom = screenHexModel::onScrolledToBottom


    private val _ctrlButtonsRowVisible = mutableStateOf(false)
    val ctrlButtonsRowVisible: State<Boolean> = _ctrlButtonsRowVisible
    private val _shouldReportIfAtBottom = mutableStateOf(false)
    val shouldReportIfAtBottom: State<Boolean> = _shouldReportIfAtBottom
    fun onReportIfAtBottom(isAtBottom: Boolean) {
        _shouldReportIfAtBottom.value = false
        _ctrlButtonsRowVisible.value = settingsData.showCtrlButtonsRow
        if (isAtBottom) {
            setShouldScrollToBottom()
        }
    }

    private fun setShouldScrollToBottom() {
        if (settingsData.displayType == SettingsRepository.DisplayType.TEXT) {
            screenTextModel.shouldScrollToBottom()
        } else {
            screenHexModel.setShouldScrollToBottom()
        }
    }

    data class ScreenDimensions(val width: Int, val height: Int)

    private val _screenDimensions = mutableStateOf(ScreenDimensions(0, 0))
    val screenDimensions: State<ScreenDimensions> = _screenDimensions

    private var uid: Int = 1
        get() {
            field++; return field
        }

    enum class ScreenMeasurementCommand { NOOP, SHOULD_MEASURE_AFTER_NEXT_COMPOSITION, SHOULD_MEASURE }
    data class ShouldMeasureScreenDimensionsCmd(val cmd: ScreenMeasurementCommand, val uid: Int)

    private val _shouldMeasureScreenDimensions = mutableStateOf(
        ShouldMeasureScreenDimensionsCmd(
            ScreenMeasurementCommand.SHOULD_MEASURE_AFTER_NEXT_COMPOSITION,
            uid
        )
    )
    val shouldMeasureScreenDimensions: State<ShouldMeasureScreenDimensionsCmd> =
        _shouldMeasureScreenDimensions

    fun onScreenDimensionsMeasured(
        screenDimensions: ScreenDimensions,
        measurementTriggeringCommand: ScreenMeasurementCommand
    ) {
        when (measurementTriggeringCommand) {
            ScreenMeasurementCommand.SHOULD_MEASURE_AFTER_NEXT_COMPOSITION -> {
                _shouldMeasureScreenDimensions.value =
                    ShouldMeasureScreenDimensionsCmd(ScreenMeasurementCommand.SHOULD_MEASURE, uid)
            }

            ScreenMeasurementCommand.SHOULD_MEASURE -> {
                if (screenDimensions != _screenDimensions.value) {
                    val shouldRedrawScreen = screenDimensions.width != _screenDimensions.value.width
                    _screenDimensions.value = screenDimensions
                    screenTextModel.setScreenDimensions(
                        screenDimensions.width,
                        screenDimensions.height
                    )
                    if (shouldRedrawScreen) {
                        redrawScreen()
                    }
                }
                _shouldMeasureScreenDimensions.value =
                    ShouldMeasureScreenDimensionsCmd(ScreenMeasurementCommand.NOOP, uid)
            }

            else -> {}
        }
    }

    fun remeasureScreenDimensions() {
        _shouldMeasureScreenDimensions.value = ShouldMeasureScreenDimensionsCmd(
            ScreenMeasurementCommand.SHOULD_MEASURE_AFTER_NEXT_COMPOSITION,
            uid
        )
    }

    val shouldTerminateApp = MutableStateFlow(false)

    private val _shouldShowWelcomeMsg = mutableStateOf(false)
    private val _shouldShowUpgradeFromV1Msg = mutableStateOf(false)
    val shouldShowUpgradeFromV1Msg: State<Boolean> = _shouldShowUpgradeFromV1Msg
    fun onUserAcceptedWelcomeOrUpgradeMsg() {
        _shouldShowWelcomeMsg.value = false
        _shouldShowUpgradeFromV1Msg.value = false
        settingsRepository.setShowedV2WelcomeMsg(true)
    }

    fun onUserDeclinedWelcomeOrUpgradeMsg() {
        terminateApp()
    }

    private val _textToXmit = MutableStateFlow("")
    val textToXmit = _textToXmit.asStateFlow()

    private val _isTopBarInContextualMode = MutableStateFlow(false)
    val isTopBarInContextualMode = _isTopBarInContextualMode.asStateFlow()
    fun setIsTopBarInContextualMode(v: Boolean) {
        _isTopBarInContextualMode.value = v
    }

    private val _topBarClearButtonClicked = MutableStateFlow(false)
    val topBarClearButtonClicked = _topBarClearButtonClicked.asStateFlow()
    fun onTopBarClearButtonClicked() {
        _topBarClearButtonClicked.value = true
    }

    fun topBarClearButtonHandled() {
        _topBarClearButtonClicked.value = false
    }

    class TopBarTitleParams(val fmtResId: Int, val params: Array<out Any>)

    private val _topBarTitle = MutableStateFlow(TopBarTitleParams(R.string.app_name, emptyArray()))
    val topBarTitle = _topBarTitle.asStateFlow()
    fun setTopBarTitle(fmtResId: Int, vararg args: Any) {
        _topBarTitle.value = TopBarTitleParams(fmtResId, args)
    }

    data class DefaultTextColorDialogParams(
        val preSelectedIndex: Int,
        val freeTextInputField: String,
        @StringRes val exampleText: Int,
        val color: Int,
        val isOk: Boolean,
    )

    private val _defaultTextColorDialogParams = mutableStateOf(
        DefaultTextColorDialogParams(
            exampleText = R.string.this_is_how_it_looks,
            freeTextInputField = settingsData.defaultTextColorFreeInput.let {
                if (it == -1) "" else it.toString(
                    16
                )
            },
            color = settingsData.defaultTextColor,
            isOk = true,
            preSelectedIndex = settingsRepository.indexOfTextColor(settingsData.defaultTextColor)
        )
    )
    val defaultTextColorDialogParams: State<DefaultTextColorDialogParams> =
        _defaultTextColorDialogParams

    fun onDefaultTextColorFreeTextChanged(newText: String) {
        val colorValue = newText.toIntOrNull(16) ?: -1
        if (colorValue in 0..0xffffff && newText.length == 6) {
            _defaultTextColorDialogParams.value = DefaultTextColorDialogParams(
                preSelectedIndex = SettingsRepository.DefaultTextColorValues.preDefined.size,
                freeTextInputField = newText,
                exampleText = R.string.this_is_how_it_looks,
                color = colorValue,
                isOk = true,
            )
        } else {
            _defaultTextColorDialogParams.value = DefaultTextColorDialogParams(
                preSelectedIndex = SettingsRepository.DefaultTextColorValues.preDefined.size,
                freeTextInputField = newText,
                exampleText = R.string.ooops_illegal_color,
                color = 0xff0044,
                isOk = false,
            )
        }
    }

    fun onDefaultTextColorSelected(selectionIndex: Int, colorStr: String) {
        if (selectionIndex == SettingsRepository.DefaultTextColorValues.preDefined.size) {
            val colorValue = colorStr.toIntOrNull(16) ?: -1
            if (colorValue in 0..0xffffff) {
                settingsRepository.setDefaultTextColor(colorValue)
            }
        } else {
            settingsRepository.setDefaultTextColor(SettingsRepository.DefaultTextColorValues.preDefined[selectionIndex])
        }
    }

    @SuppressLint("StaticFieldLeak")
    private var usbCommService: UsbCommService? = null

    private val ioPacketsListObserver = IOPacketsListObserver()
    private var nextByteToProcessInIOPacketsList = IOPacketsList.DataPointer(0, 0)
    private val nextByteToProcessInIOPacketsListMutex = Mutex()

    private var usbDeviceToConnectOnStartup: UsbDevice? = null
    private var alreadyTriedToConnectToDeviceOnStartup = false

    val userInputHandler = UserInputHandler(settingsRepository, viewModelScope)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            usbCommService = (iBinder as UsbCommService.CommunicationServiceBinder).getService()
            userInputHandler.usbCommService = usbCommService

            usbCommService?.addObserver(communicationServiceObserver)
            usbCommService?.ioPacketsList?.addObserver(ioPacketsListObserver)

            usbCommService?.becomeBackgroundService()

            usbDeviceToConnectOnStartup?.let { usbDevice ->
                connectToUsbPort(
                    usbDevice,
                    portNumber = 0,
                    deviceType = UsbSerialDevice.DeviceType.AUTO_DETECT
                )
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Timber.d("serviceConnection.onServiceDisconnected() $arg0")
            userInputHandler.usbCommService = null
            usbCommService = null
            usbDeviceToConnectOnStartup = null
        }
    }

    private val communicationServiceObserver = Observer { _, arg ->
        val event = arg as UsbCommService.Event
        Timber.d("communicationServiceObserver: type=${event.eventType.name}  current thread: ${Thread.currentThread()}")
        when (event.eventType) {
            UsbCommService.Event.Type.CONNECTED -> onUsbConnected(event.obj as UsbCommService.Event.UsbConnectionParams)
            UsbCommService.Event.Type.DISCONNECTED -> onUsbDisconnected(event.obj as UsbCommService.Event.UsbDisconnectionParams?)
            UsbCommService.Event.Type.CONNECTION_ERROR -> onUsbConnectionError(event.obj as UsbSerialPort.ConnectResult)
            UsbCommService.Event.Type.NO_USB_PERMISSION -> requestUsbPermission(event.obj as UsbCommService.Event.UsbPermissionRequestParams)
            UsbCommService.Event.Type.SHOULD_UNBIND_SERVICE -> unbindCommunicationService()
            UsbCommService.Event.Type.UNRECOGNIZED_DEVICE_TYPE -> Timber.e("CommunicationServiceObserver : Observer - Got UsbCommService.Event.Type.UNRECOGNIZED_DEVICE_TYPE")
        }
    }

    private val usbAttachedOrDetachedBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            Timber.d("UsbAttachedOrDetachedBroadcastReceiver#onReceive(): action=$action")

            _portListState.value = getSerialPortList(getApplication())

            if (action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                val attachedDevice = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                if (attachedDevice != null) {
                    Timber.d("UsbAttachedOrDetachedBroadcastReceiver#onReceive(): attached-device-Id=${attachedDevice.deviceId}")
                    if (_usbConnectionState.value.statusCode != UsbSerialPort.ConnectStatusCode.CONNECTED) {
                        connectToUsbPort(
                            attachedDevice,
                            portNumber = 0,
                            deviceType = UsbSerialDevice.DeviceType.AUTO_DETECT
                        )
                    }
                } else {
                    Timber.e("UsbAttachedOrDetachedBroadcastReceiver#onReceive(): no USB device to attach")
                }
            } else if (action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                val detachedDevice = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                if (detachedDevice != null) {
                    Timber.d("UsbAttachedOrDetachedBroadcastReceiver#onReceive(): detached-device-Id=${detachedDevice.deviceId}")
                } else {
                    Timber.e("UsbAttachedOrDetachedBroadcastReceiver#onReceive(): no USB device to detach")
                }
                if (_usbConnectionState.value.connectedUsbPort?.usbSerialDevice?.usbDevice?.deviceId == detachedDevice?.deviceId) {
                    disconnectFromUsbPort()
                }
            }
        }
    }

    init {
        val intentFilter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        application.registerReceiver(usbAttachedOrDetachedBroadcastReceiver, intentFilter)

        _portListState.value = getSerialPortList(application)

        if (initialIntent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            usbDeviceToConnectOnStartup = initialIntent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }

        when (settingsData.displayType) {
            SettingsRepository.DisplayType.HEX -> screenHexModel.onStart()
            else -> screenTextModel.onStart()
        }

        viewModelScope.launch {
            settingsRepository.settingsStateFlow.collect { newSettingsData ->
                onSettingsUpdated(newSettingsData)
            }
        }
    }

    fun onMainActivityCreate() {
        remeasureScreenDimensions()
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unregisterReceiver(usbAttachedOrDetachedBroadcastReceiver)

        usbCommService?.ioPacketsList?.deleteObserver(ioPacketsListObserver)
        usbCommService?.deleteObserver(communicationServiceObserver)
        getApplication<Application>().unbindService(serviceConnection)
    }

    fun onActivityResume() {
        if (usbCommService == null) {
            val application = getApplication<UsbTerminalApplication>()
            Intent(getApplication(), UsbCommService::class.java).also { intent ->
                application.startService(intent)
                application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            }
        } else {
            usbCommService?.becomeBackgroundService()
        }
    }

    fun onActivityPause() {
        if (settingsData.workAlsoInBackground &&
            _usbConnectionState.value.statusCode == UsbSerialPort.ConnectStatusCode.CONNECTED
        ) {
            usbCommService?.becomeForegroundService()
        }
    }

    fun connectToUsbPort(
        usbDevice: UsbDevice,
        portNumber: Int,
        deviceType: UsbSerialDevice.DeviceType
    ) {
        Timber.d("connectToUsbPort(): usbDevice=${usbDevice.deviceName} portNumber=$portNumber deviceType=${deviceType.name}")
        val serialParams = SerialCommunicationParams(
            baudRate = settingsData.baudRate,
            dataBits = settingsData.dataBits,
            stopBits = settingsData.stopBits,
            parity = settingsData.parity,
        )
        usbCommService?.connectToUsbPort(
            usbDevice, portNumber, deviceType,
            serialParams, settingsData.setDTRTrueOnConnect, settingsData.setRTSTrueOnConnect
        )
    }

    fun disconnectFromUsbPort() {
        usbCommService?.disconnectFromUsbPort(exception = null)
    }

    inner class IOPacketsListObserver : Observer {
        @Deprecated("Deprecated in Java")
        override fun update(o: Observable?, arg: Any?) {
            viewModelScope.launch(Dispatchers.Default) {
                nextByteToProcessInIOPacketsListMutex.withLock {
                    nextByteToProcessInIOPacketsList = usbCommService?.ioPacketsList?.processData(
                        startAt = nextByteToProcessInIOPacketsList,
                        processor = ::processIOBytesIntoDisplayData,
                    ) ?: nextByteToProcessInIOPacketsList
                }
            }
        }
    }

    private fun redrawScreen() {
        viewModelScope.launch(Dispatchers.Default) {
            clearScreen(alsoEraseBufferedData = false)
            nextByteToProcessInIOPacketsListMutex.withLock {
                nextByteToProcessInIOPacketsList = usbCommService?.ioPacketsList?.processData(
                    startAt = IOPacketsList.DataPointer(0, 0),
                    processor = ::processIOBytesIntoDisplayData,
                ) ?: nextByteToProcessInIOPacketsList
            }
            setShouldScrollToBottom()
        }
    }

    private val highWatterMark = IOPacketsList.MutableDataPointer(0, 0)
    private fun processIOBytesIntoDisplayData(
        data: ByteArray,
        packetSerialNumber: Int,
        offset: Int,
        dataDirection: IOPacketsList.DataDirection,
        timeStamp: Long,
    ) {
        if (settingsData.displayType == SettingsRepository.DisplayType.HEX) {
            screenHexModel.onNewData(data, offset, dataDirection, timeStamp)
        } else {
            if (highWatterMark.packetSerialNumber == packetSerialNumber) {
                if (offset >= highWatterMark.offsetInPacket) {
                    screenTextModel.onNewData(data, offset, dataDirection, false)
                    highWatterMark.offsetInPacket = data.size
                } else {
                    val buf1 = data.copyOfRange(offset, highWatterMark.offsetInPacket)
                    screenTextModel.onNewData(buf1, 0, dataDirection, true)
                    if (data.size > highWatterMark.offsetInPacket) {
                        val buf2 = data.copyOfRange(highWatterMark.offsetInPacket, data.size)
                        screenTextModel.onNewData(buf2, 0, dataDirection, false)
                        highWatterMark.offsetInPacket = data.size
                    }
                }
            } else if (packetSerialNumber > highWatterMark.packetSerialNumber) {
                screenTextModel.onNewData(data, offset, dataDirection, false)
                highWatterMark.packetSerialNumber = packetSerialNumber
                highWatterMark.offsetInPacket = data.size
            } else {
                screenTextModel.onNewData(data, offset, dataDirection, true)
            }
        }
    }


    private fun onUsbConnected(connectionParams: UsbCommService.Event.UsbConnectionParams?) {
        if (connectionParams != null) {
            Timber.d("onUsbConnected(): vId=${connectionParams.usbSerialPort.usbSerialDevice.usbDevice.vendorId} pId=${connectionParams.usbSerialPort.usbSerialDevice.usbDevice.productId}")
            _usbConnectionState.value = UsbConnectionState(
                statusCode = UsbSerialPort.ConnectStatusCode.CONNECTED,
                connectedUsbPort = connectionParams.usbSerialPort,
            )

            _portListState.value = getSerialPortList(getApplication())
        } else {
            Timber.e("onUsbConnected(): null arg!")
        }
    }

    private fun onUsbDisconnected(disconnectionParam: UsbCommService.Event.UsbDisconnectionParams?) {
        _usbConnectionState.value = UsbConnectionState(UsbSerialPort.ConnectStatusCode.IDLE)
        disconnectionParam?.exception?.let {
            Timber.d("onUsbDisconnected(): USB disconnect due to: ${it.message ?: it.toString()}")
        } ?: run {
            Timber.d("onUsbDisconnected(): Nominal disconnection")
        }
        _portListState.value = getSerialPortList(getApplication())
    }

    private fun onUsbConnectionError(connectResult: UsbSerialPort.ConnectResult?) {
        if (connectResult != null) {
            Timber.d("onUsbConnectionError(): code=${connectResult.statusCode.name} msg=${connectResult.msg}")
            _usbConnectionState.value = UsbConnectionState(
                statusCode = connectResult.statusCode,
                msg = connectResult.msg
            )
        } else {
            Timber.e("onUsbConnectionError(): null arg!")
        }
    }

    private fun sendBuf(buf: ByteArray) {
        usbCommService?.sendUsbData(buf)
    }

    private fun requestUsbPermission(usbPermissionRequestParams: UsbCommService.Event.UsbPermissionRequestParams) {
        _shouldRequestUsbPermissionFlow.value = UsbPermissionRequester.PermissionRequestParams(
            shouldRequestPermission = true,
            shouldRequestEvenIfAlreadyDenied = false,
            usbDevice = usbPermissionRequestParams.usbDevice,
            portNumber = usbPermissionRequestParams.portNumber,
            usbDeviceType = usbPermissionRequestParams.deviceType,
            onPermissionDecision = ::onUsbPermissionUserDecision
        )
    }

    private fun onUsbPermissionUserDecision(
        permissionGranted: Boolean,
        usbDevice: UsbDevice?,
        portNumber: Int,
        deviceType: UsbSerialDevice.DeviceType
    ) {
        if (permissionGranted) {
            usbDevice?.let {
                connectToUsbPort(it, portNumber, deviceType)
            } ?: Timber.e("onUsbPermissionUserDecision(): no usbDevice")
        } else {
            Timber.i("User refused to grant us USB access permission")
            _usbConnectionState.value =
                UsbConnectionState(UsbSerialPort.ConnectStatusCode.ERR_NO_PERMISSION)
        }
    }

    private fun unbindCommunicationService() {
        getApplication<Application>().unbindService(serviceConnection)
    }

    fun setTextToXmit(text: String) {
        _textToXmit.value = text
    }

    fun onXmitButtonClick() {
        usbCommService?.sendUsbData(textToXmit.value.toByteArray(charset = Charsets.UTF_8))
    }

    fun onToggleHexTxtButtonClick() {
        settingsRepository.setDisplayType(
            when (settingsData.displayType) {
                SettingsRepository.DisplayType.TEXT -> SettingsRepository.DisplayType.HEX.value
                else -> SettingsRepository.DisplayType.TEXT.value
            }
        )
    }

    fun onToggleShowCtrlButtonsRowButtonClick() {
        settingsRepository.setShowCtrlButtonsRow(!settingsData.showCtrlButtonsRow)
    }

    suspend fun clearScreen(alsoEraseBufferedData: Boolean = true) {
        if (alsoEraseBufferedData) usbCommService?.eraseBufferedData()
        screenTextModel.clear()
        screenHexModel.clear()
        nextByteToProcessInIOPacketsListMutex.withLock {
            nextByteToProcessInIOPacketsList = IOPacketsList.DataPointer(0, 0)
        }
    }

    fun setDTR(value: Boolean) {
        usbCommService?.setDTR(value)
    }

    fun setRTS(value: Boolean) {
        usbCommService?.setRTS(value)
    }

    fun esp32BootloaderReset() {
        viewModelScope.launch(Dispatchers.IO) {
            usbCommService?.apply {
                val oldDTR = getDTR()
                val oldRTS = getRTS()
                setDTR(false)
                setRTS(false)
                delay(200)
                setRTS(true)
                delay(400)
                setRTS(false)
                delay(200)
                setDTR(oldDTR)
                setRTS(oldRTS)
            }
        }
    }

    fun arduinoReset() {
        viewModelScope.launch(Dispatchers.IO) {
            usbCommService?.apply {
                val oldDTR = getDTR()
                val oldRTS = getRTS()
                setDTR(false)
                setRTS(false)
                delay(10)
                setRTS(true)
                setDTR(true)
                delay(10)
                setDTR(oldDTR)
                setRTS(oldRTS)
            }
        }
    }

    private fun onSettingsUpdated(newSettingsData: SettingsData) {
        if (newSettingsData.isDefaultValues) {
            return
        }
        val oldSettingsData = settingsData
        settingsData = newSettingsData
        if (!settingsData.showedV2WelcomeMsg) {
            if (settingsData.showedEulaV1) {
                _shouldShowUpgradeFromV1Msg.value = true
            } else {
                _shouldShowWelcomeMsg.value = true
            }
        }

        if (newSettingsData.displayType != oldSettingsData.displayType) {
            onDisplayTypeChanged()
        }

        if (newSettingsData.showCtrlButtonsRow != oldSettingsData.showCtrlButtonsRow) {
            _shouldReportIfAtBottom.value = true
            remeasureScreenDimensions()
        }

        if (newSettingsData.maxBytesToRetainForBackScroll != oldSettingsData.maxBytesToRetainForBackScroll) {
            screenTextModel.setMaxTotalSize(newSettingsData.maxBytesToRetainForBackScroll)
            screenHexModel.setMaxTotalSize(newSettingsData.maxBytesToRetainForBackScroll)
        }

        if (newSettingsData.fontSize != oldSettingsData.fontSize) {
            viewModelScope.launch {
                clearScreen(alsoEraseBufferedData = false)
                remeasureScreenDimensions()
            }
        }

        if (newSettingsData.soundOn != oldSettingsData.soundOn) {
            screenTextModel.soundOn = newSettingsData.soundOn
        }

        if (newSettingsData.silentlyDropUnrecognizedCtrlChars != oldSettingsData.silentlyDropUnrecognizedCtrlChars) {
            screenTextModel.silentlyDropUnrecognizedCtrlChars =
                newSettingsData.silentlyDropUnrecognizedCtrlChars
        }

        if (settingsData.connectToDeviceOnStart && !alreadyTriedToConnectToDeviceOnStartup) {
            tryToConnectToFirstDevice()
        }
        alreadyTriedToConnectToDeviceOnStartup = true

        if (newSettingsData.defaultTextColor != oldSettingsData.defaultTextColor) {
            var preSelectedIndex =
                settingsRepository.indexOfTextColor(settingsData.defaultTextColor)
            if (preSelectedIndex == -1) {
                preSelectedIndex = SettingsRepository.DefaultTextColorValues.preDefined.size
            }
            _defaultTextColorDialogParams.value =
                _defaultTextColorDialogParams.value.copy(
                    preSelectedIndex = preSelectedIndex,
                    color = newSettingsData.defaultTextColor,
                )
        }
        if (newSettingsData.defaultTextColorFreeInput != oldSettingsData.defaultTextColorFreeInput) {
            _defaultTextColorDialogParams.value =
                _defaultTextColorDialogParams.value.copy(
                    freeTextInputField = newSettingsData.defaultTextColorFreeInput.toString(16),
                )
        }
    }

    private fun tryToConnectToFirstDevice() {
        val usbSerialPort = _portListState.value.firstOrNull() ?: return
        val portDeviceType = usbSerialPort.usbSerialDevice.deviceType
        Timber.d("tryToConnectToFirstDevice(): deviceType=${portDeviceType.name} port#=${usbSerialPort.portNumber}")
        if (usbCommService == null) {
            if (usbDeviceToConnectOnStartup == null) {
                usbDeviceToConnectOnStartup = usbSerialPort.usbSerialDevice.usbDevice
            }
        } else {
            connectToUsbPort(
                usbSerialPort.usbSerialDevice.usbDevice,
                usbSerialPort.portNumber,
                portDeviceType,
            )
        }
    }

    private fun onDisplayTypeChanged() {
        when (settingsData.displayType) {
            SettingsRepository.DisplayType.HEX -> {
                screenHexModel.onStart()
                screenTextModel.onStop()
            }

            else -> {
                screenTextModel.onStart()
                screenHexModel.onStop()
            }
        }
        redrawScreen()
    }

    private var currentlyIsDarkTheme: Boolean? = null
    fun setIsDarkTheme(isDarkTheme: Boolean) {
        if (isDarkTheme != currentlyIsDarkTheme) {
            currentlyIsDarkTheme = isDarkTheme
            screenTextModel.setIsDarkTheme(isDarkTheme)
            screenHexModel.setIsDarkTheme(isDarkTheme)
            redrawScreen()
        }
    }

    private fun terminateApp() {
        usbCommService?.stop()
        shouldTerminateApp.value = true
    }

    fun debug1() {
        usbCommService?.debug('1') ?: run { Timber.d("No usbCommService") }
    }

    fun debug2() {
        usbCommService?.debug('2') ?: run { Timber.d("No usbCommService") }
    }



    class Factory(
        private val application: Application,
        private val initialIntent: Intent
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(application, initialIntent) as T
        }
    }
}