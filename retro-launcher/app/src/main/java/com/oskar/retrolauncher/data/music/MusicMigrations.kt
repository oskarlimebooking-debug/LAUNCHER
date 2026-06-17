package com.oskar.retrolauncher.data.music

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * AppDb 1 → 2: adds the music tables (likes + playlists). Purely additive —
 * the existing `trips` / `trip_points` tables are untouched so recorded trips
 * survive the upgrade. The CREATE statements must byte-match Room's generated
 * schema for the v2 entities; `MusicMigrationTest` asserts that equality.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `liked_tracks` " +
                "(`trackId` INTEGER NOT NULL, `uri` TEXT NOT NULL, `addedMs` INTEGER NOT NULL, " +
                "PRIMARY KEY(`trackId`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `playlists` " +
                "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
                "`createdMs` INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `playlist_items` " +
                "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `playlistId` INTEGER NOT NULL, " +
                "`trackId` INTEGER NOT NULL, `uri` TEXT NOT NULL, `position` INTEGER NOT NULL, " +
                "FOREIGN KEY(`playlistId`) REFERENCES `playlists`(`id`) ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_playlist_items_playlistId` " +
                "ON `playlist_items` (`playlistId`)",
        )
    }
}
