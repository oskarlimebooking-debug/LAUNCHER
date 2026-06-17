package com.oskar.retrolauncher.data.music

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Likes + playlists, layered over [MusicDao]. Decouples the UI from Room and
 * exposes liked-track ids as a `Set` for cheap membership checks in adapters.
 */
class MusicLibraryRepository(
    private val dao: MusicDao,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    val likedTrackIds: Flow<Set<Long>> = dao.likedTrackIds().map { it.toSet() }
    val playlists: Flow<List<Playlist>> = dao.playlists()

    fun itemsForPlaylist(playlistId: Long): Flow<List<PlaylistItem>> =
        dao.itemsForPlaylist(playlistId)

    suspend fun toggleLike(track: Track) {
        if (dao.isLiked(track.id)) {
            dao.deleteLike(track.id)
        } else {
            dao.insertLike(LikedTrack(track.id, track.uri, clock()))
        }
    }

    suspend fun createPlaylist(name: String): Long =
        dao.insertPlaylist(Playlist(name = name.trim(), createdMs = clock()))

    suspend fun renamePlaylist(id: Long, name: String) = dao.renamePlaylist(id, name.trim())

    suspend fun deletePlaylist(id: Long) = dao.deletePlaylist(id)

    /** Append [track] to the end of the playlist. */
    suspend fun addToPlaylist(playlistId: Long, track: Track) {
        val position = (dao.maxPosition(playlistId) ?: -1) + 1
        dao.insertItem(PlaylistItem(playlistId = playlistId, trackId = track.id, uri = track.uri, position = position))
    }

    suspend fun removeItem(itemId: Long) = dao.deleteItem(itemId)
}
