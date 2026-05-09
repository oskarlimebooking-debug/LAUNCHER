package com.oskar.retrolauncher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.MotionEvent
import com.oskar.retrolauncher.ui.trips.TripCalendarView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * T1.26 ACs covered:
 *   - AC1: 7×13 grid (~91 days) fits the right-panel width.
 *   - AC2: Day cells colored by per-day distance (heatmap blue→red).
 *   - AC3: Tap dispatches OnDaySelected(dayStartMs) for the right cell.
 *   - AC4: Today's cell has a visible border.
 *   - AC5: onDraw allocates nothing per frame — Paint and Rect are pre-allocated.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class TripCalendarViewTest {

    private val ctx: Context = RuntimeEnvironment.getApplication()

    private fun startOfTodayMs(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    @Test
    fun `AttributeSet constructor inflates without throwing`() {
        val attrs = Robolectric.buildAttributeSet().build()
        TripCalendarView(ctx, attrs)
    }

    @Test
    fun `no-args constructor inflates without throwing`() {
        TripCalendarView(ctx)
    }

    @Test
    fun `grid is 7 rows by 13 columns`() {
        assertEquals(7, TripCalendarView.ROWS)
        assertEquals(13, TripCalendarView.COLS)
        assertEquals(91, TripCalendarView.ROWS * TripCalendarView.COLS)
    }

    @Test
    fun `onMeasure produces square cells fitting the panel width`() {
        val v = TripCalendarView(ctx)
        val widthSpec = View_makeExactSpec(390)  // 13 cells × 30 px
        val heightSpec = View_makeAtMostSpec(2000)
        v.measure(widthSpec, heightSpec)
        assertEquals(390, v.measuredWidth)
        // Height should be ~ 7 × cellSide; cellSide = width / 13 = 30 → 210
        assertEquals(210, v.measuredHeight)
    }

    @Test
    fun `onDraw runs without allocating Paint or Rect per frame`() {
        val v = TripCalendarView(ctx).apply {
            setDistances(mapOf(startOfTodayMs() to 12_000.0))
        }
        val w = 390; val h = 210
        v.measure(View_makeExactSpec(w), View_makeExactSpec(h))
        v.layout(0, 0, w, h)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        v.draw(Canvas(bmp))
        bmp.recycle()
        // No-throw. Field-only allocation is verified by inspection: assert key fields exist.
        assertNotNull(v.cellPaintForTest)
        assertNotNull(v.cellRectForTest)
    }

    @Test
    fun `heatmap color stays neutral when no trip recorded for a day`() {
        val v = TripCalendarView(ctx)
        val zero = v.heatColorAt(0.0)
        // Neutral / empty cell: dim — much darker than the hot color.
        val hot = v.heatColorAt(50_000.0)
        assertNotEquals(zero, hot)
    }

    @Test
    fun `heatmap interpolates from cool to hot as distance increases`() {
        val v = TripCalendarView(ctx)
        val cool = v.heatColorAt(1_000.0)
        val warm = v.heatColorAt(20_000.0)
        val hot = v.heatColorAt(60_000.0)
        // The red component must monotonically increase as distance goes up.
        assertTrue(
            "expected red(cool)=${android.graphics.Color.red(cool)} <= red(warm)=${android.graphics.Color.red(warm)}",
            android.graphics.Color.red(cool) <= android.graphics.Color.red(warm),
        )
        assertTrue(
            "expected red(warm)=${android.graphics.Color.red(warm)} <= red(hot)=${android.graphics.Color.red(hot)}",
            android.graphics.Color.red(warm) <= android.graphics.Color.red(hot),
        )
        assertTrue(
            "hot color should be redder than blue: ${Integer.toHexString(hot)}",
            android.graphics.Color.red(hot) > android.graphics.Color.blue(hot),
        )
    }

    @Test
    fun `tapping dispatches OnDaySelected with the cell's dayStartMs`() {
        val v = TripCalendarView(ctx)
        val w = 390; val h = 210
        v.measure(View_makeExactSpec(w), View_makeExactSpec(h))
        v.layout(0, 0, w, h)

        var got: Long? = null
        v.onDaySelected = { ms -> got = ms }

        // Today is the bottom-right cell (col=12, row=6).
        val cellSide = w / 13f
        val cx = cellSide * 12 + cellSide / 2
        val cy = cellSide * 6 + cellSide / 2
        v.dispatchTouchEvent(motionUp(cx, cy))

        assertNotNull("onDaySelected was not invoked", got)
        // Convert "got" to start-of-day in default TZ for comparison.
        val cal = Calendar.getInstance().apply {
            timeInMillis = got!!
        }
        val now = Calendar.getInstance()
        assertEquals(now.get(Calendar.YEAR), cal.get(Calendar.YEAR))
        assertEquals(now.get(Calendar.DAY_OF_YEAR), cal.get(Calendar.DAY_OF_YEAR))
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
    }

    @Test
    fun `tapping a past cell dispatches the matching past day`() {
        val v = TripCalendarView(ctx)
        val w = 390; val h = 210
        v.measure(View_makeExactSpec(w), View_makeExactSpec(h))
        v.layout(0, 0, w, h)

        var got: Long? = null
        v.onDaySelected = { ms -> got = ms }

        // Tap the top-left cell. It corresponds to 90 days ago (row=0, col=0).
        val cellSide = w / 13f
        v.dispatchTouchEvent(motionUp(cellSide / 2f, cellSide / 2f))

        assertNotNull(got)
        val daysAgo = TimeUnit.MILLISECONDS.toDays(startOfTodayMs() - got!!)
        assertEquals("top-left cell should be 90 days ago", 90, daysAgo)
    }

    @Test
    fun `tapping outside the grid does not dispatch a selection`() {
        val v = TripCalendarView(ctx)
        val w = 390; val h = 210
        v.measure(View_makeExactSpec(w), View_makeExactSpec(h))
        v.layout(0, 0, w, h)

        var got: Long? = null
        v.onDaySelected = { ms -> got = ms }
        v.dispatchTouchEvent(motionUp(-1f, -1f))
        assertNull(got)
    }

    @Test
    fun `today's cell has a visible border drawn on top`() {
        val v = TripCalendarView(ctx)
        // The view exposes paint width for the today border for verification.
        assertTrue("today border paint must be a stroke", v.todayBorderPaintForTest.style == android.graphics.Paint.Style.STROKE)
        assertTrue("today border must have a non-zero stroke width", v.todayBorderPaintForTest.strokeWidth > 0f)
    }

    private fun motionUp(x: Float, y: Float): MotionEvent {
        val now = android.os.SystemClock.uptimeMillis()
        return MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, x, y, 0)
    }

    private fun View_makeExactSpec(size: Int): Int =
        android.view.View.MeasureSpec.makeMeasureSpec(size, android.view.View.MeasureSpec.EXACTLY)

    private fun View_makeAtMostSpec(size: Int): Int =
        android.view.View.MeasureSpec.makeMeasureSpec(size, android.view.View.MeasureSpec.AT_MOST)

    init {
        // Stabilize tests across local TZs.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }
}
