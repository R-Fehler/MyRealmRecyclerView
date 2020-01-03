package com.example.myrealmrecyclerview

import android.content.Intent
import android.os.Bundle
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
import com.example.myrealmrecyclerview.ui.recyclerview.ExercisesRecyclerViewAdapter
import com.example.myrealmrecyclerview.ui.recyclerview.MyRecyclerViewAdapter
import io.realm.Realm

import kotlinx.android.synthetic.main.activity_edit_training.*

class EditTrainingActivity : AppCompatActivity() {

    private var realm: Realm? = null
    private var recyclerView: RecyclerView? = null
    private var menu: Menu? = null
    private var adapter: ExercisesRecyclerViewAdapter? = null

    companion object {
        const val TRAINING_ID = "com.example.myrealmrecyclerview"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_training)
        realm = Realm.getDefaultInstance()
        recyclerView = findViewById(R.id.recycler_view_exercises)
        setUpRecyclerView()
        val training_uuid=intent.getLongExtra(TRAINING_ID,0)
        Toast.makeText(this,"$training_uuid ist die UUID", Toast.LENGTH_LONG).show()



        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            ///TODO SAVE TRAINING
            realm?.let { DataHelper.addExerciseAsync(it,training_uuid) }

            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
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
                Toast.makeText(this@EditTrainingActivity,"next Activity",Toast.LENGTH_SHORT).show()
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
    }


