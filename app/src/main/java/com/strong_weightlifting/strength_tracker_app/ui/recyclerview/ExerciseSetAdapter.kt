package com.strong_weightlifting.strength_tracker_app.ui.recyclerview

import android.annotation.SuppressLint
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.strong_weightlifting.strength_tracker_app.R
import com.strong_weightlifting.strength_tracker_app.model.ExerciseSet
import io.realm.OrderedRealmCollection
import io.realm.Realm
import io.realm.RealmRecyclerViewAdapter
import kotlin.math.roundToInt


class ExerciseSetAdapter(data: OrderedRealmCollection<ExerciseSet>) :
    RealmRecyclerViewAdapter<ExerciseSet, ExerciseSetAdapter.MyViewHolder>(data, false) {
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


    override fun getItemId(index: Int): Long {

        return getItem(index)!!.uuid
    }

    @SuppressLint("ServiceCast")
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val exerciseSet = getItem(position)
        val exercise=exerciseSet?.doneInExercises?.first()
        val training=exercise?.doneInTrainings?.first()
        holder.data = exerciseSet
        val itemUUID = exerciseSet?.uuid
        holder.checkBox.isEnabled = !training?.isDone!!
        holder.checkBox.isChecked = holder.data?.isDone!!
        if (holder.checkBox.isChecked){
            holder.weightEditText.isEnabled=false
            holder.repsEditText.isEnabled=false
            holder.percentageWeight.isEnabled=false
        }
        else{
            holder.weightEditText.isEnabled=true
            holder.repsEditText.isEnabled=true
            holder.percentageWeight.isEnabled=true
        }
        val prevWeight= holder.data?.weightPlanned
        val prevReps= holder.data?.repsPlanned
        val prevtxt="$prevWeight kg / $prevReps "
        holder.prev.text=prevtxt

      updateEpley(holder)

//            (holder.data?.doneInExercises?.first()?.knownExercise?.prCalculated?.let {
//            holder.data?.weight?.toDouble()?.div(
//                it
//            )
//        }).roundToInt().toString()
        val weightString = holder.data?.weight.toString()
        val repsString = holder.data?.reps.toString()
        fun percentOfOneRepMaxWeight(): Int= (holder.data?.weight?.times(100))?.div((exercise.prCalculatedAtTheMoment))!!.roundToInt()
        holder.percentageWeight.text.clear()
        holder.percentageWeight.text.append(percentOfOneRepMaxWeight().toString())
        holder.percentageWeight.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                if(!s.isNullOrEmpty()) {
                    if (s.toString() != "0") {
                        val newWeight=holder.percentageWeight.text.toString().trim().toDouble()*0.01* exercise.prCalculatedAtTheMoment
                        realm?.executeTransaction { holder.data?.weightPercentOf1RM=newWeight.roundToInt() }
                        realm?.executeTransaction {  holder.data?.weightPercentageForRoutine=holder.percentageWeight.text.toString().trim().toInt()}
                        if (holder.percentageWeight.hasFocus()){
                            holder.weightEditText.text.clear()
                            holder.weightEditText.text.append(newWeight.roundToInt().toString())

                        }

                        updateEpley(holder)
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        holder.weightEditText.text.clear()
        holder.weightEditText.text.append(weightString)
        holder.weightEditText.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                if(!s.isNullOrEmpty()) {
                    if (s.toString() != holder.data?.weight.toString()) {
                        realm?.executeTransaction { holder.data?.weight = holder.weightEditText.text.toString().trim().toInt()}
                        val percentOfOneRepMW= percentOfOneRepMaxWeight()
                        if(holder.weightEditText.hasFocus()) {
                            holder.percentageWeight.text.clear()
                            holder.percentageWeight.text.append(percentOfOneRepMW.toString())
                        }
                       updateEpley(holder)
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        if(training.isRoutine && training.isDone.not()){
            holder.weightEditText.isEnabled=training.isRoutineWithPercentage.not()
            holder.percentageWeight.isEnabled=training.isRoutineWithPercentage
        }



        holder.repsEditText.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                if(!s.isNullOrEmpty()) {
                    if (s.toString() != holder.data?.reps.toString()) {
                        realm?.executeTransaction { holder.data?.reps = holder.repsEditText.text.toString().trim().toInt() }
                        updateEpley(holder)
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        holder.repsEditText.text.clear()
        holder.repsEditText.text.append(repsString)


        holder.checkBox.setOnClickListener {
            realm?.executeTransaction{ holder.data?.isDone = holder.checkBox.isChecked }

            if (holder.checkBox.isChecked){
                holder.weightEditText.isEnabled=false
                holder.repsEditText.isEnabled=false
                holder.percentageWeight.isEnabled=false
            }
            else{
                holder.weightEditText.isEnabled=true
                holder.repsEditText.isEnabled=true
                holder.percentageWeight.isEnabled=true
            }
        }


    }

    private fun updateEpley(holder: MyViewHolder) {
        val prCalculated=holder.data?.doneInExercises?.first()?.prCalculatedAtTheMoment
        val epley=ExerciseSet.epleyValue(holder.data!!)
        val percentage= (epley.times(100.0)).div(prCalculated!!)
        if(percentage>100f){
            val text=percentage.roundToInt().toString().plus("%")
            val str= SpannableStringBuilder(text.plus("\u2B50"))

            str.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(holder.prev.context, R.color.secondaryColor)),
                str.length-1,str.length,
                Spanned.SPAN_INCLUSIVE_INCLUSIVE)

            holder.percentageOfRM.text=str
        }
        else holder.percentageOfRM.text= percentage.roundToInt().toString().plus(" %")
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val repsEditText: EditText = itemView.findViewById(R.id.editTextView_reps)
        val weightEditText: EditText = itemView.findViewById(R.id.editTextView_weight)
        var data: ExerciseSet? = null
        val checkBox: CheckBox=itemView.findViewById(R.id.set_done_checkbox)
        val prev: TextView=itemView.findViewById(R.id.previousSet_TextView)
        val percentageWeight: EditText=itemView.findViewById(R.id.weightPercentOf1RM)
        val percentageSign: TextView=itemView.findViewById(R.id.percentageSign)
        val percentageOfRM: TextView=itemView.findViewById(R.id.percentage_of_EpleyRM)
        init {
            val sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(repsEditText.context)
            val percentageIsActive = sharedPreferences.getBoolean("percentageInputActive", false)
            if(!percentageIsActive){
            percentageWeight.visibility=View.GONE
            percentageSign.visibility=View.GONE
            }
        }


        }



}
