package com.oskar.retrolauncher.ui.trips

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.oskar.retrolauncher.R

/**
 * Small two-line stat tile: a top mono label and a larger bold value
 * underneath. Used in the trips stats row (distance, duration, avg, max).
 */
class StatCellView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {

    private val label: TextView
    private val value: TextView

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_stat_cell, this, true)
        label = findViewById(R.id.stat_label)
        value = findViewById(R.id.stat_value)
    }

    fun set(label: String, value: String) {
        this.label.text = label
        this.value.text = value
    }
}
