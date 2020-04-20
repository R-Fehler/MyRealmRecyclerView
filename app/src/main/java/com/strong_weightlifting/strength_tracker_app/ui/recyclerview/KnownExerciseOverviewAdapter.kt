package com.strong_weightlifting.strength_tracker_app.ui.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.strong_weightlifting.strength_tracker_app.R
import com.strong_weightlifting.strength_tracker_app.model.Exercise
import com.strong_weightlifting.strength_tracker_app.model.Training
import io.realm.OrderedRealmCollection
import io.realm.Realm
import io.realm.RealmRecyclerViewAdapter
import org.w3c.dom.Text
import java.text.SimpleDateFormat
import java.util.*

class KnownExerciseOverviewAdapter(data: OrderedRealmCollection<Exercise>) :
    RealmRecyclerViewAdapter<Exercise, KnownExerciseOverviewAdapter.MyViewHolder>(data, false) {


    //    private var weightTextChangedListener:onWeightTextChangedListener?=null
    private var realm: Realm? = null
    private val viewPool = RecyclerView.RecycledViewPool()
    private var listener: OnItemClickListener? = null
    var showShortVersion=false


    init {
        setHasStableIds(true)
        realm = Realm.getDefaultInstance()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.exercises_overview_item, parent, false)
        return MyViewHolder(itemView)
    }

    override fun getItemId(index: Int): Long {

        return getItem(index)!!.uuid
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val exercise = getItem(position)
        holder.data = exercise
        val itemUUID = exercise?.uuid
        holder.date.text =
            SimpleDateFormat("EEE, d MMM yyyy HH:mm", Locale.getDefault()).format(holder.data?.date ?: 0)
        if(holder.data?.notes.isNullOrBlank().not()) {
            holder.notes.text = holder.data?.notes
            holder.notes.visibility=View.VISIBLE
            holder.notesHeader.visibility=View.VISIBLE
        }
        else{
            holder.notes.visibility=View.GONE
            holder.notesHeader.visibility=View.GONE
        }
        var volume=0
        holder.data?.sets?.forEach {
            volume+=it.weight*it.reps
        }
        val volumeText="Volume: $volume kg"
        holder.volume.text=volumeText

        holder.short.text=holder.data?.toShortString()

        holder.name.text=holder.data?.doneInTrainings?.first()?.name

        //Child ExerciseSet RV
        if (showShortVersion){
            holder.short.visibility=View.VISIBLE
            holder.recyclerView.visibility=View.GONE
        }
        else {
            holder.short.visibility=View.GONE
            holder.recyclerView.visibility=View.VISIBLE
            val childManager = LinearLayoutManager(holder.recyclerView.context)
            val childAdapter = holder.data?.sets?.let { ExerciseSetOverViewAdapter(it) }
            holder.recyclerView.apply {
                layoutManager = childManager
                adapter = childAdapter

                setRecycledViewPool(viewPool)
            }
                .setHasFixedSize(true)
        }
    }

    interface OnItemClickListener {
        fun onItemClick(exercise: Exercise)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }


    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val date: TextView = itemView.findViewById(R.id.exerciseOverViewDate)
        val name: TextView = itemView.findViewById(R.id.exerciseOverViewTrainingName)
        val volume: TextView = itemView.findViewById(R.id.ExerciseOverViewTotalVolume)
        val short: TextView = itemView.findViewById(R.id.ExerciseOverViewShort)
        var data: Exercise? = null
        val recyclerView: RecyclerView = itemView.findViewById(R.id.exerciseOverViewRV)
        val menu: TextView = itemView.findViewById(R.id.exerciseOverViewItemOptions)
        val notesHeader: TextView = itemView.findViewById(R.id.notesExerciseOverViewHeader)
        val notes: TextView = itemView.findViewById(R.id.ExerciseOverViewNotesTextView)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    getItem(position)?.let { it -> listener?.onItemClick(it) }
                }
            }
        }
    }
}

