package com.oskar.retrolauncher.data.music

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.oskar.retrolauncher.service.MediaItemFactory
import com.oskar.retrolauncher.service.MusicService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * UI-facing bridge to [MusicService]. Connects a Media3 [MediaController] to the
 * launcher's own session and exposes now-playing state + transport for the full
 * player UI (the simple media card still reads `App.media` via the listener).
 *
 * All controller access happens on the main thread — Media3's requirement — so
 * [initialize] and the transport methods must be called from the UI thread.
 */
class PlayerController(private val context: Context) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    /** A play request issued before the controller finished connecting. */
    private var pendingPlay: Pair<List<Track>, Int>? = null

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying

    private val _queue = MutableStateFlow<List<QueueItem>>(emptyList())
    val queue: StateFlow<List<QueueItem>> = _queue

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = pushState()
    }

    /** Idempotently connect to the music session, starting the service if needed. */
    fun initialize() {
        if (controllerFuture != null) return
        val token = SessionToken(context, ComponentName(context, MusicService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                controller = runCatching { future.get() }.getOrNull()?.also { it.addListener(listener) }
                controller?.let { c -> pendingPlay?.let { (tracks, i) -> pendingPlay = null; doPlay(c, tracks, i) } }
                pushState()
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    fun release() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        controllerFuture = null
    }

    /**
     * Replace the queue with [tracks] and start playing at [startIndex]. If the
     * controller is still connecting, the request is held and flushed on connect.
     */
    fun playTracks(tracks: List<Track>, startIndex: Int) {
        val c = controller
        if (c != null) {
            doPlay(c, tracks, startIndex)
        } else {
            pendingPlay = tracks to startIndex
            initialize()
        }
    }

    private fun doPlay(controller: MediaController, tracks: List<Track>, startIndex: Int) {
        controller.setMediaItems(tracks.map(MediaItemFactory::toMediaItem), startIndex, 0L)
        controller.prepare()
        controller.play()
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun prev() {
        controller?.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    /** Live playback position in ms (read on the main thread). */
    fun positionMs(): Long = controller?.currentPosition ?: 0L

    /** Jump to queue entry [index] and play. */
    fun playIndex(index: Int) {
        controller?.seekTo(index, 0L)
        controller?.play()
    }

    fun moveQueueItem(from: Int, to: Int) {
        controller?.moveMediaItem(from, to)
    }

    private fun pushState() {
        val c = controller ?: return
        val item = c.currentMediaItem
        _nowPlaying.value = item?.let {
            NowPlaying(
                mediaId = it.mediaId,
                title = it.mediaMetadata.title?.toString().orEmpty(),
                artist = it.mediaMetadata.artist?.toString().orEmpty(),
                album = it.mediaMetadata.albumTitle?.toString().orEmpty(),
                artworkUri = it.mediaMetadata.artworkUri?.toString(),
                durationMs = c.duration.coerceAtLeast(0L),
                isPlaying = c.isPlaying,
            )
        }
        _queue.value = buildQueue(c)
        _currentIndex.value = c.currentMediaItemIndex
    }

    private fun buildQueue(c: MediaController): List<QueueItem> =
        (0 until c.mediaItemCount).map { i ->
            val md = c.getMediaItemAt(i).mediaMetadata
            QueueItem(
                mediaId = c.getMediaItemAt(i).mediaId,
                title = md.title?.toString().orEmpty(),
                artist = md.artist?.toString().orEmpty(),
                artworkUri = md.artworkUri?.toString(),
            )
        }
}
