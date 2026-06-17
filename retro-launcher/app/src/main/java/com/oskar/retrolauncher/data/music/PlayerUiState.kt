package com.oskar.retrolauncher.data.music

/** Snapshot of what the built-in player is currently playing. */
data class NowPlaying(
    val mediaId: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUri: String?,
    val durationMs: Long,
    val isPlaying: Boolean,
)

/** One entry in the player's playback queue. */
data class QueueItem(
    val mediaId: String,
    val title: String,
    val artist: String,
    val artworkUri: String?,
)
