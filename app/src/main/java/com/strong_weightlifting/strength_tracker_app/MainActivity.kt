package com.strong_weightlifting.strength_tracker_app

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.strong_weightlifting.strength_tracker_app.model.*
import com.strong_weightlifting.strength_tracker_app.ui.recyclerview.TrainingRecyclerViewAdapter
import io.realm.Realm
import io.realm.RealmList
import io.realm.Sort
import kotlinx.android.synthetic.main.activity_main.*
import java.io.FileInputStream
import java.io.FileNotFoundException
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


    private var realm: Realm? = null
    private var recyclerView: RecyclerView? = null
    private var menu: Menu? = null
    private var adapter: TrainingRecyclerViewAdapter? = null
    private var fabAddTraining: FloatingActionButton? = null
    private var dateSetListener: DatePickerDialog.OnDateSetListener? = null
    private lateinit var inputPFD: ParcelFileDescriptor


    companion object {
        val REQUESTCSVCODE = 0
        val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1
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
        recyclerView = findViewById(R.id.recycler_view_trainings)




        fabAddTraining = findViewById(R.id.add_Training_FAB)


        fabAddTraining?.setOnClickListener {
            realm?.let { DataHelper.addTraining(it) }
            adapter?.updateData(adapter?.data)
            val llm: LinearLayoutManager = recyclerView?.layoutManager as LinearLayoutManager
            llm.scrollToPositionWithOffset(0, 0)
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
            R.id.action_importCSV -> {
                getCSVFileForImport()
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
                var intent = Intent(baseContext, EditTrainingActivity::class.java)
                intent.putExtra(EditTrainingActivity.TRAINING_ID, training.uuid) //TODO LongExtra?

//                startActivityForResult(intent,1)
                startActivity(intent)
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

                startActivityForResult(editIntent, 1)
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

    fun getCSVFileForImport() {
        val getFileIntent = Intent(Intent.ACTION_GET_CONTENT)
        getFileIntent.type = "text/comma-separated-values"
        startActivityForResult(getFileIntent, REQUESTCSVCODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val trainingID = data?.getLongExtra(EditTrainingActivity.TRAINING_ID, -1)
        val notes = data?.getStringExtra(EditTrainingActivity.NOTES)
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {

                realm?.let {

                    if (trainingID != null) {
                        DataHelper.setNotesToTraining(it, trainingID, notes.toString())
                    }

                }
                adapter?.updateData(adapter?.data)
            }
        }

        if (requestCode == REQUESTCSVCODE) {
            Toast.makeText(this, "Import noch WIP", Toast.LENGTH_SHORT).show()
        }
        //add to training


        // Get the file's content URI from the incoming Intent
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
            lines.removeAt(0)
            lines.reverse()


            realm?.executeTransactionAsync(object : Realm.Transaction {
                override fun execute(realm: Realm) {
                    //Date ; Workout Name ;Exercise Name ; Set Order ; Weight ; Weight Unit ; Reps ; Distance ; Distance Unit ; Seconds ; Notes ; Workout Notes
                    // 0        1           2               3           4       5               6       7           8           9           10          11
                    var training: Training?=Training.create(realm)
                    var prevtraining: Training?
                    var exercise: Exercise? = training?.uuid?.let { Exercise.createWithReturn(realm, it) }
                    var prevExercise: Exercise?
                    var i=1
                    loop@ for(line in lines) {
                        if (i == 1) {
                            val field = line.split(";")
                            if (field.size!=CSV.values().size) continue@loop
                            training?.notes = field[CSV.WorkoutNotes.ordinal].trim('"')
                            val dateString = field[CSV.Date.ordinal].trim()
                            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            val parsedDate = format.parse(dateString)
                            training?.date = parsedDate
                            training?.year = parsedDate.year + 1900
                            training?.month = parsedDate.month
                            exercise?.date= training?.date!!
                            val knownName = field[CSV.ExerciseName.ordinal].trim('"').trim().toUpperCase()
                            val allKnownExercises = realm.where(KnownExercise::class.java).findAll()
                            var newKnown = allKnownExercises.find { it.name == knownName }
                            val maxKnownID: Int = allKnownExercises.max("user_custom_id")?.toInt() ?: 1
                            if (newKnown == null) {
                                newKnown = KnownExercise.create(realm, knownName, maxKnownID+1)
                            }
                            exercise?.uuid?.let { exUUID ->
                                newKnown?.uuid?.let { knownUUID ->
                                    KnownExercise.addToExercise(realm, exUUID, knownUUID)
                                }
                            }
                            val set = exercise?.uuid?.let { it1 -> ExerciseSet.create(realm, it1) }
                            set?.weight = field[CSV.Weight.ordinal].trim().replace(',', '.').toFloat().roundToInt()
                            set?.reps = field[CSV.Reps.ordinal].trim().toInt()
                            set?.unit = field[CSV.WeightUnit.ordinal].trim()
                            set?.isDone = true
                            exercise?.sets?.add(set)
                        }
                        ///////////////////////////////////////// alle weitere lines
                        //TODO die weightplanned repsplanned werden
                        // nicht richtig gesetzt. weil sie die daten nur von der neuesten exercise (datum)
                        // ziehen dh. alle planned werte sind identisch
                        // iwie muss es besser gehen. prüfen welches wod zuerst eingefügt wird. unten / oben
                        if (i > 1) {
                            val field = line.split(";")
                            val prevField = lines[i - 1].split(";")
                            if (field.size!=CSV.values().size) continue@loop
                            prevtraining=training
                            if (field[CSV.Date.ordinal] != prevField[CSV.Date.ordinal]
                                || field[CSV.WorkoutName.ordinal] != prevField[CSV.WorkoutName.ordinal]) {
                                training = Training.create(realm)
                                training?.notes = field[CSV.WorkoutNotes.ordinal].trim('"')
                                val dateString = field[CSV.Date.ordinal].trim()
                                try {
                                    val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                    val parsedDate = format.parse(dateString)
                                    training?.date = parsedDate
                                    training?.year = parsedDate.year + 1900
                                    training?.month = parsedDate.month
                                }
                                catch (e: ParseException){
                                    val format = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
                                    val parsedDate = format.parse(dateString)
                                    parsedDate?.year= prevtraining?.date?.year ?: throw e
                                    training?.date = parsedDate
                                    training?.year = parsedDate.year + 1900
                                    training?.month = parsedDate.month
                                }


                            }
                            if(field[CSV.ExerciseName.ordinal]!=prevField[CSV.ExerciseName.ordinal]){
                                 exercise = training?.uuid?.let { trainingUUID -> Exercise.createWithReturn(realm, trainingUUID) }
                                exercise?.date=training?.date!!

                                val knownName = field[CSV.ExerciseName.ordinal].trim('"').trim().toUpperCase()
                                val allKnownExercises = realm.where(KnownExercise::class.java).findAll()
                                var newKnown = allKnownExercises.find { it.name == knownName }
                                val maxKnownID: Int = allKnownExercises.max("user_custom_id")?.toInt() ?: 1
                                if (newKnown == null) {
                                    newKnown = KnownExercise.create(realm, knownName, maxKnownID+1)
                                }
                                exercise?.uuid?.let { exUUID ->
                                    newKnown?.uuid?.let { knownUUID ->
                                        KnownExercise.addToExercise(realm, exUUID, knownUUID)
                                    }
                                }
                            }
                            val set = exercise?.uuid?.let { it1 -> ExerciseSet.createWithoutAdd(realm, it1) }
                            val weightstr= field[CSV.Weight.ordinal].trim().replace(',', '.')
                            val repsStr=field[CSV.Reps.ordinal].trim()
                            val unitStr=field[CSV.WeightUnit.ordinal].trim()
                            set?.weight = if(weightstr.isNotEmpty() && weightstr.isNotBlank()) weightstr.toFloat().roundToInt() else 0
                            set?.reps = if(repsStr.isNotEmpty() && repsStr.isNotBlank()) repsStr.toInt() else 0
                            set?.unit = if(unitStr.isNotEmpty() && unitStr.isNotBlank()) unitStr else ""
                            set?.isDone = true
                            set?.orderNumber=field[CSV.SetOrder.ordinal].trim().toIntOrNull() ?: set?.orderNumber!!
                            exercise?.sets?.add(0,set)
                        }
                        i++
                    }
                }


            }, Realm.Transaction.OnSuccess { adapter?.updateData(adapter?.data) })


        }
    }

    override fun onResume() {
        super.onResume()
        adapter?.updateData(adapter?.data)
    }
}

