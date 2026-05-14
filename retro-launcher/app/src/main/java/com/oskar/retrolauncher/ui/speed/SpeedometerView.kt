package com.oskar.retrolauncher.ui.speed

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import com.oskar.retrolauncher.R
import com.oskar.retrolauncher.data.prefs.Units

/**
 * Custom speedometer per spec section 11.3 / T1.13.
 *
 * Pure `Canvas` drawing — track arc, threshold-shaded speed arc, centered numeric speed
 * with km/h subscript. Speed transitions animate over [ANIMATION_DURATION_MS] using
 * `ValueAnimator`, and the view runs on a hardware layer (GPU-accelerated path).
 */
class SpeedometerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    var thresholdKmh: Float = DEFAULT_THRESHOLD_KMH
        set(value) { field = value.coerceAtLeast(1f); invalidate() }

    var maxSpeedKmh: Float = DEFAULT_MAX_KMH
        set(value) { field = value.coerceAtLeast(thresholdKmh); invalidate() }

    /** Drives both the numeric speed text and the unit subscript. */
    var displayUnits: Units = Units.METRIC
        set(value) { field = value; invalidate() }

    @VisibleForTesting
    internal var speedKmh: Float = 0f
        private set

    private var animator: ValueAnimator? = null

    private val okColor: Int by lazy { ContextCompat.getColor(context, R.color.ok) }
    private val warnColor: Int by lazy { ContextCompat.getColor(context, R.color.warn) }
    private val errColor: Int by lazy { ContextCompat.getColor(context, R.color.err) }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.track)
    }

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.ok)
    }

    private val speedPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_primary)
        textAlign = Paint.Align.CENTER
        textSize = resources.getDimension(R.dimen.text_speed_xl)
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
    }

    private val unitPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_tertiary)
        textAlign = Paint.Align.CENTER
        textSize = resources.getDimension(R.dimen.grid_label) * 1.4f
    }

    private val rect = RectF()

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    @VisibleForTesting internal val speedTextSizePx: Float get() = speedPaint.textSize
    @VisibleForTesting internal val speedTextAlign: Paint.Align get() = speedPaint.textAlign

    fun setSpeed(kmh: Float, animated: Boolean = true) {
        val target = kmh.coerceIn(0f, maxSpeedKmh)
        animator?.cancel()
        if (!animated || target == speedKmh) {
            speedKmh = target
            invalidate()
            return
        }
        animator = ValueAnimator.ofFloat(speedKmh, target).apply {
            duration = ANIMATION_DURATION_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                speedKmh = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val stroke = (minOf(w, h) * 0.045f).coerceAtLeast(8f)
        trackPaint.strokeWidth = stroke
        arcPaint.strokeWidth = stroke
        val pad = stroke
        rect.set(pad, pad, w - pad, h - pad)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawArc(rect, ARC_START_DEG, ARC_SWEEP_DEG, false, trackPaint)
        val sweep = (speedKmh / maxSpeedKmh).coerceIn(0f, 1f) * ARC_SWEEP_DEG
        arcPaint.color = arcColorAt(speedKmh)
        canvas.drawArc(rect, ARC_START_DEG, sweep, false, arcPaint)
        val cx = width / 2f
        val cy = height / 2f - (speedPaint.descent() + speedPaint.ascent()) / 2f
        val (displayValue, label) = when (displayUnits) {
            Units.METRIC -> Pair(speedKmh, "km/h")
            Units.IMPERIAL -> Pair(speedKmh * MPH_PER_KMH, "mph")
        }
        canvas.drawText(formatSpeed(displayValue), cx, cy, speedPaint)
        canvas.drawText(label, cx, cy + speedPaint.textSize * 0.55f, unitPaint)
    }

    // AC5 — speed threshold colors are now loaded from @color/ok, @color/warn,
    // @color/err so they remain readable when the active theme changes.
    @VisibleForTesting
    internal fun arcColorAt(kmh: Float): Int {
        val t = (kmh / thresholdKmh.coerceAtLeast(1f)).coerceIn(0f, 2f)
        return if (t < 1f) lerpColor(okColor, warnColor, t)
        else lerpColor(warnColor, errColor, t - 1f)
    }

    private fun lerpColor(from: Int, to: Int, t: Float): Int = Color.argb(
        lerp(Color.alpha(from), Color.alpha(to), t),
        lerp(Color.red(from), Color.red(to), t),
        lerp(Color.green(from), Color.green(to), t),
        lerp(Color.blue(from), Color.blue(to), t),
    )

    private fun lerp(from: Int, to: Int, t: Float): Int =
        (from + (to - from) * t).toInt().coerceIn(0, 255)

    private fun formatSpeed(kmh: Float): String = "%.0f".format(kmh)

    companion object {
        const val ANIMATION_DURATION_MS = 250L
        private const val DEFAULT_THRESHOLD_KMH = 50f
        private const val DEFAULT_MAX_KMH = 220f
        private const val ARC_START_DEG = 135f
        private const val ARC_SWEEP_DEG = 270f
        private const val MPH_PER_KMH = 0.621371f
    }
}
