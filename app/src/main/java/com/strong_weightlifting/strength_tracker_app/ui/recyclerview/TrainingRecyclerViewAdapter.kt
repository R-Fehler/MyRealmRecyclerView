package com.strong_weightlifting.strength_tracker_app.ui.recyclerview

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.strong_weightlifting.strength_tracker_app.MainActivity
import com.strong_weightlifting.strength_tracker_app.R
import com.strong_weightlifting.strength_tracker_app.model.DataHelper
import com.strong_weightlifting.strength_tracker_app.model.Training
import io.realm.OrderedRealmCollection
import io.realm.Realm
import io.realm.RealmRecyclerViewAdapter
import kotlinx.android.synthetic.main.training_item.view.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class TrainingRecyclerViewAdapter(data: OrderedRealmCollection<Training>) :
    RealmRecyclerViewAdapter<Training, TrainingRecyclerViewAdapter.MyViewHolder>(data, false) {
    private var listener: OnItemClickListener? = null
    private var onNoteListener: OnNotesEditListener?=null
    private var onDateListener: OnDateClickListener?=null
    private var onItemLongClickListener: OnItemLongClickListener?=null
    private var onCreateRoutineFailedListener: OnCreateRoutineListener?=null
    var showRoutines=false


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

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val training = getItem(position)
        holder.data = training
        val itemUUID = training?.uuid

        holder.date.text = SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss").format(training?.date)
        holder.date.setOnClickListener {
            holder.data?.let { it1 -> onDateListener?.onDateClicked(it1) }
        }

        holder.itemView.setOnLongClickListener {
            holder.data?.let { it1 -> onItemLongClickListener?.onItemLongClick(it1) }
            true
        }
        var text=""
        for(exercise in holder.data?.exercises!!){
            text += exercise.toString() +"\n"
        }
        val namePattern= Pattern.compile("\\[\\d*][^:]*")
        val nameMatcher=namePattern.matcher(text)
        val prPattern=Pattern.compile("\\(PR\\)")
        val prMatcher=prPattern.matcher(text)
        val str=SpannableStringBuilder(text)
        while(nameMatcher.find()){
            str.setSpan(StyleSpan(android.graphics.Typeface.BOLD),nameMatcher.start(),nameMatcher.end(), Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        }
        while (prMatcher.find()){
            str.setSpan(StyleSpan(android.graphics.Typeface.BOLD),prMatcher.start(),prMatcher.end(),Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        }


        holder.description.text =str
        holder.notes.text= holder.data?.notes
        holder.nameOfTraining.text=holder.data?.name
        if(holder.data?.isRoutine==true){
            holder.nameOfTraining.textSize=20F
            holder.date.date.textSize=0F
        }
        else{
            holder.nameOfTraining.textSize=12F
            holder.date.date.textSize=16F
        }
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

//        if (holder.notes.text.isNullOrBlank()){
//            holder.notes.visibility=View.GONE
//            holder.notesHeader.visibility=View.GONE
//        }

        val popup= PopupMenu(holder.itemView.context,holder.menu)
        popup.inflate(R.menu.training_menu)
        popup.setOnMenuItemClickListener { item ->
            val id=item.itemId
            when(id) {
                R.id.action_training_addNote -> {
//                    holder.notes.visibility=View.VISIBLE
//                    holder.notesHeader.visibility=View.VISIBLE

                    onNoteListener?.onNotesEdit(holder.data!!)


                }
                R.id.action_training_delete ->{
                    realm?.let { it1 -> holder.data?.uuid?.let { it2 -> DataHelper.deleteTraining(it1, it2) } }
                    this.updateData(data)
                }

                R.id.action_copy_training ->{
                    DataHelper.copyTraining(realm!!, holder.data!!)
                    this.updateData(this.data)
                }
                R.id.action_star -> {
                   var createdSuccess=false
                    realm?.executeTransaction {  createdSuccess=Training.createAsRoutine(it, holder.data!!) }
                    if(!createdSuccess){
                        onCreateRoutineFailedListener?.onCreateRoutineFailed(holder.data!!)
                    }
                }
            }

            true
        }
    holder.menu.setOnClickListener {
        popup.show()
    }

        popup.menu.findItem(R.id.action_star).isVisible = showRoutines.not()

    }


    interface OnItemClickListener {
        fun onItemClick(training: Training)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    interface OnNotesEditListener {
        fun onNotesEdit(training: Training)
    }
    fun setOnNotesEditListener(listener: OnNotesEditListener){
        onNoteListener=listener
    }
    
    interface OnDateClickListener {
        fun onDateClicked(training: Training)
    }
    
    fun setOnDateClickListener(listener:OnDateClickListener){
        this.onDateListener=listener
    }

    interface OnItemLongClickListener{
        fun onItemLongClick(training: Training)
    }

    fun setOnItemLongClickListener(listener:OnItemLongClickListener){
        this.onItemLongClickListener=listener
    }

    interface OnCreateRoutineListener{
        fun onCreateRoutineFailed(training: Training)
    }
    fun setOnCreateRoutineListener(listener: OnCreateRoutineListener){
        this.onCreateRoutineFailedListener=listener
    }

        inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val date: TextView = itemView.findViewById(R.id.date)
            val description: TextView = itemView.findViewById(R.id.description)
            var data: Training? = null
            val isDoneCheckBox: CheckBox = itemView.findViewById(R.id.isDoneCheckBox)
            val nameOfTraining: TextView=itemView.findViewById(R.id.nameOfTraining_TextView)
            val menu: TextView = itemView.findViewById(R.id.trainingItemOptions)
            val notes: TextView = itemView.findViewById(R.id.trainingNotesTextView)
            val notesHeader= itemView.notesTrainingHeader
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
