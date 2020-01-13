package com.example.myrealmrecyclerview

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myrealmrecyclerview.model.DataHelper
import com.example.myrealmrecyclerview.model.Exercise
import com.example.myrealmrecyclerview.model.Training
import com.example.myrealmrecyclerview.ui.recyclerview.ExerciseSetAdapter
import com.example.myrealmrecyclerview.ui.recyclerview.ExercisesRecyclerViewAdapter
import io.realm.Realm

import kotlinx.android.synthetic.main.activity_edit_training.*
import kotlinx.android.synthetic.main.content_edit_training.*

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

    companion object {
        const val TRAINING_ID = "com.example.myrealmrecyclerview.TRAINING_ID"
        const val KNOWNEXERCISE_ID = "com.example.myrealmrecyclerview.KNOWNEXERCISE_ID"
        const val EXERCISE_ID = "com.example.myrealmrecyclerview.EXERCISE_ID"
        const val VIEWKNOWNEXERCISES = "com.example.myrealmrecyclerview.VIEWKNOWNEXERCISES"
        const val NOTES = "com.example.myrealmrecyclerview.NOTES"

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_training)
        realm = Realm.getDefaultInstance()


        recyclerView = findViewById(R.id.recycler_view_exercises)

        val training=realm!!.where(Training::class.java).equalTo(
            "uuid", intent.getLongExtra(
                TRAINING_ID, 0
            )
        )!!.findFirst()
        nameOfTrainingEditText.text.let { it.clear()
        it.insert(0,training?.name)}

        nameOfTrainingEditText.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                if(!s.isNullOrEmpty()) {
                    if (s.toString() != training?.name) {
                        realm?.executeTransaction {
                            training?.name=s.toString()
                             }
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })





        fab.setOnClickListener { view ->
            ///TODO SAVE TRAINING
            realm?.let {

                val intent= Intent(this@EditTrainingActivity,KnownExerciseListActivity::class.java)
                startActivityForResult(intent,KnownExerciseListActivity.CHOOSEKNOWNEXERCISE)
            }



            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
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
//                var intent = Intent(baseContext, EditTrainingActivity::class.java)
//                intent.putExtra(EditTrainingActivity.TRAINING_ID, training.uuid)
//
//
//                startActivityForResult(intent,1)
                // TODO add new Set to Exercise
                Toast.makeText(this@EditTrainingActivity, "next Activity", Toast.LENGTH_SHORT).show()
            }
        })
        adapter!!.setAddClickListener(object : ExercisesRecyclerViewAdapter.OnAddClickListener {
            override fun onAddClick(uuid: Long, adapter: ExerciseSetAdapter) {
                realm?.let { DataHelper.addExerciseSet(it, uuid) }
            }

        })
        adapter!!.setOnNameClickListener(object : ExercisesRecyclerViewAdapter.OnNameClickListener {
            override fun onNameClick(exercise: Exercise) {
                val intent = Intent(this@EditTrainingActivity, KnownExerciseListActivity::class.java)
                intent.putExtra(EXERCISE_ID, exercise.uuid)
                startActivityForResult(intent, KnownExerciseListActivity.CHANGEKNOWNEXERCISE)
            }
        })


        recyclerView!!.layoutManager = LinearLayoutManager(this)
        recyclerView!!.adapter = adapter
        recyclerView!!.setHasFixedSize(true)

        recyclerView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)


                if (dy > 0) {
                    nameOfTrainingEditText.visibility = View.GONE
                } else {
                    nameOfTrainingEditText.visibility = View.VISIBLE
                }

            }
        })
    }

//        val touchHelperCallback = TouchHelperCallback()
//        val touchHelper = ItemTouchHelper(touchHelperCallback)
//        touchHelper.attachToRecyclerView(recyclerView)


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode==KnownExerciseListActivity.CHOOSEKNOWNEXERCISE){
            if(resultCode== Activity.RESULT_OK){
                val knownExID=data?.getLongExtra(KNOWNEXERCISE_ID,0)

                realm?.let {
                    if (knownExID != null) {

                            val training_uuid=intent.getLongExtra(TRAINING_ID,0)
                            val ExID=DataHelper.addExercise(it, training_uuid)
                            DataHelper.addKnownExToExercise(it,knownExID,ExID)


                    }
                }
            }
        }
        if(requestCode==KnownExerciseListActivity.CHANGEKNOWNEXERCISE){
            if(resultCode== Activity.RESULT_OK){
                val knownExID=data?.getLongExtra(KNOWNEXERCISE_ID,0)
                val exerciseUUID=data?.getLongExtra(EXERCISE_ID,-1)

                realm?.let {
                    if (knownExID != null) {

                        if (exerciseUUID != null) {
                            DataHelper.addKnownExToExercise(it,knownExID,exerciseUUID)
                        }
                    }
                }
            }
        }
        adapter?.updateData(adapter?.data)
    }

    override fun onResume() {
        super.onResume()
        adapter?.updateData(adapter?.data)
    }
    }


