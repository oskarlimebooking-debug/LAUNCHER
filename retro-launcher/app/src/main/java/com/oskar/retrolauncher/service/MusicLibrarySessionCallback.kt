package com.oskar.retrolauncher.service

import androidx.media3.common.MediaItem
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Session callback. For now its job is to make controller-supplied items
 * playable again: when a controller sends `MediaItem`s across the binder Media3
 * strips their `localConfiguration` (URI) for security, so we rebuild each from
 * its `requestMetadata.mediaUri` via [MediaItemFactory.resolve].
 *
 * The browse tree (onGetLibraryRoot / onGetChildren) is added in M2 once the
 * MediaStore library exists.
 */
class MusicLibrarySessionCallback : MediaLibrarySession.Callback {

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
    ): ListenableFuture<MutableList<MediaItem>> {
        val resolved = mediaItems.mapTo(ArrayList(mediaItems.size), MediaItemFactory::resolve)
        return Futures.immediateFuture(resolved)
    }
}
