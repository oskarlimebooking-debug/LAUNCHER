package com.oskar.retrolauncher

import androidx.media3.common.MediaItem
import com.oskar.retrolauncher.data.music.Track
import com.oskar.retrolauncher.service.MediaItemFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * M1 — Track <-> MediaItem mapping. The playable URI must survive the binder
 * (carried in requestMetadata) and `resolve` must rebuild a playable item from
 * one whose localConfiguration was stripped.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MediaItemFactoryTest {

    private val track = Track(
        id = 77,
        uri = "content://media/external/audio/media/77",
        title = "Midnight Drive",
        artist = "The Cassettes",
        album = "Night Roads",
        albumId = 9,
        durationMs = 215_000,
        trackNo = 4,
        albumArtUri = "content://media/external/audio/albumart/9",
    )

    @Test
    fun `toMediaItem maps id, metadata and carries uri in requestMetadata`() {
        val item = MediaItemFactory.toMediaItem(track)
        assertEquals("77", item.mediaId)
        assertEquals("Midnight Drive", item.mediaMetadata.title)
        assertEquals("The Cassettes", item.mediaMetadata.artist)
        assertEquals("Night Roads", item.mediaMetadata.albumTitle)
        assertEquals(4, item.mediaMetadata.trackNumber)
        assertEquals(track.uri, item.localConfiguration?.uri?.toString())
        assertEquals(track.uri, item.requestMetadata.mediaUri?.toString())
    }

    @Test
    fun `resolve rebuilds playable uri from a binder-stripped item`() {
        val original = MediaItemFactory.toMediaItem(track)
        // Simulate the controller->session binder: localConfiguration is dropped.
        val stripped = MediaItem.Builder()
            .setMediaId(original.mediaId)
            .setMediaMetadata(original.mediaMetadata)
            .setRequestMetadata(original.requestMetadata)
            .build()
        assertNull("precondition: stripped item has no uri", stripped.localConfiguration)

        val resolved = MediaItemFactory.resolve(stripped)
        assertNotNull(resolved.localConfiguration)
        assertEquals(track.uri, resolved.localConfiguration?.uri?.toString())
    }

    @Test
    fun `trackId round-trips, returns null for non-numeric ids`() {
        assertEquals(77L, MediaItemFactory.trackId(MediaItemFactory.toMediaItem(track)))
        assertNull(MediaItemFactory.trackId(MediaItem.Builder().setMediaId("root").build()))
    }
}
