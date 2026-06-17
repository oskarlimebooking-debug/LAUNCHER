package com.oskar.retrolauncher.service

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession

/**
 * The launcher's own music engine: a Media3 [MediaLibraryService] wrapping an
 * [ExoPlayer] and publishing a [MediaLibrarySession]. Because the session is a
 * normal platform `MediaSession`, the launcher's own `MediaNotificationListener`
 * discovers it like any other app's — so the existing media card and
 * steering-wheel keys drive local playback with no extra wiring.
 *
 * Audio focus + becoming-noisy (unplugged headphones) are handled by ExoPlayer.
 * The foreground media notification is provided by Media3's default provider.
 */
class MusicService : MediaLibraryService() {

    private var player: ExoPlayer? = null
    private var librarySession: MediaLibrarySession? = null

    override fun onCreate() {
        super.onCreate()
        val exo = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        player = exo
        librarySession = MediaLibrarySession.Builder(this, exo, MusicLibrarySessionCallback())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        librarySession

    /** If the task is swiped away while idle, let the service stop. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        librarySession?.release()
        player?.release()
        librarySession = null
        player = null
        super.onDestroy()
    }
}
