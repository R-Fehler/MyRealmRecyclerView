package com.strong_weightlifting.strength_tracker_app

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SmoothScroller
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.strong_weightlifting.strength_tracker_app.R.drawable.*
import com.strong_weightlifting.strength_tracker_app.model.DataHelper
import com.strong_weightlifting.strength_tracker_app.model.Exercise
import com.strong_weightlifting.strength_tracker_app.model.ExerciseSet
import com.strong_weightlifting.strength_tracker_app.model.KnownExercise
import com.strong_weightlifting.strength_tracker_app.ui.recyclerview.KnownExerciseOverviewAdapter
import io.realm.Realm
import io.realm.Sort
import kotlinx.android.synthetic.main.activity_known_exercise_overview.*
import java.lang.Math.abs
import java.text.SimpleDateFormat
import kotlin.math.roundToInt


class KnownExerciseOverviewActivity : AppCompatActivity(), OnChartValueSelectedListener {
    private var realm: Realm? = null
    private var recyclerView: RecyclerView? = null
    private var menu: Menu? = null
    private var adapter: KnownExerciseOverviewAdapter? = null
    private var knownExercise: KnownExercise?=null
    private var dataset: LineDataSet?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_known_exercise_overview)
        setSupportActionBar(exerciseOverViewToolbar)
        realm = Realm.getDefaultInstance()
        recyclerView = findViewById(R.id.recycler_view_exercisesOverView)

         knownExercise=realm!!.where(KnownExercise::class.java)
            .equalTo("uuid",intent.getLongExtra(EditTrainingActivity.KNOWNEXERCISE_ID,0)).findFirst()

        knownExercise?.let { setUpRecyclerView(it) }
        val idstring="[${knownExercise?.user_custom_id.toString()}]"
        title=idstring+knownExercise?.name
        var maxWeight=knownExercise?.prWeight
        var maxReps=knownExercise?.repsAtPRWeight
        var maxDate =knownExercise?.dateOfPR
        var prCalculated =knownExercise?.prCalculated?.roundToInt()
        val dateformatted= SimpleDateFormat("EEE, d MMM yyyy").format(maxDate)
        val textPR="PR: $dateformatted : $maxWeight kg / $maxReps --> $prCalculated "
        KnownOverViewPRtextview.text=textPR

        val chart=linechart

        val query=knownExercise?.doneInExercises
        val entryList= mutableListOf<Entry>()
        query?.forEachIndexed { index, it ->
            val bestSet=it.sets.maxBy { ExerciseSet.epleyValue(it.weight,it.reps) }

            val entry= Entry(bestSet?.doneInExercises?.first()?.date?.time?.div(8.64e+7)?.toFloat()!!, ExerciseSet.epleyValue(bestSet?.weight!!,bestSet.reps).toFloat())
            entryList.add(entry)
            if(bestSet.isPR){
                entry.icon= resources.getDrawable( ic_star_yellow_16dp,theme);
            }
        }

        dataset= LineDataSet(entryList,"estimated 1RM kg")
        // draw dashed line
        dataset!!.apply {
            this.enableDashedLine(10f, 5f, 0f)
            this.setDrawValues(false)
            this.setCircleColor(Color.TRANSPARENT)
            this.color= Color.WHITE
            this.circleHoleColor= Color.WHITE
            this.valueTextColor= Color.WHITE
            // text size of values
            this.setValueTextSize(9f)
        }


        val data= LineData(dataset)

        chart.data=data


        // create marker to display box when values are selected
        val mv = MyMarkerView(this, R.layout.custom_marker_view)
        mv.chartView = chart
        chart.marker = mv
        chart.xAxis.axisLineColor= Color.WHITE
        chart.xAxis.textColor= Color.WHITE
        chart.axisLeft.axisLineColor= Color.WHITE
        chart.axisLeft.textColor= Color.WHITE
        chart.axisRight.isEnabled=false


        chart.legend.textColor= Color.WHITE
        chart.setOnChartValueSelectedListener(this)

        chart.xAxis.valueFormatter=MyValueFormatter()
        chart.xAxis.granularity = 1f
        chart.xAxis.labelCount=4
        chart.description.isEnabled = false
        chart.legend.isEnabled = false


        chart.invalidate()


    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menu = menu
        menuInflater.inflate(R.menu.knownexercise_overview_menu, menu)
        menu.setGroupVisible(R.id.group_normal_mode, true)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {

            R.id.action_editKnownExercises -> {
                var intent = Intent(this, ChangeKnownExerciseActivity::class.java)
                intent.putExtra(EditTrainingActivity.KNOWNEXERCISE_ID, knownExercise?.uuid)
                startActivity(intent)

                return true
            }

            R.id.action_delete_KnownExercise -> {
                if(knownExercise?.doneInExercisesSize==0) {
                    DataHelper.deleteKnownExerciseSafely(this.realm!!, this.knownExercise?.uuid!!)
                    finish()
                }
                else Toast.makeText(this,"can't delete Exercise if already done, edit --> rename?",Toast.LENGTH_LONG).show()

                return true
            }
            R.id.action_showGraph -> {
                linechart.isVisible=linechart.isVisible.not()
                return true
            }
            R.id.action_shortVersion ->{
                adapter?.showShortVersion=adapter?.showShortVersion?.not()!!
                item.isChecked= !item.isChecked
                recyclerView?.adapter?.notifyDataSetChanged()
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun setUpRecyclerView(known:KnownExercise) {
        adapter =
            known.doneInExercises?.sort("date", Sort.DESCENDING)?.let{
                KnownExerciseOverviewAdapter(it) }

        adapter?.setOnItemClickListener(object : KnownExerciseOverviewAdapter.OnItemClickListener{
            override fun onItemClick(exercise: Exercise) {
                val intent = Intent(baseContext, EditTrainingActivity::class.java)
                intent.putExtra(EditTrainingActivity.TRAINING_ID, exercise.doneInTrainings?.first()?.uuid) //TODO LongExtra?

                startActivityForResult(intent, MainActivity.REQUEST_TRAINING)

            }

        })
        recyclerView!!.layoutManager = LinearLayoutManager(this)
        recyclerView!!.adapter = adapter
        recyclerView!!.setHasFixedSize(true)
    }

    override fun onResume() {
        adapter?.updateData(adapter?.data)
        super.onResume()
    }

    override fun onNothingSelected() {
    }

    override fun onValueSelected(e: Entry?, h: Highlight?) {
        val smoothScroller: SmoothScroller = object : LinearSmoothScroller(this) {
            override fun getVerticalSnapPreference(): Int {
                return SNAP_TO_START
            }
        }
        smoothScroller.targetPosition=abs((dataset?.getEntryIndex(e)!!- (dataset?.entryCount!!-1)))
        recyclerView?.layoutManager?.startSmoothScroll(smoothScroller)
    }
}
