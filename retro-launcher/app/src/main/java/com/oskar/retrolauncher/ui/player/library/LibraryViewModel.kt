package com.oskar.retrolauncher.ui.player.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oskar.retrolauncher.App
import com.oskar.retrolauncher.data.music.MusicScanState
import com.oskar.retrolauncher.data.music.Playlist
import com.oskar.retrolauncher.data.music.PlaylistItem
import com.oskar.retrolauncher.data.music.Track
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the music browser: library StateFlows, likes/playlists, and playback
 * routing. Shared across the browser tabs so the scan runs once.
 */
class LibraryViewModel : ViewModel() {

    val songs = App.localMusic.songs
    val albums = App.localMusic.albums
    val artists = App.localMusic.artists
    val scanState = App.localMusic.scanState

    val likedIds = App.musicLibrary.likedTrackIds
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val playlists = App.musicLibrary.playlists
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val likedTracks = combine(songs, App.musicLibrary.likedTrackIds) { all, ids ->
        all.filter { it.id in ids }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun ensureScanned() {
        when (scanState.value) {
            is MusicScanState.Idle, is MusicScanState.PermissionDenied -> rescan()
            else -> Unit
        }
    }

    fun rescan() {
        viewModelScope.launch { App.localMusic.scan() }
    }

    fun play(tracks: List<Track>, index: Int) {
        if (tracks.isEmpty()) return
        App.player.initialize()
        App.player.playTracks(tracks, index)
    }

    fun tracksForAlbum(albumId: Long): List<Track> = App.localMusic.tracksForAlbum(albumId)
    fun tracksForArtist(artist: String): List<Track> = App.localMusic.tracksForArtist(artist)

    fun playlistItems(playlistId: Long) = App.musicLibrary.itemsForPlaylist(playlistId)

    fun resolveTracks(items: List<PlaylistItem>): List<Track> {
        val byId = App.localMusic.songs.value.associateBy { it.id }
        return items.mapNotNull { byId[it.trackId] }
    }

    fun toggleLike(track: Track) {
        viewModelScope.launch { App.musicLibrary.toggleLike(track) }
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { App.musicLibrary.createPlaylist(name) }
    }

    fun addToPlaylist(playlist: Playlist, track: Track) {
        viewModelScope.launch { App.musicLibrary.addToPlaylist(playlist.id, track) }
    }

    fun createPlaylistAndAdd(name: String, track: Track) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = App.musicLibrary.createPlaylist(name)
            App.musicLibrary.addToPlaylist(id, track)
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch { App.musicLibrary.deletePlaylist(id) }
    }
}
