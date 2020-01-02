package com.example.myrealmrecyclerview

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
import com.example.myrealmrecyclerview.ui.recyclerview.MyRecyclerViewAdapter
import io.realm.Realm

class MainActivity : AppCompatActivity() {


    private var realm: Realm? = null
    private var recyclerView: RecyclerView? = null
    private var menu: Menu? = null
    private var adapter: MyRecyclerViewAdapter? = null

    private inner class TouchHelperCallback internal constructor() :
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
        }

        override fun isLongPressDragEnabled(): Boolean {
            return true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        realm = Realm.getDefaultInstance()
        recyclerView = findViewById(R.id.recycler_view)
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
        adapter = MyRecyclerViewAdapter(realm!!.where(MasterParent::class.java!!).findFirst()!!.trainingList)
        recyclerView!!.layoutManager = LinearLayoutManager(this)
        recyclerView!!.adapter = adapter
        recyclerView!!.setHasFixedSize(true)
        recyclerView!!.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        val touchHelperCallback = TouchHelperCallback()
        val touchHelper = ItemTouchHelper(touchHelperCallback)
        touchHelper.attachToRecyclerView(recyclerView)
    }

}
