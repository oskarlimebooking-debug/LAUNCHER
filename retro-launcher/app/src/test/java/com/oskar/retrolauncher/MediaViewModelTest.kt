package com.oskar.retrolauncher

import com.oskar.retrolauncher.data.media.MediaState
import com.oskar.retrolauncher.ui.media.MediaUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * T1.17 ACs covered by `MediaUiState.from(...)` pure transform tests:
 *   - AC5: `MediaState.empty` produces a UI state with isEmpty=true, no title/artist/art.
 *   - AC4: a populated state surfaces title / artist / playing for transport binding.
 *
 * Robolectric is required because `MediaState.livePosition` defaults to
 * `SystemClock.elapsedRealtime()` even when `playing=false` (the default arg is
 * still evaluated). Robolectric provides a working SystemClock shadow.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class MediaViewModelTest {

    @Test
    fun `from MediaState empty produces isEmpty UI state`() {
        val ui = MediaUiState.from(MediaState.empty)
        assertTrue("empty MediaState must produce isEmpty=true", ui.isEmpty)
        assertNull(ui.title)
        assertNull(ui.artist)
        assertNull(ui.art)
        assertEquals(0L, ui.durationMs)
        assertEquals(0L, ui.positionMs)
        assertEquals(false, ui.playing)
    }

    @Test
    fun `from populated MediaState surfaces title artist playing duration`() {
        val s = MediaState(
            title = "Track",
            artist = "Artist",
            durationMs = 200_000L,
            positionMs = 1_000L,
            positionAtMs = 0L,
            playing = false, // false so livePosition() returns positionMs verbatim
            speed = 1f,
        )
        val ui = MediaUiState.from(s)
        assertEquals("Track", ui.title)
        assertEquals("Artist", ui.artist)
        assertEquals(200_000L, ui.durationMs)
        assertEquals(1_000L, ui.positionMs)
        assertTrue("populated state cannot be isEmpty", !ui.isEmpty)
    }

    @Test
    fun `MediaUiState data class equality holds`() {
        val a = MediaUiState.from(MediaState.empty)
        val b = MediaUiState.from(MediaState.empty)
        assertEquals(a, b)
    }
}
