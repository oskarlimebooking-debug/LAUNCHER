package com.oskar.retrolauncher.data.music

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A track the user has favourited. Keyed by the MediaStore audio id so a like
 * survives re-scans; [uri] is cached so a liked track can still be resolved if
 * the row is read without a fresh MediaStore lookup.
 */
@Entity(tableName = "liked_tracks")
data class LikedTrack(
    @PrimaryKey val trackId: Long,
    val uri: String,
    val addedMs: Long,
)

/** A user-created playlist. */
@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdMs: Long,
)

/**
 * One track within a playlist. [position] is the 0-based order within the
 * playlist; rows cascade-delete when their parent playlist is removed.
 */
@Entity(
    tableName = "playlist_items",
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("playlistId")],
)
data class PlaylistItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val trackId: Long,
    val uri: String,
    val position: Int,
)
