package com.strong_weightlifting.strength_tracker_app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.strong_weightlifting.strength_tracker_app.model.*
import com.strong_weightlifting.strength_tracker_app.ui.recyclerview.TrainingRecyclerViewAdapter
import io.realm.Realm
import io.realm.RealmList
import io.realm.Sort
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/*TODO
In Traininglist suchen: Namen, known exercise, Monat/Jahr,
Training(Planung) aus txt parsen und verschicken können. Zb für coaches bzw Freunde. Gleiche Schnittstelle wie gadget?? Oder kompakte Schreibweise?
Trainingsplanung zb wöchentlich kopieren. Mit neuen RealmObjects die aber die selben properties ausser primary key haben
Traininglist: die Views der einzelnen Trainings anpassen um Datum, Wochentag, Uhrzeit, Dauer, exercises und Kurzfassung der Sets anzuzeigen. Dazu Gesamtvolumen und PRs
Bei add Training neuen erstellen und gleich edit Training acitivity mit der uuid starten
Training als geplant in Traininglist activity markieren.. Dient später als Plan für gadget. (Das mit dem aktuellsten Datum)

 */


class MainActivity : AppCompatActivity() {
    private val TAG: String = MainActivity::class.java.name
    private var realm: Realm? = null
    private var recyclerView: RecyclerView? = null
    private var menu: Menu? = null
    private var adapter: TrainingRecyclerViewAdapter? = null
    private var fabAddTraining: FloatingActionButton? = null
    private var fabResumeTraining: FloatingActionButton? = null
    private var dateSetListener: DatePickerDialog.OnDateSetListener? = null
    private var activeTrainingUUID: Long = -1
    private lateinit var inputPFD: ParcelFileDescriptor


    companion object {
        val REQUEST_STRONG_CSV_CODE = 0
        val REQUESTCODENOTES = 1
        val REQUEST_OLD_CSV_CODE = 2
        val REQUEST_TRAINING = 3
//        val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1
    }
//
//    inner class TouchHelperCallback internal constructor() :
//        ItemTouchHelper.SimpleCallback(
//            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
//            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
//        ) {
//
//        override fun onMove(
//            recyclerView: RecyclerView,
//            viewHolder: RecyclerView.ViewHolder,
//            target: RecyclerView.ViewHolder
//        ): Boolean {
//            return true
//        }
//
//        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
//
//        }
//
//        override fun isLongPressDragEnabled(): Boolean {
//            return true
//        }
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(mainToolBar)
        realm = Realm.getDefaultInstance()

        when {
            intent?.action == Intent.ACTION_SEND -> {
                if ("text/csv" == intent.type) {
                    readStrongCSV(intent) // Handle text being sent
                }
            }
        }

        val sharedPref = this.getSharedPreferences(getString(R.string.KEY_PREFERENCE_FILE), Context.MODE_PRIVATE)
        activeTrainingUUID = sharedPref.getLong(getString(R.string.KEY_ACTIVE_TRAINING), -1)
        if (activeTrainingUUID > 0)
            resume_Training_FAB.show()
        else
            resume_Training_FAB.hide()


        recyclerView = findViewById(R.id.recycler_view_trainings)


        // TODO catalogfile erst auf action einlesen
        //
//        readCatalogFile("mycatalog")


        fabAddTraining = findViewById(R.id.add_Training_FAB)
        fabResumeTraining = findViewById(R.id.resume_Training_FAB)


        fabAddTraining?.setOnClickListener {
            realm?.let { DataHelper.addTraining(it) }
            adapter?.updateData(adapter?.data)
            val llm: LinearLayoutManager = recyclerView?.layoutManager as LinearLayoutManager
            llm.scrollToPositionWithOffset(0, 0)
        }
        fabResumeTraining?.setOnClickListener {
            var intent = Intent(baseContext, EditTrainingActivity::class.java)
            intent.putExtra(EditTrainingActivity.TRAINING_ID, activeTrainingUUID) //TODO LongExtra?

            startActivityForResult(intent, REQUEST_TRAINING)
        }

        setUpRecyclerView()

        close_searchViews_btn.setOnClickListener {
            CompleteSearchBar.visibility = View.GONE
        }

        search_TrainingName_EditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                adapter?.updateData(
                    realm?.where(Training::class.java)?.contains(
                        "name",
                        s.toString().trim()
                    )?.findAll()?.sort("date", Sort.DESCENDING)
                )
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        searchNotes_EditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                adapter?.updateData(
                    realm?.where(Training::class.java)?.contains(
                        "notes",
                        s.toString().trim()
                    )?.findAll()?.sort("date", Sort.DESCENDING)
                )
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        search_Date_EditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrBlank()) {
                    adapter?.updateData(
                        realm?.where(Training::class.java)?.equalTo(
                            "month",
                            s.toString().trim().toInt() - 1
                        )?.findAll()?.sort("date", Sort.DESCENDING)
                    )
                } else {
                    adapter?.updateData(
                        realm!!.where(MasterParent::class.java).findFirst()!!.trainingList.sort(
                            "date",
                            Sort.DESCENDING
                        )
                    )
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        search_bar_ExerciseName_editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrBlank()) {
                    val knownToSearch =
                        realm?.where(KnownExercise::class.java)?.contains("name", s.toString().trim().toUpperCase())
                            ?.findAll()
                    val exercisesToSearch: RealmList<Exercise> = RealmList()
                    val trainingsToDisplay: RealmList<Training> = RealmList()
                    if (knownToSearch != null) {
                        for (known in knownToSearch) {
                            known.doneInExercises?.let { exercisesToSearch.addAll(it) }
                        }
                    }
                    for (ex in exercisesToSearch) {
                        ex.doneInTrainings?.let {

                            for (training in it) {
                                if (!trainingsToDisplay.contains(training)) {
                                    trainingsToDisplay.add(training)
                                }
                            }


                        }
                    }
                    trainingsToDisplay.sortByDescending { it.date }
                    adapter?.updateData(trainingsToDisplay)
                } else {
                    adapter?.updateData(
                        realm!!.where(MasterParent::class.java).findFirst()!!.trainingList.sort(
                            "date",
                            Sort.DESCENDING
                        )
                    )
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}


        })

        search_bar_ExerciseID_editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrBlank()) {
                    val knownToSearch =
                        realm?.where(KnownExercise::class.java)?.equalTo("user_custom_id", s.toString().trim().toInt())
                            ?.findFirst()
                    val exercisesToSearch: RealmList<Exercise> = RealmList()
                    val trainingsToDisplay: RealmList<Training> = RealmList()
                    if (knownToSearch != null) {
                        knownToSearch.doneInExercises?.let { exercisesToSearch.addAll(it) }
                    }
                    for (ex in exercisesToSearch) {
                        ex.doneInTrainings?.let {

                            for (training in it) {
                                if (!trainingsToDisplay.contains(training)) {
                                    trainingsToDisplay.add(training)
                                }
                            }


                        }
                    }
                    trainingsToDisplay.sortByDescending { it.date }
                    adapter?.updateData(trainingsToDisplay)
                } else {
                    adapter?.updateData(
                        realm!!.where(MasterParent::class.java).findFirst()!!.trainingList.sort(
                            "date",
                            Sort.DESCENDING
                        )
                    )
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })


    }

    /*
     * It is good practice to null the reference from the view to the adapter when it is no longer needed.
     * Because the <code>RealmRecyclerViewAdapter</code> registers itself as a <code>RealmResult.ChangeListener</code>
     * the view may still be reachable if anybody is still holding a reference to the <code>RealmResult>.
     */
    override fun onDestroy() {
        super.onDestroy()
        recyclerView!!.adapter = null
        realm!!.close()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menu = menu
        menuInflater.inflate(R.menu.listview_options, menu)
        menu.setGroupVisible(R.id.group_normal_mode, true)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {

            R.id.action_searchTrainings -> {
                CompleteSearchBar.visibility = View.VISIBLE
                return true
            }

            R.id.action_KnownExerciseOverView -> {
                val knownExIntent = Intent(this, KnownExerciseListActivity::class.java)
                knownExIntent.putExtra(EditTrainingActivity.VIEWKNOWNEXERCISES, true)
                startActivity(knownExIntent)
                return true
            }
//            R.id.action_importCSV -> {
//                getCSVFileForImport()
//                return true
//            }
//            R.id.action_importOldTrainingCSV -> {
//                getOldTrainingCSVFileForImport()
//                return true
//            }
            R.id.action_delete_all_realm_data -> {
                deleteAllRealmData()
                return true
            }

            R.id.action_backup_realm_file -> {
                backupRealmFile()
                return true
            }
            R.id.action_import_realm_file ->{
                importRealm()
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun setUpRecyclerView() {
        adapter = TrainingRecyclerViewAdapter(
            realm!!.where(MasterParent::class.java).findFirst()!!.trainingList.sort(
                "date",
                Sort.DESCENDING
            )
        )
        adapter!!.setOnItemClickListener(object : TrainingRecyclerViewAdapter.OnItemClickListener {
            override fun onItemClick(training: Training) {
                if (activeTrainingUUID < 0) {
                    var intent = Intent(baseContext, EditTrainingActivity::class.java)
                    intent.putExtra(EditTrainingActivity.TRAINING_ID, training.uuid) //TODO LongExtra?

                    startActivityForResult(intent, REQUEST_TRAINING)
                }
            }
        })
        adapter!!.setOnItemLongClickListener(object : TrainingRecyclerViewAdapter.OnItemLongClickListener {
            override fun onItemLongClick(training: Training) {
                DataHelper.copyTraining(realm!!, training)
                adapter?.updateData(adapter?.data)
            }

        })

        adapter!!.setOnNotesEditListener(object : TrainingRecyclerViewAdapter.OnNotesEditListener {
            override fun onNotesEdit(training: Training) {
                val editIntent = Intent(this@MainActivity, EditTextActivity::class.java)
                editIntent.putExtra(EditTrainingActivity.TRAINING_ID, training.uuid)
                editIntent.putExtra(EditTrainingActivity.NOTES, training.notes)

                startActivityForResult(editIntent, REQUESTCODENOTES)
            }
        })

        adapter?.setOnDateClickListener(object : TrainingRecyclerViewAdapter.OnDateClickListener {
            val cal = Calendar.getInstance()
            var date = Date()
            override fun onDateClicked(training: Training) {

                dateSetListener = DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
                    date = Date(year - 1900, month, dayOfMonth, 18, 0)

                    realm?.executeTransaction {
                        training.date = date
                        training.year = year - 1900
                        training.month = month
                        training.exercises.forEachIndexed { index, exercise ->
                            exercise.date = Date(date.time + index)
                        }
                    }
                    adapter?.updateData(adapter?.data)
                }
                val datePickerDialog = DatePickerDialog(
                    this@MainActivity,
                    dateSetListener,
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                )
                datePickerDialog.show()
            }
        })

        recyclerView!!.layoutManager = LinearLayoutManager(this)
        recyclerView!!.adapter = adapter
        recyclerView!!.setHasFixedSize(false)


//        val touchHelperCallback = TouchHelperCallback()
//        val touchHelper = ItemTouchHelper(touchHelperCallback)
//        touchHelper.attachToRecyclerView(recyclerView)
    }

    private fun getCSVFileForImport() {
        val getFileIntent = Intent(Intent.ACTION_GET_CONTENT)
        getFileIntent.type = "text/comma-separated-values"
        startActivityForResult(getFileIntent, REQUEST_STRONG_CSV_CODE)
    }

    private fun getOldTrainingCSVFileForImport() {
        val getFileIntent = Intent(Intent.ACTION_GET_CONTENT)
        getFileIntent.type = "text/comma-separated-values"
        startActivityForResult(getFileIntent, REQUEST_OLD_CSV_CODE)
    }

    private fun deleteAllRealmData() {
        if(this.activeTrainingUUID ==-1L) {

            AlertDialog.Builder(this)
                .setTitle("WARNING: Delete all Data?")
                .setMessage("Are you sure you want to delete Everything? There is no way to restore it. it deletes your Exercise Catalog too!")
                .setPositiveButton(android.R.string.yes) { dialog, which ->
                    realm?.let {
                        DataHelper.deleteAllData(it)
                        adapter?.updateData(
                            it.where(MasterParent::class.java).findFirst()!!.trainingList.sort(
                                "date",
                                Sort.DESCENDING
                            )
                        )
                    }
                }
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
        }
        else{
            Toast.makeText(this,"Finish Training before deleting all Data",Toast.LENGTH_LONG).show()
        }

    }


    fun isReadStoragePermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                Log.v(TAG, "Permission is granted1")
                return true
            } else {

                Log.v(TAG, "Permission is revoked1");
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 3);
                return false;
            }


        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG, "Permission is granted1");
            return true;
        }
    }

    fun isWriteStoragePermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                Log.v(TAG, "Permission is granted2");
                return true;
            } else {

                Log.v(TAG, "Permission is revoked2");
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 2);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG, "Permission is granted2");
            return true;
        }
    }

    private fun backupRealmFile() {
        isWriteStoragePermissionGranted()
        if (isWriteStoragePermissionGranted()) {

            try {
                val file =
                    File(Environment.getExternalStorageDirectory().path.toString() + "/com.strong_weightlifting.strength_tracker_app.default_realm")
                if (file.exists()) {
                    file.delete()
                }

                realm?.writeCopyTo(file)
                Toast.makeText(this, "Success backing up realm file", Toast.LENGTH_LONG)
                    .show()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun importRealm() {
        isReadStoragePermissionGranted()
        if (isReadStoragePermissionGranted()) {
            try {
                realm!!.close()
                val file =
                    File(Environment.getExternalStorageDirectory().path.toString() + "/com.strong_weightlifting.strength_tracker_app.default_realm")
                if (file.exists()) {
                    realm?.configuration?.path.let {
                        val oldFile = File(it!!)
                        file.copyTo(oldFile, true)
                    }


                    Toast.makeText(this, "Success importing database file, COMPLETELY RESTART APP NOW!", Toast.LENGTH_LONG).show()
                    realm= Realm.getDefaultInstance()
                    setUpRecyclerView()
                }


            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val trainingID = data?.getLongExtra(EditTrainingActivity.TRAINING_ID, -1)
        val notes = data?.getStringExtra(EditTrainingActivity.NOTES)
        val sharedPref = this.getSharedPreferences(getString(R.string.KEY_PREFERENCE_FILE), Context.MODE_PRIVATE)

        if (resultCode != Activity.RESULT_OK) {
            return
        }
        if (requestCode == REQUESTCODENOTES) {
            if (resultCode == Activity.RESULT_OK) {

                realm?.let {

                    if (trainingID != null) {
                        DataHelper.setNotesToTraining(it, trainingID, notes.toString())
                    }

                }
                adapter?.updateData(adapter?.data)
            }
        }

        if (requestCode == REQUEST_STRONG_CSV_CODE) {
            Toast.makeText(this, "Import noch WIP", Toast.LENGTH_SHORT).show()
            readStrongCSV(data)
        }

        if (requestCode == REQUEST_OLD_CSV_CODE) {
            readOldTraining(data)
        }
        if (requestCode == REQUEST_TRAINING) {
            if (trainingID != null) {
                activeTrainingUUID = trainingID
                with(sharedPref.edit()) {
                    putLong(getString(R.string.KEY_ACTIVE_TRAINING), activeTrainingUUID)
                    commit()
                }
            }
            if (activeTrainingUUID > 0) {
                resume_Training_FAB.show()
            } else
                resume_Training_FAB.hide()
        }


    }

    private fun readStrongCSV(data: Intent?) {
        // Get the file's content URI from the incoming Intent
        data?.clipData?.getItemAt(0)?.uri.also { returnUri ->
            /*
             * Try to open the file for "read" access using the
             * returned URI. If the file isn't found, write to the
             * error log and return.
             */
            inputPFD = try {
                /*
                 * Get the content resolver instance for this context, and use it
                 * to get a ParcelFileDescriptor for the file.
                 */
                returnUri?.let { contentResolver.openFileDescriptor(it, "r") }!!
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                Log.e("MainActivity", "File not found.")
                return
            }

            // Get a regular file descriptor for the file
            val fd = inputPFD.fileDescriptor
            val fis = FileInputStream(fd)
            val scanner = Scanner(fis)
            var i = 0
            val lines: MutableList<String> = mutableListOf()
            while (scanner.hasNextLine()) {
                lines.add(scanner.nextLine())
            }
            scanner.close()
            //remove header line of csv file
            lines.removeAt(0)
            // reverse because the last line is the oldest Set
            lines.reverse()


            realm?.executeTransactionAsync(Realm.Transaction { realm ->
                //Date ; Workout Name ;Exercise Name ; Set Order ; Weight ; Weight Unit ; Reps ; Distance ; Distance Unit ; Seconds ; Notes ; Workout Notes
                // 0        1           2               3           4       5               6       7           8           9           10          11
                var training: Training? = Training.create(realm)
                var prevtraining: Training?
                var exercise: Exercise? = training?.uuid?.let { Exercise.createWithReturn(realm, it) }
                var prevExercise: Exercise?
                loop@ for (i in 0 until lines.size) {

                    if (i == 0) {
                        val field = lines[i].split(";")
                        if (field.size != StrongCSV.values().size) continue@loop
                        training?.notes = field[StrongCSV.WorkoutNotes.ordinal].trim('"')
                        training?.name=field[StrongCSV.WorkoutName.ordinal].trim('"')
                        training?.isDone=true
                        val dateString = field[StrongCSV.Date.ordinal].trim()
                        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val parsedDate = format.parse(dateString)
                        training?.date = parsedDate
                        training?.year = parsedDate.year + 1900
                        training?.month = parsedDate.month
                        exercise?.date = training?.date!!
                        val knownName = field[StrongCSV.ExerciseName.ordinal].trim('"').trim()
                            .replace("Barbell","BB",true)
                            .replace("Machine","M",true)
                            .replace("Dumbbell","DB",true)
                            .toUpperCase()
                        val allKnownExercises = realm.where(KnownExercise::class.java).findAll()
                        var newKnown = allKnownExercises.find { it.name == knownName }
                        val maxKnownID: Int = allKnownExercises.max("user_custom_id")?.toInt() ?: 1
                        if (newKnown == null) {
                            newKnown = KnownExercise.create(realm, knownName, maxKnownID + 1)
                        }
                        exercise?.uuid?.let { exUUID ->
                            newKnown?.uuid?.let { knownUUID ->
                                KnownExercise.addToExercise(realm, exUUID, knownUUID)
                            }
                        }
                        val set = exercise?.uuid?.let { it1 -> ExerciseSet.create(realm, it1) }
                        set?.weight = field[StrongCSV.Weight.ordinal].trim().replace(',', '.').toFloat().roundToInt()
                        set?.reps = field[StrongCSV.Reps.ordinal].trim().toInt()
                        set?.unit = field[StrongCSV.WeightUnit.ordinal].trim()
                        set?.isDone = true
                        exercise?.sets?.add(set)
                    }
                    ///////////////////////////////////////// alle weitere lines
                    if (i > 1) {
                        val field = lines[i].split(";")
                        val prevField = lines[i - 1].split(";")
                        if (field.size != StrongCSV.values().size) continue@loop
                        prevtraining = training
                        if (field[StrongCSV.Date.ordinal] != prevField[StrongCSV.Date.ordinal]
                            || field[StrongCSV.WorkoutName.ordinal] != prevField[StrongCSV.WorkoutName.ordinal]
                        ) {
                            training = Training.create(realm)
                            training?.notes = field[StrongCSV.WorkoutNotes.ordinal].trim('"')
                            training?.name=field[StrongCSV.WorkoutName.ordinal].trim('"')
                            training?.isDone=true
                            val dateString = field[StrongCSV.Date.ordinal].trim()
                            try {
                                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                val parsedDate = format.parse(dateString)
                                training?.date = parsedDate
                                training?.year = parsedDate.year + 1900
                                training?.month = parsedDate.month
                            } catch (e: ParseException) {
                                val format = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
                                val parsedDate = format.parse(dateString)
                                parsedDate?.year = prevtraining?.date?.year ?: throw e
                                training?.date = parsedDate
                                training?.year = parsedDate.year + 1900
                                training?.month = parsedDate.month
                            }


                        }
                        if (field[StrongCSV.ExerciseName.ordinal] != prevField[StrongCSV.ExerciseName.ordinal]) {
                            val rmSetEpley = exercise?.sets?.maxBy { ExerciseSet.epleyValue(it) }
                            rmSetEpley?.let {

                                val epValue = it.let { it1 -> ExerciseSet.epleyValue(it1) }
                                val epWeight = it.weight
                                val epReps = it.reps
                                if (exercise?.knownExercise?.prCalculated!! < epValue) {
                                    exercise!!.knownExercise!!.prCalculated = epValue
                                    exercise!!.knownExercise!!.prWeight = epWeight
                                    exercise!!.knownExercise!!.repsAtPRWeight = epReps
                                    exercise!!.knownExercise!!.dateOfPR = exercise!!.doneInTrainings?.first()?.date!!

                                }
                            }

                            exercise = training?.uuid?.let { trainingUUID ->
                                Exercise.createWithReturn(
                                    realm,
                                    trainingUUID
                                )
                            }
                            exercise?.date = training?.date!!

                            val knownName = field[StrongCSV.ExerciseName.ordinal].trim('"').trim()
                                .replace("Barbell","BB",true)
                                .replace("Machine","M",true)
                                .replace("Dumbbell","DB",true)
                                .toUpperCase()
                            val allKnownExercises = realm.where(KnownExercise::class.java).findAll()
                            var newKnown = allKnownExercises.find { it.name == knownName }
                            val maxKnownID: Int = allKnownExercises.max("user_custom_id")?.toInt() ?: 1
                            if (newKnown == null) {
                                newKnown = KnownExercise.create(realm, knownName, maxKnownID + 1)
                            }
                            exercise?.uuid?.let { exUUID ->
                                newKnown?.uuid?.let { knownUUID ->
                                    KnownExercise.addToExercise(realm, exUUID, knownUUID)
                                }
                            }
                        }
                        val set = exercise?.uuid?.let { it1 -> ExerciseSet.createWithoutAdd(realm, it1) }
                        val weightstr = field[StrongCSV.Weight.ordinal].trim().replace(',', '.')
                        val repsStr = field[StrongCSV.Reps.ordinal].trim()
                        val unitStr = field[StrongCSV.WeightUnit.ordinal].trim()
                        set?.weight =
                            if (weightstr.isNotEmpty() && weightstr.isNotBlank()) weightstr.toFloat().roundToInt() else 0
                        set?.reps = if (repsStr.isNotEmpty() && repsStr.isNotBlank()) repsStr.toInt() else 0
                        set?.unit = if (unitStr.isNotEmpty() && unitStr.isNotBlank()) unitStr else ""
                        set?.isDone = true
                        set?.orderNumber = field[StrongCSV.SetOrder.ordinal].trim().toIntOrNull() ?: set?.orderNumber!!
                        exercise?.sets?.add(0, set)
                    }

                }
            }, Realm.Transaction.OnSuccess { adapter?.updateData(adapter?.data) })


        }
    }

    private fun readOldTraining(data: Intent?) {
//Datum;Woche;Wochentag;Ort;ID;Notizen;Gewicht;WDH;Gewicht2;WDH2;Gewicht3;WDH3;Gewicht4;WDH4;Gewicht5;WDH5;Gewicht6;WDH6;Gewicht7;WDH7;Gewicht8;WDH8;Gewicht9;WDH9;Gewicht10;WDH10;Gewicht11;WDH11;"Durschnittliches Gewicht";"Maximal Gewicht";reps_max;Summe WDH;AVG*WDH(Arbeit);Boolean Training;1RM Epley
        data?.data.also { returnUri ->
            /*
             * Try to open the file for "read" access using the
             * returned URI. If the file isn't found, write to the
             * error log and return.
             */
            inputPFD = try {
                /*
                 * Get the content resolver instance for this context, and use it
                 * to get a ParcelFileDescriptor for the file.
                 */
                returnUri?.let { contentResolver.openFileDescriptor(it, "r") }!!
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                Log.e("MainActivity", "File not found.")
                return
            }

            // Get a regular file descriptor for the file
            val fd = inputPFD.fileDescriptor
            val fis = FileInputStream(fd)
            val scanner = Scanner(fis)
            var i = 0
            val lines: MutableList<String> = mutableListOf()
            while (scanner.hasNextLine()) {
                lines.add(scanner.nextLine())
            }
            scanner.close()
            //remove header line of csv file
            lines.removeAt(0)
            // reverse because the last line is the oldest Set
            lines.reverse()

            realm?.executeTransactionAsync(Realm.Transaction { realm ->
                var training: Training? = Training.create(realm)
                var prevtraining: Training?
                var exercise: Exercise? = training?.uuid?.let { Exercise.createWithReturn(realm, it) }
                var prevExercise: Exercise?
                loop@ for (i in 0 until lines.size) {
                    if (i == 0) {
                        val field = lines[i].split(";")
                        if (field.size != CSV.values().size || field[CSV.Datum.ordinal].trim().let {
                                it.isNullOrEmpty().or(it.isBlank())
                            }) continue@loop
                        val dateString = field[CSV.Datum.ordinal].trim()
                        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val parsedDate = format.parse(dateString)
                        training?.date = parsedDate
                        training?.year = parsedDate.year + 1900
                        training?.month = parsedDate.month
                        exercise?.date = training?.date!!
                        exercise?.notes = field[CSV.Notizen.ordinal].trim('"')
                        training?.isDone=true

                        val knownName = field[CSV.Name.ordinal].trim('"').trim().toUpperCase()
                        var userCustomID =
                            field[CSV.ID.ordinal].trim().let { if (it.isNullOrBlank()) 0 else it.toInt() }
                        val allKnownExercises = realm.where(KnownExercise::class.java).findAll()
                        var newKnown = allKnownExercises.find { it.name == knownName }
                        if (newKnown == null) {
                            newKnown = KnownExercise.create(realm, knownName, userCustomID)
                        }
                        exercise?.uuid?.let { exUUID ->
                            newKnown?.uuid?.let { knownUUID ->
                                KnownExercise.addToExercise(realm, exUUID, knownUUID)
                            }
                        }
                        for (index in 0..10) {
                            val set = exercise?.uuid?.let { it1 -> ExerciseSet.createWithoutAdd(realm, it1) }
                            val weightstr = field[CSV.Gewicht.ordinal + 2 * index].trim('"', ' ').replace(',', '.')
                            val repsStr = field[CSV.WDH.ordinal + 2 * index].trim()
                            if (weightstr.isNotEmpty() && weightstr.isNotBlank()) {
                                set?.weight =
                                    if (weightstr.isNotEmpty() && weightstr.isNotBlank()) weightstr.toFloat().roundToInt() else 0
                                set?.reps = if (repsStr.isNotEmpty() && repsStr.isNotBlank()) repsStr.toInt() else 0
                                set?.isDone = true
                                set?.orderNumber = index + 1
                                exercise?.sets?.add(set)
                            }
                        }
                    }
                    ///////////////////////////////////////// alle weitere lines
                    if (i > 0) {
                        val field = lines[i].split(";")
                        val prevField = lines[i - 1].split(";")
                        if (field.size != CSV.values().size) continue@loop
                        prevtraining = training
                        if (field[CSV.Datum.ordinal] != prevField[CSV.Datum.ordinal]
                        ) {
                            training = Training.create(realm)
                            if (training?.notes?.contains(field[CSV.Notizen.ordinal].trim('"')) == false) training.notes += field[CSV.Notizen.ordinal].trim(
                                '"'
                            )
                            val dateString = field[CSV.Datum.ordinal].trim()
                            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val parsedDate = format.parse(dateString)
                            training?.date = parsedDate
                            training?.year = parsedDate.year + 1900
                            training?.month = parsedDate.month
                            training?.isDone=true



                        }
                        if (field[CSV.Name.ordinal] != prevField[CSV.Name.ordinal]) {
                            exercise = training?.uuid?.let { trainingUUID ->
                                Exercise.createWithReturn(
                                    realm,
                                    trainingUUID
                                )
                            }
                            exercise?.date = training?.date!!
                            exercise?.notes = field[CSV.Notizen.ordinal].trim()

                            val knownName = field[CSV.Name.ordinal].trim('"').trim().toUpperCase()
                            var userCustomID =
                                field[CSV.ID.ordinal].trim().let { if (it.isNullOrBlank()) 0 else it.toInt() }
                            val allKnownExercises = realm.where(KnownExercise::class.java).findAll()
                            var newKnown = allKnownExercises.find { it.name == knownName }
                            if (newKnown == null) {
                                newKnown = KnownExercise.create(realm, knownName, userCustomID)
                            }
                            exercise?.uuid?.let { exUUID ->
                                newKnown?.uuid?.let { knownUUID ->
                                    KnownExercise.addToExercise(realm, exUUID, knownUUID)
                                }
                            }
                        }
                        for (index in 0..10) {
                            val set = exercise?.uuid?.let { it1 -> ExerciseSet.createWithoutAdd(realm, it1) }
                            val weightstr = field[CSV.Gewicht.ordinal + 2 * index].trim('"', ' ').replace(',', '.')
                            val repsStr = field[CSV.WDH.ordinal + 2 * index].trim()
                            if (weightstr.isNotEmpty() && weightstr.isNotBlank()) {
                                set?.weight =
                                    if (weightstr.isNotEmpty() && weightstr.isNotBlank()) weightstr.toFloat().roundToInt() else 0
                                set?.reps = if (repsStr.isNotEmpty() && repsStr.isNotBlank()) repsStr.toInt() else 0
                                set?.isDone = true
                                set?.orderNumber = index + 1
                                exercise?.sets?.add(set)
                            }
                        }
                        val rmSetEpley = exercise?.sets?.maxBy { ExerciseSet.epleyValue(it) }
                        rmSetEpley?.let {

                            val epValue = it.let { it1 -> ExerciseSet.epleyValue(it1) }
                            val epWeight = it.weight
                            val epReps = it.reps
                            if (exercise?.knownExercise?.prCalculated!! < epValue) {
                                exercise.knownExercise!!.prCalculated = epValue
                                exercise.knownExercise!!.prWeight = epWeight
                                exercise.knownExercise!!.repsAtPRWeight = epReps
                                exercise.knownExercise!!.dateOfPR = exercise.doneInTrainings?.first()?.date!!
                            }
                            exercise.prWeightAtTheMoment= exercise.knownExercise!!.prWeight
                            exercise.repsAtPRWeightAtTheMoment=exercise.knownExercise!!.repsAtPRWeight
                            exercise.prCalculatedAtTheMoment=exercise.knownExercise!!.prCalculated
                        }
                    }

                }
            }, Realm.Transaction.OnSuccess { adapter?.updateData(adapter?.data) })


        }
    }

    enum class CSV {
        Datum, Woche, Wochentag, Ort, ID, Notizen, Name, Gewicht, WDH, Gewicht2, WDH2, Gewicht3,
        WDH3, Gewicht4, WDH4, Gewicht5, WDH5, Gewicht6, WDH6, Gewicht7, WDH7, Gewicht8, WDH8, Gewicht9,
        WDH9, Gewicht10, WDH10, Gewicht11, WDH11
    }

    fun readCatalogFile(name: String) {
        val ins = resources.openRawResource(
            resources.getIdentifier(
                name,
                "raw", packageName
            )
        )
        val bufferedReader = BufferedReader(InputStreamReader(ins))
        var line = ""
        val lines = bufferedReader.readLines().toMutableList()
        realm?.executeTransactionAsync(object : Realm.Transaction {
            override fun execute(realm: Realm) {
                for (i in 0..lines.size - 1) {
                    val args = lines[i].split(Regex(":"))
                    if (args.size == 2) {
                        val name = args[1].trim('\t', ' ').toUpperCase()
                        val id = args[0].trim('\t', ' ').toInt()
                        if (realm.where(KnownExercise::class.java).equalTo("name", name).equalTo("user_custom_id", id)
                                .findFirst() == null
                        ) {
                            KnownExercise.create(realm, name, id)
                        }
                    }
                }
            }

        }
            , Realm.Transaction.OnSuccess { adapter?.updateData(adapter?.data) })
    }


    override fun onResume() {
        super.onResume()
        adapter?.updateData(adapter?.data)
    }
}

