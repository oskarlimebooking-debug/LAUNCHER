package com.oskar.retrolauncher.data.music

/**
 * Local-library models, populated from MediaStore (M2). Kept as plain data
 * classes with no Android types so they're trivial to construct in tests and
 * to map to/from Media3 `MediaItem`s ([MediaItemFactory]).
 */
data class Track(
    val id: Long,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val trackNo: Int = 0,
    val albumArtUri: String? = null,
)

data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val artUri: String? = null,
    val trackCount: Int = 0,
)

data class Artist(
    val name: String,
    val albumCount: Int = 0,
    val trackCount: Int = 0,
)
