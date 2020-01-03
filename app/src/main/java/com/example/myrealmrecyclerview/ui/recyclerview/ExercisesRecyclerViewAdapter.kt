package com.example.myrealmrecyclerview.ui.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myrealmrecyclerview.R
import com.example.myrealmrecyclerview.model.Exercise
import com.example.myrealmrecyclerview.model.Training
import io.realm.OrderedRealmCollection
import io.realm.RealmRecyclerViewAdapter
import java.util.*

class ExercisesRecyclerViewAdapter(data: OrderedRealmCollection<Exercise>) :
    RealmRecyclerViewAdapter<Exercise, ExercisesRecyclerViewAdapter.MyViewHolder>(data, true) {
init {
    setHasStableIds(true)

}
    private var inDeletionMode = false
    val uuidsToDelete: MutableSet<Long> = HashSet()

    private var listener: OnItemClickListener? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExercisesRecyclerViewAdapter.MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.exercise_item, parent, false)
        return MyViewHolder(itemView)
    }


    override fun getItemId(index: Int): Long {

        return getItem(index)!!.uuid
    }
        fun enableDeletionMode(enabled: Boolean) {
            inDeletionMode = enabled
            if (!enabled) {
            uuidsToDelete.clear()
            }
            notifyDataSetChanged()
        }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val exercise = getItem(position)
        holder.data = exercise
        val itemUUID = exercise?.uuid

        holder.name.hint = exercise?.knownExercise?.name ?: "DefaultName"
        holder.description.text = exercise?.uuid.toString()
//        holder.deletedCheckBox.isChecked = uuidsToDelete.contains(itemUUID)
//        if (inDeletionMode) {
//            holder.deletedCheckBox.setOnCheckedChangeListener { buttonView, isChecked ->
//                if (isChecked) {
//                    if (itemUUID != null) {
//                        uuidsToDelete.add(itemUUID)
//                    }
//                } else {
//                    uuidsToDelete.remove(itemUUID)
//                }
//            }
//        } else {
//            holder.deletedCheckBox.setOnCheckedChangeListener(null)
//        }
//        holder.deletedCheckBox.visibility = if (inDeletionMode) View.VISIBLE else View.GONE
    }

    interface OnItemClickListener {
        fun onItemClick(exercise: Exercise)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

        inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val name: EditText = itemView.findViewById(R.id.exerciseName)
            val description: TextView = itemView.findViewById(R.id.sets_Text)
            var data: Exercise? = null
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
