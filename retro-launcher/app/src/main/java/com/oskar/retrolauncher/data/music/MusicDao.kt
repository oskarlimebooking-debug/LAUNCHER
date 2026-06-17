package com.oskar.retrolauncher.data.music

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Likes + playlists persistence. Kept to a single DAO (< 15 methods) since the
 * two concerns are small; split into `LikedDao` + `PlaylistDao` if it grows.
 */
@Dao
interface MusicDao {

    // ---- Likes ----

    @Query("SELECT trackId FROM liked_tracks")
    fun likedTrackIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLike(track: LikedTrack)

    @Query("DELETE FROM liked_tracks WHERE trackId = :trackId")
    suspend fun deleteLike(trackId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM liked_tracks WHERE trackId = :trackId)")
    suspend fun isLiked(trackId: Long): Boolean

    // ---- Playlists ----

    @Query("SELECT * FROM playlists ORDER BY createdMs DESC")
    fun playlists(): Flow<List<Playlist>>

    @Insert
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun renamePlaylist(id: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    // ---- Playlist items ----

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY position ASC")
    fun itemsForPlaylist(playlistId: Long): Flow<List<PlaylistItem>>

    @Query("SELECT MAX(position) FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun maxPosition(playlistId: Long): Int?

    @Insert
    suspend fun insertItem(item: PlaylistItem)

    @Query("DELETE FROM playlist_items WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("UPDATE playlist_items SET position = :position WHERE id = :id")
    suspend fun updateItemPosition(id: Long, position: Int)
}
