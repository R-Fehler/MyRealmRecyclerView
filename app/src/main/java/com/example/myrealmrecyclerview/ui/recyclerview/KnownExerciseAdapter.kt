package com.example.myrealmrecyclerview.ui.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myrealmrecyclerview.R
import com.example.myrealmrecyclerview.model.KnownExercise
import io.realm.OrderedRealmCollection
import io.realm.RealmRecyclerViewAdapter
import java.util.HashSet



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
