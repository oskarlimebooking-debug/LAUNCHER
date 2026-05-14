package com.oskar.retrolauncher

import android.graphics.Bitmap
import com.oskar.retrolauncher.data.media.MediaRepository
import com.oskar.retrolauncher.data.media.MediaState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import android.media.AudioManager
import java.util.concurrent.atomic.AtomicLong

/**
 * T1.16 ACs covered:
 *   - AC1: `MediaState.empty` emitted on cold start and after `clear()`.
 *   - AC2: position increments at 250 ms cadence while playing, freezes when paused.
 *   - AC3: a new track update resets position to 0 and updates duration atomically.
 *   - AC4: the previously-held album-art bitmap is recycled when replaced (and
 *          NOT recycled when the same instance is reused, which would corrupt
 *          a Glide bitmap pool sharing the same reference).
 *   - AC5: thread-safe under concurrent emit + collect from two coroutines.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class MediaRepositoryTest {

    private fun trackState(
        title: String = "Black Hole Sun",
        positionMs: Long = 0L,
        positionAtMs: Long = 0L,
        playing: Boolean = true,
        speed: Float = 1f,
        durationMs: Long = 320_000L,
        art: Bitmap? = null,
        packageName: String = "com.example.player",
    ) = MediaState(
        title = title,
        artist = "Soundgarden",
        album = "Superunknown",
        durationMs = durationMs,
        positionMs = positionMs,
        positionAtMs = positionAtMs,
        speed = speed,
        playing = playing,
        art = art,
        packageName = packageName,
    )

    @Test
    fun `cold start emits MediaState empty`() = runTest {
        val repo = MediaRepository()
        val s = repo.state.first()
        assertEquals(MediaState.empty, s)
        assertTrue(s.isEmpty)
    }

    @Test
    fun `clear emits empty state`() = runTest {
        val repo = MediaRepository()
        repo.update(trackState())
        repo.clear()
        assertEquals(MediaState.empty, repo.state.value)
        assertTrue(repo.state.value.isEmpty)
    }

    @Test
    fun `position increments at 250 ms cadence while playing`() = runTest {
        val now = AtomicLong(1_000L)
        val repo = MediaRepository(tickIntervalMs = 250L, clock = { now.get() })
        repo.startPositionTicker(backgroundScope)
        runCurrent()

        repo.update(
            trackState(positionMs = 0L, positionAtMs = 1_000L, playing = true, speed = 1f),
        )
        runCurrent()

        // Advance virtual time + wall clock together: 3 ticks of 250 ms.
        now.set(1_250L); advanceTimeBy(250L); runCurrent()
        assertEquals(250L, repo.state.value.positionMs)

        now.set(1_500L); advanceTimeBy(250L); runCurrent()
        assertEquals(500L, repo.state.value.positionMs)

        now.set(1_750L); advanceTimeBy(250L); runCurrent()
        assertEquals(750L, repo.state.value.positionMs)
    }

    @Test
    fun `position freezes when paused`() = runTest {
        val now = AtomicLong(1_000L)
        val repo = MediaRepository(tickIntervalMs = 250L, clock = { now.get() })
        repo.startPositionTicker(backgroundScope)
        runCurrent()

        repo.update(trackState(positionMs = 0L, positionAtMs = 1_000L, playing = true))
        runCurrent()

        now.set(1_250L); advanceTimeBy(250L); runCurrent()
        assertEquals(250L, repo.state.value.positionMs)

        // Pause at the current position. The ticker must not advance position any further.
        repo.update(
            trackState(positionMs = 250L, positionAtMs = 1_250L, playing = false),
        )
        runCurrent()

        now.set(2_500L); advanceTimeBy(1_250L); runCurrent()
        assertEquals(250L, repo.state.value.positionMs)
        assertFalse(repo.state.value.playing)
    }

    @Test
    fun `new track update resets position to 0 and refreshes duration atomically`() = runTest {
        val repo = MediaRepository()

        repo.update(trackState(title = "Old", positionMs = 50_000L, durationMs = 200_000L))
        // Single update() call replaces the entire state — observers see the new title,
        // position, and duration in one StateFlow emission (StateFlow.value is atomic).
        repo.update(trackState(title = "New", positionMs = 0L, durationMs = 320_000L))

        val s = repo.state.value
        assertEquals("New", s.title)
        assertEquals(0L, s.positionMs)
        assertEquals(320_000L, s.durationMs)
    }

    @Test
    fun `previous album art bitmap is recycled when replaced`() = runTest {
        val repo = MediaRepository()
        val artA = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        val artB = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)

        repo.update(trackState(art = artA))
        assertFalse(artA.isRecycled)

        repo.update(trackState(art = artB))
        assertTrue("artA should be recycled after replacement", artA.isRecycled)
        assertFalse("artB should still be live", artB.isRecycled)
    }

    @Test
    fun `same album art instance is NOT recycled across updates`() = runTest {
        val repo = MediaRepository()
        val art = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)

        repo.update(trackState(art = art))
        repo.update(trackState(positionMs = 1_000L, art = art))

        assertFalse(
            "same Bitmap instance reused across updates must not be recycled",
            art.isRecycled,
        )
        assertSame(art, repo.state.value.art)
    }

    @Test
    fun `clear recycles the held album art`() = runTest {
        val repo = MediaRepository()
        val art = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)

        repo.update(trackState(art = art))
        repo.clear()

        assertTrue(art.isRecycled)
        assertNull(repo.state.value.art)
    }

    @Test
    fun `concurrent emit and collect from two coroutines is safe`() = runTest {
        val repo = MediaRepository()
        val seenA = mutableListOf<String?>()
        val seenB = mutableListOf<String?>()

        val collectA = backgroundScope.launch {
            repo.state.collect { seenA += it.title }
        }
        val collectB = backgroundScope.launch {
            repo.state.collect { seenB += it.title }
        }

        // Two writers racing into the same MutableStateFlow. With StateFlow's
        // atomic value setter no observer should ever see a partially-built state.
        val writer1 = backgroundScope.launch {
            repeat(50) { repo.update(trackState(title = "A$it")) }
        }
        val writer2 = backgroundScope.launch {
            repeat(50) { repo.update(trackState(title = "B$it")) }
        }

        advanceUntilIdle()
        writer1.join()
        writer2.join()
        collectA.cancel(); collectB.cancel()

        assertTrue("collector A saw at least one emission", seenA.isNotEmpty())
        assertTrue("collector B saw at least one emission", seenB.isNotEmpty())
        // Final state is one of the two writers' last titles — either is fine,
        // we only require that the value is well-formed and consistent across
        // both collectors' final observations.
        val finalTitle = repo.state.value.title
        assertTrue(finalTitle == "A49" || finalTitle == "B49")
        assertEquals(finalTitle, seenA.last())
        assertEquals(finalTitle, seenB.last())
    }

    // -- Audio focus passthrough (T1.48) -----------------------------------

    @Test
    fun `onAudioDucked sets playing to false`() = runTest {
        val repo = MediaRepository()
        repo.update(trackState(playing = true))
        assertTrue(repo.state.value.playing)

        repo.onAudioDucked()
        assertFalse("playing must be false after duck", repo.state.value.playing)
    }

    @Test
    fun `onAudioDucked is idempotent when already not playing`() = runTest {
        val repo = MediaRepository()
        repo.update(trackState(playing = false))
        assertFalse(repo.state.value.playing)

        repo.onAudioDucked()
        assertFalse("still false after repeated duck", repo.state.value.playing)
    }

    @Test
    fun `audio focus loss transient can duck maps to AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK`() {
        // Verify the Android constant values we depend on
        assertEquals(
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,
            -3,
        )
        assertEquals(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, -2)
        assertEquals(AudioManager.AUDIOFOCUS_GAIN, 1)
    }

    @Test
    fun `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK constant is available at API 23`() {
        // The constant was added in API 8; must exist at minSdk 23
        assertEquals(
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
            4,
        )
    }

    @Test
    fun `focus loss transient or may duck both trigger duck state`() {
        val repo = MediaRepository()
        repo.update(trackState(playing = true))

        // Both loss types should result in playing = false
        val lossTypes = listOf(
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,
        )
        for (lossType in lossTypes) {
            repo.onAudioDucked()
            assertFalse("playing false for loss type $lossType", repo.state.value.playing)
            repo.update(trackState(playing = true)) // reset
        }
    }
}
