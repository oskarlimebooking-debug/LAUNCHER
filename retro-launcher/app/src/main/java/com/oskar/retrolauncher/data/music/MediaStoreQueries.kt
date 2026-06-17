package com.oskar.retrolauncher.data.music

import android.database.Cursor
import android.provider.MediaStore

/**
 * Pure MediaStore.Audio query plumbing: the projection / selection / sort plus
 * cursor → model mappers and album/artist derivation. Takes a [Cursor] (never a
 * ContentResolver) so it's unit-testable with a `MatrixCursor`.
 */
object MediaStoreQueries {

    val PROJECTION = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.TRACK,
    )

    /** Music files only, with a real title. */
    const val SELECTION = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND " +
        "${MediaStore.Audio.Media.TITLE} IS NOT NULL"

    const val SORT = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

    fun readTracks(cursor: Cursor): List<Track> {
        val out = ArrayList<Track>(cursor.count.coerceAtLeast(0))
        while (cursor.moveToNext()) out += cursorToTrack(cursor)
        return out
    }

    fun cursorToTrack(c: Cursor): Track {
        val id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
        val albumId = c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
        val rawTrack = c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK))
        return Track(
            id = id,
            uri = AlbumArtUris.forTrack(id),
            title = c.stringOr(MediaStore.Audio.Media.TITLE, "Unknown"),
            artist = c.stringOr(MediaStore.Audio.Media.ARTIST, "Unknown artist"),
            album = c.stringOr(MediaStore.Audio.Media.ALBUM, "Unknown album"),
            albumId = albumId,
            durationMs = c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)),
            trackNo = rawTrack % 1000, // MediaStore encodes disc*1000 + track
            albumArtUri = AlbumArtUris.forAlbum(albumId),
        )
    }

    fun deriveAlbums(tracks: List<Track>): List<Album> =
        tracks.groupBy { it.albumId }.map { (albumId, ts) ->
            val first = ts.first()
            Album(albumId, first.album, first.artist, first.albumArtUri, ts.size)
        }.sortedBy { it.name.lowercase() }

    fun deriveArtists(tracks: List<Track>): List<Artist> =
        tracks.groupBy { it.artist }.map { (name, ts) ->
            Artist(name, ts.map { it.albumId }.distinct().size, ts.size)
        }.sortedBy { it.name.lowercase() }

    private fun Cursor.stringOr(column: String, fallback: String): String {
        val idx = getColumnIndex(column)
        return if (idx >= 0) getString(idx) ?: fallback else fallback
    }
}
