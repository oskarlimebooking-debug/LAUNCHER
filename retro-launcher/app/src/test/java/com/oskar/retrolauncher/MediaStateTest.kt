package com.oskar.retrolauncher

import com.oskar.retrolauncher.data.media.MediaState
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaStateTest {

    @Test
    fun `livePosition with playback returns base position when not playing`() {
        val s = MediaState(
            positionMs = 1_000L,
            positionAtMs = 0L,
            playing = false,
            speed = 1f,
        )
        // Not playing → no extrapolation regardless of "now"
        assertEquals(1_000L, s.livePosition(now = 5_000L))
    }

    @Test
    fun `livePosition extrapolates while playing`() {
        val s = MediaState(
            positionMs = 1_000L,
            positionAtMs = 100L,
            playing = true,
            speed = 1f,
        )
        // 2 seconds elapsed since positionAt → position should be 3_000
        assertEquals(3_000L, s.livePosition(now = 2_100L))
    }

    @Test
    fun `livePosition respects playback speed`() {
        val s = MediaState(
            positionMs = 1_000L,
            positionAtMs = 0L,
            playing = true,
            speed = 2f,
        )
        // 1 s elapsed at 2x → +2000 ms
        assertEquals(3_000L, s.livePosition(now = 1_000L))
    }

    @Test
    fun `isEmpty when no title`() {
        val s = MediaState()
        assert(s.isEmpty)
    }

    @Test
    fun `isEmpty false when title set`() {
        val s = MediaState(title = "X")
        assert(!s.isEmpty)
    }
}
