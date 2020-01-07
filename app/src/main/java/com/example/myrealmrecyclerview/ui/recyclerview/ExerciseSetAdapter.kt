package com.example.myrealmrecyclerview.ui.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.example.myrealmrecyclerview.R
import com.example.myrealmrecyclerview.model.ExerciseSet
import io.realm.OrderedRealmCollection
import io.realm.RealmRecyclerViewAdapter

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

        holder.weight.hint = ExerciseSet?.weight.toString()
        holder.reps.hint = ExerciseSet?.uuid.toString()

    }


    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val reps: EditText = itemView.findViewById(R.id.editTextView_reps)
        val weight: EditText = itemView.findViewById(R.id.editTextView_weight)
        var data: ExerciseSet? = null

        }



}
