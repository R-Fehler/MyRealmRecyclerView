package com.strong_weightlifting.strength_tracker_app

import android.annotation.SuppressLint
import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.CandleEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.utils.Utils
import kotlin.math.roundToInt

/**
 * Custom implementation of the MarkerView.
 * for 2D plotting of exercise data.
 */
@SuppressLint("ViewConstructor")
class PlottingMarkerView(context: Context?, layoutResource: Int) :
    MarkerView(context, layoutResource) {
    private val tvContent: TextView
    // runs every time the MarkerView is redrawn, can be used to update the
// content (user-interface)
    override fun refreshContent(
        e: Entry,
        highlight: Highlight
    ) {
        if (e is CandleEntry) {
            tvContent.text = Utils.formatNumber(e.high, 0, true)
        } else {
            val text="${e.y.roundToInt()}kg"
            tvContent.text = text
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF((-(width / 2)).toFloat(), (-height).toFloat())
    }

    init {
        tvContent = findViewById(R.id.tvContent)
    }
}