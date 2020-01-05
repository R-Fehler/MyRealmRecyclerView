package com.example.myrealmrecyclerview.ui.recyclerview

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myrealmrecyclerview.R
import com.example.myrealmrecyclerview.model.DataHelper
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
    private var addSetListener: OnAddClickListener?=null

    //  Nested RV
    private val viewPool = RecyclerView.RecycledViewPool()

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

    interface OnItemClickListener {

        fun onItemClick(exercise: Exercise)
    }
    interface OnAddClickListener {
        fun onAddClick(uuid:Long,adapter: ExerciseSetAdapter)
    }
    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    fun setAddClickListener(listener: OnAddClickListener){
        this.addSetListener=listener
    }
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val exercise = getItem(position)
        holder.data = exercise
        val itemUUID = exercise?.uuid


        //Child ExerciseSet RV

        val childManager=LinearLayoutManager(holder.recyclerView.context)
        val childAdapter= holder.data?.sets?.let { ExerciseSetAdapter(it) }
        holder.add_btn.setOnClickListener{
            holder.data?.uuid?.let { it1 -> childAdapter?.let { it2 -> addSetListener?.onAddClick(it1, it2) } }
        }

        holder.recyclerView.apply {
            layoutManager=childManager
            adapter=childAdapter

            setRecycledViewPool(viewPool)
        }
            .setHasFixedSize(true)
        holder.recyclerView.addItemDecoration(DividerItemDecoration(holder.recyclerView.context,DividerItemDecoration.VERTICAL))

        holder.name.hint = exercise?.knownExercise?.name ?: "DefaultName"
        holder.description.text = exercise?.uuid.toString()
    }

        inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val name: EditText = itemView.findViewById(R.id.exerciseName)
            val description: TextView = itemView.findViewById(R.id.sets_Text)
            var data: Exercise? = null
            val recyclerView: RecyclerView = itemView.findViewById(R.id.exerciseSetRV)
            val add_btn: Button =itemView.findViewById(R.id.add_set_btn)

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
