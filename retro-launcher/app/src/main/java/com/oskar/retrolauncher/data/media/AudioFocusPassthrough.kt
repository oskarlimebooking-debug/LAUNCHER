package com.oskar.retrolauncher.data.media

import android.media.AudioManager
import timber.log.Timber

/**
 * Requests transient audio focus that others may duck, purely to observe
 * focus-loss events triggered by embedded apps (e.g., Maps voice prompts).
 *
 * Uses the deprecated [AudioManager.requestAudioFocus] API (available since
 * API 8) instead of [android.media.AudioFocusRequest] (API 26+) to avoid
 * class-verification failures on Android 6 (API 23) head units.
 *
 * When the embedded VirtualDisplay app takes audio focus the system broadcasts
 * [AudioManager.AUDIOFOCUS_LOSS_TRANSIENT] or
 * [AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK] to the listener, which
 * propagates the duck state to [MediaRepository].
 *
 * Lifecycle: call [start] when embedding becomes active and [stop] when it
 * is paused/destroyed.
 */
class AudioFocusPassthrough(
    private val audioManager: AudioManager,
    private val media: MediaRepository,
) {

    private val focusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,
            -> {
                Timber.d("AudioFocusPassthrough: duck — focusChange=$focusChange")
                media.onAudioDucked()
            }
        }
    }

    private var started = false

    @Suppress("DEPRECATION")
    fun start() {
        if (started) return
        val result = audioManager.requestAudioFocus(
            focusListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
        )
        started = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    @Suppress("DEPRECATION")
    fun stop() {
        if (!started) return
        audioManager.abandonAudioFocus(focusListener)
        started = false
    }
}
