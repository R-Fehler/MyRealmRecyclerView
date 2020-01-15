package com.strong_weightlifting.strength_tracker_app.ui.recyclerview

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.strong_weightlifting.strength_tracker_app.R
import com.strong_weightlifting.strength_tracker_app.model.Exercise
import io.realm.OrderedRealmCollection
import io.realm.Realm
import io.realm.RealmRecyclerViewAdapter

class KnownExerciseOverviewAdapter(data: OrderedRealmCollection<Exercise>) :
    RealmRecyclerViewAdapter<Exercise, KnownExerciseOverviewAdapter.MyViewHolder>(data, false) {


    //    private var weightTextChangedListener:onWeightTextChangedListener?=null
    private var realm: Realm? = null

    init {
        setHasStableIds(true)
        realm = Realm.getDefaultInstance()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KnownExerciseOverviewAdapter.MyViewHolder {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onBindViewHolder(holder: KnownExerciseOverviewAdapter.MyViewHolder, position: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.exerciseName)
        val user_custom_id: TextView = itemView.findViewById(R.id.exerciseName_ID)
        var data: Exercise? = null
        val recyclerView: RecyclerView = itemView.findViewById(R.id.exerciseSetRV)
        val add_btn: Button = itemView.findViewById(R.id.add_set_btn)
        val menu: TextView = itemView.findViewById(R.id.exerciseItemOptions)
        val notesHeader: TextView = itemView.findViewById(R.id.notesExerciseHeader)
        val notes: TextView = itemView.findViewById(R.id.ExerciseNotesTextView)

        init {
        }
    }
}


