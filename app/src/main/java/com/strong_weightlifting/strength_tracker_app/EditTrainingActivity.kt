package com.strong_weightlifting.strength_tracker_app

import PreCachingLayoutManager
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
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
class EditTrainingActivity : AppCompatActivity() {

    private var realm: Realm? = null
    private var recyclerView: RecyclerView? = null
    private var menu: Menu? = null
    private var adapter: ExercisesRecyclerViewAdapter? = null
    private var training: Training? = null
    private var dateSetListener: DatePickerDialog.OnDateSetListener? = null
    private var timeSetListener: TimePickerDialog.OnTimeSetListener? = null


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
        val nameAdaper: ArrayAdapter<String> =
            ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, nameArray)
        nameOfTrainingEditText.setAdapter(nameAdaper)
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
        super.onDestroy()
        recyclerView!!.adapter = null
        realm!!.close()
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
                super.onBackPressed()
                return
            }
        }
        val returnIntent = Intent()
        returnIntent.putExtra(TRAINING_ID, intent.getLongExtra(TRAINING_ID, 0))
        setResult(Activity.RESULT_OK, returnIntent)
        super.onBackPressed()
    }


}


