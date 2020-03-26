package com.strong_weightlifting.strength_tracker_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.strong_weightlifting.strength_tracker_app.model.DataHelper
import com.strong_weightlifting.strength_tracker_app.model.KnownExercise
import com.strong_weightlifting.strength_tracker_app.ui.recyclerview.KnownExerciseOverviewAdapter
import io.realm.Realm
import io.realm.Sort
import kotlinx.android.synthetic.main.activity_known_exercise_overview.*
import java.text.SimpleDateFormat
import kotlin.math.roundToInt

class KnownExerciseOverviewActivity : AppCompatActivity() {
    private var realm: Realm? = null
    private var recyclerView: RecyclerView? = null
    private var menu: Menu? = null
    private var adapter: KnownExerciseOverviewAdapter? = null
    private var knownExercise: KnownExercise?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_known_exercise_overview)
        setSupportActionBar(exerciseOverViewToolbar)
        realm = Realm.getDefaultInstance()
        recyclerView = findViewById(R.id.recycler_view_exercisesOverView)

         knownExercise=realm!!.where(KnownExercise::class.java)
            .equalTo("uuid",intent.getLongExtra(EditTrainingActivity.KNOWNEXERCISE_ID,0)).findFirst()

        knownExercise?.let { setUpRecyclerView(it) }
        KnownExerciseOverViewName.text=knownExercise?.name
        val idstring="[${knownExercise?.user_custom_id.toString()}]"
        KnownExerciseOverViewID.text=idstring
        var maxWeight=knownExercise?.prWeight
        var maxReps=knownExercise?.repsAtPRWeight
        var maxDate =knownExercise?.dateOfPR
        var prCalculated =knownExercise?.prCalculated?.roundToInt()
        val dateformatted= SimpleDateFormat("EEE, d MMM yyyy").format(maxDate)
        val textPR="PR: $dateformatted : $maxWeight kg / $maxReps --> $prCalculated "
        KnownOverViewPRtextview.text=textPR



    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menu = menu
        menuInflater.inflate(R.menu.knownexercise_overview_menu, menu)
        menu.setGroupVisible(R.id.group_normal_mode, true)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {

            R.id.action_editKnownExercises -> {
                var intent = Intent(this, ChangeKnownExerciseActivity::class.java)
                intent.putExtra(EditTrainingActivity.KNOWNEXERCISE_ID, knownExercise?.uuid)
                startActivity(intent)

                return true
            }

            R.id.action_delete_KnownExercise -> {
                if(knownExercise?.doneInExercisesSize==0) {
                    DataHelper.deleteKnownExerciseSafely(this.realm!!, this.knownExercise?.uuid!!)
                    finish()
                }
                else Toast.makeText(this,"can't delete Exercise if already done, edit --> rename?",Toast.LENGTH_LONG).show()

                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun setUpRecyclerView(known:KnownExercise) {
        adapter =
            known.doneInExercises?.sort("date", Sort.DESCENDING)?.let{
                KnownExerciseOverviewAdapter(it) }

        recyclerView!!.layoutManager = LinearLayoutManager(this)
        recyclerView!!.adapter = adapter
        recyclerView!!.setHasFixedSize(true)
    }

    override fun onResume() {
        adapter?.updateData(adapter?.data)
        super.onResume()
    }
}
