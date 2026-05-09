package com.oskar.retrolauncher

import android.os.Handler
import android.os.Looper
import com.oskar.retrolauncher.util.ClockTicker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * T1.9 AC1: clock ticker is a Handler-based 1Hz ticker (no BroadcastReceiver overhead).
 *
 * Tests use Robolectric's ShadowLooper to advance the main looper deterministically
 * and verify the ticker fires once per second, self-aligns to the next-second boundary,
 * and stops cleanly when stop() is called.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class ClockTickerTest {

    @Test
    fun `start fires onTick immediately`() {
        val handler = Handler(Looper.getMainLooper())
        val emissions = mutableListOf<Long>()
        val ticker = ClockTicker(handler, onTick = { emissions += it }, now = { 1_000_000L })
        ticker.start()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, emissions.size)
        assertEquals(1_000_000L, emissions.first())
        ticker.stop()
    }

    @Test
    fun `ticker self-schedules every second`() {
        val handler = Handler(Looper.getMainLooper())
        val emissions = mutableListOf<Long>()
        var fakeNow = 1_000_000L
        val ticker = ClockTicker(handler, onTick = { emissions += it }, now = { fakeNow })
        ticker.start()
        shadowOf(Looper.getMainLooper()).idle()
        // Advance 3 seconds; expect 3 more emissions.
        repeat(3) {
            fakeNow += 1000L
            shadowOf(Looper.getMainLooper()).idleFor(1000, java.util.concurrent.TimeUnit.MILLISECONDS)
        }
        assertTrue("expected at least 4 ticks (initial + 3); got ${emissions.size}", emissions.size >= 4)
        ticker.stop()
    }

    @Test
    fun `stop cancels future ticks`() {
        val handler = Handler(Looper.getMainLooper())
        val emissions = mutableListOf<Long>()
        var fakeNow = 1_000_000L
        val ticker = ClockTicker(handler, onTick = { emissions += it }, now = { fakeNow })
        ticker.start()
        shadowOf(Looper.getMainLooper()).idle()
        val before = emissions.size
        ticker.stop()
        // Advance time past the next tick — no further emissions should arrive.
        fakeNow += 5_000L
        shadowOf(Looper.getMainLooper()).idleFor(5_000, java.util.concurrent.TimeUnit.MILLISECONDS)
        assertEquals("no ticks should fire after stop()", before, emissions.size)
    }

    @Test
    fun `delay is computed to align with the next second boundary`() {
        // At now = 1_000_500 (mid-second), next boundary is at 1_001_000 → delay 500 ms.
        val computed = ClockTicker.delayToNextSecond(1_000_500L)
        assertEquals(500L, computed)
    }

    @Test
    fun `delay at exact second boundary is one full second`() {
        val computed = ClockTicker.delayToNextSecond(2_000_000L)
        assertEquals(1000L, computed)
    }
}
