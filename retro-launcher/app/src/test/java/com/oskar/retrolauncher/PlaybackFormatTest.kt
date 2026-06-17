package com.oskar.retrolauncher

import com.oskar.retrolauncher.ui.player.formatTime
import org.junit.Assert.assertEquals
import org.junit.Test

/** M3 — m:ss / h:mm:ss formatting for position + duration. */
class PlaybackFormatTest {

    @Test
    fun `formats sub-hour durations as m colon ss`() {
        assertEquals("0:00", formatTime(0))
        assertEquals("0:05", formatTime(5_000))
        assertEquals("1:05", formatTime(65_000))
        assertEquals("3:35", formatTime(215_000))
        assertEquals("59:59", formatTime(3_599_000))
    }

    @Test
    fun `formats hour-plus durations as h colon mm colon ss`() {
        assertEquals("1:00:00", formatTime(3_600_000))
        assertEquals("1:01:05", formatTime(3_665_000))
    }

    @Test
    fun `negative or partial values clamp and floor`() {
        assertEquals("0:00", formatTime(-1))
        assertEquals("0:00", formatTime(999)) // < 1s floors to 0
    }
}
