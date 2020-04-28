package com.strong_weightlifting.strength_tracker_app

import PreCachingLayoutManager
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.strong_weightlifting.strength_tracker_app.bluetooth.BleAdapterService
import com.strong_weightlifting.strength_tracker_app.bluetooth.ConnectionStatusListener
import com.strong_weightlifting.strength_tracker_app.microbit_blue.Constants
import com.strong_weightlifting.strength_tracker_app.microbit_blue.MicroBit
import com.strong_weightlifting.strength_tracker_app.microbit_blue.Utility
import com.strong_weightlifting.strength_tracker_app.model.DataHelper
import com.strong_weightlifting.strength_tracker_app.model.Exercise
import com.strong_weightlifting.strength_tracker_app.model.ExerciseSet
import com.strong_weightlifting.strength_tracker_app.model.Training
import com.strong_weightlifting.strength_tracker_app.ui.recyclerview.ExerciseSetAdapter
import com.strong_weightlifting.strength_tracker_app.ui.recyclerview.ExercisesRecyclerViewAdapter
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_edit_training.*
import java.text.SimpleDateFormat
import java.util.*

/*TODO
In edit exercise activity noch oben edit Text mit Datum, Notizen dauer usw.
Edittraining activity in readonly Mode um alte Trainings anzuschauen ohne was ausversehen zu löschen oder zu ändern. Kann man edittext noneditable machen wenn ein intentcode gesetzt wird
Testen wie die edit Text persistent werden : ontextchanged updaterealm
Geplant vs done? : setze den Wert von planned in normale properties. Wenn gleich ist der Set wie geplant ausgeführt
Vorschläge in edittext aufgrund des letzten Trainings mit der known exercise. Query in oncreate für exercise mit known exercise Feld equalto this known exercise und Sets orderNo equalto this orderNo Else den vom letzten Satz

 */
class EditTrainingActivity : AppCompatActivity(), ConnectionStatusListener {

    private var realm: Realm? = null
    private var recyclerView: RecyclerView? = null
    private var menu: Menu? = null
    private var adapter: ExercisesRecyclerViewAdapter? = null
    private var training: Training? = null
    private var dateSetListener: DatePickerDialog.OnDateSetListener? = null
    private var timeSetListener: TimePickerDialog.OnTimeSetListener? = null



    private var deviceAddress: String? = null
    private var newline = "\n"

    private var bluetooth_le_adapter: BleAdapterService? = null

    private var exiting = false
    private var notifications_on = false

    // micro:bit event codes:
    // 9000 = 0x2823 (LE) = temperature alarm. Value=0 means OK, 1 means cold, 2 means hot
    // client event codes:
    // 9001=0x2923 (LE) = set lower limit, value is the limit value in celsius
    // 9002=0x2A23 (LE) = set upper limit, value is the limit value in celsius
    private val event_set_lower = byteArrayOf(0x29, 0x23, 0x00, 0x00) // event 9001

    private val event_set_upper = byteArrayOf(0x2A, 0x23, 0x00, 0x00) // event 9002


    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            Log.d(Constants.TAG, "onServiceConnected")
            notifications_on = false
            bluetooth_le_adapter = (service as BleAdapterService.LocalBinder).service
            bluetooth_le_adapter?.setActivityHandler(mMessageHandler)
            connectToDevice()


            setLowerLimit()
            setUpperLimit()


            if (bluetooth_le_adapter?.setNotificationsState(
                    Utility.normaliseUUID(BleAdapterService.EVENTSERVICE_SERVICE_UUID),
                    Utility.normaliseUUID(BleAdapterService.MICROBITEVENT_CHARACTERISTIC_UUID),
                    true
                )!!
            ) {
                showMsg(Utility.htmlColorGreen("micro:bit event notifications ON"))
            } else {
                showMsg(Utility.htmlColorRed("Failed to set micro:bit event notifications ON"))
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            bluetooth_le_adapter = null
        }
    }

    private fun connectToDevice() {
        status("Connecting to micro:bit")
        if (bluetooth_le_adapter!!.connect(MicroBit.getInstance().microbit_address)) {
        } else {
            status("onConnect: failed to connect")
        }
    }

    private fun refreshBluetoothServices() {
        if (MicroBit.getInstance().isMicrobit_connected) {
            val toast = Toast.makeText(this, "Refreshing GATT services", Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
            MicroBit.getInstance().resetAttributeTables()
            bluetooth_le_adapter!!.refreshDeviceCache()
            bluetooth_le_adapter!!.discoverServices()
        } else {
            val toast = Toast.makeText(this, "Request Ignored - Not Connected", Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
        }
    }

    private fun setUpperLimit() {
        event_set_upper[2] = 10.toByte()
        Log.d(Constants.TAG, Utility.byteArrayAsHexString(event_set_upper))
        bluetooth_le_adapter!!.writeCharacteristic(
            Utility.normaliseUUID(BleAdapterService.EVENTSERVICE_SERVICE_UUID),
            Utility.normaliseUUID(BleAdapterService.CLIENTEVENT_CHARACTERISTIC_UUID),
            event_set_upper
        )
    }

    private fun setLowerLimit() {
        event_set_lower[2] = 1.toByte()
        Log.d(Constants.TAG, Utility.byteArrayAsHexString(event_set_lower))
        bluetooth_le_adapter!!.writeCharacteristic(
            Utility.normaliseUUID(BleAdapterService.EVENTSERVICE_SERVICE_UUID),
            Utility.normaliseUUID(BleAdapterService.CLIENTEVENT_CHARACTERISTIC_UUID),
            event_set_lower
        )
    }

    private fun showMsg(msg: String) {
        Log.d(Constants.TAG, msg)

    }
    @SuppressLint("HandlerLeak")
    private val mMessageHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            val bundle: Bundle
            var service_uuid: String? = ""
            var characteristic_uuid: String? = ""
            var descriptor_uuid: String? = ""
            var b: ByteArray? = null
            val value_text: TextView? = null


            when (msg.what) {
                BleAdapterService.GATT_CONNECTED -> {
                    showMsg(Utility.htmlColorGreen("Connected"))
                    showMsg(Utility.htmlColorGreen("Discovering services..."))
                    bluetooth_le_adapter!!.discoverServices()
                }
                BleAdapterService.GATT_DISCONNECT -> {
                    showMsg(Utility.htmlColorRed("Disconnected"))
                }
                BleAdapterService.GATT_SERVICES_DISCOVERED -> {
                    Log.d(Constants.TAG, "XXXX Services discovered")
                    showMsg(Utility.htmlColorGreen("Ready"))
                    val slist =
                        bluetooth_le_adapter!!.supportedGattServices
                    for (svc in slist) {
                        Log.d(
                            Constants.TAG,
                            "UUID=" + svc.uuid.toString().toUpperCase() + " INSTANCE=" + svc.instanceId
                        )
                        MicroBit.getInstance().addService(svc)
                    }
                    MicroBit.getInstance().isMicrobit_services_discovered = true
                }

                BleAdapterService.GATT_CHARACTERISTIC_WRITTEN -> {
                    Log.d(Constants.TAG, "Handler received characteristic written result")
                    bundle = msg.data
                    service_uuid = bundle.getString(BleAdapterService.PARCEL_SERVICE_UUID)
                    characteristic_uuid = bundle.getString(BleAdapterService.PARCEL_CHARACTERISTIC_UUID)
                    Log.d(
                        Constants.TAG,
                        "characteristic $characteristic_uuid of service $service_uuid written OK"
                    )
                    showMsg(Utility.htmlColorGreen("Subscribed to micro:bit event"))
                }
                BleAdapterService.GATT_DESCRIPTOR_WRITTEN -> {
                    Log.d(Constants.TAG, "Handler received descriptor written result")
                    bundle = msg.data
                    service_uuid = bundle.getString(BleAdapterService.PARCEL_SERVICE_UUID)
                    characteristic_uuid = bundle.getString(BleAdapterService.PARCEL_CHARACTERISTIC_UUID)
                    descriptor_uuid = bundle.getString(BleAdapterService.PARCEL_DESCRIPTOR_UUID)
                    Log.d(
                        Constants.TAG,
                        "descriptor $descriptor_uuid of characteristic $characteristic_uuid of service $service_uuid written OK"
                    )
                    if (!exiting) {
                        showMsg(Utility.htmlColorGreen("Temperature Alarm notifications ON"))
                        notifications_on = true
                        bluetooth_le_adapter!!.writeCharacteristic(
                            Utility.normaliseUUID(BleAdapterService.EVENTSERVICE_SERVICE_UUID),
                            Utility.normaliseUUID(BleAdapterService.CLIENTREQUIREMENTS_CHARACTERISTIC_UUID),
                            Utility.leBytesFromTwoShorts(
                                Constants.MICROBIT_EVENT_TYPE_TEMPERATURE_ALARM,
                                Constants.MICROBIT_EVENT_VALUE_ANY
                            )
                        )
                    } else {
                        showMsg(Utility.htmlColorGreen("Temperature Alarm notifications OFF"))
                        notifications_on = false
                        finish()
                    }
                }
                BleAdapterService.NOTIFICATION_OR_INDICATION_RECEIVED -> {
                    bundle = msg.data
                    service_uuid = bundle.getString(BleAdapterService.PARCEL_SERVICE_UUID)
                    characteristic_uuid = bundle.getString(BleAdapterService.PARCEL_CHARACTERISTIC_UUID)
                    b = bundle.getByteArray(BleAdapterService.PARCEL_VALUE)
                    Log.d(Constants.TAG, "Value=" + Utility.byteArrayAsHexString(b))
                    if (characteristic_uuid.equals(
                            Utility.normaliseUUID(BleAdapterService.MICROBITEVENT_CHARACTERISTIC_UUID),
                            ignoreCase = true
                        )
                    ) {
                        val event_bytes = ByteArray(2)
                        val value_bytes = ByteArray(2)
                        System.arraycopy(b!!, 0, event_bytes, 0, 2)
                        System.arraycopy(b, 2, value_bytes, 0, 2)
                        val event: Short = Utility.shortFromLittleEndianBytes(event_bytes)
                        val value: Short = Utility.shortFromLittleEndianBytes(value_bytes)
                        Log.d(Constants.TAG, "Temperature Alarm received: event=$event value=$value")
                        if (event == Constants.MICROBIT_EVENT_TYPE_TEMPERATURE_ALARM) {
                            val indexEx=training?.exercises?.indexOfFirst {
                                it.sets.any { it.isDone.not() }
                            }

                            val firstExNotDone= training?.exercises?.get(indexEx!!)
                            val indexSet=firstExNotDone?.sets?.indexOfFirst{ it.isDone.not() }
                            val firstSetNotDone=firstExNotDone?.sets?.get(indexSet!!)
                            realm?.executeTransaction { firstSetNotDone?.reps=value.toInt() }
                            indexEx?.let { adapter?.notifyItemChanged(it) }
                        }
                    }
                }
                BleAdapterService.MESSAGE -> {
                    bundle = msg.data
                    val text = bundle.getString(BleAdapterService.PARCEL_TEXT)
                    showMsg(Utility.htmlColorRed(text))
                }
            }
        }
    }

    companion object {
        const val TRAINING_ID = "com.strong_weightlifting.strength_tracker_app.TRAINING_ID"
        const val KNOWNEXERCISE_ID = "com.strong_weightlifting.strength_tracker_app.KNOWNEXERCISE_ID"
        const val EXERCISE_ID = "com.strong_weightlifting.strength_tracker_app.EXERCISE_ID"
        const val VIEWKNOWNEXERCISES = "com.strong_weightlifting.strength_tracker_app.VIEWKNOWNEXERCISES"
        const val NOTES = "com.strong_weightlifting.strength_tracker_app.NOTES"
        const val REQUESTCODE_NOTE = 2
    }

    inner class TouchHelperCallbackParent internal constructor() :
        ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            onItemMove(viewHolder.adapterPosition, target.adapterPosition)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

        }

        override fun isLongPressDragEnabled(): Boolean {
            return true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_training)
        realm = Realm.getDefaultInstance()
        setSupportActionBar(editTrainingToolbar)
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(this)
        deviceAddress = sharedPreferences.getString("device","0") //TODO aus Settings holen
        if(deviceAddress=="0"){
            Toast.makeText(this,"no ble device paired",Toast.LENGTH_SHORT).show()
        }
        val microbit = MicroBit.getInstance()

        MicroBit.getInstance().microbit_name = "MyMicro"
        MicroBit.getInstance().microbit_address =deviceAddress
        MicroBit.getInstance().connection_status_listener = this


        // connect to the Bluetooth service
        val gattServiceIntent = Intent(this, BleAdapterService::class.java)
        bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)

        notesOfTrainingEditText.setOnTouchListener { v, event ->
            setLowerLimit()
            setUpperLimit()
            return@setOnTouchListener true
        }

        recyclerView = findViewById(R.id.recycler_view_exercises)

        training = realm!!.where(Training::class.java).equalTo(
            "uuid", intent.getLongExtra(
                TRAINING_ID, 0
            )
        )!!.findFirst()


        if (training!!.isRoutine) {
            if (!training!!.isDone) {
                title = "Create Routine"
                fab.show()
            } else {
                title = "Routine"
                fab.hide()
            }
        } else {
            if (training?.isDone!!.not()) {
                setTitle(R.string.title_activity_edit_training_newTraining)
                fab.show()
            } else {
                fab.hide()
            }
        }

        nameOfTrainingEditText.text.let {
            it.clear()
            it.insert(0, training?.name)
        }
        val myFormat = SimpleDateFormat("EEE, d MMM yyyy HH:mm", Locale.getDefault())

        editTraining_dateTextView.text = myFormat.format(training!!.date)
        editTraining_dateTextView.setOnClickListener {
            val cal = Calendar.getInstance()
            var date = Date()


            dateSetListener = DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                date = cal.time

                realm?.executeTransaction {
                    training!!.date = date
                    training!!.year = cal.get(Calendar.YEAR)
                    training!!.month = cal.get(Calendar.MONTH)
                    training!!.exercises.forEachIndexed { index, exercise ->
                        exercise.date = Date(date.time + index)
                    }
                }
                editTraining_dateTextView.text = myFormat.format(training!!.date)
            }


            val datePickerDialog = DatePickerDialog(
                this,
                dateSetListener,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )




            timeSetListener = TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                cal.set(Calendar.MINUTE, minute)
                date = cal.time
                realm?.executeTransaction {
                    training!!.date = date
                    training!!.year = cal.get(Calendar.YEAR)
                    training!!.month = cal.get(Calendar.MONTH)
                    training!!.exercises.forEachIndexed { index, exercise ->
                        exercise.date = Date(date.time + index)
                    }
                }
                editTraining_dateTextView.text = myFormat.format(training!!.date)

            }
            TimePickerDialog(
                this,
                timeSetListener,
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true
            ).show()

            datePickerDialog.show()

        }


        val distinctTrainings = realm?.where(Training::class.java)?.distinct("name")?.findAll()
        val nameArray = Array(distinctTrainings?.size!!) { i -> distinctTrainings[i]?.name }
        val nameAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, nameArray)!!
        nameOfTrainingEditText.setAdapter(nameAdapter)
        nameOfTrainingEditText.setOnTouchListener { v, event ->
            nameOfTrainingEditText.showDropDown()
            return@setOnTouchListener false
        }

        notesOfTrainingEditText.text.insert(0, training?.notes)

        nameOfTrainingEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s != null) {
                    if (s.toString() != training?.name) {
                        realm?.executeTransaction {
                            training?.name = s.toString()

                        }
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        notesOfTrainingEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s != null) {
                    if (s.toString() != training?.notes) {
                        realm?.executeTransaction {
                            training?.notes = s.toString()
                        }
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })





        fab.setOnClickListener { view ->
            realm?.let {

                val intent = Intent(this@EditTrainingActivity, KnownExerciseListActivity::class.java)
                startActivityForResult(intent, KnownExerciseListActivity.CHOOSEKNOWNEXERCISE)
            }


        }
        setUpRecyclerView()
    }

    /*
 * It is good practice to null the reference from the view to the adapter when it is no longer needed.
 * Because the <code>RealmRecyclerViewAdapter</code> registers itself as a <code>RealmResult.ChangeListener</code>
 * the view may still be reachable if anybody is still holding a reference to the <code>RealmResult>.
 */
    override fun onDestroy() {
        recyclerView!!.adapter = null
        realm!!.close()

        try {
            // may already have unbound. No API to check state so....
            unbindService(mServiceConnection)
        } catch (e: Exception) {
        }
         super.onDestroy()
    }
    override fun onStart() {
        super.onStart()

    }


    override fun onStop() {

        super.onStop()
    }






    private fun status(str: String) {
    Log.d(Constants.TAG,str)
    }


    private fun setUpRecyclerView() {
        adapter = realm!!.where(Training::class.java).equalTo(
            "uuid", intent.getLongExtra(
                TRAINING_ID, 0
            )
        )!!.findFirst()?.exercises?.let { ExercisesRecyclerViewAdapter(it) }
        adapter!!.setOnItemClickListener(object : ExercisesRecyclerViewAdapter.OnItemClickListener {
            override fun onItemClick(exercise: Exercise) {

            }
        })
        adapter!!.setAddClickListener(object : ExercisesRecyclerViewAdapter.OnAddClickListener {
            override fun onAddClick(uuid: Long, adapter: ExerciseSetAdapter) {
                realm?.let { DataHelper.addExerciseSet(it, uuid) }
            }

        })
        adapter!!.setOnNameClickListener(object : ExercisesRecyclerViewAdapter.OnNameClickListener {
            override fun onNameClick(exercise: Exercise) {
                val overviewIntent = Intent(this@EditTrainingActivity, KnownExerciseOverviewActivity::class.java)
                overviewIntent.putExtra(KNOWNEXERCISE_ID, exercise.knownExercise?.uuid)
                startActivity(overviewIntent)
//                intent.putExtra(EXERCISE_ID, exercise.uuid)
//                startActivityForResult(intent, KnownExerciseListActivity.CHANGEKNOWNEXERCISE)
            }
        })


//        recyclerView!!.layoutManager = LinearLayoutManager(this)
        val displayMetrics: DisplayMetrics = this.resources.displayMetrics
        val height = displayMetrics.heightPixels
        recyclerView!!.layoutManager = PreCachingLayoutManager(this, height * 2)

        recyclerView!!.adapter = adapter
        recyclerView!!.setHasFixedSize(true)
        recyclerView!!.setItemViewCacheSize(15)

        adapter!!.setOnNotesEditListener(object : ExercisesRecyclerViewAdapter.OnNotesEditListener {
            override fun onNotesEdit(exercise: Exercise) {
                val editIntent = Intent(this@EditTrainingActivity, EditTextActivity::class.java)
                editIntent.putExtra(EXERCISE_ID, exercise.uuid)
                editIntent.putExtra(NOTES, exercise.notes)
                startActivityForResult(editIntent, REQUESTCODE_NOTE)
            }
        })

        val touchHelperCallback = TouchHelperCallbackParent()
        val touchHelper = ItemTouchHelper(touchHelperCallback)
        touchHelper.attachToRecyclerView(recyclerView)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == KnownExerciseListActivity.CHOOSEKNOWNEXERCISE) {
            if (resultCode == Activity.RESULT_OK) {
                val knownExID = data?.getLongExtra(KNOWNEXERCISE_ID, 0)

                realm?.let {
                    if (knownExID != null) {

                        val training_uuid = intent.getLongExtra(TRAINING_ID, 0)
                        val ExID = DataHelper.addExercise(it, training_uuid)
                        DataHelper.addKnownExToExercise(it, knownExID, ExID)
                        DataHelper.addExerciseSet(it, ExID)


                    }
                }
            }
        }
        if (requestCode == KnownExerciseListActivity.CHANGEKNOWNEXERCISE) {
            if (resultCode == Activity.RESULT_OK) {
                val knownExID = data?.getLongExtra(KNOWNEXERCISE_ID, 0)
                val exerciseUUID = data?.getLongExtra(EXERCISE_ID, -1)

                realm?.let {
                    if (knownExID != null) {

                        if (exerciseUUID != null) {
                            DataHelper.addKnownExToExercise(it, knownExID, exerciseUUID)
                        }
                    }
                }
            }
        }

        if (requestCode == REQUESTCODE_NOTE) {
            if (resultCode == Activity.RESULT_OK) {
                val exerciseUUID = data?.getLongExtra(EXERCISE_ID, -1)
                realm?.let {
                    if (exerciseUUID != null) {
                        DataHelper.setNotesToExercise(it, exerciseUUID, data.getStringExtra(NOTES))
                    }
                }

            }
        }
        adapter?.updateData(adapter?.data)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menu = menu
        menuInflater.inflate(R.menu.edit_training_menu, menu)
        menu.setGroupVisible(R.id.group_normal_mode, true)
        if (training?.isDone?.not()!!) {
            menu.findItem(R.id.action_editItems).isVisible = false
        } else {
            menu.findItem(R.id.action_done).isVisible = true
        }

        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(this)
        val percentageIsActive = sharedPreferences.getBoolean("percentageInputActive", false)
        if (percentageIsActive) {
            menu.findItem(R.id.action_RoutineAsPercentage)?.isVisible = training?.isRoutine!!
            menu.findItem(R.id.action_RoutineAsPercentage)?.isChecked = training?.isRoutineWithPercentage!!
        } else {
            menu.findItem(R.id.action_RoutineAsPercentage)?.isVisible = false
        }




        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val returnIntent = Intent()
        when (id) {
            R.id.action_test ->{
                if (bluetooth_le_adapter?.setNotificationsState(
                        Utility.normaliseUUID(BleAdapterService.EVENTSERVICE_SERVICE_UUID),
                        Utility.normaliseUUID(BleAdapterService.MICROBITEVENT_CHARACTERISTIC_UUID),
                        true
                    )!!
                ) {
                    showMsg(Utility.htmlColorGreen("micro:bit event notifications ON"))
                } else {
                    showMsg(Utility.htmlColorRed("Failed to set micro:bit event notifications ON"))
                }
                return true
            }
            R.id.action_editItems -> {
                realm?.executeTransaction {
                    training?.isDone = false
                    adapter?.data?.forEach { exercise ->
                        exercise.sets.forEach { it.isDone = false }
                    }
                }
                title = "Edit Training Session"
                adapter?.updateData(adapter?.data)
                menu?.findItem(R.id.action_editItems)?.isVisible = false

                fab.show()
                return true
            }

            R.id.action_done -> {
                realm?.executeTransaction {
                    adapter?.data?.forEach { exercise ->
                        val rmSetEpley = exercise.sets.maxBy { ExerciseSet.epleyValue(it) }
                        rmSetEpley?.let { set ->

                            val epValue = set.let { it1 -> ExerciseSet.epleyValue(it1) }
                            val epWeight = set.weight
                            val epReps = set.reps
                            if (exercise.knownExercise?.prCalculated!! < epValue) {
                                exercise.knownExercise!!.prCalculated = epValue
                                exercise.knownExercise!!.prWeight = epWeight
                                exercise.knownExercise!!.repsAtPRWeight = epReps
                                exercise.knownExercise!!.dateOfPR = training?.date!!
                                set.isPR = true
                            }

                            exercise.sets.forEach { it.isDone = true }
                        }
                    }

                }

                realm?.executeTransaction { training?.isDone = true }
                returnIntent.putExtra(TRAINING_ID, -1)
                setResult(Activity.RESULT_OK, returnIntent)
                super.onBackPressed()
                return true
            }

            R.id.action_delete -> {
                if (training?.exercises?.size!! < 1) {
                    realm?.let { DataHelper.deleteTraining(it, training?.uuid!!) }
                    returnIntent.putExtra(TRAINING_ID, -1)
                    setResult(Activity.RESULT_OK, returnIntent)
                    finish()
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("WARNING: Delete all Data?")
                        .setMessage("are you sure?")
                        .setPositiveButton(android.R.string.yes) { dialog, which ->
                            realm?.let { DataHelper.deleteTraining(it, training?.uuid!!) }
                            returnIntent.putExtra(TRAINING_ID, -1)
                            setResult(Activity.RESULT_OK, returnIntent)
                            finish()
                        }

                        .setNegativeButton(android.R.string.no, null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show()
                }
                return true
            }

            R.id.action_RoutineAsPercentage -> {
                item.isChecked = item.isChecked.not()
                realm?.executeTransaction { training?.isRoutineWithPercentage = item.isChecked }
                adapter?.notifyDataSetChanged()
                return true
            }


            else -> return super.onOptionsItemSelected(item)
        }
    }

    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        realm?.executeTransaction {

            training?.exercises!![fromPosition] = training!!.exercises[toPosition]
                .also { training!!.exercises[toPosition] = training!!.exercises[fromPosition] }
        }
        adapter?.notifyItemMoved(fromPosition, toPosition)
        return true
    }

    override fun onResume() {
        super.onResume()
        adapter?.updateData(adapter?.data)
    }

    override fun onBackPressed() {
        training?.let {
            if (it.isDone) {
                Log.d(
                    Constants.TAG,
                    "onBackPressed connected=" + MicroBit.getInstance().isMicrobit_connected
                        .toString() + " notifications_on=" + notifications_on.toString() + " exiting=" + exiting
                )
                if (MicroBit.getInstance().isMicrobit_connected && notifications_on) {
                    bluetooth_le_adapter!!.setNotificationsState(
                        Utility.normaliseUUID(BleAdapterService.EVENTSERVICE_SERVICE_UUID),
                        Utility.normaliseUUID(BleAdapterService.MICROBITEVENT_CHARACTERISTIC_UUID),
                        false
                    )
                }
                exiting = true
                if (!MicroBit.getInstance().isMicrobit_connected) {
                    try {
                        // may already have unbound. No API to check state so....
                        unbindService(mServiceConnection)
                    } catch (e: Exception) {
                    }
                    finish()
                }
                super.onBackPressed()
                return
            }
        }
        val returnIntent = Intent()
        returnIntent.putExtra(TRAINING_ID, intent.getLongExtra(TRAINING_ID, 0))
        setResult(Activity.RESULT_OK, returnIntent)

        Log.d(
            Constants.TAG,
            "onBackPressed connected=" + MicroBit.getInstance().isMicrobit_connected
                .toString() + " notifications_on=" + notifications_on.toString() + " exiting=" + exiting
        )
        if (MicroBit.getInstance().isMicrobit_connected && notifications_on) {
            bluetooth_le_adapter!!.setNotificationsState(
                Utility.normaliseUUID(BleAdapterService.EVENTSERVICE_SERVICE_UUID),
                Utility.normaliseUUID(BleAdapterService.MICROBITEVENT_CHARACTERISTIC_UUID),
                false
            )
        }
        exiting = true
        if (!MicroBit.getInstance().isMicrobit_connected) {
            try {
                // may already have unbound. No API to check state so....
                unbindService(mServiceConnection)
            } catch (e: Exception) {
            }
            finish()
        }
        super.onBackPressed()
    }

    override fun serviceDiscoveryStatusChanged(new_state: Boolean) {
    }

    override fun connectionStatusChanged(connected: Boolean) {
        if (connected) {
            status(("Connected"))
        } else {
            status(("Disconnected"))
        }    }


}


