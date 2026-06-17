package com.oskar.retrolauncher

import androidx.room.Room
import com.oskar.retrolauncher.data.music.MusicLibraryRepository
import com.oskar.retrolauncher.data.music.Track
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

/** M4 — likes + playlists over an in-memory database. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MusicLibraryRepositoryTest {

    private lateinit var db: AppDb
    private lateinit var repo: MusicLibraryRepository

    private fun track(id: Long) = Track(
        id = id, uri = "u$id", title = "T$id", artist = "A", album = "Al", albumId = 1, durationMs = 1000,
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDb::class.java)
            .allowMainThreadQueries().build()
        repo = MusicLibraryRepository(db.music(), clock = { 42L })
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `toggleLike adds then removes, idempotent set`() = runTest {
        repo.toggleLike(track(7))
        assertEquals(setOf(7L), repo.likedTrackIds.first())
        repo.toggleLike(track(7))
        assertTrue(repo.likedTrackIds.first().isEmpty())
    }

    @Test
    fun `addToPlaylist appends with increasing positions`() = runTest {
        val pid = repo.createPlaylist("Road")
        repo.addToPlaylist(pid, track(1))
        repo.addToPlaylist(pid, track(2))
        repo.addToPlaylist(pid, track(3))
        val items = repo.itemsForPlaylist(pid).first()
        assertEquals(listOf(1L, 2L, 3L), items.map { it.trackId })
        assertEquals(listOf(0, 1, 2), items.map { it.position })
    }

    @Test
    fun `removeItem drops a single entry`() = runTest {
        val pid = repo.createPlaylist("Mix")
        repo.addToPlaylist(pid, track(1))
        repo.addToPlaylist(pid, track(2))
        val first = repo.itemsForPlaylist(pid).first().first()
        repo.removeItem(first.id)
        assertEquals(listOf(2L), repo.itemsForPlaylist(pid).first().map { it.trackId })
    }

    @Test
    fun `deletePlaylist cascades its items`() = runTest {
        val pid = repo.createPlaylist("Mix")
        repo.addToPlaylist(pid, track(1))
        repo.deletePlaylist(pid)
        assertTrue(repo.playlists.first().isEmpty())
        assertTrue(repo.itemsForPlaylist(pid).first().isEmpty())
    }

    @Test
    fun `rename changes the playlist name`() = runTest {
        val pid = repo.createPlaylist("Old")
        repo.renamePlaylist(pid, "New")
        assertEquals("New", repo.playlists.first().single().name)
    }
}
