package com.example.myrealmrecyclerview.ui.recyclerview

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import androidx.core.text.isDigitsOnly
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.example.myrealmrecyclerview.R
import com.example.myrealmrecyclerview.model.Exercise
import com.example.myrealmrecyclerview.model.ExerciseSet
import io.realm.OrderedRealmCollection
import io.realm.Realm
import io.realm.RealmRecyclerViewAdapter

class ExerciseSetAdapter(data: OrderedRealmCollection<ExerciseSet>) :
    RealmRecyclerViewAdapter<ExerciseSet, ExerciseSetAdapter.MyViewHolder>(data, true) {
//    private var weightTextChangedListener:onWeightTextChangedListener?=null
    private var realm: Realm?=null
    init {
        setHasStableIds(true)
        realm= Realm.getDefaultInstance()
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseSetAdapter.MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.exercise_set_item, parent, false)
        return MyViewHolder(itemView)
    }
//TODO nochmal ansehen und denken
//    interface onWeightTextChangedListener{
//        fun onWeightTextChanged(exerciseSet :ExerciseSet)
//    }
//
//    fun setOnWeightTextChangedListener(listener: onWeightTextChangedListener){
//        this.weightTextChangedListener=listener
//    }

    override fun getItemId(index: Int): Long {

        return getItem(index)!!.uuid
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        val ExerciseSet = getItem(position)
        holder.data = ExerciseSet
        val itemUUID = ExerciseSet?.uuid
        val bool = holder.data?.isDone

        holder.checkBox.isChecked = bool!!

        var weightString = holder.data?.weight.toString()
        var repsString = holder.data?.reps.toString()
        holder.weight.text.clear()
        holder.weight.text.insert(0, weightString)
        holder.reps.text.clear()
        holder.reps.text.insert(0, repsString)
        if (holder.checkBox.isChecked){
            holder.weight.isEnabled=false
            holder.reps.isEnabled=false
        }
        else{
            holder.weight.isEnabled=true
            holder.reps.isEnabled=true
        }
        holder.checkBox.setOnClickListener {
            if (holder.checkBox.isChecked) {
                realm?.executeTransaction { holder.data?.weight = holder.weight.text.toString().toInt() }
                realm?.executeTransaction { holder.data?.reps = holder.reps.text.toString().toInt() }
                realm?.executeTransaction{ holder.data?.isDone=true }
            } else {
                realm?.executeTransaction{holder.data?.isDone=false}
            }
        }

    }


    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val reps: EditText = itemView.findViewById(R.id.editTextView_reps)
        val weight: EditText = itemView.findViewById(R.id.editTextView_weight)
        var data: ExerciseSet? = null
        val checkBox: CheckBox=itemView.findViewById(R.id.set_done_checkbox)
        }



}
