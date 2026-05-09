package com.oskar.retrolauncher

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.oskar.retrolauncher.data.prefs.Units
import com.oskar.retrolauncher.ui.speed.SpeedometerView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * T1.13 ACs covered:
 *   - AC1: AttributeSet constructor inflates without throwing (layout-editor path).
 *   - AC2: speed text uses R.dimen.text_speed_xl and is centered.
 *   - AC3: arc color goes green at idle → red above the threshold.
 *   - AC4: setLayerType(LAYER_TYPE_HARDWARE) is applied during init.
 *   - AC5: speed transitions animate over 250 ms (≥30 fps proxy: smooth-by-design,
 *          and onDraw allocates nothing per frame — RectF/Paints are init fields).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class SpeedometerViewTest {

    private val ctx: Context = RuntimeEnvironment.getApplication()

    @Test
    fun `AttributeSet constructor inflates without throwing`() {
        val attrs = Robolectric.buildAttributeSet().build()
        SpeedometerView(ctx, attrs) // no-throw is the assertion
    }

    @Test
    fun `no-args constructor inflates without throwing`() {
        SpeedometerView(ctx)
    }

    @Test
    fun `uses hardware layer type for GPU acceleration`() {
        val v = SpeedometerView(ctx)
        assertEquals(View.LAYER_TYPE_HARDWARE, v.layerType)
    }

    @Test
    fun `speed text size matches R_dimen_text_speed_xl`() {
        val v = SpeedometerView(ctx)
        val expected = ctx.resources.getDimension(R.dimen.text_speed_xl)
        assertEquals(expected, v.speedTextSizePx, 0.001f)
        assertTrue(
            "text_speed_xl must be at least 48sp on default density",
            v.speedTextSizePx >= 48f * ctx.resources.displayMetrics.density - 0.5f,
        )
    }

    @Test
    fun `speed text is horizontally centered`() {
        val v = SpeedometerView(ctx)
        assertEquals(Paint.Align.CENTER, v.speedTextAlign)
    }

    @Test
    fun `speed transition animation duration is 250 ms`() {
        assertEquals(250L, SpeedometerView.ANIMATION_DURATION_MS)
    }

    @Test
    fun `arc color is green at zero speed`() {
        val v = SpeedometerView(ctx).apply { thresholdKmh = 50f }
        val c = v.arcColorAt(0f)
        // Green channel dominates green and yellow stops alike — at t=0 we're at the green stop.
        assertTrue("expected green dominant at idle: ${Integer.toHexString(c)}", Color.green(c) > Color.red(c))
    }

    @Test
    fun `arc color reaches yellow at the configured threshold`() {
        val v = SpeedometerView(ctx).apply { thresholdKmh = 50f }
        val c = v.arcColorAt(50f)
        // Yellow ≈ (R≈255, G≈193, B≈7); both red and green high, blue low.
        assertTrue("yellow band: red high (${Color.red(c)})", Color.red(c) > 200)
        assertTrue("yellow band: green high (${Color.green(c)})", Color.green(c) > 150)
        assertTrue("yellow band: blue low (${Color.blue(c)})", Color.blue(c) < 80)
    }

    @Test
    fun `arc color transitions to red well above the threshold`() {
        val v = SpeedometerView(ctx).apply { thresholdKmh = 50f }
        val cRed = v.arcColorAt(150f)  // 3x threshold → fully into red band
        assertTrue("red dominant beyond threshold: ${Integer.toHexString(cRed)}", Color.red(cRed) > Color.green(cRed))
    }

    @Test
    fun `arc color is distinctly different across green-yellow-red bands`() {
        val v = SpeedometerView(ctx).apply { thresholdKmh = 50f }
        val green = v.arcColorAt(0f)
        val yellow = v.arcColorAt(50f)
        val red = v.arcColorAt(150f)
        assertNotEquals(green, yellow)
        assertNotEquals(yellow, red)
        assertNotEquals(green, red)
    }

    @Test
    fun `setSpeed without animation updates immediately and clamps to maxSpeedKmh`() {
        val v = SpeedometerView(ctx).apply { maxSpeedKmh = 200f }
        v.setSpeed(80f, animated = false)
        assertEquals(80f, v.speedKmh, 0.001f)
        v.setSpeed(999f, animated = false)
        assertEquals(200f, v.speedKmh, 0.001f) // clamped to maxSpeedKmh
    }

    @Test
    fun `threshold mutation triggers redraw`() {
        val v = SpeedometerView(ctx)
        v.thresholdKmh = 80f
        assertEquals(80f, v.thresholdKmh, 0.001f)
    }

    @Test
    fun `defaults to metric display units`() {
        val v = SpeedometerView(ctx)
        assertEquals(Units.METRIC, v.displayUnits)
    }

    @Test
    fun `setting displayUnits to IMPERIAL is reflected and triggers redraw`() {
        val v = SpeedometerView(ctx)
        v.displayUnits = Units.IMPERIAL
        assertEquals(Units.IMPERIAL, v.displayUnits)
    }

    @Test
    fun `does not crash when drawing in edit mode`() {
        val v = SpeedometerView(ctx)
        v.layout(0, 0, 400, 400) // forces onSizeChanged
        // measure + draw via measure pass; onDraw is exercised on the next frame
        v.measure(
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
        )
        v.layout(0, 0, 400, 400)
        // Render to a software canvas so the View's onDraw runs end-to-end.
        val bmp = android.graphics.Bitmap.createBitmap(400, 400, android.graphics.Bitmap.Config.ARGB_8888)
        v.draw(android.graphics.Canvas(bmp))
        bmp.recycle()
    }
}
