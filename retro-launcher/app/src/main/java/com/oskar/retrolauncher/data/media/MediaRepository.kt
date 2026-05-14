package com.oskar.retrolauncher.data.media

import android.os.SystemClock
import com.oskar.retrolauncher.service.MediaNotificationListenerHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Single source of truth for the active media session.
 *
 * Holds a [MutableStateFlow] of [MediaState] that the notification listener
 * pushes into via [update] / [clear]. Independently runs a 250 ms position-tick
 * coroutine that extrapolates `positionMs` while playing, so the seek bar in
 * the UI advances smoothly without depending on the controller emitting a fresh
 * `PlaybackState` every frame.
 *
 * Thread-safety: every state mutation goes through [MutableStateFlow] (atomic);
 * the ticker uses [MutableStateFlow.compareAndSet] so a racing [update] (e.g.
 * a track change firing on the listener thread mid-tick) is never overwritten
 * with a stale extrapolated position.
 */
class MediaRepository(
    private val tickIntervalMs: Long = TICK_INTERVAL_MS,
    private val clock: () -> Long = { SystemClock.elapsedRealtime() },
) {

    private val _state = MutableStateFlow(MediaState.empty)
    val state: StateFlow<MediaState> = _state

    private var tickerJob: Job? = null

    fun update(s: MediaState) {
        val prev = _state.value
        _state.value = s
        recycleIfReplaced(prev.art, s.art)
    }

    fun clear() {
        update(MediaState.empty)
    }

    /**
     * Marks playback as paused because another app (e.g., Maps voice prompts
     * in the embedded VirtualDisplay) has taken audio focus. The media tile
     * reflects [playing] = false while ducked.
     */
    fun onAudioDucked() {
        val current = _state.value
        if (current.playing) {
            _state.value = current.copy(playing = false)
        }
    }

    /**
     * Launches the position-tick coroutine on [scope]. Cancels any previously
     * running ticker first, so calling this from `ServiceLocator.startup()` is
     * idempotent.
     *
     * The ticker only emits while the current state has `playing = true`. When
     * paused it short-circuits and waits for the next interval — so a paused
     * state is never overwritten with a "tick-advanced" position (AC2 freeze).
     */
    fun startPositionTicker(scope: CoroutineScope) {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                delay(tickIntervalMs)
                val current = _state.value
                if (!current.playing) continue
                val now = clock()
                val advanced = current.copy(
                    positionMs = current.livePosition(now),
                    positionAtMs = now,
                )
                // CAS so a concurrent update() (e.g. a new track from the
                // listener thread) is never clobbered by stale extrapolation.
                _state.compareAndSet(current, advanced)
            }
        }
    }

    fun stopPositionTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    fun play() = controller()?.transportControls?.play()
    fun pause() = controller()?.transportControls?.pause()
    fun next() = controller()?.transportControls?.skipToNext()
    fun prev() = controller()?.transportControls?.skipToPrevious()
    fun seekTo(positionMs: Long) = controller()?.transportControls?.seekTo(positionMs)

    private fun controller() = MediaNotificationListenerHolder.instance?.controller()

    /**
     * Recycle the previous album-art bitmap when it is replaced by a different
     * instance (or by null). Skips when the same Bitmap reference is reused
     * across updates — recycling a bitmap still in Glide's bitmap pool would
     * corrupt it for every other consumer.
     */
    private fun recycleIfReplaced(prev: android.graphics.Bitmap?, next: android.graphics.Bitmap?) {
        if (prev == null) return
        if (prev === next) return
        if (prev.isRecycled) return
        runCatching { prev.recycle() }
    }

    companion object {
        const val TICK_INTERVAL_MS = 250L
    }
}
