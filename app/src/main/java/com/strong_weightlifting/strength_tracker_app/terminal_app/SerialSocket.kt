package com.strong_weightlifting.strength_tracker_app.terminal_app

import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.strong_weightlifting.strength_tracker_app.R
import java.io.IOException
import java.util.*

/**
 * wrap BLE communication into socket like class
 * - connect, disconnect and write as methods,
 * - read + status is returned by SerialListener
 */
internal class SerialSocket : BluetoothGattCallback() {
    /**
     * delegate device specific behaviour to inner class
     */
    private open inner class DeviceDelegate {
        open fun connectCharacteristics(s: BluetoothGattService): Boolean {
            return true
        }

        // following methods only overwritten for Telit devices
        open fun onDescriptorWrite(g: BluetoothGatt, d: BluetoothGattDescriptor, status: Int) { /*nop*/
        }

        open fun onCharacteristicChanged(g: BluetoothGatt?, c: BluetoothGattCharacteristic) { /*nop*/
        }

        open fun onCharacteristicWrite(
            g: BluetoothGatt?,
            c: BluetoothGattCharacteristic,
            status: Int
        ) { /*nop*/
        }

        open fun canWrite(): Boolean {
            return true
        }

        open fun disconnect() { /*nop*/
        }
    }

    private val writeBuffer: ArrayList<ByteArray?>
    private val pairingIntentFilter: IntentFilter
    private val pairingBroadcastReceiver: BroadcastReceiver
    private val disconnectBroadcastReceiver: BroadcastReceiver
    private var context: Context? = null
    private var listener: SerialListener? = null
    private var delegate: DeviceDelegate? = null
    private var device: BluetoothDevice? = null
    private var gatt: BluetoothGatt? = null
    private var readCharacteristic: BluetoothGattCharacteristic? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var writePending = false
    private var canceled = false
    private var connected = false
    private var payloadSize = DEFAULT_MTU - 3
    fun disconnect() {
        Log.d(TAG, "disconnect")
        listener = null // ignore remaining data and errors
        device = null
        canceled = true
        synchronized(writeBuffer) {
            writePending = false
            writeBuffer.clear()
        }
        readCharacteristic = null
        writeCharacteristic = null
        if (delegate != null) delegate!!.disconnect()
        if (gatt != null) {
            Log.d(TAG, "gatt.disconnect")
            gatt!!.disconnect()
            Log.d(TAG, "gatt.close")
            try {
                gatt!!.close()
            } catch (ignored: Exception) {
            }
            gatt = null
            connected = false
        }
        try {
            context!!.unregisterReceiver(pairingBroadcastReceiver)
        } catch (ignored: Exception) {
        }
        try {
            context!!.unregisterReceiver(disconnectBroadcastReceiver)
        } catch (ignored: Exception) {
        }
    }

    /**
     * connect-success and most connect-errors are returned asynchronously to listener
     */
    @Throws(IOException::class)
    fun connect(context: Context?, listener: SerialListener?, device: BluetoothDevice) {
        if (connected || gatt != null) throw IOException("already connected")
        canceled = false
        this.context = context
        this.listener = listener
        this.device = device
        context!!.registerReceiver(
            disconnectBroadcastReceiver,
            IntentFilter(Constants.INTENT_ACTION_DISCONNECT)
        )
        Log.d(TAG, "connect $device")
        context.registerReceiver(pairingBroadcastReceiver, pairingIntentFilter)
        gatt = if (Build.VERSION.SDK_INT < 23) {
            Log.d(TAG, "connectGatt")
            device.connectGatt(context, false, this)
        } else {
            Log.d(TAG, "connectGatt,LE")
            device.connectGatt(context, false, this, BluetoothDevice.TRANSPORT_LE)
        }
        if (gatt == null) throw IOException("connectGatt failed")
        // continues asynchronously in onPairingBroadcastReceive() and onConnectionStateChange()
    }

    private fun onPairingBroadcastReceive(context: Context, intent: Intent) {
        // for ARM Mbed, Microbit, ... use pairing from Android bluetooth settings
        // for HM10-clone, ... pairing is initiated here
        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
        if (device == null || device != this.device) return
        when (intent.action) {
            BluetoothDevice.ACTION_PAIRING_REQUEST -> {
                val pairingVariant = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, -1)
                Log.d(TAG, "pairing request $pairingVariant")
                onSerialConnectError(IOException(context.getString(R.string.pairing_request)))
            }
            BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
                val previousBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1)
                Log.d(TAG, "bond state $previousBondState->$bondState")
            }
            else -> Log.d(TAG, "unknown broadcast " + intent.action)
        }
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        // status directly taken from gat_api.h, e.g. 133=0x85=GATT_ERROR ~= timeout
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            Log.d(TAG, "connect status $status, discoverServices")
            if (!gatt.discoverServices()) onSerialConnectError(IOException("discoverServices failed"))
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            if (connected) onSerialIoError(IOException("gatt status $status")) else onSerialConnectError(
                IOException(
                    "gatt status $status"
                )
            )
        } else {
            Log.d(TAG, "unknown connect state $newState $status")
        }
        // continues asynchronously in onServicesDiscovered()
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        Log.d(TAG, "servicesDiscovered, status $status")
        if (canceled) return
        connectCharacteristics1(gatt)
    }

    private fun connectCharacteristics1(gatt: BluetoothGatt) {
        var sync = true
        writePending = false
        for (gattService in gatt.services) {
            if (gattService.uuid == BLUETOOTH_LE_CC254X_SERVICE) delegate =
                Cc245XDelegate()
            if (gattService.uuid == BLUETOOTH_LE_RN4870_SERVICE) delegate =
                Rn4870Delegate()
            if (gattService.uuid == BLUETOOTH_LE_NRF_SERVICE) delegate = NrfDelegate()
            if (gattService.uuid == BLUETOOTH_LE_TIO_SERVICE) delegate = TelitDelegate()
            if (delegate != null) {
                sync = delegate!!.connectCharacteristics(gattService)
                break
            }
        }
        if (canceled) return
        if (delegate == null || readCharacteristic == null || writeCharacteristic == null) {
            for (gattService in gatt.services) {
                Log.d(TAG, "service " + gattService.uuid)
                for (characteristic in gattService.characteristics) Log.d(
                    TAG,
                    "characteristic " + characteristic.uuid
                )
            }
            onSerialConnectError(IOException("no serial profile found"))
            return
        }
        if (sync) connectCharacteristics2(gatt)
    }

    private fun connectCharacteristics2(gatt: BluetoothGatt) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.d(TAG, "request max MTU")
            if (!gatt.requestMtu(MAX_MTU)) onSerialConnectError(IOException("request MTU failed"))
            // continues asynchronously in onMtuChanged
        } else {
            connectCharacteristics3(gatt)
        }
    }

    override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
        Log.d(TAG, "mtu size $mtu, status=$status")
        if (status == BluetoothGatt.GATT_SUCCESS) {
            payloadSize = mtu - 3
            Log.d(TAG, "payload size $payloadSize")
        }
        connectCharacteristics3(gatt)
    }

    private fun connectCharacteristics3(gatt: BluetoothGatt) {
        val writeProperties = writeCharacteristic!!.properties
        if (writeProperties and BluetoothGattCharacteristic.PROPERTY_WRITE +  // Microbit,HM10-clone have WRITE
            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE == 0
        ) { // HM10,TI uart,Telit have only WRITE_NO_RESPONSE
            onSerialConnectError(IOException("write characteristic not writable"))
            return
        }
        if (!gatt.setCharacteristicNotification(readCharacteristic, true)) {
            onSerialConnectError(IOException("no notification for read characteristic"))
            return
        }
        val readDescriptor =
            readCharacteristic!!.getDescriptor(BLUETOOTH_LE_CCCD)
        if (readDescriptor == null) {
            onSerialConnectError(IOException("no CCCD descriptor for read characteristic"))
            return
        }
        val readProperties = readCharacteristic!!.properties
        if (readProperties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
            Log.d(TAG, "enable read indication")
            readDescriptor.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        } else if (readProperties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
            Log.d(TAG, "enable read notification")
            readDescriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        } else {
            onSerialConnectError(IOException("no indication/notification for read characteristic ($readProperties)"))
            return
        }
        Log.d(TAG, "writing read characteristic descriptor")
        if (!gatt.writeDescriptor(readDescriptor)) {
            onSerialConnectError(IOException("read characteristic CCCD descriptor not writable"))
        }
        // continues asynchronously in onDescriptorWrite()
    }

    override fun onDescriptorWrite(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        status: Int
    ) {
        delegate!!.onDescriptorWrite(gatt, descriptor, status)
        if (canceled) return
        if (descriptor.characteristic === readCharacteristic) {
            Log.d(TAG, "writing read characteristic descriptor finished, status=$status")
            if (status != BluetoothGatt.GATT_SUCCESS) {
                onSerialConnectError(IOException("write descriptor failed"))
            } else {
                // onCharacteristicChanged with incoming data can happen after writeDescriptor(ENABLE_INDICATION/NOTIFICATION)
                // before confirmed by this method, so receive data can be shown before device is shown as 'Connected'.
                onSerialConnect()
                connected = true
                Log.d(TAG, "connected")
            }
        }
    }

    /*
     * read
     */
    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        if (canceled) return
        delegate!!.onCharacteristicChanged(gatt, characteristic)
        if (canceled) return
        if (characteristic === readCharacteristic) { // NOPMD - test object identity
            val data = readCharacteristic!!.value// data stored in BluetoothGattCharacteristic readCharacteristic
            onSerialRead(data) // call handler interface in line 427
            Log.d(TAG, "read, len=" + data.size)
        }
    }

    /*
     * write
     */
    @Throws(IOException::class)
    fun write(data: ByteArray) {
        if (canceled || !connected || writeCharacteristic == null) throw IOException("not connected")
        var data0: ByteArray?
        synchronized(writeBuffer) {
            data0 = if (data.size <= payloadSize) {
                data
            } else {
                Arrays.copyOfRange(data, 0, payloadSize)
            }
            if (!writePending && writeBuffer.isEmpty() && delegate!!.canWrite()) {
                writePending = true
            } else {
                writeBuffer.add(data0)
                Log.d(TAG, "write queued, len=" + data0!!.size)
                data0 = null
            }
            if (data.size > payloadSize) {
                for (i in 1 until (data.size + payloadSize - 1) / payloadSize) {
                    val from = i * payloadSize
                    val to = Math.min(from + payloadSize, data.size)
                    writeBuffer.add(Arrays.copyOfRange(data, from, to))
                    Log.d(TAG, "write queued, len=" + (to - from))
                }
            }
        }
        if (data0 != null) {
            writeCharacteristic!!.value = data0
            if (!gatt!!.writeCharacteristic(writeCharacteristic)) {
                onSerialIoError(IOException("write failed"))
            } else {
                Log.d(TAG, "write started, len=" + data0!!.size)
            }
        }
        // continues asynchronously in onCharacteristicWrite()
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        if (canceled || !connected || writeCharacteristic == null) return
        if (status != BluetoothGatt.GATT_SUCCESS) {
            onSerialIoError(IOException("write failed"))
            return
        }
        delegate!!.onCharacteristicWrite(gatt, characteristic, status)
        if (canceled) return
        if (characteristic === writeCharacteristic) { // NOPMD - test object identity
            Log.d(TAG, "write finished, status=$status")
            writeNext()
        }
    }

    private fun writeNext() {
        var data: ByteArray?
        synchronized(writeBuffer) {
            if (!writeBuffer.isEmpty() && delegate!!.canWrite()) {
                writePending = true
                data = writeBuffer.removeAt(0)
            } else {
                writePending = false
                data = null
            }
        }
        if (data != null) {
            writeCharacteristic!!.value = data
            if (!gatt!!.writeCharacteristic(writeCharacteristic)) {
                onSerialIoError(IOException("write failed"))
            } else {
                Log.d(TAG, "write started, len=" + data!!.size)
            }
        }
    }

    /**
     * SerialListener
     */
    private fun onSerialConnect() {
        if (listener != null) listener!!.onSerialConnect()
    }

    private fun onSerialConnectError(e: Exception) {
        canceled = true
        if (listener != null) listener!!.onSerialConnectError(e)
    }

    private fun onSerialRead(data: ByteArray) {
        if (listener != null) listener!!.onSerialRead(data)//// von socket an entweder --> service oder fragment
    }

    private fun onSerialIoError(e: Exception) {
        writePending = false
        canceled = true
        if (listener != null) listener!!.onSerialIoError(e)
    }

    /**
     * device delegates
     */
    private inner class Cc245XDelegate : DeviceDelegate() {
        override fun connectCharacteristics(gattService: BluetoothGattService): Boolean {
            Log.d(TAG, "service cc254x uart")
            readCharacteristic = gattService.getCharacteristic(BLUETOOTH_LE_CC254X_CHAR_RW)
            writeCharacteristic = gattService.getCharacteristic(BLUETOOTH_LE_CC254X_CHAR_RW)
            return true
        }
    }

    private inner class Rn4870Delegate : DeviceDelegate() {
        override fun connectCharacteristics(gattService: BluetoothGattService): Boolean {
            Log.d(TAG, "service rn4870 uart")
            readCharacteristic = gattService.getCharacteristic(BLUETOOTH_LE_RN4870_CHAR_RW)
            writeCharacteristic = gattService.getCharacteristic(BLUETOOTH_LE_RN4870_CHAR_RW)
            return true
        }
    }

    private inner class NrfDelegate : DeviceDelegate() {
        override fun connectCharacteristics(gattService: BluetoothGattService): Boolean {
            Log.d(TAG, "service nrf uart")
            val rw2 =
                gattService.getCharacteristic(BLUETOOTH_LE_NRF_CHAR_RW2)
            val rw3 =
                gattService.getCharacteristic(BLUETOOTH_LE_NRF_CHAR_RW3)
            if (rw2 != null && rw3 != null) {
                val rw2prop = rw2.properties
                val rw3prop = rw3.properties
                val rw2write = rw2prop and BluetoothGattCharacteristic.PROPERTY_WRITE != 0
                val rw3write = rw3prop and BluetoothGattCharacteristic.PROPERTY_WRITE != 0
                Log.d(TAG, "characteristic properties $rw2prop/$rw3prop")
                if (rw2write && rw3write) {
                    onSerialConnectError(IOException("multiple write characteristics ($rw2prop/$rw3prop)"))
                } else if (rw2write) {
                    writeCharacteristic = rw2
                    readCharacteristic = rw3
                } else if (rw3write) {
                    writeCharacteristic = rw3
                    readCharacteristic = rw2
                } else {
                    onSerialConnectError(IOException("no write characteristic ($rw2prop/$rw3prop)"))
                }
            }
            return true
        }
    }

    private inner class TelitDelegate : DeviceDelegate() {
        private var readCreditsCharacteristic: BluetoothGattCharacteristic? = null
        private var writeCreditsCharacteristic: BluetoothGattCharacteristic? = null
        private var readCredits = 0
        private var writeCredits = 0
        override fun connectCharacteristics(gattService: BluetoothGattService): Boolean {
            Log.d(TAG, "service telit tio 2.0")
            readCredits = 0
            writeCredits = 0
            readCharacteristic = gattService.getCharacteristic(BLUETOOTH_LE_TIO_CHAR_RX)
            writeCharacteristic = gattService.getCharacteristic(BLUETOOTH_LE_TIO_CHAR_TX)
            readCreditsCharacteristic =
                gattService.getCharacteristic(BLUETOOTH_LE_TIO_CHAR_RX_CREDITS)
            writeCreditsCharacteristic =
                gattService.getCharacteristic(BLUETOOTH_LE_TIO_CHAR_TX_CREDITS)
            if (readCharacteristic == null) {
                onSerialConnectError(IOException("read characteristic not found"))
                return false
            }
            if (writeCharacteristic == null) {
                onSerialConnectError(IOException("write characteristic not found"))
                return false
            }
            if (readCreditsCharacteristic == null) {
                onSerialConnectError(IOException("read credits characteristic not found"))
                return false
            }
            if (writeCreditsCharacteristic == null) {
                onSerialConnectError(IOException("write credits characteristic not found"))
                return false
            }
            if (!gatt!!.setCharacteristicNotification(readCreditsCharacteristic, true)) {
                onSerialConnectError(IOException("no notification for read credits characteristic"))
                return false
            }
            val readCreditsDescriptor =
                readCreditsCharacteristic!!.getDescriptor(BLUETOOTH_LE_CCCD)
            if (readCreditsDescriptor == null) {
                onSerialConnectError(IOException("no CCCD descriptor for read credits characteristic"))
                return false
            }
            readCreditsDescriptor.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            Log.d(TAG, "writing read credits characteristic descriptor")
            if (!gatt!!.writeDescriptor(readCreditsDescriptor)) {
                onSerialConnectError(IOException("read credits characteristic CCCD descriptor not writable"))
                return false
            }
            Log.d(TAG, "writing read credits characteristic descriptor")
            return false
            // continues asynchronously in connectCharacteristics2
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (descriptor.characteristic === readCreditsCharacteristic) {
                Log.d(
                    TAG,
                    "writing read credits characteristic descriptor finished, status=$status"
                )
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    onSerialConnectError(IOException("write credits descriptor failed"))
                } else {
                    connectCharacteristics2(gatt)
                }
            }
            if (descriptor.characteristic === readCharacteristic) {
                Log.d(TAG, "writing read characteristic descriptor finished, status=$status")
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    readCharacteristic!!.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    writeCharacteristic!!.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    grantReadCredits()
                    // grantReadCredits includes gatt.writeCharacteristic(writeCreditsCharacteristic)
                    // but we do not have to wait for confirmation, as it is the last write of connect phase.
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic === readCreditsCharacteristic) { // NOPMD - test object identity
                val newCredits = readCreditsCharacteristic!!.value[0].toInt()
                synchronized(writeBuffer) { writeCredits += newCredits }
                Log.d(TAG, "got write credits +$newCredits =$writeCredits")
                if (!writePending && !writeBuffer.isEmpty()) {
                    Log.d(TAG, "resume blocked write")
                    writeNext()
                }
            }
            if (characteristic === readCharacteristic) { // NOPMD - test object identity
                grantReadCredits()
                Log.d(TAG, "read, credits=$readCredits")
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (characteristic === writeCharacteristic) { // NOPMD - test object identity
                synchronized(writeBuffer) { if (writeCredits > 0) writeCredits -= 1 }
                Log.d(TAG, "write finished, credits=$writeCredits")
            }
            if (characteristic === writeCreditsCharacteristic) { // NOPMD - test object identity
                Log.d(TAG, "write credits finished, status=$status")
            }
        }

        override fun canWrite(): Boolean {
            if (writeCredits > 0) return true
            Log.d(TAG, "no write credits")
            return false
        }

        override fun disconnect() {
            readCreditsCharacteristic = null
            writeCreditsCharacteristic = null
        }

        private fun grantReadCredits() {
            val minReadCredits = 16
            val maxReadCredits = 64
            if (readCredits > 0) readCredits -= 1
            if (readCredits <= minReadCredits) {
                val newCredits = maxReadCredits - readCredits
                readCredits += newCredits
                val data = byteArrayOf(newCredits.toByte())
                Log.d(TAG, "grant read credits +$newCredits =$readCredits")
                writeCreditsCharacteristic!!.value = data
                if (!gatt!!.writeCharacteristic(writeCreditsCharacteristic)) {
                    if (connected) onSerialIoError(IOException("write read credits failed")) else onSerialConnectError(
                        IOException("write read credits failed")
                    )
                }
            }
        }
    }

    companion object {
        private val BLUETOOTH_LE_CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private val BLUETOOTH_LE_CC254X_SERVICE = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        private val BLUETOOTH_LE_CC254X_CHAR_RW = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
        private val BLUETOOTH_LE_NRF_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        private val BLUETOOTH_LE_NRF_CHAR_RW2 =
            UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e") // read on microbit, write on adafruit
        private val BLUETOOTH_LE_NRF_CHAR_RW3 = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
        private val BLUETOOTH_LE_RN4870_SERVICE = UUID.fromString("49535343-FE7D-4AE5-8FA9-9FAFD205E455")
        private val BLUETOOTH_LE_RN4870_CHAR_RW = UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616")

        // https://play.google.com/store/apps/details?id=com.telit.tiosample
        // https://www.telit.com/wp-content/uploads/2017/09/TIO_Implementation_Guide_r6.pdf
        private val BLUETOOTH_LE_TIO_SERVICE = UUID.fromString("0000FEFB-0000-1000-8000-00805F9B34FB")
        private val BLUETOOTH_LE_TIO_CHAR_TX = UUID.fromString("00000001-0000-1000-8000-008025000000") // WNR
        private val BLUETOOTH_LE_TIO_CHAR_RX = UUID.fromString("00000002-0000-1000-8000-008025000000") // N
        private val BLUETOOTH_LE_TIO_CHAR_TX_CREDITS =
            UUID.fromString("00000003-0000-1000-8000-008025000000") // W
        private val BLUETOOTH_LE_TIO_CHAR_RX_CREDITS =
            UUID.fromString("00000004-0000-1000-8000-008025000000") // I
        private const val MAX_MTU =
            512 // BLE standard does not limit, some BLE 4.2 devices support 251, various source say that Android has max 512
        private const val DEFAULT_MTU = 23
        private const val TAG = "SerialSocket"
    }

    init {
        writeBuffer = ArrayList()
        pairingIntentFilter = IntentFilter()
        pairingIntentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        pairingIntentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
        pairingBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                onPairingBroadcastReceive(context, intent)
            }
        }
        disconnectBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (listener != null) listener!!.onSerialIoError(IOException("background disconnect"))
                disconnect() // disconnect now, else would be queued until UI re-attached
            }
        }
    }
}