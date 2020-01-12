package com.example.myrealmrecyclerview

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myrealmrecyclerview.model.DataHelper
import com.example.myrealmrecyclerview.model.KnownExercise
import com.example.myrealmrecyclerview.ui.recyclerview.KnownExerciseAdapter
import io.realm.Realm
import io.realm.RealmResults

import kotlinx.android.synthetic.main.activity_change_known_exercise.*
import kotlinx.android.synthetic.main.content_change_known_exercise.*
import kotlinx.android.synthetic.main.content_known_exercise_list.*

class ChangeKnownExerciseActivity : AppCompatActivity() {


    private var realm: Realm? = null
    private var recyclerView: RecyclerView? = null
    private var menu: Menu? = null
    private var adapter: KnownExerciseAdapter? = null
    private var knownExerciseToChangeUUID: Long? = null
    private var knownExerciseToChange: KnownExercise? = null
    private var allKnownExercises: RealmResults<KnownExercise>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_known_exercise)

        realm = Realm.getDefaultInstance()
        recyclerView = findViewById(R.id.OldKnownExerciseList_RecyclerView)
        knownExerciseToChangeUUID = intent.getLongExtra(EditTrainingActivity.KNOWNEXERCISE_ID, 0)
        knownExerciseToChange =
            realm?.where(KnownExercise::class.java)?.equalTo(KnownExercise.FIELD_UUID, knownExerciseToChangeUUID)
                ?.findFirst()
        KnownExerciseToChange_Name.text = knownExerciseToChange?.name
        KnownExerciseToChange_ID.text = knownExerciseToChange?.user_custom_id.toString()
        allKnownExercises = realm!!.where(KnownExercise::class.java).findAll()

        changeKnownExerciseName_EditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                adapter?.updateData(realm?.where(KnownExercise::class.java)?.contains("name",s.toString().trim())?.findAll()?.sort("doneInExercisesSize"))
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        setUpRecyclerView()
        changeKnown_BTN.setOnClickListener {
            val name = changeKnownExerciseName_EditText.text.toString().trim().toUpperCase()
            val id = changeKnownExerciseID_EditText.text.toString().trim().toInt()
            val resultName = allKnownExercises!!.find { it.name == name }
            val resultID = allKnownExercises!!.find { it.user_custom_id == id }
            if ((resultName == knownExerciseToChange && resultID == null) || (resultName == null && resultID == knownExerciseToChange)) {
                realm?.let {
                    DataHelper.changeKnownExercise(it, knownExerciseToChangeUUID!!, name, id)
                }
            } else {
                Snackbar.make(
                    findViewById(R.id.NewNameAndID_LinearLayout),
                    "ID oder Name schon vergeben",
                    Snackbar.LENGTH_LONG
                )
                    .setAction("!!!", null).show()

            }
        }


    }

    private fun setUpRecyclerView() {
        adapter = KnownExerciseAdapter(allKnownExercises!!.sort("doneInExercisesSize"))
        adapter!!.setOnItemClickListener(object : KnownExerciseAdapter.OnItemClickListener {
            override fun onItemClick(knownExercise: KnownExercise) {
                knownExerciseToChangeUUID = knownExercise.uuid
                knownExerciseToChange = realm?.where(KnownExercise::class.java)
                    ?.equalTo(KnownExercise.FIELD_UUID, knownExerciseToChangeUUID)?.findFirst()
                KnownExerciseToChange_Name.text = knownExerciseToChange?.name
                KnownExerciseToChange_ID.text = knownExerciseToChange?.user_custom_id.toString()

            }
        })
        recyclerView!!.layoutManager = LinearLayoutManager(this)
        recyclerView!!.adapter = adapter
        recyclerView!!.setHasFixedSize(true)

    }

}
