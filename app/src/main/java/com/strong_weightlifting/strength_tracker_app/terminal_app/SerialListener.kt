package com.strong_weightlifting.strength_tracker_app.terminal_app

interface SerialListener {
    fun onSerialConnect()
    fun onSerialConnectError(e: Exception)
    fun onSerialRead(data: ByteArray?)
    fun onSerialIoError(e: Exception)
}