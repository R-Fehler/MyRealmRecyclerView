package com.strong_weightlifting.strength_tracker_app.terminal_app

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.LeScanCallback
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.ListFragment
import androidx.preference.PreferenceManager
import com.strong_weightlifting.strength_tracker_app.R
import java.util.*

/**
 * show list of BLE devices
 */
class DevicesFragment : ListFragment() {
    private enum class ScanState {
        NONE, LESCAN, DISCOVERY, DISCOVERY_FINISHED
    }

    private var scanState =
        ScanState.NONE
    private val leScanStopHandler = Handler()
    private val leScanCallback: LeScanCallback
    private val discoveryBroadcastReceiver: BroadcastReceiver
    private val discoveryIntentFilter: IntentFilter
    private var menu: Menu? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val listItems = ArrayList<BluetoothDevice>()
    private var listAdapter: ArrayAdapter<BluetoothDevice>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        if (activity!!.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) bluetoothAdapter =
            BluetoothAdapter.getDefaultAdapter()
        listAdapter = object : ArrayAdapter<BluetoothDevice>(activity!!, 0, listItems) {
            override fun getView(position: Int, view: View?, parent: ViewGroup): View {
                var view = view
                val device = listItems[position]
                if (view == null) view =
                    activity!!.layoutInflater.inflate(R.layout.device_list_item, parent, false)
                val text1 = view!!.findViewById<TextView>(R.id.text1)
                val text2 = view.findViewById<TextView>(R.id.text2)
                if (device.name == null || device.name
                        .isEmpty()
                ) text1.text = "<unnamed>" else text1.text = device.name
                text2.text = device.address
                return view
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setListAdapter(null)
        val header = activity!!.layoutInflater.inflate(R.layout.device_list_header, null, false)
        listView.addHeaderView(header, null, false)
        setEmptyText("initializing...")
        (listView.emptyView as TextView).textSize = 18f
        setListAdapter(listAdapter)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_devices, menu)
        this.menu = menu
        if (bluetoothAdapter == null) {
            menu.findItem(R.id.bt_settings).isEnabled = false
            menu.findItem(R.id.ble_scan).isEnabled = false
        } else if (!bluetoothAdapter!!.isEnabled) {
            menu.findItem(R.id.ble_scan).isEnabled = false
        }
    }

    override fun onResume() {
        super.onResume()
        activity!!.registerReceiver(discoveryBroadcastReceiver, discoveryIntentFilter)
        if (bluetoothAdapter == null) {
            setEmptyText("<bluetooth LE not supported>")
        } else if (!bluetoothAdapter!!.isEnabled) {
            setEmptyText("<bluetooth is disabled>")
            if (menu != null) {
                listItems.clear()
                listAdapter!!.notifyDataSetChanged()
                menu!!.findItem(R.id.ble_scan).isEnabled = false
            }
        } else {
            setEmptyText("<use SCAN to refresh devices>")
            if (menu != null) menu!!.findItem(R.id.ble_scan).isEnabled = true
        }
    }

    override fun onPause() {
        super.onPause()
        stopScan()
        activity!!.unregisterReceiver(discoveryBroadcastReceiver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        menu = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        return if (id == R.id.ble_scan) {
            startScan()
            true
        } else if (id == R.id.ble_scan_stop) {
            stopScan()
            true
        } else if (id == R.id.bt_settings) {
            val intent = Intent()
            intent.action = Settings.ACTION_BLUETOOTH_SETTINGS
            startActivity(intent)
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    @SuppressLint("StaticFieldLeak") // AsyncTask needs reference to this fragment
    private fun startScan() {
        if (scanState != ScanState.NONE) return
        scanState =
            ScanState.LESCAN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity!!.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                scanState =
                    ScanState.NONE
                val builder = AlertDialog.Builder(activity)
                builder.setTitle(R.string.location_permission_title)
                builder.setMessage(R.string.location_permission_message)
                builder.setPositiveButton(
                    android.R.string.ok
                ) { dialog: DialogInterface?, which: Int ->
                    requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                        0
                    )
                }
                builder.show()
                return
            }
            val locationManager =
                activity!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            var locationEnabled = false
            try {
                locationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            } catch (ignored: Exception) {
            }
            try {
                locationEnabled = locationEnabled or locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            } catch (ignored: Exception) {
            }
            if (!locationEnabled) scanState =
                ScanState.DISCOVERY
            // Starting with Android 6.0 a bluetooth scan requires ACCESS_COARSE_LOCATION permission, but that's not all!
            // LESCAN also needs enabled 'location services', whereas DISCOVERY works without.
            // Most users think of GPS as 'location service', but it includes more, as we see here.
            // Instead of asking the user to enable something they consider unrelated,
            // we fall back to the older API that scans for bluetooth classic _and_ LE
            // sometimes the older API returns less results or slower
        }
        listItems.clear()
        listAdapter!!.notifyDataSetChanged()
        setEmptyText("<scanning...>")
        menu!!.findItem(R.id.ble_scan).isVisible = false
        menu!!.findItem(R.id.ble_scan_stop).isVisible = true
        if (scanState == ScanState.LESCAN) {
            leScanStopHandler.postDelayed({ stopScan() },
                LESCAN_PERIOD
            )
            object : AsyncTask<Void?, Void?, Void?>() {
                override fun doInBackground(params: Array<Void?>): Void? {
                    bluetoothAdapter!!.startLeScan(null, leScanCallback)
                    return null
                }
            }.execute() // start async to prevent blocking UI, because startLeScan sometimes take some seconds
        } else {
            bluetoothAdapter!!.startDiscovery()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        // ignore requestCode as there is only one in this fragment
        if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Handler(Looper.getMainLooper())
                .postDelayed({ startScan() }, 1) // run after onResume to avoid wrong empty-text
        } else {
            val builder = AlertDialog.Builder(activity)
            builder.setTitle(getText(R.string.location_denied_title))
            builder.setMessage(getText(R.string.location_denied_message))
            builder.setPositiveButton(android.R.string.ok, null)
            builder.show()
        }
    }

    private fun updateScan(device: BluetoothDevice) {
        if (scanState == ScanState.NONE) return
        if (listItems.indexOf(device) < 0) {
            listItems.add(device)
            listItems.sortWith(Comparator { a: BluetoothDevice, b: BluetoothDevice ->
                compareTo(a, b)
            })
            listAdapter!!.notifyDataSetChanged()
        }
    }

    private fun stopScan() {
        if (scanState == ScanState.NONE) return
        setEmptyText("<no bluetooth devices found>")
        if (menu != null) {
            menu!!.findItem(R.id.ble_scan).isVisible = true
            menu!!.findItem(R.id.ble_scan_stop).isVisible = false
        }
        when (scanState) {
            ScanState.LESCAN -> {
                leScanStopHandler.removeCallbacks { stopScan() }
                bluetoothAdapter!!.stopLeScan(leScanCallback)
            }
            ScanState.DISCOVERY -> bluetoothAdapter!!.cancelDiscovery()
            else -> {
            }
        }
        scanState =
            ScanState.NONE
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        stopScan()
        val device = listItems[position - 1]
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)
        val editor = sharedPreferences.edit()
        with(editor){
            putString("device",device.address)
            apply()
        }
        Toast.makeText(context,"device: ${device.address}",Toast.LENGTH_LONG).show()

//        val args = Bundle()
//        args.putString("device", device.address)

//        val fragment: Fragment =
//            TerminalFragment()
//        fragment.arguments = args
//        fragmentManager!!.beginTransaction().replace(R.id.fragment, fragment, "terminal").addToBackStack(null)
//            .commit()
    }

    companion object {
        private const val LESCAN_PERIOD: Long = 10000 // similar to bluetoothAdapter.startDiscovery

        /**
         * sort by name, then address. sort named devices first
         */
        fun compareTo(a: BluetoothDevice, b: BluetoothDevice): Int {
            val aValid = a.name != null && !a.name.isEmpty()
            val bValid = b.name != null && !b.name.isEmpty()
            if (aValid && bValid) {
                val ret = a.name.compareTo(b.name)
                return if (ret != 0) ret else a.address.compareTo(b.address)
            }
            if (aValid) return -1
            return if (bValid) +1 else a.address.compareTo(b.address)
        }
    }

    init {
        leScanCallback = LeScanCallback { device: BluetoothDevice?, rssi: Int, scanRecord: ByteArray? ->
            if (device != null && activity != null) {
                activity!!.runOnUiThread { updateScan(device) }
            }
        }
        discoveryBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == BluetoothDevice.ACTION_FOUND) {
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    if (device.type != BluetoothDevice.DEVICE_TYPE_CLASSIC && activity != null) {
                        activity!!.runOnUiThread { updateScan(device) }
                    }
                }
                if (intent.action == BluetoothAdapter.ACTION_DISCOVERY_FINISHED) {
                    scanState =
                        ScanState.DISCOVERY_FINISHED // don't cancel again
                    stopScan()
                }
            }
        }
        discoveryIntentFilter = IntentFilter()
        discoveryIntentFilter.addAction(BluetoothDevice.ACTION_FOUND)
        discoveryIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
    }
}