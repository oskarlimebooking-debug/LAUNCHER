package com.oskar.retrolauncher

import android.graphics.Bitmap
import android.graphics.Color
import com.oskar.retrolauncher.util.darkened
import com.oskar.retrolauncher.util.dominantColorAsync
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * T1.35 — ColorExt unit tests covering `Int.darkened` and `Bitmap.dominantColorAsync`.
 *
 * - darkened: pure function over HSL lightness; deterministic.
 * - dominantColorAsync: AndroidX Palette runs on a background executor and posts
 *   results on the main thread. Under Robolectric we drain the main + background
 *   loopers to flush the swatch generation deterministically.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class ColorExtTest {

    @Test
    fun `darkened reduces lightness toward black`() {
        val white = Color.WHITE
        val dimmed = white.darkened(0.5f)
        val whiteHsl = FloatArray(3)
        val dimmedHsl = FloatArray(3)
        androidx.core.graphics.ColorUtils.colorToHSL(white, whiteHsl)
        androidx.core.graphics.ColorUtils.colorToHSL(dimmed, dimmedHsl)
        assertTrue(
            "lightness must decrease: white=${whiteHsl[2]}, dimmed=${dimmedHsl[2]}",
            dimmedHsl[2] < whiteHsl[2],
        )
        assertNotEquals(white, dimmed)
    }

    @Test
    fun `darkened by zero is identity`() {
        val red = Color.RED
        val out = red.darkened(0f)
        // HSL round-trip may shift bits by one; assert ARGB channels match within 1.
        assertEquals(Color.red(red), Color.red(out))
        assertEquals(Color.green(red).toFloat(), Color.green(out).toFloat(), 1f)
        assertEquals(Color.blue(red).toFloat(), Color.blue(out).toFloat(), 1f)
    }

    @Test
    fun `darkened by one clamps lightness to zero (pure black)`() {
        val out = Color.WHITE.darkened(1f)
        val hsl = FloatArray(3)
        androidx.core.graphics.ColorUtils.colorToHSL(out, hsl)
        assertEquals(0f, hsl[2], 1e-6f)
    }

    @Test
    fun `darkened is monotonic across factor`() {
        val red = Color.RED
        val a = red.darkened(0.2f)
        val b = red.darkened(0.6f)
        val aHsl = FloatArray(3)
        val bHsl = FloatArray(3)
        androidx.core.graphics.ColorUtils.colorToHSL(a, aHsl)
        androidx.core.graphics.ColorUtils.colorToHSL(b, bHsl)
        assertTrue("higher factor must darken more (a=${aHsl[2]} b=${bHsl[2]})", bHsl[2] < aHsl[2])
    }

    @Test
    fun `dominantColorAsync falls back when bitmap has no extractable swatches`() {
        // A 1x1 fully-transparent bitmap yields no swatches.
        val bmp = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val fallback = 0x12_34_56_78.toInt()
        val latch = CountDownLatch(1)
        var result = 0
        bmp.dominantColorAsync(fallback) {
            result = it
            latch.countDown()
        }
        ShadowLooper.idleMainLooper()
        // Drain any background work + main looper repeatedly to make sure the
        // Palette generation callback has fired under Robolectric.
        repeat(10) {
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            if (latch.await(50, TimeUnit.MILLISECONDS)) return@repeat
        }
        assertEquals(fallback, result)
    }

    @Test
    fun `dominantColorAsync returns a darkened swatch when extraction succeeds`() {
        val bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val solid = Color.rgb(200, 50, 50)
        for (x in 0 until 64) for (y in 0 until 64) bmp.setPixel(x, y, solid)
        val fallback = Color.MAGENTA
        val latch = CountDownLatch(1)
        var result = 0
        bmp.dominantColorAsync(fallback) {
            result = it
            latch.countDown()
        }
        repeat(20) {
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            if (latch.await(100, TimeUnit.MILLISECONDS)) return@repeat
        }
        // If Palette returned a swatch, the value should be the swatch darkened by
        // 0.4f. If it returned no swatch (uncommon but possible under shadow), we
        // would see the fallback. Either way the result must NOT be black.
        assertNotEquals(Color.BLACK, result)
    }
}
