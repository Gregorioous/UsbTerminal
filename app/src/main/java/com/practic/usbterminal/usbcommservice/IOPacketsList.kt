package com.practic.usbterminal.usbcommservice

import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.math.min

@Suppress("DEPRECATION")
class IOPacketsList(
    private val maxPacketSize: Int = MAX_PACKET_SIZE,
    maxTotalSize: Int = MAX_TOTAL_SIZE,
    private val maxDurationOfSinglePacket: Int = MAX_DURATION_OF_SINGLE_PACKET
) {
    companion object {
        const val MAX_TOTAL_SIZE = 100_000
        const val MAX_PACKET_SIZE = 1024
        const val MAX_DURATION_OF_SINGLE_PACKET = 2000
    }

    enum class DataDirection {
        IN, OUT, UNDEFINED
    }

    private val packetsMap = HashMap<Int, IOPacket>(503)
    private var firstPacketSerialNumber = 0
    private var lastPacketSerialNumber = 0
    private var totalBytesIn = 0
    private var totalBytesOut = 0
    private var lastPacketStartTime = 0L
    private var lastDirection = DataDirection.UNDEFINED
    private var nConsecutivePackets = 0
    private var totalSizeUpperBound = maxTotalSize + MAX_PACKET_SIZE

    private val observable = IOPacketListObservable()

    @Suppress("DEPRECATION")
    private class IOPacketListObservable : Observable() {
        @Deprecated("Deprecated in Java")
        public override fun setChanged() {
            super.setChanged()
        }
    }

    fun addObserver(observer: Observer) {
        observable.addObserver(observer)
    }

    fun deleteObserver(observer: Observer) {
        observable.deleteObserver(observer)
    }

    class DataPointer(val packetSerialNumber: Int = 0, val offsetInPacket: Int = 0) {
        constructor(mdp: MutableDataPointer) : this(mdp.packetSerialNumber, mdp.offsetInPacket)

        operator fun compareTo(other: DataPointer): Int {
            return when {
                this.packetSerialNumber > other.packetSerialNumber -> 1
                this.packetSerialNumber < other.packetSerialNumber -> -1
                this.offsetInPacket > other.offsetInPacket -> 1
                this.offsetInPacket < other.offsetInPacket -> -1
                else -> 0
            }
        }
    }

    class MutableDataPointer(var packetSerialNumber: Int = 0, var offsetInPacket: Int = 0) {
        fun set(packetSerialNumber: Int, offsetInPacket: Int) {
            this.packetSerialNumber = packetSerialNumber
            this.offsetInPacket = offsetInPacket
        }

        fun set(dp: DataPointer) {
            this.packetSerialNumber = dp.packetSerialNumber
            this.offsetInPacket = dp.offsetInPacket
        }
    }

    private val resultDataPointer = MutableDataPointer()

    fun appendData(data: ByteArray, direction: DataDirection) {
        synchronized(this) {
            val now = System.currentTimeMillis()
            val itsALongTimeFromLastUpdate = now > (lastPacketStartTime + maxDurationOfSinglePacket)

            if (itsALongTimeFromLastUpdate || lastDirection != direction || packetsMap.isEmpty()) {
                if (packetsMap.isEmpty()) {
                    firstPacketSerialNumber = addPacket(
                        ++lastPacketSerialNumber,
                        currentTime = now,
                        direction = direction
                    )
                } else {
                    addPacket(++lastPacketSerialNumber, currentTime = now, direction = direction)
                }
                lastDirection = direction
            }

            var nBytesToCopy = data.size
            var offset = 0
            while (nBytesToCopy > 0) {
                val currentPacket = requireNotNull(packetsMap[lastPacketSerialNumber])
                when {
                    currentPacket.data.size + nBytesToCopy <= maxPacketSize -> {
                        currentPacket.append(data, offset, nBytesToCopy)
                        nBytesToCopy = 0
                    }

                    currentPacket.data.size == maxPacketSize -> {
                        addPacket(
                            ++lastPacketSerialNumber,
                            currentTime = now,
                            direction = direction
                        )
                    }

                    else -> {
                        val spaceLeft = maxPacketSize - currentPacket.data.size
                        val len = min(nBytesToCopy, spaceLeft)
                        currentPacket.append(data, offset, len)
                        offset += len
                        nBytesToCopy -= len
                    }
                }
            }
            when (direction) {
                DataDirection.IN -> totalBytesIn += data.size
                else -> totalBytesOut += data.size
            }
            trim(totalSizeUpperBound)
            observable.setChanged()

            if (nConsecutivePackets++ >= 4) {
                nConsecutivePackets = 0
                observable.notifyObservers()
            }
        }
    }

    fun inputPaused() {
        nConsecutivePackets = 0
        observable.notifyObservers()
    }

    fun setMaxSize(newMaxSize: Int) {
        val oldTotalSizeUpperBound = totalSizeUpperBound
        totalSizeUpperBound = newMaxSize + maxPacketSize
        if (totalSizeUpperBound < oldTotalSizeUpperBound) {
            trim(newMaxSize + maxPacketSize)
        }
    }

    fun trim(targetTotalSizeUpperBound: Int) {
        synchronized(this) {
            if (packetsMap.isEmpty()) return
            var totalSize = totalBytesIn + totalBytesOut
            while (totalSize >= targetTotalSizeUpperBound) {
                val firstPacket = packetsMap[firstPacketSerialNumber]
                if (firstPacket == null) {
                    Timber.wtf("trim(): firstPacket == null")
                    break
                }
                val firstPacketSize = firstPacket.data.size
                totalSize -= firstPacketSize
                when (firstPacket.direction) {
                    DataDirection.IN -> totalBytesIn -= firstPacketSize
                    else -> totalBytesOut -= firstPacketSize
                }
                packetsMap.remove(firstPacketSerialNumber)
                firstPacketSerialNumber++
            }
        }
    }

    fun processData(
        startAt: DataPointer,
        processor: (data: ByteArray, packetSerialNumber: Int, offset: Int, direction: DataDirection, timeStamp: Long) -> Unit
    ): DataPointer {
        synchronized(this) {
            if (lastPacketSerialNumber == 0) {
                return startAt
            }
            if (startAt.packetSerialNumber > lastPacketSerialNumber) {
                Timber.wtf("processData() bad startAt startAt.packetSerialNumber=${startAt.packetSerialNumber}  lastPacketSerialNumber=$lastPacketSerialNumber")
                Thread.dumpStack()
                return startAt
            }

            var packetSerialNumber = startAt.packetSerialNumber
            var offsetInPacket = startAt.offsetInPacket
            if (packetSerialNumber < firstPacketSerialNumber) {
                packetSerialNumber = firstPacketSerialNumber
                offsetInPacket = 0
            }
            var packet = packetsMap[packetSerialNumber]

            resultDataPointer.set(startAt)
            while (packet != null) {
                if (packet.data.size > offsetInPacket) {
                    processor(
                        packet.data,
                        packetSerialNumber,
                        offsetInPacket,
                        packet.direction,
                        packet.timeStamp
                    )
                }
                resultDataPointer.set(packetSerialNumber, packet.data.size)
                packetSerialNumber++
                packet = packetsMap[packetSerialNumber]
                offsetInPacket = 0
            }
            return DataPointer(resultDataPointer)
        }
    }

    fun getCurrentLocation() = DataPointer(
        lastPacketSerialNumber,
        packetsMap[lastPacketSerialNumber]?.data?.size ?: 0
    )

    fun getTotalSize(): Pair<Int, Int> {
        synchronized(this) {
            return Pair(totalBytesIn, totalBytesOut)
        }
    }

    fun clear() {
        synchronized(this) {
            packetsMap.clear()
            firstPacketSerialNumber = lastPacketSerialNumber
            totalBytesIn = 0
            totalBytesOut = 0
            lastPacketStartTime = 0L
            lastDirection = DataDirection.UNDEFINED
        }
    }

    private fun addPacket(
        packetSerialNumber: Int,
        currentTime: Long,
        direction: DataDirection
    ): Int {
        lastPacketStartTime = currentTime
        packetsMap[packetSerialNumber] =
            IOPacket(maxPacketSize, direction, packetSerialNumber, lastPacketStartTime)
        return packetSerialNumber
    }

    private class IOPacket(
        len: Int,
        val direction: DataDirection,
        @Suppress("unused") val serialNumber: Int,
        val timeStamp: Long,
    ) {
        val data: ByteArray
            get() = byteArrayOutputStream.toByteArray()

        private val byteArrayOutputStream = ByteArrayOutputStream(len)

        fun append(data: ByteArray, offset: Int, len: Int) {
            byteArrayOutputStream.write(data, offset, len)
        }
    }
}