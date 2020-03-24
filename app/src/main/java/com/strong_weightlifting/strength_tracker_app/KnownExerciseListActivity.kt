package com.strong_weightlifting.strength_tracker_app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.strong_weightlifting.strength_tracker_app.model.DataHelper
import com.strong_weightlifting.strength_tracker_app.model.KnownExercise
import com.strong_weightlifting.strength_tracker_app.ui.recyclerview.KnownExerciseAdapter
import com.google.android.material.snackbar.Snackbar
import com.strong_weightlifting.strength_tracker_app.model.Training
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import kotlinx.android.synthetic.main.activity_known_exercise_list.*
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class KnownExerciseListActivity : AppCompatActivity() {

    private var realm: Realm? = null
    private var recyclerView: RecyclerView? = null
    private var menu: Menu? = null
    private var adapter: KnownExerciseAdapter? = null
    private var allKnownExercises: RealmResults<KnownExercise>?= null

    companion object{
        val CHOOSEKNOWNEXERCISE= 5
        val CHANGEKNOWNEXERCISE= 1
    }

    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_known_exercise_list)
        setSupportActionBar(knownExerciseListToolbar)
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
                adapter?.updateData(realm?.where(KnownExercise::class.java)?.contains("name",s.toString().trim())?.findAll()?.sort("doneInExercisesSize",Sort.DESCENDING))
            }


            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })
        search_KnownEx_ID_editTxt.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                if(!s.isNullOrBlank())
                adapter?.updateData(realm?.where(KnownExercise::class.java)?.equalTo("user_custom_id",s.toString().trim().toInt())?.findAll())
                else adapter?.updateData(allKnownExercises!!.sort("doneInExercisesSize", Sort.DESCENDING))

            }


            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })
        add_KnownExercise_btn.setOnClickListener {
            val name=search_KnownEx_Name_editTxt.text.toString().trim().toUpperCase(Locale.getDefault())
            val idd=search_KnownEx_ID_editTxt.text.toString().trim()
            var id=0
            val nextID = allKnownExercises!!.max("user_custom_id")?.toInt()?.plus(1)
            if(!name.isBlank()) {

                if(idd.isBlank()){

                    nextID?.let {
                        id=it
                    }
                }

                 else id= idd.toInt()

                val existingID = allKnownExercises!!.find { it.user_custom_id == id }
                val existingName = allKnownExercises!!.find { it.name == name }
                if (existingName == null) {
                    if (existingID == null) {
                        realm?.let { DataHelper.createKnownExercise(it, name, id) }

                    } else {
                        Snackbar.make(findViewById(R.id.search_fields), "ID schon vergeben", Snackbar.LENGTH_LONG)
                            .setAction("Change ID") {
                                //TODO Replace by Longclick
                                var intent = Intent(this, ChangeKnownExerciseActivity::class.java)
                                intent.putExtra(EditTrainingActivity.KNOWNEXERCISE_ID, existingID.uuid)
                                startActivity(intent)
                            }.setAction("Auto ID: $nextID"){
                                nextID?.let {
                                    id=it.toInt()+1
                                    search_KnownEx_ID_editTxt.text.clear()
                                    search_KnownEx_ID_editTxt.text.append(id.toString())
                                    realm?.let { DataHelper.createKnownExercise(it, name, id) }
                                }
                            }
                            .show()

                    }

                } else {
                    Snackbar.make(findViewById(R.id.search_fields), "Name schon vergeben", Snackbar.LENGTH_LONG)
                        .setAction("Change Name") {
                            var intent = Intent(this, ChangeKnownExerciseActivity::class.java)
                            intent.putExtra(EditTrainingActivity.KNOWNEXERCISE_ID, existingName.uuid)
                            startActivity(intent)
                        }.show()
                }
            }
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

        adapter = KnownExerciseAdapter(allKnownExercises!!.sort("doneInExercisesSize", Sort.DESCENDING))
        val isViewKnown=intent.getBooleanExtra(EditTrainingActivity.VIEWKNOWNEXERCISES,false)

        if(isViewKnown){
            adapter!!.setOnItemClickListener(object: KnownExerciseAdapter.OnItemClickListener{
                override fun onItemClick(knownExercise: KnownExercise) {
//                    var chooseIntent=Intent(this@KnownExerciseListActivity,ChangeKnownExerciseActivity::class.java)
//                    chooseIntent.putExtra(EditTrainingActivity.KNOWNEXERCISE_ID,knownExercise.uuid)
//                    startActivity(chooseIntent)

                    val overviewIntent = Intent(this@KnownExerciseListActivity, KnownExerciseOverviewActivity::class.java)
                    overviewIntent.putExtra(EditTrainingActivity.KNOWNEXERCISE_ID, knownExercise.uuid)
                    startActivity(overviewIntent)
                }
            })
        }
        else{
            adapter!!.setOnItemClickListener(object : KnownExerciseAdapter.OnItemClickListener {

                override fun onItemClick(knownExercise: KnownExercise) {
                    var returnIntent = Intent()
                    returnIntent.putExtra(EditTrainingActivity.KNOWNEXERCISE_ID, knownExercise.uuid)
                    returnIntent.putExtra(EditTrainingActivity.EXERCISE_ID,intent.getLongExtra(EditTrainingActivity.EXERCISE_ID,-1))
                    setResult(Activity.RESULT_OK,returnIntent)
                    finish()
                }
            })
        }

        recyclerView!!.layoutManager = LinearLayoutManager(this)
        recyclerView!!.adapter = adapter
        recyclerView!!.setHasFixedSize(true)

    }

    }



