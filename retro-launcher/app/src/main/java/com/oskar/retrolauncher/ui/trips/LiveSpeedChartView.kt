package com.oskar.retrolauncher.ui.trips

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Speed-over-time line chart with a cyberpunk neon look:
 *   - dark backdrop with subtle horizontal grid lines
 *   - glowing cyan stroke + a translucent gradient fill below the line
 *   - max-speed marker, y-axis labels (km/h), x-axis labels (elapsed mm:ss)
 *
 * Accepts pairs of (epoch ms, speed m/s). The view picks its own y-range
 * (rounded up to the next multiple of 20 km/h), recomputes on every [setData],
 * and survives empty data (renders the empty grid + "—" placeholder).
 */
class LiveSpeedChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : View(context, attrs, defStyle) {

    private var samples: List<Pair<Long, Float>> = emptyList()

    private val accent = 0xFF4DD0E1.toInt()
    private val accentDim = 0x554DD0E1.toInt()
    private val grid = 0x33335060.toInt()
    private val textDim = 0xAAB0C0CC.toInt()
    private val textPrimary = 0xFFE6EEF2.toInt()

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 1f; color = grid
    }
    private val axisLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textDim; textSize = sp(10f); typeface = android.graphics.Typeface.MONOSPACE
    }
    private val maxLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accent; textSize = sp(11f); typeface = android.graphics.Typeface.MONOSPACE
        isFakeBoldText = true
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND; strokeWidth = dp(2.2f); color = accent
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND; strokeWidth = dp(7f); color = accentDim
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val maxLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 1f; color = 0x66FF5252.toInt()
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(dp(4f), dp(3f)), 0f)
    }

    /** Update the data set and trigger a redraw. */
    fun setData(samples: List<Pair<Long, Float>>) {
        this.samples = samples
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val leftPad = dp(34f)
        val rightPad = dp(8f)
        val topPad = dp(10f)
        val bottomPad = dp(20f)
        val plotW = w - leftPad - rightPad
        val plotH = h - topPad - bottomPad
        if (plotW <= 0f || plotH <= 0f) return

        val maxKmh = niceMaxKmh()
        // Horizontal grid lines + y-axis labels (every 20 km/h).
        val step = 20
        var v = 0
        while (v <= maxKmh) {
            val y = topPad + plotH - (v / maxKmh.toFloat()) * plotH
            canvas.drawLine(leftPad, y, leftPad + plotW, y, gridPaint)
            val label = "$v"
            canvas.drawText(label, leftPad - axisLabel.measureText(label) - dp(4f),
                y + axisLabel.textSize / 3f, axisLabel)
            v += step
        }

        if (samples.size < 2) {
            val msg = "—"
            val tw = axisLabel.measureText(msg)
            canvas.drawText(msg, leftPad + plotW / 2f - tw / 2f, topPad + plotH / 2f, axisLabel)
            return
        }

        // Time range.
        val t0 = samples.first().first
        val t1 = samples.last().first
        val dur = max(t1 - t0, 1L).toFloat()

        // Build polyline path + fill polygon.
        val linePath = Path()
        val fillPath = Path()
        samples.forEachIndexed { i, (t, ms) ->
            val kmh = ms * 3.6f
            val x = leftPad + ((t - t0).toFloat() / dur) * plotW
            val y = topPad + plotH - (kmh / maxKmh.toFloat()).coerceIn(0f, 1f) * plotH
            if (i == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, topPad + plotH); fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        // Close fill polygon back along the baseline.
        val lastX = leftPad + plotW
        fillPath.lineTo(lastX, topPad + plotH)
        fillPath.close()

        // Gradient fill underneath.
        fillPaint.shader = LinearGradient(
            0f, topPad, 0f, topPad + plotH,
            intArrayOf(0x554DD0E1, 0x004DD0E1),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(fillPath, fillPaint)
        fillPaint.shader = null

        // Glow stroke then crisp core stroke.
        canvas.drawPath(linePath, glowPaint)
        canvas.drawPath(linePath, linePaint)

        // Max-speed marker (dashed horizontal line + label).
        val maxObserved = samples.maxOf { it.second } * 3.6f
        val maxY = topPad + plotH - (maxObserved / maxKmh.toFloat()).coerceIn(0f, 1f) * plotH
        canvas.drawLine(leftPad, maxY, leftPad + plotW, maxY, maxLine)
        val maxText = "MAX ${maxObserved.roundToInt()}"
        val maxTw = maxLabel.measureText(maxText)
        canvas.drawText(maxText, leftPad + plotW - maxTw - dp(2f), maxY - dp(4f), maxLabel)

        // X-axis time labels (start, mid, end).
        drawTimeLabel(canvas, leftPad, topPad + plotH + dp(14f), 0L)
        drawTimeLabel(canvas, leftPad + plotW / 2f, topPad + plotH + dp(14f), (dur / 2).toLong())
        drawTimeLabel(canvas, leftPad + plotW, topPad + plotH + dp(14f), dur.toLong())
    }

    private fun drawTimeLabel(canvas: Canvas, x: Float, y: Float, ms: Long) {
        val s = formatMs(ms)
        val tw = axisLabel.measureText(s)
        canvas.drawText(s, x - tw / 2f, y, axisLabel)
    }

    private fun niceMaxKmh(): Int {
        if (samples.isEmpty()) return 80
        val maxMs = samples.maxOf { it.second }
        val maxKmh = maxMs * 3.6f
        // Round up to nearest 20.
        return max(20, (ceil(maxKmh / 20f) * 20f).toInt())
    }

    private fun formatMs(ms: Long): String {
        val totalSec = ms / 1000L
        val m = totalSec / 60L
        val s = totalSec % 60L
        return "%d:%02d".format(m, s)
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
    private fun sp(v: Float): Float = v * resources.displayMetrics.scaledDensity
}
