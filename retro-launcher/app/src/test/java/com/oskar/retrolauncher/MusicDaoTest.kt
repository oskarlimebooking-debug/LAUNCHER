package com.oskar.retrolauncher

import androidx.room.Room
import com.oskar.retrolauncher.data.music.LikedTrack
import com.oskar.retrolauncher.data.music.MusicDao
import com.oskar.retrolauncher.data.music.Playlist
import com.oskar.retrolauncher.data.music.PlaylistItem
import com.oskar.retrolauncher.data.trip.AppDb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * M0 — likes + playlists DAO over an in-memory v2 database. Verifies the new
 * entities produce a schema Room accepts, plus CRUD + FK cascade behaviour.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MusicDaoTest {

    private lateinit var db: AppDb
    private lateinit var dao: MusicDao

    @Before
    fun setUp() {
        val ctx = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDb::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.music()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `like then unlike toggles membership and is idempotent`() = runTest {
        assertEquals(emptyList<Long>(), dao.likedTrackIds().first())

        dao.insertLike(LikedTrack(trackId = 42, uri = "content://audio/42", addedMs = 1000))
        // REPLACE conflict strategy: re-liking the same id keeps a single row.
        dao.insertLike(LikedTrack(trackId = 42, uri = "content://audio/42", addedMs = 2000))
        assertEquals(listOf(42L), dao.likedTrackIds().first())

        dao.deleteLike(42)
        assertEquals(emptyList<Long>(), dao.likedTrackIds().first())
    }

    @Test
    fun `playlist create rename delete`() = runTest {
        val id = dao.insertPlaylist(Playlist(name = "Road", createdMs = 10))
        assertEquals("Road", dao.playlists().first().single().name)

        dao.renamePlaylist(id, "Highway")
        assertEquals("Highway", dao.playlists().first().single().name)

        dao.deletePlaylist(id)
        assertTrue(dao.playlists().first().isEmpty())
    }

    @Test
    fun `playlist items order by position and report max`() = runTest {
        val pid = dao.insertPlaylist(Playlist(name = "Mix", createdMs = 0))
        assertEquals(null, dao.maxPosition(pid))

        dao.insertItem(PlaylistItem(playlistId = pid, trackId = 1, uri = "u1", position = 0))
        dao.insertItem(PlaylistItem(playlistId = pid, trackId = 2, uri = "u2", position = 1))
        dao.insertItem(PlaylistItem(playlistId = pid, trackId = 3, uri = "u3", position = 2))

        val items = dao.itemsForPlaylist(pid).first()
        assertEquals(listOf(1L, 2L, 3L), items.map { it.trackId })
        assertEquals(2, dao.maxPosition(pid))
    }

    @Test
    fun `deleting a playlist cascades to its items`() = runTest {
        val pid = dao.insertPlaylist(Playlist(name = "Mix", createdMs = 0))
        dao.insertItem(PlaylistItem(playlistId = pid, trackId = 1, uri = "u1", position = 0))
        dao.insertItem(PlaylistItem(playlistId = pid, trackId = 2, uri = "u2", position = 1))
        assertEquals(2, dao.itemsForPlaylist(pid).first().size)

        dao.deletePlaylist(pid)
        assertTrue(dao.itemsForPlaylist(pid).first().isEmpty())
    }
}
