package com.example.myrealmrecyclerview.ui.recyclerview

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myrealmrecyclerview.R
import com.example.myrealmrecyclerview.model.Training
import io.realm.OrderedRealmCollection
import io.realm.RealmRecyclerViewAdapter
import java.text.SimpleDateFormat
import java.util.*

class TrainingRecyclerViewAdapter(data: OrderedRealmCollection<Training>) :
    RealmRecyclerViewAdapter<Training, TrainingRecyclerViewAdapter.MyViewHolder>(data, true) {
init {
    setHasStableIds(true)

}
    private var inDeletionMode = false
    val uuidsToDelete: MutableSet<Long> = HashSet()

    private var listener: OnItemClickListener? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrainingRecyclerViewAdapter.MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.training_item, parent, false)
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
        val training = getItem(position)
        holder.data = training
        val itemUUID = training?.uuid

        holder.date.text =SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss").format(training?.date)

        holder.description.text = training?.exercises.toString()
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
        fun onItemClick(training: Training)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

        inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val date: TextView = itemView.findViewById(R.id.date)
            val description: TextView = itemView.findViewById(R.id.description)
            var data: Training? = null
            val deletedCheckBox: CheckBox = itemView.findViewById(R.id.checkBox)
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
