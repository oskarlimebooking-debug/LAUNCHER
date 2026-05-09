package com.oskar.retrolauncher

import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.PlaybackState
import com.oskar.retrolauncher.service.MediaNotificationListener
import com.oskar.retrolauncher.service.metadataToMediaState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * T1.15 ACs covered:
 *   - AC3: `metadataToMediaState` extracts title, artist, album, art bitmap, duration.
 *   - AC4: `metadataToMediaState` produces isPlaying / position / speed from PlaybackState.
 *   - AC5: null metadata + null playback-state are safe (no NPE; produces an empty state).
 *
 * AC1 (manual settings grant) is exercised via `MediaNotificationListener.isEnabled(ctx)`,
 * which reads `enabled_notification_listeners` from Settings.Secure.
 *
 * AC2 (Spotify / YouTube Music / stock detection) is a runtime check — generic
 * `MediaSessionManager.getActiveSessions(...)` already covers any MediaStyle source.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class MediaNotificationListenerTest {

    @Test
    fun `metadata extraction pulls title artist album duration and art`() {
        val art = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        val md = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, "Black Hole Sun")
            .putString(MediaMetadata.METADATA_KEY_ARTIST, "Soundgarden")
            .putString(MediaMetadata.METADATA_KEY_ALBUM, "Superunknown")
            .putLong(MediaMetadata.METADATA_KEY_DURATION, 320_000L)
            .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, art)
            .build()
        val state = metadataToMediaState(md, playbackState = null, packageName = "com.example.player")
        assertEquals("Black Hole Sun", state.title)
        assertEquals("Soundgarden", state.artist)
        assertEquals("Superunknown", state.album)
        assertEquals(320_000L, state.durationMs)
        assertSame(art, state.art)
        assertEquals("com.example.player", state.packageName)
    }

    @Test
    fun `art falls back to METADATA_KEY_ART when ALBUM_ART is missing`() {
        val art = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        val md = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, "T")
            .putBitmap(MediaMetadata.METADATA_KEY_ART, art)
            .build()
        val state = metadataToMediaState(md, playbackState = null, packageName = null)
        assertSame(art, state.art)
    }

    @Test
    fun `playback state extracts isPlaying position and speed`() {
        val ps = PlaybackState.Builder()
            .setState(PlaybackState.STATE_PLAYING, /* position */ 5_500L, /* speed */ 1.5f, /* updateTime */ 100L)
            .build()
        val state = metadataToMediaState(metadata = null, playbackState = ps, packageName = null)
        assertTrue("expected playing=true", state.playing)
        assertEquals(5_500L, state.positionMs)
        assertEquals(1.5f, state.speed, 0.0001f)
        assertEquals(100L, state.positionAtMs)
    }

    @Test
    fun `non-playing state reports isPlaying false`() {
        val ps = PlaybackState.Builder()
            .setState(PlaybackState.STATE_PAUSED, 0L, 1f, 0L)
            .build()
        val state = metadataToMediaState(metadata = null, playbackState = ps, packageName = null)
        assertFalse(state.playing)
    }

    @Test
    fun `all-null inputs produce a safe empty state without crashing`() {
        val state = metadataToMediaState(metadata = null, playbackState = null, packageName = null)
        assertNull(state.title)
        assertNull(state.artist)
        assertNull(state.album)
        assertNull(state.art)
        assertEquals(0L, state.durationMs)
        assertEquals(0L, state.positionMs)
        assertEquals(1f, state.speed, 0.0001f)
        assertFalse(state.playing)
        assertTrue(state.isEmpty)
    }

    @Test
    fun `isEnabled returns false when notification access is not granted`() {
        val ctx = RuntimeEnvironment.getApplication()
        // Robolectric's Settings.Secure starts empty for this key.
        assertFalse(MediaNotificationListener.isEnabled(ctx))
    }

    @Test
    fun `isEnabled returns true when our package appears in enabled listeners`() {
        val ctx = RuntimeEnvironment.getApplication()
        val component = "${ctx.packageName}/.service.MediaNotificationListener"
        android.provider.Settings.Secure.putString(
            ctx.contentResolver,
            "enabled_notification_listeners",
            component,
        )
        assertTrue(MediaNotificationListener.isEnabled(ctx))
    }
}
