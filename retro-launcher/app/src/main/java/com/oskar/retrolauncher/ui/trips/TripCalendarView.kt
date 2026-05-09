package com.oskar.retrolauncher.ui.trips

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.oskar.retrolauncher.util.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Horizontal day strip for the current month. Today highlighted in accent.
 * Days with trips show a small accent dot underneath the number.
 */
class TripCalendarView @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null
) : View(ctx, attrs) {

    private val accent = Color.parseColor("#FF8500")
    private val cardColor = Color.parseColor("#1F1F1F")
    private val textColor = Color.WHITE
    private val dimText = Color.parseColor("#888888")

    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
    private val dayNumPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 14f.dp(resources)
        textAlign = Paint.Align.CENTER
        color = textColor
    }
    private val dayNamePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 9f.dp(resources)
        textAlign = Paint.Align.CENTER
        color = dimText
    }

    private val cellRect = RectF()

    private var daysInMonth = 31
    private var todayDay = 1
    private var selectedDay = 1
    private var tripDays: Set<Int> = emptySet()
    private val baseCal: Calendar = Calendar.getInstance()
    private val dayNameFmt = SimpleDateFormat("EEE", Locale.getDefault())

    var onDaySelected: ((day: Int) -> Unit)? = null

    init {
        val now = Calendar.getInstance()
        daysInMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH)
        todayDay = now.get(Calendar.DAY_OF_MONTH)
        selectedDay = todayDay
        baseCal.timeInMillis = now.timeInMillis
    }

    fun setMonth(cal: Calendar) {
        baseCal.timeInMillis = cal.timeInMillis
        daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val now = Calendar.getInstance()
        todayDay = if (sameMonth(cal, now)) now.get(Calendar.DAY_OF_MONTH) else -1
        selectedDay = if (todayDay > 0) todayDay else 1
        invalidate()
    }

    fun setTripDays(days: Set<Int>) {
        tripDays = days
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (daysInMonth <= 0) return
        val cellW = width.toFloat() / daysInMonth
        val pad = 2f.dp(resources)
        val baseDate = baseCal.clone() as Calendar
        for (day in 1..daysInMonth) {
            val cx = cellW * (day - 0.5f)
            cellRect.set(cellW * (day - 1) + pad, 0f, cellW * day - pad, height.toFloat())
            val isToday = day == todayDay
            val isSelected = day == selectedDay
            val isTripDay = tripDays.contains(day)
            cellPaint.color = when {
                isSelected -> accent
                isTripDay -> cardColor
                else -> Color.TRANSPARENT
            }
            if (cellPaint.color != Color.TRANSPARENT) {
                canvas.drawRoundRect(cellRect, 8f.dp(resources), 8f.dp(resources), cellPaint)
            }

            // Day name (Mon/Tue/...)
            baseDate.set(Calendar.DAY_OF_MONTH, day)
            val name = dayNameFmt.format(baseDate.time).take(3)
            dayNamePaint.color = if (isSelected) Color.parseColor("#22000000").let { Color.WHITE } else dimText
            canvas.drawText(name, cx, 14f.dp(resources), dayNamePaint)

            // Day number
            dayNumPaint.color = when {
                isSelected -> Color.BLACK
                isToday -> accent
                else -> textColor
            }
            canvas.drawText(day.toString(), cx, 30f.dp(resources), dayNumPaint)

            // Trip indicator dot
            if (isTripDay && !isSelected) {
                canvas.drawCircle(cx, 40f.dp(resources), 2f.dp(resources), dotPaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) return super.onTouchEvent(event)
        val cellW = width.toFloat() / daysInMonth
        val day = (event.x / cellW).toInt() + 1
        if (day in 1..daysInMonth) {
            selectedDay = day
            onDaySelected?.invoke(day)
            invalidate()
            performClick()
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()

    private fun sameMonth(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.MONTH) == b.get(Calendar.MONTH)
}
