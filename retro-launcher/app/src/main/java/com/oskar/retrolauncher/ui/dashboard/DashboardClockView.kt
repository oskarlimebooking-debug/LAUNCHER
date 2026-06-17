package com.oskar.retrolauncher.ui.dashboard

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.util.ClockTicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Self-contained clock + date. Drives itself with [ClockTicker] between
 * attach/detach, so the hosting fragment needs no clock lifecycle wiring.
 */
class DashboardClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFmt = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    private val clock = TextView(context).apply {
        textSize = 52f
        setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        includeFontPadding = false
    }
    private val date = TextView(context).apply {
        textSize = 16f
        setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
    }
    private val ticker = ClockTicker(Handler(Looper.getMainLooper()), onTick = ::render)

    init {
        orientation = VERTICAL
        gravity = Gravity.START
        addView(clock)
        addView(date)
    }

    private fun render(now: Long) {
        val instant = Date(now)
        clock.text = timeFmt.format(instant)
        date.text = dateFmt.format(instant)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ticker.start()
    }

    override fun onDetachedFromWindow() {
        ticker.stop()
        super.onDetachedFromWindow()
    }
}
