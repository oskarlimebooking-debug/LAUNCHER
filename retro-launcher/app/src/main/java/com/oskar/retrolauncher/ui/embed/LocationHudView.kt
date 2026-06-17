package com.oskar.retrolauncher.ui.embed

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import com.oskar.retrolauncher.data.location.LocationSample
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Cyberpunk location HUD. North-up (no canvas rotation).
 *
 * Composition (from top to bottom):
 *  - Heading tape: aircraft-HDI-style scrolling band showing current bearing
 *    relative to a static N/E/S/W tick scale. Tells you which way you're going
 *    without rotating the rest of the world.
 *  - Big speed digits (top-right corner, 60sp).
 *  - Radar: centered range rings (50/100/200 m), accuracy halo, recent GPS
 *    trail (north-up, glowing), and the "you" dot with a small direction
 *    chevron that points along the current bearing.
 *  - Speed arc gauge: 0→150 km/h semicircle at the bottom with a neon
 *    gradient fill from cyan → yellow above 90 → red above 130.
 *  - Coords (lat,lon) bottom-left, accuracy bottom-right.
 *  - CRT scanline overlay: subtle, slow.
 *
 * Lifecycle: animators start in onAttachedToWindow, stop in onDetached.
 */
class LocationHudView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : View(context, attrs, defStyle) {

    private val trail = ArrayDeque<LocationSample>(TRAIL_MAX)
    private var current: LocationSample? = null

    private val accent = 0xFF4DD0E1.toInt()
    private val accentDim = 0x554DD0E1.toInt()
    private val grid = 0x33335060.toInt()
    private val textPrimary = 0xFFE6EEF2.toInt()
    private val textDim = 0xCCBAC8D0.toInt()
    private val warn = 0xFFFFD54F.toInt()
    private val danger = 0xFFFF5252.toInt()

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 1f; color = grid
    }
    private val trailGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
    }
    private val trailCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
    }
    private val youGlow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val youCore = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val chevronPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }

    private val tapeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xAA0A1014.toInt() }
    private val tapeTick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textDim; strokeWidth = 1f; style = Paint.Style.STROKE
    }
    private val tapeLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textDim; textSize = sp(11f); typeface = android.graphics.Typeface.MONOSPACE
    }
    private val tapePointer = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
    private val tapeHeadingText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accent; textSize = sp(13f); isFakeBoldText = true
        typeface = android.graphics.Typeface.MONOSPACE
    }

    private val speedDigits = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textPrimary; textSize = sp(60f); isFakeBoldText = true
        typeface = android.graphics.Typeface.MONOSPACE
        setShadowLayer(12f, 0f, 0f, accentDim)
    }
    private val speedUnit = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textDim; textSize = sp(11f); typeface = android.graphics.Typeface.MONOSPACE
    }
    private val arcTrack = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = dp(4f); strokeCap = Paint.Cap.ROUND
        color = 0x44335060.toInt()
    }
    private val arcFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = dp(4f); strokeCap = Paint.Cap.ROUND
    }
    private val arcTick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textDim; strokeWidth = 1f; style = Paint.Style.STROKE
    }
    private val arcLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textDim; textSize = sp(9f); typeface = android.graphics.Typeface.MONOSPACE
    }
    private val mono = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textPrimary; textSize = sp(11f); typeface = android.graphics.Typeface.MONOSPACE
    }
    private val acquiring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textDim; textSize = sp(16f); typeface = android.graphics.Typeface.MONOSPACE
        isFakeBoldText = true
    }
    private val scanline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x14FFFFFF; style = Paint.Style.FILL
    }

    private var pulsePhase = 0f
    private var scanlinePhase = 0f
    private val pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1600L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { pulsePhase = it.animatedValue as Float; invalidate() }
    }
    private val scanlineAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 6000L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { scanlinePhase = it.animatedValue as Float }
    }

    private var pxPerMeter: Float = 1f
    private var radarCx: Float = 0f
    private var radarCy: Float = 0f
    private var radarR: Float = 0f

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        pulseAnimator.start(); scanlineAnimator.start()
    }

    override fun onDetachedFromWindow() {
        pulseAnimator.cancel(); scanlineAnimator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bgPaint.shader = RadialGradient(
            w / 2f, h / 2f, min(w, h) * 0.7f,
            intArrayOf(0xFF0A1014.toInt(), 0xFF06090C.toInt()),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP,
        )
        // Radar lives in the band between the heading tape and the speed arc.
        val tapeH = TAPE_HEIGHT_DP * resources.displayMetrics.density
        val arcH = SPEED_ARC_HEIGHT_DP * resources.displayMetrics.density
        radarCx = w / 2f
        radarCy = tapeH + (h - tapeH - arcH) / 2f
        radarR = min(w / 2f - dp(12f), (h - tapeH - arcH) / 2f - dp(8f))
        pxPerMeter = radarR / RING_OUTER_M
    }

    fun update(sample: LocationSample) {
        current?.let {
            trail.addLast(it)
            if (trail.size > TRAIL_MAX) trail.removeFirst()
        }
        current = sample
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        drawRadar(canvas)
        drawHeadingTape(canvas, w)
        drawSpeedDigits(canvas, w)
        drawSpeedArc(canvas, w, h)
        drawCornerReadouts(canvas, w, h)
        drawScanline(canvas, w, h)
    }

    // -- Radar (no rotation — north-up) -----------------------------------

    private fun drawRadar(canvas: Canvas) {
        // Range rings.
        for (m in RING_RADII_M) {
            canvas.drawCircle(radarCx, radarCy, m * pxPerMeter, gridPaint)
        }
        // Faint cross-hairs.
        canvas.drawLine(radarCx - radarR, radarCy, radarCx + radarR, radarCy, gridPaint)
        canvas.drawLine(radarCx, radarCy - radarR, radarCx, radarCy + radarR, gridPaint)
        // Static N/E/S/W ticks at edge of outer ring.
        drawCardinalTicks(canvas)

        val cur = current
        if (cur == null) {
            val msg = "ACQUIRING GPS"
            val tw = acquiring.measureText(msg)
            canvas.drawText(msg, radarCx - tw / 2f, radarCy + acquiring.textSize / 3f, acquiring)
            return
        }

        // Accuracy ring (translucent, real-size in meters).
        if (cur.accuracy > 0f) {
            val ar = cur.accuracy * pxPerMeter
            val accPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x224DD0E1; style = Paint.Style.FILL
            }
            canvas.drawCircle(radarCx, radarCy, ar, accPaint)
            accPaint.style = Paint.Style.STROKE
            accPaint.color = 0x664DD0E1.toInt()
            accPaint.strokeWidth = 1f
            canvas.drawCircle(radarCx, radarCy, ar, accPaint)
        }

        // Trail — north-up. dx = lon delta * mPerDegLon. dy = -lat delta * mPerDegLat (screen Y flipped).
        if (trail.isNotEmpty()) drawTrail(canvas, cur)

        // Center pulse.
        drawYouPulse(canvas, cur.bearing)
    }

    private fun drawCardinalTicks(canvas: Canvas) {
        val labels = arrayOf("N", "E", "S", "W")
        for (i in 0..3) {
            val a = Math.toRadians(-90.0 + i * 90.0)
            val x = radarCx + (radarR * cos(a)).toFloat()
            val y = radarCy + (radarR * Math.sin(a)).toFloat()
            tapeLabel.color = if (i == 0) accent else textDim
            val text = labels[i]
            val tw = tapeLabel.measureText(text)
            canvas.drawText(text, x - tw / 2f, y + tapeLabel.textSize / 3f, tapeLabel)
        }
        tapeLabel.color = textDim
    }

    private fun drawTrail(canvas: Canvas, cur: LocationSample) {
        var prev: FloatArray? = null
        val n = trail.size
        trail.forEachIndexed { i, s ->
            val dLat = s.lat - cur.lat
            val dLon = s.lon - cur.lon
            val mPerLat = 111_320.0
            val mPerLon = 111_320.0 * cos(Math.toRadians(cur.lat))
            val dxm = (dLon * mPerLon).toFloat()
            val dym = (dLat * mPerLat).toFloat()
            val px = radarCx + dxm * pxPerMeter
            val py = radarCy - dym * pxPerMeter
            val cur2 = floatArrayOf(px, py)
            prev?.let { p ->
                val age = (i + 1f) / n
                val coreA = (age * 255f).toInt().coerceIn(0, 255)
                val glowA = (age * 90f).toInt().coerceIn(0, 90)
                trailGlowPaint.color = (glowA shl 24) or (accent and 0x00FFFFFF)
                trailGlowPaint.strokeWidth = 10f * age + 4f
                canvas.drawLine(p[0], p[1], cur2[0], cur2[1], trailGlowPaint)
                trailCorePaint.color = (coreA shl 24) or 0x00FFFFFF
                trailCorePaint.strokeWidth = 2.5f * age + 1f
                canvas.drawLine(p[0], p[1], cur2[0], cur2[1], trailCorePaint)
            }
            prev = cur2
        }
        prev?.let { p ->
            trailGlowPaint.color = (90 shl 24) or (accent and 0x00FFFFFF)
            trailGlowPaint.strokeWidth = 12f
            canvas.drawLine(p[0], p[1], radarCx, radarCy, trailGlowPaint)
            trailCorePaint.color = 0xFFFFFFFF.toInt()
            trailCorePaint.strokeWidth = 3f
            canvas.drawLine(p[0], p[1], radarCx, radarCy, trailCorePaint)
        }
    }

    private fun drawYouPulse(canvas: Canvas, bearingDeg: Float) {
        for (offset in floatArrayOf(0f, 0.5f)) {
            val t = (pulsePhase + offset) % 1f
            val r = 8f + (28f - 8f) * t
            val alpha = ((1f - t) * 200f).toInt().coerceIn(0, 255)
            youGlow.color = (alpha shl 24) or (accent and 0x00FFFFFF)
            youGlow.style = Paint.Style.STROKE; youGlow.strokeWidth = 2f
            canvas.drawCircle(radarCx, radarCy, r, youGlow)
        }
        youGlow.style = Paint.Style.FILL; youGlow.color = accentDim
        canvas.drawCircle(radarCx, radarCy, 14f, youGlow)
        canvas.drawCircle(radarCx, radarCy, 5f, youCore)

        // Direction chevron — small triangle pointing along current bearing.
        val a = Math.toRadians(bearingDeg.toDouble() - 90.0)
        val tip = floatArrayOf(
            radarCx + (22f * cos(a)).toFloat(),
            radarCy + (22f * Math.sin(a)).toFloat(),
        )
        val left = floatArrayOf(
            radarCx + (12f * cos(a + 0.45)).toFloat(),
            radarCy + (12f * Math.sin(a + 0.45)).toFloat(),
        )
        val right = floatArrayOf(
            radarCx + (12f * cos(a - 0.45)).toFloat(),
            radarCy + (12f * Math.sin(a - 0.45)).toFloat(),
        )
        val path = Path().apply {
            moveTo(tip[0], tip[1]); lineTo(left[0], left[1]); lineTo(right[0], right[1]); close()
        }
        canvas.drawPath(path, chevronPaint)
    }

    // -- Heading tape (top) -----------------------------------------------

    private fun drawHeadingTape(canvas: Canvas, w: Float) {
        val tapeH = dp(TAPE_HEIGHT_DP)
        canvas.drawRect(0f, 0f, w, tapeH, tapeBg)
        val cx = w / 2f
        val cur = current ?: return
        val bearing = ((cur.bearing % 360f) + 360f) % 360f
        // Tape scale: 1° = 3 px.
        val pxPerDeg = 3f
        // Draw ticks at every 10°, labels at multiples of 30°.
        for (d in -90..90 step 10) {
            val abs = ((bearing + d) % 360f + 360f) % 360f
            val x = cx + d * pxPerDeg
            if (x < 0f || x > w) continue
            val major = (abs.roundToInt() % 30 == 0)
            val tickH = if (major) dp(12f) else dp(6f)
            tapeTick.color = if (major) textPrimary else textDim
            canvas.drawLine(x, tapeH - tickH, x, tapeH - dp(2f), tapeTick)
            if (major) {
                val label = when (abs.roundToInt()) {
                    0, 360 -> "N"; 90 -> "E"; 180 -> "S"; 270 -> "W"
                    else -> "%03d".format(abs.roundToInt())
                }
                tapeLabel.color = if (label.length == 1) accent else textDim
                val tw = tapeLabel.measureText(label)
                canvas.drawText(label, x - tw / 2f, dp(12f), tapeLabel)
            }
        }
        tapeLabel.color = textDim
        // Center pointer (downward triangle + current heading text).
        val ptrPath = Path().apply {
            moveTo(cx - dp(6f), 0f); lineTo(cx + dp(6f), 0f); lineTo(cx, dp(8f)); close()
        }
        canvas.drawPath(ptrPath, tapePointer)
        val hdg = "%03d°".format(bearing.roundToInt())
        val hw = tapeHeadingText.measureText(hdg)
        canvas.drawText(hdg, cx - hw / 2f, tapeH - dp(16f) - tapeHeadingText.textSize, tapeHeadingText)
    }

    // -- Big speed digits (top-right) -------------------------------------

    private fun drawSpeedDigits(canvas: Canvas, w: Float) {
        val cur = current ?: return
        val kmh = (cur.speedMs * 3.6f).roundToInt().coerceAtLeast(0)
        val str = "%d".format(kmh)
        val sw = speedDigits.measureText(str)
        // Color tint at higher speeds.
        speedDigits.color = when {
            kmh >= 130 -> danger
            kmh >= 90 -> warn
            else -> textPrimary
        }
        val rightMargin = dp(14f)
        val topY = dp(TAPE_HEIGHT_DP) + speedDigits.textSize + dp(4f)
        canvas.drawText(str, w - rightMargin - sw, topY, speedDigits)
        val unit = "KM/H"
        val uw = speedUnit.measureText(unit)
        canvas.drawText(unit, w - rightMargin - uw, topY + speedUnit.textSize + dp(2f), speedUnit)
    }

    // -- Speed arc gauge (bottom) -----------------------------------------

    private fun drawSpeedArc(canvas: Canvas, w: Float, h: Float) {
        val arcH = dp(SPEED_ARC_HEIGHT_DP)
        val cy = h - dp(28f)
        val r = arcH * 0.85f
        val cx = w / 2f
        val rect = RectF(cx - r, cy - r, cx + r, cy + r)
        canvas.drawArc(rect, 180f, 180f, false, arcTrack)
        // Gradient stroke for filled portion.
        val kmh = ((current?.speedMs ?: 0f) * 3.6f).coerceIn(0f, SPEED_MAX_KMH)
        val sweep = (kmh / SPEED_MAX_KMH) * 180f
        if (sweep > 0f) {
            arcFill.shader = LinearGradient(
                cx - r, cy, cx + r, cy,
                intArrayOf(accent, warn, danger),
                floatArrayOf(0f, 0.6f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawArc(rect, 180f, sweep, false, arcFill)
            arcFill.shader = null
        }
        // Ticks every 30 km/h.
        for (v in 0..SPEED_MAX_KMH.toInt() step 30) {
            val frac = v / SPEED_MAX_KMH
            val ang = Math.toRadians(180.0 + frac * 180.0)
            val inner = r - dp(4f)
            val outer = r + dp(2f)
            val ix = cx + (inner * cos(ang)).toFloat()
            val iy = cy + (inner * Math.sin(ang)).toFloat()
            val ox = cx + (outer * cos(ang)).toFloat()
            val oy = cy + (outer * Math.sin(ang)).toFloat()
            arcTick.color = if (v % 60 == 0) textPrimary else textDim
            canvas.drawLine(ix, iy, ox, oy, arcTick)
            if (v % 30 == 0) {
                val lr = r + dp(12f)
                val lx = cx + (lr * cos(ang)).toFloat()
                val ly = cy + (lr * Math.sin(ang)).toFloat()
                val s = "$v"
                val lw = arcLabel.measureText(s)
                canvas.drawText(s, lx - lw / 2f, ly + arcLabel.textSize / 3f, arcLabel)
            }
        }
    }

    // -- Corner readouts (very bottom) ------------------------------------

    private fun drawCornerReadouts(canvas: Canvas, w: Float, h: Float) {
        val cur = current ?: return
        val padX = dp(10f); val baseline = h - dp(8f)
        val coord = "%.5f, %.5f".format(cur.lat, cur.lon)
        canvas.drawText(coord, padX, baseline, mono)
        val acc = "±${cur.accuracy.roundToInt()} m"
        val aw = mono.measureText(acc)
        canvas.drawText(acc, w - padX - aw, baseline, mono)
    }

    // -- CRT scanline overlay ---------------------------------------------

    private fun drawScanline(canvas: Canvas, w: Float, h: Float) {
        val y = scanlinePhase * h
        val band = dp(2f)
        canvas.drawRect(0f, y, w, y + band, scanline)
    }

    // -- helpers ----------------------------------------------------------

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
    private fun sp(v: Float): Float = v * resources.displayMetrics.scaledDensity

    companion object {
        private const val TRAIL_MAX = 60
        private const val RING_OUTER_M = 200f
        private val RING_RADII_M = floatArrayOf(50f, 100f, 150f, 200f)
        private const val TAPE_HEIGHT_DP = 32f
        private const val SPEED_ARC_HEIGHT_DP = 64f
        private const val SPEED_MAX_KMH = 150f
    }
}
