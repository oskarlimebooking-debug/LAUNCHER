package com.oskar.retrolauncher

import com.oskar.retrolauncher.data.location.LocationSample
import com.oskar.retrolauncher.data.location.SpeedFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SpeedFilterTest {

    private fun loc(speedMs: Float, accuracy: Float, ts: Long = 0L): LocationSample =
        LocationSample(0.0, 0.0, speedMs, 0f, ts, accuracy)

    @Test
    fun `cold start produces a non-NaN finite output`() {
        val f = SpeedFilter()
        val out = f.update(loc(15f, 5f))
        assertFalse("output was NaN", out.isNaN())
        assertFalse("output was infinite", out.isInfinite())
    }

    @Test
    fun `cold start with zero accuracy does not divide by zero`() {
        val f = SpeedFilter()
        val out = f.update(loc(7f, 0f))
        assertFalse(out.isNaN())
        assertFalse(out.isInfinite())
    }

    @Test
    fun `converges to true speed within 5 samples on clean GPS`() {
        val f = SpeedFilter()
        var out = 0f
        repeat(5) { out = f.update(loc(15f, 5f)) }
        // Within 1 m/s of 15 m/s after 5 clean samples.
        assertEquals(15f, out, 1.0f)
    }

    @Test
    fun `high accuracy noise is dampened more than low noise`() {
        val f1 = SpeedFilter()
        val f2 = SpeedFilter()
        // Pre-warm both filters at 10 m/s with good accuracy (Kalman path).
        repeat(5) {
            f1.update(loc(10f, 5f))
            f2.update(loc(10f, 5f))
        }
        // f1: outlier at acc=100 m → MA-fallback path, heavily dampened
        val outHigh = f1.update(loc(100f, 100f))
        // f2: same outlier at acc=5 m → Kalman path, larger gain
        val outLow = f2.update(loc(100f, 5f))
        assertTrue(
            "high-noise output ($outHigh) should not jump near 100",
            outHigh < 50f,
        )
        assertTrue(
            "low-noise outlier moves more than high-noise: low=$outLow high=$outHigh",
            outLow > outHigh,
        )
    }

    @Test
    fun `outputs zero within two samples of speedMs becoming zero`() {
        val f = SpeedFilter()
        repeat(5) { f.update(loc(10f, 5f)) }
        f.update(loc(0f, 5f))           // first zero
        val out = f.update(loc(0f, 5f)) // second zero → snap to 0
        assertEquals(0f, out, 0.0001f)
    }

    @Test
    fun `single zero sample does not force output to zero`() {
        val f = SpeedFilter()
        repeat(3) { f.update(loc(10f, 5f)) }
        val out = f.update(loc(0f, 5f))
        assertTrue("one zero alone should not snap output: out=$out", out > 0f)
    }

    @Test
    fun `falls back to 5-sample moving average when accuracy above 20m`() {
        val f = SpeedFilter()
        var last = 0f
        listOf(2f, 4f, 6f, 8f, 10f).forEach { last = f.update(loc(it, 25f)) }
        assertEquals((2f + 4f + 6f + 8f + 10f) / 5f, last, 0.001f)
    }

    @Test
    fun `moving average window is bounded to 5 samples`() {
        val f = SpeedFilter()
        listOf(10f, 10f, 10f, 10f, 10f, 20f).forEach { f.update(loc(it, 25f)) }
        // After 6 inputs at acc>20 the window holds last 5: [10,10,10,10,20].
        // One more 20 evicts the oldest 10: [10,10,10,20,20] → avg 14.
        val out = f.update(loc(20f, 25f))
        assertEquals(14f, out, 0.001f)
    }

    @Test
    fun `threshold clamps low filtered speed to zero`() {
        val f = SpeedFilter(threshold = 5f)
        val out = f.update(loc(2f, 5f))
        assertEquals(0f, out, 0.0001f)
    }

    @Test
    fun `flow exposes latest filtered value`() = runTest {
        val f = SpeedFilter()
        f.update(loc(7f, 5f))
        val latest = f.flow.first()
        assertEquals(7f, latest, 0.5f)
    }

    @Test
    fun `nan accuracy does not produce NaN output`() {
        val f = SpeedFilter()
        val out = f.update(loc(10f, Float.NaN))
        assertFalse(out.isNaN())
        assertFalse(out.isInfinite())
    }

    @Test
    fun `reset returns filter to cold-start state`() {
        val f = SpeedFilter()
        repeat(5) { f.update(loc(20f, 5f)) }
        f.reset()
        // After reset, the next sample re-initializes the Kalman state to that sample.
        val out = f.update(loc(3f, 5f))
        assertEquals(3f, out, 0.0001f)
    }
}
