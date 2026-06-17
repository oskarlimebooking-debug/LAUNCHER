package com.oskar.retrolauncher

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.oskar.retrolauncher.data.music.MIGRATION_1_2
import com.oskar.retrolauncher.data.trip.AppDb
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * M0 — verifies the 1 → 2 migration:
 *  - the music tables it creates byte-match Room's own generated v2 schema
 *    (so Room's runtime integrity check passes after a real device upgrade), and
 *  - existing `trips` rows survive the upgrade.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MusicMigrationTest {

    /** name -> CREATE sql, for every object SQLite stored verbatim. */
    private fun schema(db: SupportSQLiteDatabase): Map<String, String> {
        val out = mutableMapOf<String, String>()
        db.query("SELECT name, sql FROM sqlite_master WHERE sql IS NOT NULL").use { c ->
            while (c.moveToNext()) out[c.getString(0)] = c.getString(1)
        }
        return out
    }

    private fun rawV1(ctx: Context, tripsSql: String, tripPointsSql: String): SupportSQLiteDatabase {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(ctx)
                .name(null) // in-memory
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(tripsSql)
                        db.execSQL(tripPointsSql)
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, old: Int, new: Int) = Unit
                })
                .build(),
        )
        return helper.writableDatabase
    }

    @Test
    fun `migration creates music tables matching Room v2 schema and preserves trips`() {
        val ctx = RuntimeEnvironment.getApplication() as Context

        // Room's canonical v2 schema (source of truth for the byte-match).
        val room = Room.inMemoryDatabaseBuilder(ctx, AppDb::class.java)
            .allowMainThreadQueries().build()
        val canonical = schema(room.openHelper.writableDatabase)
        room.close()

        // A v1 database with the (unchanged) trip tables + one trip row.
        val v1 = rawV1(ctx, canonical.getValue("trips"), canonical.getValue("trip_points"))
        v1.execSQL(
            "INSERT INTO trips (id, startMs, endMs, distanceM, avgSpeedMs, maxSpeedMs, " +
                "startLabel, endLabel) VALUES (1, 100, 200, 1500.0, 10.0, 25.0, 'A', 'B')",
        )

        MIGRATION_1_2.migrate(v1)
        val migrated = schema(v1)

        for (obj in listOf("liked_tracks", "playlists", "playlist_items", "index_playlist_items_playlistId")) {
            assertEquals("schema mismatch for $obj", canonical[obj], migrated[obj])
        }

        v1.query("SELECT COUNT(*) FROM trips").use { c ->
            c.moveToFirst()
            assertEquals("trip row must survive the upgrade", 1, c.getInt(0))
        }
        v1.close()
    }
}
