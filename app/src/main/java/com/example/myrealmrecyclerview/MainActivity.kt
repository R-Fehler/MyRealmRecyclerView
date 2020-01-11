package com.example.myrealmrecyclerview

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myrealmrecyclerview.model.DataHelper
import com.example.myrealmrecyclerview.model.MasterParent
import com.example.myrealmrecyclerview.model.Training
import com.example.myrealmrecyclerview.ui.recyclerview.TrainingRecyclerViewAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.realm.Realm
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
    private var fabAddTraining:FloatingActionButton?=null

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
            Toast.makeText(this@MainActivity,"swiped",Toast.LENGTH_SHORT).show()
            realm?.let { DataHelper.deleteTrainingAsync(it, viewHolder.itemId) }

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
        fabAddTraining=findViewById(R.id.add_Training_FAB)
        fabAddTraining?.setOnClickListener {realm?.let { DataHelper.addTrainingAsync(it) }  }
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menu = menu
        menuInflater.inflate(R.menu.listview_options, menu)
        menu.setGroupVisible(R.id.group_normal_mode, true)
        menu.setGroupVisible(R.id.group_delete_mode, false)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.action_add -> {
                realm?.let { DataHelper.addTrainingAsync(it) }
                return true
            }
            R.id.action_random -> {
                realm?.let { DataHelper.addTrainingAsync(it) }
                return true
            }
            R.id.action_start_delete_mode -> {
                adapter!!.enableDeletionMode(true)
                menu!!.setGroupVisible(R.id.group_normal_mode, false)
                menu!!.setGroupVisible(R.id.group_delete_mode, true)
                return true
            }
            R.id.action_end_delete_mode -> {
                realm?.let { DataHelper.deleteTrainingsAsync(it, adapter!!.uuidsToDelete) }
                adapter!!.enableDeletionMode(false)
                menu!!.setGroupVisible(R.id.group_normal_mode, true)
                menu!!.setGroupVisible(R.id.group_delete_mode, false)
                return true
            }
            // Fall through
            R.id.action_cancel_delete_mode -> {
                adapter!!.enableDeletionMode(false)
                menu!!.setGroupVisible(R.id.group_normal_mode, true)
                menu!!.setGroupVisible(R.id.group_delete_mode, false)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun setUpRecyclerView() {
        adapter = TrainingRecyclerViewAdapter(realm!!.where(MasterParent::class.java).findFirst()!!.trainingList)
        adapter!!.setOnItemClickListener(object : TrainingRecyclerViewAdapter.OnItemClickListener {
            override fun onItemClick(training: Training) {
                var intent = Intent(baseContext, EditTrainingActivity::class.java)
                intent.putExtra(EditTrainingActivity.TRAINING_ID, training.uuid) //TODO LongExtra?

//                startActivityForResult(intent,1)
                startActivity(intent)
            }
        })
        recyclerView!!.layoutManager = LinearLayoutManager(this)
        recyclerView!!.adapter = adapter
        recyclerView!!.setHasFixedSize(true)
        recyclerView!!.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        val touchHelperCallback = TouchHelperCallback()
        val touchHelper = ItemTouchHelper(touchHelperCallback)
        touchHelper.attachToRecyclerView(recyclerView)
    }

    fun getCSVFileForImport(){
        val getFileIntent=Intent(Intent.ACTION_GET_CONTENT)
        getFileIntent.type = "text/comma-separated-values"
        startActivityForResult(getFileIntent,0)
    }
}
