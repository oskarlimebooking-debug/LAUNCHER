package com.oskar.retrolauncher.util

import android.os.Handler

/**
 * 1 Hz ticker driven by a [Handler] post loop. Avoids the per-process
 * BroadcastReceiver overhead of `TextClock` / `ACTION_TIME_TICK`, which
 * matters on a Cortex-A7 head unit.
 *
 * Each tick calls [onTick] with the current millis from [now], then
 * self-schedules to fire at the next whole-second boundary so seconds don't
 * drift across long sessions.
 *
 * Lifecycle: caller invokes [start] in `onResume` and [stop] in `onPause`.
 */
class ClockTicker(
    private val handler: Handler,
    private val onTick: (Long) -> Unit,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val tick = object : Runnable {
        override fun run() {
            val t = now()
            onTick(t)
            handler.postDelayed(this, delayToNextSecond(t))
        }
    }

    fun start() {
        handler.removeCallbacks(tick)
        handler.post(tick)
    }

    fun stop() {
        handler.removeCallbacks(tick)
    }

    companion object {
        /** ms remaining until the next whole-second wall-clock boundary. */
        fun delayToNextSecond(nowMillis: Long): Long {
            val rem = nowMillis % 1000L
            return if (rem == 0L) 1000L else 1000L - rem
        }
    }
}
