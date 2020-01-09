package com.example.myrealmrecyclerview

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myrealmrecyclerview.model.DataHelper
import com.example.myrealmrecyclerview.model.Exercise
import com.example.myrealmrecyclerview.model.Training
import com.example.myrealmrecyclerview.ui.recyclerview.ExerciseSetAdapter
import com.example.myrealmrecyclerview.ui.recyclerview.ExercisesRecyclerViewAdapter
import io.realm.Realm

import kotlinx.android.synthetic.main.activity_edit_training.*

/*TODO
In edit exercise activity noch oben edit Text mit Datum, Notizen dauer usw.
Edittraining activity in readonly Mode um alte Trainings anzuschauen ohne was ausversehen zu löschen oder zu ändern. Kann man edittext noneditable machen wenn ein intentcode gesetzt wird
Testen wie die edit Text persistent werden : ontextchanged updaterealm
Geplant vs done? : setze den Wert von planned in normale properties. Wenn gleich ist der Set wie geplant ausgeführt
Vorschläge in edittext aufgrund des letzten Trainings mit der known exercise. Query in oncreate für exercise mit known exercise Feld equalto this known exercise und Sets orderNo equalto this orderNo Else den vom letzten Satz

 */
class EditTrainingActivity : AppCompatActivity(), ExercisesRecyclerViewAdapter.OnAddClickListener {

    private var realm: Realm? = null
    private var recyclerView: RecyclerView? = null
    private var menu: Menu? = null
    private var adapter: ExercisesRecyclerViewAdapter? = null

    companion object {
        const val TRAINING_ID = "com.example.myrealmrecyclerview.TRAINING_ID"
        const val KNOWNEXERCISE_ID = "com.example.myrealmrecyclerview.KNOWNEXERCISE_ID"
        const val EXERCISE_ID = "com.example.myrealmrecyclerview.EXERCISE_ID"
    }

    override fun onAddClick(uuid: Long, adapter: ExerciseSetAdapter) {

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_training)
        realm = Realm.getDefaultInstance()
//        for (i in 0..10){
//
//            realm?.let { DataHelper.addExerciseSetAsync(it,14) }
//        }


        recyclerView = findViewById(R.id.recycler_view_exercises)
        val training_uuid=intent.getLongExtra(TRAINING_ID,0)
        Toast.makeText(this,"$training_uuid ist die UUID", Toast.LENGTH_LONG).show()



        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            ///TODO SAVE TRAINING
            realm?.let {
               val exerciseID=DataHelper.addExercise(it, training_uuid)
                val intent= Intent(this@EditTrainingActivity,KnownExerciseListActivity::class.java)
//                TODO
                intent.putExtra(EXERCISE_ID,exerciseID)
                startActivityForResult(intent,5)
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
        adapter = realm!!.where(Training::class.java).equalTo("uuid",intent.getLongExtra(
            TRAINING_ID,0))!!.findFirst()?.exercises?.let { ExercisesRecyclerViewAdapter(it) }
        adapter!!.setOnItemClickListener(object : ExercisesRecyclerViewAdapter.OnItemClickListener {
            override fun onItemClick(exercise:Exercise) {
//                var intent = Intent(baseContext, EditTrainingActivity::class.java)
//                intent.putExtra(EditTrainingActivity.TRAINING_ID, training.uuid)
//
//
//                startActivityForResult(intent,1)
                // TODO add new Set to Exercise
                Toast.makeText(this@EditTrainingActivity,"next Activity",Toast.LENGTH_SHORT).show()
            }
        })
        adapter!!.setAddClickListener(object : ExercisesRecyclerViewAdapter.OnAddClickListener {
            override fun onAddClick(uuid: Long, adapter: ExerciseSetAdapter) {
                realm?.let { DataHelper.addExerciseSetAsync(it,uuid) }
                         }

        })
        recyclerView!!.layoutManager = LinearLayoutManager(this)
        recyclerView!!.adapter = adapter
        recyclerView!!.setHasFixedSize(true)
        recyclerView!!.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

//        val touchHelperCallback = TouchHelperCallback()
//        val touchHelper = ItemTouchHelper(touchHelperCallback)
//        touchHelper.attachToRecyclerView(recyclerView)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode==5){
            if(resultCode== Activity.RESULT_OK){
                val knownExID=data?.getLongExtra(KNOWNEXERCISE_ID,111)
                val ExID=data?.getLongExtra(EXERCISE_ID,111)
                realm?.let {
                    if (knownExID != null) {
                        if (ExID != null) {
                            DataHelper.addKnownExToExerciseAsync(it,knownExID,ExID)
                        }
                    }
                }
            }
        }
    }
    }


