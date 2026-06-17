package com.oskar.retrolauncher.data.trip

import androidx.room.Database
import androidx.room.RoomDatabase
import com.oskar.retrolauncher.data.music.LikedTrack
import com.oskar.retrolauncher.data.music.MusicDao
import com.oskar.retrolauncher.data.music.Playlist
import com.oskar.retrolauncher.data.music.PlaylistItem

@Database(
    entities = [
        TripEntity::class,
        TripPoint::class,
        LikedTrack::class,
        Playlist::class,
        PlaylistItem::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDb : RoomDatabase() {
    abstract fun trips(): TripDao
    abstract fun music(): MusicDao
}
