package com.strong_weightlifting.strength_tracker_app.ui.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.strong_weightlifting.strength_tracker_app.R
import com.strong_weightlifting.strength_tracker_app.model.KnownExercise
import io.realm.OrderedRealmCollection
import io.realm.RealmRecyclerViewAdapter
import java.text.SimpleDateFormat
import java.util.*


class KnownExerciseAdapter(data: OrderedRealmCollection<KnownExercise>) :
    RealmRecyclerViewAdapter<KnownExercise, KnownExerciseAdapter.MyViewHolder>(data, true) {
    init {
        setHasStableIds(true)

    }


    private var listener: OnItemClickListener? = null


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
        holder.user_custom_id.text = knownExercise?.user_custom_id.toString()
        holder.doneInTextView.text=knownExercise?.doneInExercises?.size.toString()
        var maxWeight=0
        var maxReps=0
        var maxDate: Date = Date()
        for(ex in knownExercise?.doneInExercises!!){
            for(set in ex.sets){
                if(maxWeight<set.weight && set.isDone &&set.reps>0){
                maxWeight=set.weight
                maxReps=set.reps
                    maxDate=ex.date
                }
            }
        }
        val dateformatted=SimpleDateFormat("EEE, d MMM yyyy").format(maxDate)
        val textPR="$dateformatted : $maxWeight kg / $maxReps "
        holder.prWeightTextVew.text=textPR
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
        val prWeightTextVew: TextView =itemView.findViewById(R.id.PRWeightKnown_TextView)
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
