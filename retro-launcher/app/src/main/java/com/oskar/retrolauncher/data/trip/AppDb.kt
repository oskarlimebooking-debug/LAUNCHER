package com.oskar.retrolauncher.data.trip

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TripEntity::class, TripPoint::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDb : RoomDatabase() {
    abstract fun trips(): TripDao
}
