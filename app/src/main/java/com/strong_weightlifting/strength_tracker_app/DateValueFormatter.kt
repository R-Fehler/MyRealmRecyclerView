package com.strong_weightlifting.strength_tracker_app

import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class DateValueFormatter() : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        val date=Date(value.toLong()*8.64e+7.toLong())
        val dateformatted= SimpleDateFormat("d MMM yyyy").format(date)

        return dateformatted
    }
}