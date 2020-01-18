package com.strong_weightlifting.strength_tracker_app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.strong_weightlifting.strength_tracker_app.model.KnownExercise
import com.strong_weightlifting.strength_tracker_app.ui.recyclerview.KnownExerciseOverviewAdapter
import com.strong_weightlifting.strength_tracker_app.ui.recyclerview.TrainingRecyclerViewAdapter
import io.realm.Realm
import io.realm.Sort
import kotlinx.android.synthetic.main.activity_known_exercise_overview.*
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*

class KnownExerciseOverviewActivity : AppCompatActivity() {
    private var realm: Realm? = null
    private var recyclerView: RecyclerView? = null
    private var menu: Menu? = null
    private var adapter: KnownExerciseOverviewAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_known_exercise_overview)
        setSupportActionBar(exerciseOverViewToolbar)
        realm = Realm.getDefaultInstance()
        recyclerView = findViewById(R.id.recycler_view_exercisesOverView)

        val known=realm!!.where(KnownExercise::class.java)
            .equalTo("uuid",intent.getLongExtra(EditTrainingActivity.KNOWNEXERCISE_ID,0)).findFirst()

        known?.let { setUpRecyclerView(it) }
        KnownExerciseOverViewName.text=known?.name
        val idstring="[${known?.user_custom_id.toString()}]"
        KnownExerciseOverViewID.text=idstring
        var maxWeight=0
        var maxReps=0
        var maxDate: Date = Date()
        for(ex in known?.doneInExercises!!){
            for(set in ex.sets){
                if(maxWeight<set.weight && set.isDone &&set.reps>0){
                    maxWeight=set.weight
                    maxReps=set.reps
                    maxDate=ex.date
                }
            }
        }
        val dateformatted= SimpleDateFormat("EEE, d MMM yyyy").format(maxDate)
        val textPR="PR: $dateformatted : $maxWeight kg / $maxReps "
        KnownOverViewPRtextview.text=textPR




    }

    private fun setUpRecyclerView(known:KnownExercise) {
        adapter =
            known.doneInExercises?.sort("date", Sort.DESCENDING)?.let{
                KnownExerciseOverviewAdapter(it) }

        recyclerView!!.layoutManager = LinearLayoutManager(this)
        recyclerView!!.adapter = adapter
        recyclerView!!.setHasFixedSize(true)
    }
}
