package com.oskar.retrolauncher.data.music

import android.content.Context
import android.provider.MediaStore
import com.oskar.retrolauncher.util.Permissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * The on-device music library. Runs a single MediaStore.Audio scan on IO,
 * caches the result in memory, and derives albums/artists from the track list
 * (one query, no extra round-trips). Exposes everything as StateFlows for the
 * browse UI. Album/artist detail lists are filtered from the cached tracks.
 */
class LocalMusicRepository(private val context: Context) {

    private val _songs = MutableStateFlow<List<Track>>(emptyList())
    val songs: StateFlow<List<Track>> = _songs

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums

    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists

    private val _scanState = MutableStateFlow<MusicScanState>(MusicScanState.Idle)
    val scanState: StateFlow<MusicScanState> = _scanState

    /** Re-read the library. No-op error path keeps the player usable if denied. */
    suspend fun scan() {
        if (!Permissions.hasAudioRead(context)) {
            _scanState.value = MusicScanState.PermissionDenied
            return
        }
        _scanState.value = MusicScanState.Scanning
        val tracks = withContext(Dispatchers.IO) { queryTracks() }
        _songs.value = tracks
        _albums.value = MediaStoreQueries.deriveAlbums(tracks)
        _artists.value = MediaStoreQueries.deriveArtists(tracks)
        _scanState.value = MusicScanState.Done(tracks.size)
    }

    private fun queryTracks(): List<Track> = runCatching {
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            MediaStoreQueries.PROJECTION,
            MediaStoreQueries.SELECTION,
            null,
            MediaStoreQueries.SORT,
        )?.use { MediaStoreQueries.readTracks(it) } ?: emptyList()
    }.getOrElse {
        Timber.w(it, "music scan failed")
        emptyList()
    }

    fun tracksForAlbum(albumId: Long): List<Track> =
        _songs.value.filter { it.albumId == albumId }.sortedBy { it.trackNo }

    fun tracksForArtist(artist: String): List<Track> =
        _songs.value.filter { it.artist == artist }
}
