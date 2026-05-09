package com.oskar.retrolauncher.ui.trips

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.VisibleForTesting
import com.oskar.retrolauncher.util.dp
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Heatmap calendar of the last 91 days (7 rows × 13 columns). Today sits in the
 * bottom-right; older days fill rightward-then-up. Cells are color-mapped by
 * total distance driven that day. Tapping a cell calls [onDaySelected] with
 * the local start-of-day in millis.
 *
 * Drawing path is allocation-free: [cellPaint], [todayBorderPaint], [emptyPaint]
 * and [cellRect] are all initialized once and mutated in [onDraw].
 */
class TripCalendarView @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null,
) : View(ctx, attrs) {

    /** today’s start-of-day in local TZ; refreshed at construction + on visibility change. */
    private var todayStartMs: Long = computeStartOfTodayMs()

    /** Map<dayStartMs, totalDistanceMeters>. Days outside the map render as empty. */
    private var distancesByDay: Map<Long, Double> = emptyMap()

    /** Optional override for max distance used to normalize the heatmap. */
    private var maxDistanceMHint: Double = 0.0

    /** Selected cell index in [0, 90]. -1 = none selected. */
    private var selectedIdx: Int = TOTAL - 1   // default to today

    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1F1F1F")
    }
    private val todayBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF8500")
        style = Paint.Style.STROKE
        strokeWidth = 2f.dp(resources)
    }
    private val selectedBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 1.5f.dp(resources)
    }
    private val cellRect = RectF()

    /** Called with the start-of-day millis (local TZ) of the tapped cell. */
    var onDaySelected: ((dayStartMs: Long) -> Unit)? = null

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    /**
     * Replace the heatmap data. Keys must be local start-of-day millis. The view
     * normalizes color intensity against the largest distance in the set.
     */
    fun setDistances(byDay: Map<Long, Double>) {
        distancesByDay = byDay
        maxDistanceMHint = byDay.values.maxOrNull() ?: 0.0
        invalidate()
    }

    /** Override the heatmap normalization ceiling. Pass 0 to auto-compute from data. */
    fun setMaxDistanceM(maxM: Double) {
        maxDistanceMHint = maxM
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec).coerceAtLeast(MIN_WIDTH_PX)
        val cellSide = widthSize / COLS
        val desiredHeight = cellSide * ROWS
        val measuredHeight = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
            MeasureSpec.AT_MOST -> minOf(desiredHeight, MeasureSpec.getSize(heightMeasureSpec))
            else -> desiredHeight
        }
        setMeasuredDimension(cellSide * COLS, measuredHeight)
    }

    override fun onDraw(canvas: Canvas) {
        if (width == 0 || height == 0) return
        val cellSide = (width / COLS).toFloat()
        val pad = 1f.dp(resources)
        val maxDist = if (maxDistanceMHint > 0.0) maxDistanceMHint else 1.0

        var todayIdx = TOTAL - 1
        for (idx in 0 until TOTAL) {
            val col = idx % COLS
            val row = idx / COLS
            cellRect.set(
                col * cellSide + pad,
                row * cellSide + pad,
                (col + 1) * cellSide - pad,
                (row + 1) * cellSide - pad,
            )

            val dayMs = dayStartMsForIndex(idx)
            val dist = distancesByDay[dayMs] ?: 0.0

            cellPaint.color = if (dist <= 0.0) emptyPaint.color else heatColorAt(dist, maxDist)
            canvas.drawRoundRect(cellRect, CORNER_RADIUS_PX, CORNER_RADIUS_PX, cellPaint)
        }

        // Today border drawn last so it sits on top of fills.
        val tCol = todayIdx % COLS
        val tRow = todayIdx / COLS
        val inset = todayBorderPaint.strokeWidth / 2f
        cellRect.set(
            tCol * cellSide + pad + inset,
            tRow * cellSide + pad + inset,
            (tCol + 1) * cellSide - pad - inset,
            (tRow + 1) * cellSide - pad - inset,
        )
        canvas.drawRoundRect(cellRect, CORNER_RADIUS_PX, CORNER_RADIUS_PX, todayBorderPaint)

        // Selected cell border (skip if the selection equals today — today already has a border).
        if (selectedIdx in 0 until TOTAL && selectedIdx != todayIdx) {
            val sCol = selectedIdx % COLS
            val sRow = selectedIdx / COLS
            val sInset = selectedBorderPaint.strokeWidth / 2f
            cellRect.set(
                sCol * cellSide + pad + sInset,
                sRow * cellSide + pad + sInset,
                (sCol + 1) * cellSide - pad - sInset,
                (sRow + 1) * cellSide - pad - sInset,
            )
            canvas.drawRoundRect(cellRect, CORNER_RADIUS_PX, CORNER_RADIUS_PX, selectedBorderPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) return super.onTouchEvent(event)
        val cellSide = width / COLS.toFloat()
        if (cellSide <= 0f) return false
        if (event.x < 0f || event.y < 0f) return false
        val col = (event.x / cellSide).toInt()
        val row = (event.y / cellSide).toInt()
        if (col !in 0 until COLS || row !in 0 until ROWS) return false
        val idx = row * COLS + col
        selectedIdx = idx
        onDaySelected?.invoke(dayStartMsForIndex(idx))
        invalidate()
        performClick()
        return true
    }

    override fun performClick(): Boolean = super.performClick()

    /**
     * Heatmap mapping in [0, 1] from cool (blue) to warm (red). Empty (0 distance)
     * is handled separately in [onDraw] by [emptyPaint].
     */
    @VisibleForTesting
    fun heatColorAt(distanceM: Double, maxM: Double = if (maxDistanceMHint > 0) maxDistanceMHint else 60_000.0): Int {
        val t = (distanceM / maxM).coerceIn(0.0, 1.0).toFloat()
        // Three-stop gradient: blue → orange → red.
        return if (t < 0.5f) {
            lerpColor(BLUE, ORANGE, t / 0.5f)
        } else {
            lerpColor(ORANGE, RED, (t - 0.5f) / 0.5f)
        }
    }

    private fun dayStartMsForIndex(idx: Int): Long {
        // idx 0 = oldest (top-left = 90 days ago). idx TOTAL-1 = today (bottom-right).
        val daysBack = (TOTAL - 1) - idx
        return todayStartMs - TimeUnit.DAYS.toMillis(daysBack.toLong())
    }

    @VisibleForTesting
    val cellPaintForTest: Paint get() = cellPaint

    @VisibleForTesting
    val cellRectForTest: RectF get() = cellRect

    @VisibleForTesting
    val todayBorderPaintForTest: Paint get() = todayBorderPaint

    companion object {
        const val ROWS = 7
        const val COLS = 13
        const val TOTAL = ROWS * COLS  // 91 days
        val CORNER_RADIUS_PX get() = 3f
        const val MIN_WIDTH_PX = 13   // 1px per col fallback so onMeasure never divides by zero
        val BLUE = Color.parseColor("#2563EB")
        val ORANGE = Color.parseColor("#FF8500")
        val RED = Color.parseColor("#DC2626")

        fun lerpColor(a: Int, b: Int, t: Float): Int {
            val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * t).toInt()
            val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * t).toInt()
            val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t).toInt()
            return Color.rgb(r, g, bl)
        }

        fun computeStartOfTodayMs(): Long {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
    }
}
