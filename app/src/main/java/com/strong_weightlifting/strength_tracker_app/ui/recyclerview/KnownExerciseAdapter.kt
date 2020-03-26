package com.strong_weightlifting.strength_tracker_app.ui.recyclerview

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.strong_weightlifting.strength_tracker_app.R
import com.strong_weightlifting.strength_tracker_app.model.ExerciseSet
import com.strong_weightlifting.strength_tracker_app.model.KnownExercise
import io.realm.OrderedRealmCollection
import io.realm.Realm
import io.realm.RealmRecyclerViewAdapter
import java.text.SimpleDateFormat
import kotlin.math.roundToInt


class KnownExerciseAdapter(data: OrderedRealmCollection<KnownExercise>) :
    RealmRecyclerViewAdapter<KnownExercise, KnownExerciseAdapter.MyViewHolder>(data, false) {
    private var realm: Realm?=null
    init {
        setHasStableIds(true)
        realm= Realm.getDefaultInstance()
    }


    private var listener: OnItemClickListener? = null
    var isEditMode=false


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KnownExerciseAdapter.MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.known_exercise_item, parent, false)
        return MyViewHolder(itemView)
    }


    override fun getItemId(index: Int): Long {

        return getItem(index)!!.uuid
    }


    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val knownExercise = getItem(position)
        holder.data = knownExercise
        val itemUUID = knownExercise?.uuid

        holder.knownExerciseName.text = knownExercise?.name.toString()
        val idtext="[${knownExercise?.user_custom_id.toString()}]"
        holder.user_custom_id.text = idtext
        holder.doneInTextView.text=knownExercise?.doneInExercises?.size.toString()
        var maxWeight=knownExercise?.prWeight
        var maxReps=knownExercise?.repsAtPRWeight
        var maxDate =knownExercise?.dateOfPR
        var prCalculated =knownExercise?.prCalculated?.roundToInt()
        val dateformatted=if(maxDate!=null)SimpleDateFormat("EEE, d MMM yyyy").format(maxDate) else ""
        val textPR="$dateformatted : $prCalculated kg  <-- == "
        holder.prCalculatedTextView.text=textPR

        holder.prWeightEditText.text.clear()
        holder.prWeightEditText.text.append(maxWeight.toString())
        holder.prWeightEditText.isEnabled = isEditMode
        holder.prWeightEditText.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if(!s.isNullOrEmpty()) {
                    if (s.toString() != holder.data?.prWeight.toString()) {
                        realm?.executeTransaction {
                            holder.data?.prWeight =
                            holder.prWeightEditText.text.toString().trim().toInt()
                            holder.data?.prCalculated=ExerciseSet.epleyValue(holder.data?.prWeight!!, holder.data?.repsAtPRWeight!!)
                        }

                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        holder.prRepsEditText.text.clear()
        holder.prRepsEditText.text.append(maxReps.toString())
        holder.prRepsEditText.isEnabled = isEditMode
        holder.prRepsEditText.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if(!s.isNullOrEmpty()) {
                    if (s.toString() != holder.data?.repsAtPRWeight.toString()) {
                        realm?.executeTransaction {
                            holder.data?.repsAtPRWeight =
                            holder.prRepsEditText.text.toString().trim().toInt()
                            holder.data?.prCalculated=ExerciseSet.epleyValue(holder.data?.prWeight!!, holder.data?.repsAtPRWeight!!)
                        }

                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    interface OnItemClickListener {
        fun onItemClick(knownExercise: KnownExercise)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val user_custom_id: TextView = itemView.findViewById(R.id.knownExerciseRV_ID)
        val knownExerciseName: TextView = itemView.findViewById(R.id.knownExerciseRV_Name)
        var data: KnownExercise? = null
        val doneInTextView: TextView = itemView.findViewById(R.id.doneInNumber_TextView)
        val prWeightEditText: EditText =itemView.findViewById(R.id.PR_Weight_EditText)
        val prRepsEditText: EditText=itemView.findViewById(R.id.PR_Reps_EditText)
        val prCalculatedTextView: TextView = itemView.findViewById(R.id.prDateTextView)
        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    getItem(position)?.let { it1 -> listener?.onItemClick(it1) }
                }
            }
        }

    }


}
