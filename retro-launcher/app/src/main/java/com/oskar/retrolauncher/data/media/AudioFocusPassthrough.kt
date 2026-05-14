package com.oskar.retrolauncher.data.media

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import timber.log.Timber

/**
 * Requests transient audio focus that others may duck, purely to observe
 * focus-loss events triggered by embedded apps (e.g., Maps voice prompts).
 *
 * When the embedded VirtualDisplay app takes audio focus the system broadcasts
 * [AudioManager.AUDIOFOCUS_LOSS_TRANSIENT] or
 * [AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK] to this listener, which
 * propagates the duck state to [MediaRepository] so the media tile reflects
 * `isPlaying = false` during the interruption.
 *
 * Lifecycle: call [start] when embedding becomes active and [stop] when it
 * is paused/destroyed.
 */
class AudioFocusPassthrough(
    private val audioManager: AudioManager,
    private val media: MediaRepository,
) {

    private var focusRequest: AudioFocusRequest? = null
    private var deprecatedListener: AudioManager.OnAudioFocusChangeListener? = null

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

    fun start() {
        if (focusRequest != null || deprecatedListener != null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener(focusListener)
                .build()
            audioManager.requestAudioFocus(request)
            focusRequest = request
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager.requestAudioFocus(
                focusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
            )
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                deprecatedListener = focusListener
            }
        }
    }

    fun stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            focusRequest = null
        } else {
            deprecatedListener?.let { audioManager.abandonAudioFocus(it) }
            deprecatedListener = null
        }
    }
}
