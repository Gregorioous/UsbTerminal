package com.practic.usbterminal.usbcommservice

data class SerialCommunicationParams(
    val baudRate: Int,
    val dataBits: Int,
    val stopBits: Int,
    val parity: Int,
)