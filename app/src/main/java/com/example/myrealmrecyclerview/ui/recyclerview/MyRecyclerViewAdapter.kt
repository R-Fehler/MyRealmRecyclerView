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
import io.realm.RealmRecyclerViewAdapter
import java.util.*

class MyRecyclerViewAdapter(data: OrderedRealmCollection<Training>) :
    RealmRecyclerViewAdapter<Training, MyRecyclerViewAdapter.MyViewHolder>(data, true) {
init {
    setHasStableIds(true)

}
    private var inDeletionMode = false
    val uuidsToDelete: MutableSet<Long> = HashSet()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyRecyclerViewAdapter.MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.training_item, parent, false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val training = getItem(position)
        holder.data = training
        val itemUUID = training?.uuid

        holder.date.text = training?.date.toString()
        holder.description.text = training?.uuid.toString()
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


        inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val date: TextView = itemView.findViewById(R.id.date)
            val description: TextView = itemView.findViewById(R.id.description)
            var data: Training? = null
            val deletedCheckBox: CheckBox = itemView.findViewById(R.id.checkBox)


        }


    }
