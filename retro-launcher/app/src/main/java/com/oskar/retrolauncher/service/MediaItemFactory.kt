package com.oskar.retrolauncher.service

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.oskar.retrolauncher.data.music.Track

/**
 * Pure mapping between local [Track]s and Media3 [MediaItem]s.
 *
 * The playable URI is carried in BOTH `localConfiguration` (set via `setUri`)
 * and `requestMetadata.mediaUri`. When a controller sends items across the
 * binder to the session, Media3 strips `localConfiguration` for security — so
 * the session's `onAddMediaItems` callback rebuilds the playable item from
 * `requestMetadata.mediaUri` via [resolve].
 */
object MediaItemFactory {

    fun toMediaItem(track: Track): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setTrackNumber(track.trackNo.takeIf { it > 0 })
            .setArtworkUri(track.albumArtUri?.let(Uri::parse))
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .build()
        val uri = Uri.parse(track.uri)
        return MediaItem.Builder()
            .setMediaId(track.id.toString())
            .setUri(uri)
            .setMediaMetadata(metadata)
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder().setMediaUri(uri).build(),
            )
            .build()
    }

    /**
     * Rebuild a playable item from one whose `localConfiguration` was stripped
     * crossing the binder. Falls back to the original item if no URI is present.
     */
    fun resolve(item: MediaItem): MediaItem {
        val uri = item.requestMetadata.mediaUri ?: return item
        return item.buildUpon().setUri(uri).build()
    }

    fun trackId(item: MediaItem): Long? = item.mediaId.toLongOrNull()
}
