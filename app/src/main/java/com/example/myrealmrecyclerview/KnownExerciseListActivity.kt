package com.example.myrealmrecyclerview

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myrealmrecyclerview.model.DataHelper
import com.example.myrealmrecyclerview.model.KnownExercise
import com.example.myrealmrecyclerview.ui.recyclerview.KnownExerciseAdapter
import com.google.android.material.snackbar.Snackbar
import io.realm.Realm
import io.realm.RealmResults
import kotlinx.android.synthetic.main.activity_known_exercise_list.*
import kotlinx.android.synthetic.main.content_known_exercise_list.*

class KnownExerciseListActivity : AppCompatActivity() {

    private var realm: Realm? = null
    private var recyclerView: RecyclerView? = null
    private var menu: Menu? = null
    private var adapter: KnownExerciseAdapter? = null
    private var allKnownExercises: RealmResults<KnownExercise>?= null
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
            //TODO duerfen nicht gel;escht werden da sonst alte Sets keiner knownExercise angehoeren
            // nur name und id darf geaendert werden
            Toast.makeText(this@KnownExerciseListActivity,"swiped", Toast.LENGTH_SHORT).show()
            realm?.let { DataHelper.deleteTrainingAsync(it, viewHolder.itemId) }

        }

        override fun isLongPressDragEnabled(): Boolean {
            return true
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_known_exercise_list)
        realm = Realm.getDefaultInstance()
        recyclerView = findViewById(R.id.recycler_view_knownExercises)
        allKnownExercises=realm!!.where(KnownExercise::class.java).findAll()
        for (known in allKnownExercises!!){
            realm?.executeTransaction {

                known.doneInExercisesSize=known.doneInExercises?.size!!
            }
        }

        setUpRecyclerView()

        search_KnownEx_Name_editTxt.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                adapter?.updateData(realm?.where(KnownExercise::class.java)?.contains("name",s.toString().trim())?.findAll()?.sort("doneInExercisesSize"))
            }


            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        add_KnownExercise_btn.setOnClickListener {
            val name=search_KnownEx_Name_editTxt.text.toString().trim().toUpperCase()
            val id=search_KnownEx_ID_editTxt.text.toString().trim().toInt()



            val existingID=allKnownExercises!!.find{ it.user_custom_id==id }
            val existingName= allKnownExercises!!.find { it.name==name }
            if(existingName==null)
            {
                if(existingID==null) {
                    realm?.let { DataHelper.createKnownExerciseAsync(it, name, id) }

                }
                else{
                    Snackbar.make(findViewById(R.id.search_fields), "ID schon vergeben", Snackbar.LENGTH_LONG)
                        .setAction("Change") {
                            var intent=Intent(this,ChangeKnownExerciseActivity::class.java)
                            intent.putExtra(EditTrainingActivity.KNOWNEXERCISE_ID,existingID.uuid)
                            startActivity(intent)
                        }.show()
                }

            }
            else{
                Snackbar.make(findViewById(R.id.search_fields), "Name schon vergeben", Snackbar.LENGTH_LONG)
                    .setAction("Change") {
                        var intent=Intent(this,ChangeKnownExerciseActivity::class.java)
                        intent.putExtra(EditTrainingActivity.KNOWNEXERCISE_ID,existingName.uuid)
                        startActivity(intent)
                    }.show()
            }
        }

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        recyclerView!!.adapter = null
        realm!!.close()
    }

//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        this.menu = menu
//        menuInflater.inflate(R.menu.listview_options, menu)
//        menu.setGroupVisible(R.id.group_normal_mode, true)
//        menu.setGroupVisible(R.id.group_delete_mode, false)
//        return true
//    }

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {

    private fun setUpRecyclerView() {

        adapter = KnownExerciseAdapter(allKnownExercises!!.sort("doneInExercisesSize"))
        adapter!!.setOnItemClickListener(object : KnownExerciseAdapter.OnItemClickListener {
            override fun onItemClick(knownExercise: KnownExercise) {
                var returnIntent = Intent()
                returnIntent.putExtra(EditTrainingActivity.KNOWNEXERCISE_ID, knownExercise.uuid)
                returnIntent.putExtra(EditTrainingActivity.EXERCISE_ID,intent.getLongExtra(EditTrainingActivity.EXERCISE_ID,0))
                setResult(Activity.RESULT_OK,returnIntent)
                finish()
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

    }



