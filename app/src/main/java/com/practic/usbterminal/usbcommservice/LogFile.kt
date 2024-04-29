package com.practic.usbterminal.usbcommservice

import android.content.Context
import com.practic.usbterminal.R
import com.practic.usbterminal.settings.model.SettingsData
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success


class LogFile private constructor(
    val context: Context,
    val coroutineScope: CoroutineScope,
    val ioPacketsList: IOPacketsList,
) {
    private val outgoingMarkHead = "OUT{".toByteArray()
    private val outgoingMarkTail = "}".toByteArray()

    object Builder {
        fun getLogFileAsync(
            context: Context,
            coroutineScope: CoroutineScope,
            ioPacketsList: IOPacketsList,
        ): Deferred<Result<LogFile>> {
            return coroutineScope.async(Dispatchers.IO) {
                try {
                    val logFile = LogFile(context, coroutineScope, ioPacketsList)
                    success(logFile)
                } catch (e: Exception) {
                    failure(e)
                }
            }
        }
    }

    private var fileName: String = generateFileName()
    private val logFilesDir = getLogFilesDir(context)

    init {
        if (logFilesDir == null) throw Exception("Cannot create log-files directory")
    }

    private var file: File = File(logFilesDir, fileName)
    private var bos: BufferedOutputStream = BufferedOutputStream(FileOutputStream(file, true))
    private var settings: SettingsData? = null
    fun updateSettings(s: SettingsData?) {
        settings = s
    }

    private var nextByteToProcessInIOPacketsList = ioPacketsList.getCurrentLocation()
    private val nextByteToProcessInIOPacketsListLock = Object()

    private val ioPacketsListObserver = IOPacketsListObserver()

    init {
        try {
            val logStartMsg = context.getString(R.string.log_start_msg, currentDateStr)
            bos.write(logStartMsg.toByteArray(charset = Charsets.UTF_8))
            bos.flush()
            ioPacketsList.addObserver(ioPacketsListObserver) // If already observed by this observer, this is NOP.
        } catch (e: Exception) {
            bos.close()
            throw e
        }
    }

    fun close() {
        ioPacketsList.deleteObserver(ioPacketsListObserver)
        bos.close()
    }

    @Suppress("DEPRECATION")
    inner class IOPacketsListObserver : Observer {
        @Deprecated("Deprecated in Java")
        override fun update(o: Observable?, arg: Any?) {
            synchronized(nextByteToProcessInIOPacketsListLock) {
                coroutineScope.launch(context = Dispatchers.IO) {
                    nextByteToProcessInIOPacketsList = ioPacketsList.processData(
                        startAt = nextByteToProcessInIOPacketsList,
                        processor = ::handleNewIOBytes
                    )
                }
            }
        }
    }

    private var flushJob: Job? = null
    private fun handleNewIOBytes(
        data: ByteArray,
        packetSerialNumber: Int,
        offset: Int,
        dataDirection: IOPacketsList.DataDirection,
        timeStamp: Long
    ) {
        if (dataDirection == IOPacketsList.DataDirection.IN) {
            bos.write(data, offset, data.size - offset)
        } else if (settings?.alsoLogOutgoingData == true) {
            if (settings?.markLoggedOutgoingData == true) {
                bos.write(outgoingMarkHead)
                bos.write(data, offset, data.size - offset)
                bos.write(outgoingMarkTail)
            } else {
                bos.write(data, offset, data.size - offset)
            }
        }
        if (flushJob?.isActive != true) {
            flushJob = coroutineScope.launch(Dispatchers.IO) {
                delay(3000)
                bos.flush()
            }
        }
    }

    companion object {
        private const val DIRECTORY_NAME = "logs"
        private const val DATE_FORMAT = "yyyyMMdd_HHmmss"
        private val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.US)
        private val currentDateStr: String
            get() = dateFormat.format(Date(System.currentTimeMillis()))

        fun getLogFilesDir(context: Context): File? {
            val dir = File(context.filesDir, DIRECTORY_NAME)
            if (!dir.isDirectory) {
                if (!dir.mkdirs()) {
                    if (!dir.mkdirs()) {
                        Timber.e("Error in mkdirs() for '${dir.path}'")
                        return null
                    }
                }
            }
            return dir
        }

        fun generateFileName(): String {
            return "UsbTerminal_$currentDateStr.log"
        }
    }

}