package com.practic.usbterminal.main

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.practic.usbterminal.usbserial.UsbSerialDevice
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber


@Suppress("DEPRECATION")
@SuppressLint("StaticFieldLeak")
object UsbPermissionRequester {
    private const val INTENT_ACTION_USB_PERMISSION =
        "com.liorhass.android.usbterminal.free.usbPermission"
    private var activity: Activity? = null
    private var requestAlreadyDenied: Boolean = false
    private var onPermissionDecision: ((
        permissionGranted: Boolean,
        UsbDevice?,
        portNumber: Int,
        deviceType: UsbSerialDevice.DeviceType
    ) -> Unit)? = null
    private var portNumber = 0
    private var deviceType: UsbSerialDevice.DeviceType = UsbSerialDevice.DeviceType.UNRECOGNIZED
    private var job: Job? = null

    data class PermissionRequestParams(
        val shouldRequestPermission: Boolean,
        val shouldRequestEvenIfAlreadyDenied: Boolean = false,
        val usbDevice: UsbDevice? = null,
        val portNumber: Int = 0,
        val usbDeviceType: UsbSerialDevice.DeviceType = UsbSerialDevice.DeviceType.UNRECOGNIZED,
        val onPermissionDecision: ((
            permissionGranted: Boolean,
            usbDevice: UsbDevice?,
            portNumber: Int,
            deviceType: UsbSerialDevice.DeviceType
        ) -> Unit)? = null
    )

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    fun bindActivity(
        activity: ComponentActivity,
        shouldAskForPermissionFlow: StateFlow<PermissionRequestParams>,
        onPermissionRequested: () -> Unit
    ) {
        if (job != null) {
            job?.cancel()
        }
        this.activity = activity

        val filter = IntentFilter(INTENT_ACTION_USB_PERMISSION)
        activity.registerReceiver(usbPermissionBroadcastReceiver, filter)

        job = activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                shouldAskForPermissionFlow.collect { permissionRequestParams ->
                    if (permissionRequestParams.shouldRequestPermission) {
                        Timber.d("Got should-request-permission event. permissionRequestParams: device-name: ${permissionRequestParams.usbDevice?.deviceName}")
                        requestPermission(permissionRequestParams)
                        onPermissionRequested()
                    }
                }
            }
        }
    }

    fun unbindActivity() {
        if (this.activity == null) {
            throw IllegalStateException("Activity not bound")
        }
        activity?.unregisterReceiver(usbPermissionBroadcastReceiver)
        activity = null
    }

    private fun requestPermission(requestParams: PermissionRequestParams) {

        if (activity == null) {
            throw IllegalStateException("bindActivity() must be called before this function")
        }
        if (requestAlreadyDenied && !requestParams.shouldRequestEvenIfAlreadyDenied) {
            Timber.w("UsbPermissionRequester#requestPermission() already denied")
            return
        }

        onPermissionDecision = requestParams.onPermissionDecision
        portNumber = requestParams.portNumber
        deviceType = requestParams.usbDeviceType

        @SuppressLint("InlinedApi")
        val usbPermissionIntent = PendingIntent.getBroadcast(
            activity, 0, Intent(INTENT_ACTION_USB_PERMISSION), PendingIntent.FLAG_MUTABLE
        )
        requestParams.usbDevice?.let { usbDevice ->
            Timber.d("Calling UsbManager.requestPermission() for device: ${usbDevice.deviceName}")
            (activity?.getSystemService(Context.USB_SERVICE) as UsbManager)
                .requestPermission(usbDevice, usbPermissionIntent)
        }
    }

    private val usbPermissionBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.d("usbPermissionBroadcastReceiver.onReceive() intent.action=${intent.action}")
            if (intent.action == INTENT_ACTION_USB_PERMISSION) {
                synchronized(this) {
                    val permissionGranted =
                        intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    if (!permissionGranted)
                        requestAlreadyDenied = true
                    onPermissionDecision?.invoke(permissionGranted, device, portNumber, deviceType)
                }
            }
        }
    }
}