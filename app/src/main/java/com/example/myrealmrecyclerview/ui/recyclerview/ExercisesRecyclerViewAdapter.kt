package com.example.myrealmrecyclerview.ui.recyclerview

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.view.forEach
import androidx.core.view.forEachIndexed
import androidx.core.view.get
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myrealmrecyclerview.EditTrainingActivity
import com.example.myrealmrecyclerview.KnownExerciseListActivity
import com.example.myrealmrecyclerview.R
import com.example.myrealmrecyclerview.model.DataHelper
import com.example.myrealmrecyclerview.model.Exercise
import com.example.myrealmrecyclerview.model.Training
import io.realm.OrderedRealmCollection
import io.realm.Realm
import io.realm.RealmRecyclerViewAdapter
import kotlinx.android.synthetic.main.exercise_item.view.*
import kotlinx.android.synthetic.main.exercise_set_item.view.*
import java.util.*

class ExercisesRecyclerViewAdapter(data: OrderedRealmCollection<Exercise>) :
    RealmRecyclerViewAdapter<Exercise, ExercisesRecyclerViewAdapter.MyViewHolder>(data, false) {


    inner class TouchHelperCallback internal constructor() :
        ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            realm?.let { DataHelper.deleteExerciseSet(it, viewHolder.itemId) }
            updateData(data)

        }

        override fun isLongPressDragEnabled(): Boolean {
            return true
        }
    }

    private var onNoteListener: OnNotesEditListener?=null
    val uuidsToDelete: MutableSet<Long> = HashSet()

    var realm: Realm? = null

    private var listener: OnItemClickListener? = null
    private var addSetListener: OnAddClickListener? = null
    private var namClickListener: OnNameClickListener? = null

    //  Nested RV
    private val viewPool = RecyclerView.RecycledViewPool()

    init {
        setHasStableIds(true)
        realm = Realm.getDefaultInstance()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExercisesRecyclerViewAdapter.MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.exercise_item, parent, false)
        return MyViewHolder(itemView)
    }


    override fun getItemId(index: Int): Long {

        return getItem(index)!!.uuid
    }



    interface OnItemClickListener {

        fun onItemClick(exercise: Exercise)
    }

    interface OnNameClickListener {

        fun onNameClick(exercise: Exercise)
    }

    interface OnAddClickListener {
        fun onAddClick(uuid: Long, adapter: ExerciseSetAdapter)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    fun setAddClickListener(listener: OnAddClickListener) {
        this.addSetListener = listener
    }

    fun setOnNameClickListener(listener: OnNameClickListener) {
        this.namClickListener = listener
    }


    interface OnNotesEditListener {
        fun onNotesEdit(exercise: Exercise)
    }
    fun setOnNotesEditListener(listener: OnNotesEditListener){
        onNoteListener=listener
    }
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val exercise = getItem(position)
        holder.data = exercise
        val itemUUID = exercise?.uuid


        //Child ExerciseSet RV

        val childManager = LinearLayoutManager(holder.recyclerView.context)
        val childAdapter = holder.data?.sets?.let { ExerciseSetAdapter(it) }
        holder.recyclerView.apply {
            layoutManager = childManager
            adapter = childAdapter

            setRecycledViewPool(viewPool)
        }
            .setHasFixedSize(true)
        holder.add_btn.setOnClickListener {
            holder.data?.uuid?.let { it1 -> childAdapter?.let { it2 -> addSetListener?.onAddClick(it1, it2) } }
            updateData(data)
        }

        val touchHelperCallback = TouchHelperCallback()
        val touchHelper = ItemTouchHelper(touchHelperCallback)
        touchHelper.attachToRecyclerView(holder.recyclerView)

        holder.name.text = exercise?.knownExercise?.name ?: "DefaultName"
        val idText="[${exercise?.knownExercise?.user_custom_id.toString()}]"
        holder.user_custom_id.text = idText

        holder.notes.text=holder.data?.notes


        val popup = PopupMenu(holder.itemView.context, holder.menu)
        popup.inflate(R.menu.exercise_menu)
        popup.setOnMenuItemClickListener {
            val id = it.itemId
            when (id) {
                R.id.action_exercise_addNote -> {
                    holder.notes.visibility = View.VISIBLE
                    holder.notesHeader.visibility = View.VISIBLE

                    onNoteListener?.onNotesEdit(holder.data!!)


                }
                R.id.action_exercise_remove_addNote -> {
                    holder.notes.visibility = View.GONE
                    holder.notesHeader.visibility = View.GONE

                }
                R.id.action_exercise_delete-> {

                        realm?.let{holder.data?.uuid?.let { it1 -> DataHelper.deleteExercise(it, it1) }}
                    this.updateData(data)
                }
            }

            true
        }
        holder.menu.setOnClickListener {
            popup.show()
        }
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.exerciseName)
        val user_custom_id: TextView = itemView.findViewById(R.id.exerciseName_ID)
        var data: Exercise? = null
        val recyclerView: RecyclerView = itemView.findViewById(R.id.exerciseSetRV)
        val add_btn: Button = itemView.findViewById(R.id.add_set_btn)
        val menu: TextView = itemView.findViewById(R.id.exerciseItemOptions)
        val notesHeader: TextView = itemView.findViewById(R.id.notesExerciseHeader)
        val notes: TextView = itemView.findViewById(R.id.ExerciseNotesTextView)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    getItem(position)?.let { it1 -> listener?.onItemClick(it1) }
                }
            }
            name.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    getItem(position)?.let { it -> namClickListener?.onNameClick(it) }
                }
            }
        }

    }


}
