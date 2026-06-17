package com.oskar.retrolauncher

import android.database.MatrixCursor
import android.provider.MediaStore
import com.oskar.retrolauncher.data.music.MediaStoreQueries
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * M2 — cursor → model mapping + album/artist derivation. Uses a MatrixCursor so
 * no real ContentResolver / device is needed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MediaStoreQueriesTest {

    private fun cursor(vararg rows: Array<Any?>): MatrixCursor =
        MatrixCursor(MediaStoreQueries.PROJECTION).apply { rows.forEach { addRow(it) } }

    // id, title, artist, album, albumId, duration, track
    private fun row(id: Long, title: String, artist: String, album: String, albumId: Long, track: Int) =
        arrayOf<Any?>(id, title, artist, album, albumId, 200_000L, track)

    @Test
    fun `readTracks maps columns and builds content uris`() {
        val tracks = MediaStoreQueries.readTracks(
            cursor(row(5, "Song A", "Artist X", "Album One", 30, 1004)),
        )
        assertEquals(1, tracks.size)
        val t = tracks.single()
        assertEquals(5L, t.id)
        assertEquals("content://media/external/audio/media/5", t.uri)
        assertEquals("Song A", t.title)
        assertEquals("Artist X", t.artist)
        assertEquals("Album One", t.album)
        assertEquals(30L, t.albumId)
        assertEquals(200_000L, t.durationMs)
        assertEquals(4, t.trackNo) // 1004 -> disc 1, track 4
        assertEquals("content://media/external/audio/albumart/30", t.albumArtUri)
    }

    @Test
    fun `deriveAlbums groups by albumId with counts, sorted by name`() {
        val tracks = MediaStoreQueries.readTracks(
            cursor(
                row(1, "B", "Artist X", "Zebra", 10, 1),
                row(2, "C", "Artist X", "Zebra", 10, 2),
                row(3, "A", "Artist Y", "Apple", 20, 1),
            ),
        )
        val albums = MediaStoreQueries.deriveAlbums(tracks)
        assertEquals(listOf("Apple", "Zebra"), albums.map { it.name })
        assertEquals(2, albums.first { it.id == 10L }.trackCount)
        assertEquals("Artist X", albums.first { it.id == 10L }.artist)
    }

    @Test
    fun `deriveArtists groups by artist with album and track counts`() {
        val tracks = MediaStoreQueries.readTracks(
            cursor(
                row(1, "B", "Artist X", "Zebra", 10, 1),
                row(2, "C", "Artist X", "Yak", 11, 1),
                row(3, "A", "Artist Y", "Apple", 20, 1),
            ),
        )
        val artists = MediaStoreQueries.deriveArtists(tracks)
        assertEquals(listOf("Artist X", "Artist Y"), artists.map { it.name })
        val x = artists.first { it.name == "Artist X" }
        assertEquals(2, x.albumCount)
        assertEquals(2, x.trackCount)
    }

    @Test
    fun `projection and selection reference IS_MUSIC`() {
        assertEquals(7, MediaStoreQueries.PROJECTION.size)
        assertEquals(true, MediaStoreQueries.SELECTION.contains(MediaStore.Audio.Media.IS_MUSIC))
    }
}
