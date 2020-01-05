package com.example.myrealmrecyclerview.ui.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myrealmrecyclerview.R
import com.example.myrealmrecyclerview.model.ExerciseSet
import com.example.myrealmrecyclerview.model.Training
import io.realm.OrderedRealmCollection
import io.realm.RealmRecyclerViewAdapter
import java.util.*

class ExerciseSetAdapter(data: OrderedRealmCollection<ExerciseSet>) :
    RealmRecyclerViewAdapter<ExerciseSet, ExerciseSetAdapter.MyViewHolder>(data, true) {
    init {
        setHasStableIds(true)

    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseSetAdapter.MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.exercise_set_item, parent, false)
        return MyViewHolder(itemView)
    }


    override fun getItemId(index: Int): Long {

        return getItem(index)!!.uuid
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val ExerciseSet = getItem(position)
        holder.data = ExerciseSet
        val itemUUID = ExerciseSet?.uuid

        holder.weight.text = ExerciseSet?.weight.toString()
        holder.reps.text = ExerciseSet?.uuid.toString()

    }


    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val reps: TextView = itemView.findViewById(R.id.textView_reps)
        val weight: TextView = itemView.findViewById(R.id.textView_weight)
        var data: ExerciseSet? = null

        }



}
