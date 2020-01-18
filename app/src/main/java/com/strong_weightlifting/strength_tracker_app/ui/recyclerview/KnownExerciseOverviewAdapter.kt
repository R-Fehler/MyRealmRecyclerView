package com.strong_weightlifting.strength_tracker_app.ui.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.strong_weightlifting.strength_tracker_app.R
import com.strong_weightlifting.strength_tracker_app.model.Exercise
import io.realm.OrderedRealmCollection
import io.realm.Realm
import io.realm.RealmRecyclerViewAdapter
import java.text.SimpleDateFormat
import java.util.*

class KnownExerciseOverviewAdapter(data: OrderedRealmCollection<Exercise>) :
    RealmRecyclerViewAdapter<Exercise, KnownExerciseOverviewAdapter.MyViewHolder>(data, false) {


    //    private var weightTextChangedListener:onWeightTextChangedListener?=null
    private var realm: Realm? = null
    private val viewPool = RecyclerView.RecycledViewPool()


    init {
        setHasStableIds(true)
        realm = Realm.getDefaultInstance()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.exercises_overview_item, parent, false)
        return MyViewHolder(itemView)    }

    override fun getItemId(index: Int): Long {

        return getItem(index)!!.uuid
    }
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val exercise = getItem(position)
        holder.data = exercise
        val itemUUID = exercise?.uuid
        holder.date.text = SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss", Locale.getDefault()).format(holder.data?.date ?: 0)
        holder.notes.text=holder.data?.notes


        //Child ExerciseSet RV

        val childManager = LinearLayoutManager(holder.recyclerView.context)
        val childAdapter = holder.data?.sets?.let { ExerciseSetOverViewAdapter(it) }
        holder.recyclerView.apply {
            layoutManager = childManager
            adapter = childAdapter

            setRecycledViewPool(viewPool)
        }
            .setHasFixedSize(true)
    }


    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val date: TextView = itemView.findViewById(R.id.exerciseOverViewDate)
        var data: Exercise? = null
        val recyclerView: RecyclerView = itemView.findViewById(R.id.exerciseOverViewRV)
        val menu: TextView = itemView.findViewById(R.id.exerciseOverViewItemOptions)
        val notesHeader: TextView = itemView.findViewById(R.id.notesExerciseOverViewHeader)
        val notes: TextView = itemView.findViewById(R.id.ExerciseOverViewNotesTextView)
    }
}


