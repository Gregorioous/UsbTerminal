package com.practic.usbterminal

import android.app.Application
import com.hoho.android.usbserial.BuildConfig
import timber.log.Timber

@Suppress("unused")
class UsbTerminalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(LineNumberDebugTree())
        }
    }

    class LineNumberDebugTree : Timber.DebugTree() {
        override fun createStackElementTag(element: StackTraceElement): String {
            return "(${element.fileName}:${element.lineNumber})"
        }
    }
}