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

class ExerciseSetAdapter(data: OrderedRealmCollection<ExerciseSet>) :
    RealmRecyclerViewAdapter<ExerciseSet, ExerciseSetAdapter.MyViewHolder>(data, false) {
//    private var weightTextChangedListener:onWeightTextChangedListener?=null
    private var realm: Realm?=null
    init {
        setHasStableIds(true)
        realm= Realm.getDefaultInstance()
    }

    interface EditTextActionListener {
        fun onAction(position: Int): Boolean
    }

    var editTextActionListener: EditTextActionListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseSetAdapter.MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.exercise_set_item, parent, false)
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
        val bool = holder.data?.isDone

        holder.checkBox.isChecked = bool!!
        if (holder.checkBox.isChecked){
            holder.weightEditText.isEnabled=false
            holder.repsEditText.isEnabled=false
        }
        else{
            holder.weightEditText.isEnabled=true
            holder.repsEditText.isEnabled=true
        }
        val prevWeight= holder.data?.weightPlanned
        val prevReps= holder.data?.repsPlanned
        val prevtxt="$prevWeight kg / $prevReps "
        holder.prev.text=prevtxt

        val weightString = holder.data?.weight.toString()
        val repsString = holder.data?.reps.toString()



        holder.weightEditText.text.clear()
        holder.weightEditText.text.insert(0, weightString)
        holder.weightEditText.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                if(!s.isNullOrEmpty()) {
                    if (s.toString() != holder.data?.weight.toString()) {
                        realm?.executeTransaction { holder.data?.weight = holder.weightEditText.text.toString().trim().toInt() }
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        holder.repsEditText.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                if(!s.isNullOrEmpty()) {
                    if (s.toString() != holder.data?.reps.toString()) {
                        realm?.executeTransaction { holder.data?.reps = holder.repsEditText.text.toString().trim().toInt() }
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        holder.repsEditText.text.clear()
        holder.repsEditText.text.insert(0, repsString)


        holder.checkBox.setOnClickListener {
            realm?.executeTransaction{ holder.data?.isDone = holder.checkBox.isChecked }

            if (holder.checkBox.isChecked){
                holder.weightEditText.isEnabled=false
                holder.repsEditText.isEnabled=false
            }
            else{
                holder.weightEditText.isEnabled=true
                holder.repsEditText.isEnabled=true
            }
        }


    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val repsEditText: EditText = itemView.findViewById(R.id.editTextView_reps)
        val weightEditText: EditText = itemView.findViewById(R.id.editTextView_weight)
        var data: ExerciseSet? = null
        val checkBox: CheckBox=itemView.findViewById(R.id.set_done_checkbox)
        val prev: TextView=itemView.findViewById(R.id.previousSet_TextView)
        fun save(set:ExerciseSet){
            realm?.executeTransaction { set.weight = weightEditText.text.toString().toInt() }
            realm?.executeTransaction { set.reps = repsEditText.text.toString().toInt() }
            realm?.executeTransaction { set.isDone = checkBox.isChecked }
        }


        }



}
