package com.oskar.retrolauncher.ui.trips

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

/**
 * Minimal speed-profile sparkline for the trip card strip. Renders a glow
 * polyline scaled to the view bounds. Empty = nothing drawn (no placeholder),
 * keeps the card looking clean while points load asynchronously.
 */
class SparklineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : View(context, attrs, defStyle) {

    private var values: FloatArray = FloatArray(0)

    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND; strokeWidth = dp(1.2f)
        color = 0xFF4DD0E1.toInt()
    }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND; strokeWidth = dp(3.2f)
        color = 0x444DD0E1
    }

    fun setValues(vs: FloatArray) {
        values = vs
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (values.size < 2) return
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val maxV = values.maxOrNull() ?: return
        if (maxV <= 0f) return
        val path = Path()
        val n = values.size
        for (i in 0 until n) {
            val x = i.toFloat() / (n - 1) * w
            val y = h - (values[i] / maxV) * h
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, glow)
        canvas.drawPath(path, line)
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}
