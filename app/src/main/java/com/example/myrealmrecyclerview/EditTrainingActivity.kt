package com.example.myrealmrecyclerview

import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Menu
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myrealmrecyclerview.model.DataHelper
import com.example.myrealmrecyclerview.model.Exercise
import com.example.myrealmrecyclerview.model.MasterParent
import com.example.myrealmrecyclerview.model.Training
import com.example.myrealmrecyclerview.ui.recyclerview.ExerciseSetAdapter
import com.example.myrealmrecyclerview.ui.recyclerview.ExercisesRecyclerViewAdapter
import com.example.myrealmrecyclerview.ui.recyclerview.MyRecyclerViewAdapter
import io.realm.Realm

import kotlinx.android.synthetic.main.activity_edit_training.*
import java.sql.Time
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
        const val TRAINING_ID = "com.example.myrealmrecyclerview"
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
                DataHelper.addExerciseAsync(it, training_uuid)
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
                Toast.makeText(this@EditTrainingActivity,"uuid ist $uuid",Toast.LENGTH_SHORT).show()            }

        })
        recyclerView!!.layoutManager = LinearLayoutManager(this)
        recyclerView!!.adapter = adapter
        recyclerView!!.setHasFixedSize(true)
        recyclerView!!.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

//        val touchHelperCallback = TouchHelperCallback()
//        val touchHelper = ItemTouchHelper(touchHelperCallback)
//        touchHelper.attachToRecyclerView(recyclerView)
    }
    }


