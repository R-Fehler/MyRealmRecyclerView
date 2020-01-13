package com.example.myrealmrecyclerview

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myrealmrecyclerview.model.*
import com.example.myrealmrecyclerview.ui.recyclerview.TrainingRecyclerViewAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmResults
import io.realm.Sort
import kotlinx.android.synthetic.main.content_main_activity.*
import java.util.*

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

    inner class TouchHelperCallback internal constructor() :
        ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            Toast.makeText(this@MainActivity, "deleted", Toast.LENGTH_SHORT).show()
            realm?.let { DataHelper.deleteTraining(it, viewHolder.itemId) }
            adapter?.updateData(adapter?.data)

        }

        override fun isLongPressDragEnabled(): Boolean {
            return true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        realm = Realm.getDefaultInstance()
        recyclerView = findViewById(R.id.recycler_view_trainings)




        fabAddTraining = findViewById(R.id.add_Training_FAB)


        fabAddTraining?.setOnClickListener {
            realm?.let { DataHelper.addTraining(it) }
            adapter?.updateData(adapter?.data)
            val llm: LinearLayoutManager= recyclerView?.layoutManager as LinearLayoutManager
            llm.scrollToPositionWithOffset(0,0)
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
                    )?.findAll()?.sort("date",Sort.DESCENDING)
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
                    )?.findAll()?.sort("date",Sort.DESCENDING)
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
                            s.toString().trim().toInt()-1
                        )?.findAll()?.sort("date",Sort.DESCENDING)
                    )
                } else {
                    adapter?.updateData(realm!!.where(MasterParent::class.java).findFirst()!!.trainingList.sort("date",Sort.DESCENDING))
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        search_bar_ExerciseName_editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrBlank()) {
                    val knownToSearch=realm?.where(KnownExercise::class.java)?.contains("name",s.toString().trim().toUpperCase())?.findAll()
                    val exercisesToSearch: RealmList<Exercise> = RealmList()
                    val trainingsToDisplay: RealmList<Training> = RealmList()
                    if(knownToSearch!=null) {
                        for (known in knownToSearch) {
                            known.doneInExercises?.let { exercisesToSearch.addAll(it) }
                        }
                    }
                    for (ex in exercisesToSearch){
                        ex.doneInTrainings?.let {

                            for (training in it){
                                if (!trainingsToDisplay.contains(training))
                                { trainingsToDisplay.add(training)}
                            }


                        }
                    }
                    trainingsToDisplay.sortByDescending { it.date }
                    adapter?.updateData(trainingsToDisplay)
                }
                    else {
                    adapter?.updateData(realm!!.where(MasterParent::class.java).findFirst()!!.trainingList.sort("date",Sort.DESCENDING))
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        search_bar_ExerciseID_editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrBlank()) {
                    val knownToSearch=realm?.where(KnownExercise::class.java)?.equalTo("user_custom_id",s.toString().trim().toInt())?.findFirst()
                    val exercisesToSearch: RealmList<Exercise> = RealmList()
                    val trainingsToDisplay: RealmList<Training> = RealmList()
                    if(knownToSearch!=null) {
                        knownToSearch.doneInExercises?.let { exercisesToSearch.addAll(it) }
                    }
                    for (ex in exercisesToSearch){
                        ex.doneInTrainings?.let {

                            for (training in it){
                                if (!trainingsToDisplay.contains(training))
                                { trainingsToDisplay.add(training)}
                            }


                        }
                    }
                    trainingsToDisplay.sortByDescending { it.date }
                    adapter?.updateData(trainingsToDisplay)
                }
                else {
                    adapter?.updateData(realm!!.where(MasterParent::class.java).findFirst()!!.trainingList.sort("date",Sort.DESCENDING))
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

            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun setUpRecyclerView() {
        adapter = TrainingRecyclerViewAdapter(realm!!.where(MasterParent::class.java).findFirst()!!.trainingList.sort("date",Sort.DESCENDING))
        adapter!!.setOnItemClickListener(object : TrainingRecyclerViewAdapter.OnItemClickListener {
            override fun onItemClick(training: Training) {
                var intent = Intent(baseContext, EditTrainingActivity::class.java)
                intent.putExtra(EditTrainingActivity.TRAINING_ID, training.uuid) //TODO LongExtra?

//                startActivityForResult(intent,1)
                startActivity(intent)
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


        val touchHelperCallback = TouchHelperCallback()
        val touchHelper = ItemTouchHelper(touchHelperCallback)
        touchHelper.attachToRecyclerView(recyclerView)
    }

    fun getCSVFileForImport() {
        val getFileIntent = Intent(Intent.ACTION_GET_CONTENT)
        getFileIntent.type = "text/comma-separated-values"
        startActivityForResult(getFileIntent, 0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val trainingID = data?.getLongExtra(EditTrainingActivity.TRAINING_ID, -1)
        val notes = data?.getStringExtra(EditTrainingActivity.NOTES)

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
        //add to training
    }

    override fun onResume() {
        super.onResume()
        adapter?.updateData(adapter?.data)
    }
}
