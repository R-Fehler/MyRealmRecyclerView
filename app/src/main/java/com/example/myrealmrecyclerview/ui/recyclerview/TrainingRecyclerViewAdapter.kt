package com.example.myrealmrecyclerview.ui.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myrealmrecyclerview.R
import com.example.myrealmrecyclerview.model.Training
import io.realm.OrderedRealmCollection
import io.realm.Realm
import io.realm.RealmRecyclerViewAdapter
import java.text.SimpleDateFormat
import java.util.*

class TrainingRecyclerViewAdapter(data: OrderedRealmCollection<Training>) :
    RealmRecyclerViewAdapter<Training, TrainingRecyclerViewAdapter.MyViewHolder>(data, true) {
    private var inDeletionMode = false
    val uuidsToDelete: MutableSet<Long> = HashSet()
    private var listener: OnItemClickListener? = null
    var realm: Realm?=null

    init {
        setHasStableIds(true)
        realm=Realm.getDefaultInstance()

    }

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
        holder.isDoneCheckBox.isChecked= holder.data?.isDone!!

        holder.isDoneCheckBox.setOnClickListener {
            realm?.executeTransaction{holder.data?.isDone=holder.isDoneCheckBox.isChecked
            if(holder.isDoneCheckBox.isChecked){
                for(exercise in holder.data?.exercises!!){
                    for(set in exercise.sets) {
                        set.isDone = true
                    }
                    }
                }
            }
        }
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
            val isDoneCheckBox: CheckBox = itemView.findViewById(R.id.isDoneCheckBox)
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
