package com.oskar.retrolauncher

import com.oskar.retrolauncher.data.music.AlbumArtUris
import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumArtUrisTest {

    @Test
    fun `album art uri appends album id`() {
        assertEquals("content://media/external/audio/albumart/42", AlbumArtUris.forAlbum(42))
    }

    @Test
    fun `track uri appends media id`() {
        assertEquals("content://media/external/audio/media/7", AlbumArtUris.forTrack(7))
    }
}
