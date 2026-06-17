package com.oskar.retrolauncher

import androidx.test.core.app.ApplicationProvider
import com.oskar.retrolauncher.data.music.LocalMusicRepository
import com.oskar.retrolauncher.data.music.MusicScanState
import com.oskar.retrolauncher.util.Permissions
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * M2 — the scan respects the audio-read permission and degrades gracefully.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = App::class, sdk = [28])
class LocalMusicRepositoryTest {

    @Test
    fun `scan without permission yields PermissionDenied`() = runTest {
        val repo = LocalMusicRepository(ApplicationProvider.getApplicationContext())
        repo.scan()
        assertEquals(MusicScanState.PermissionDenied, repo.scanState.value)
    }

    @Test
    fun `scan with permission over an empty store yields Done zero`() = runTest {
        val app = ApplicationProvider.getApplicationContext<App>()
        shadowOf(app).grantPermissions(Permissions.audioReadPermission())
        val repo = LocalMusicRepository(app)
        repo.scan()
        assertEquals(MusicScanState.Done(0), repo.scanState.value)
        assertEquals(emptyList<Any>(), repo.songs.value)
    }
}
