package com.strong_weightlifting.strength_tracker_app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import androidx.recyclerview.widget.RecyclerView
import com.strong_weightlifting.strength_tracker_app.model.KnownExercise
import com.strong_weightlifting.strength_tracker_app.ui.recyclerview.KnownExerciseOverviewAdapter
import com.strong_weightlifting.strength_tracker_app.ui.recyclerview.TrainingRecyclerViewAdapter
import io.realm.Realm
import io.realm.Sort
import kotlinx.android.synthetic.main.activity_main.*

class KnownExerciseOverviewActivity : AppCompatActivity() {
    private var realm: Realm? = null
    private var recyclerView: RecyclerView? = null
    private var menu: Menu? = null
    private var adapter: KnownExerciseOverviewAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_known_exercise_overview)
        setSupportActionBar(mainToolBar)
        realm = Realm.getDefaultInstance()
        recyclerView = findViewById(R.id.recycler_view_trainings)


    }

    private fun setUpRecyclerView() {
        adapter =
            realm!!.where(KnownExercise::class.java).equalTo("uuid",intent.getLongExtra(EditTrainingActivity.KNOWNEXERCISE_ID,0))
                .findFirst()?.doneInExercises?.sort("date", Sort.DESCENDING)?.let{
                KnownExerciseOverviewAdapter(it) }
    }
}
