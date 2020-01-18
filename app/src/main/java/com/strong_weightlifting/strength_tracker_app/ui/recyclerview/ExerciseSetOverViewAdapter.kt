package com.strong_weightlifting.strength_tracker_app.ui.recyclerview

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.strong_weightlifting.strength_tracker_app.R
import com.strong_weightlifting.strength_tracker_app.model.ExerciseSet
import io.realm.OrderedRealmCollection
import io.realm.Realm
import io.realm.RealmRecyclerViewAdapter

class ExerciseSetOverViewAdapter(data: OrderedRealmCollection<ExerciseSet>) :
    RealmRecyclerViewAdapter<ExerciseSet, ExerciseSetOverViewAdapter.MyViewHolder>(data, false) {
    //    private var weightTextChangedListener:onWeightTextChangedListener?=null
    private var realm: Realm?=null
    init {
        setHasStableIds(true)
        realm= Realm.getDefaultInstance()
    }




    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.exercise_overview_set_item, parent, false)
        return MyViewHolder(itemView)
    }


    override fun getItemId(index: Int): Long {

        return getItem(index)!!.uuid
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.itemView.parent
        val exerciseSet = getItem(position)
        holder.data = exerciseSet
        val itemUUID = exerciseSet?.uuid



        val weightString = holder.data?.weight.toString()
        val repsString = holder.data?.reps.toString()
        holder.weightEditText.text=weightString
        holder.repsEditText.text=repsString
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val weightEditText: TextView = itemView.findViewById(R.id.setOverViewWeight)
        val repsEditText: TextView = itemView.findViewById(R.id.setOverViewReps)
        var data: ExerciseSet? = null



    }



}
